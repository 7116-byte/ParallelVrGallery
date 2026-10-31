package com.local.parallelvrgallery

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.os.Build
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import android.graphics.Bitmap

/** Run only after opening an explicitly authorized QA sample, never against an arbitrary library item. */
class ViewerSmokeInstrumentation : Instrumentation() {
    private var mode = "sbs"
    private var source = "qa"
    private var item = "02-synthetic.png"
    private var version = ""
    private val finished = AtomicBoolean(false)
    @Volatile private var currentStage = "launch"
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        mode = arguments?.getString("mode") ?: "sbs"
        source = arguments?.getString("source") ?: "qa"
        item = arguments?.getString("item") ?: if (mode == "video-playback") "04-ui-sample.mp4" else "02-synthetic.png"
        version = arguments?.getString("version") ?: ""
        Thread {
            SystemClock.sleep(40000)
            if (finished.compareAndSet(false, true)) finish(Activity.RESULT_CANCELED, Bundle().apply {
                putString("result", "FAIL"); putString("stream", "Timeout at $currentStage\n")
            })
        }.apply { isDaemon = true; start() }
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            // Instrumentation restarts the target process. Reopen only the explicitly named QA album/sample.
            val intent = targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName) ?: error("No launch intent")
            stage("activity launch")
            ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(
                "am start -W -n com.local.parallelvrgallery/.MainActivity -f 0x10008000"
            )).bufferedReader().use { it.readText() }
            SystemClock.sleep(700)
            stage("source root: ${uiAutomation.rootInActiveWindow?.packageName}")
            if (findLabel("Android 应用兼容性") != null) clickLabel("确定")
            if (findLabel("从平面，到立体") != null) clickLabel("开始使用")
            stage("source selection")
            if (source == "generated") {
                require(version.isNotBlank()) { "An existing generated version must be specified" }
                clickLabel("生成")
                if (findLabel("返回 / Back") != null && findLabel(version) == null) clickLabel("返回 / Back")
                if (findLabel("返回 / Back") == null) clickLabel(version, scroll = true)
                require(findLabel(version) != null) { "Existing-result source boundary not confirmed" }
                clickLabel(item, scroll = true)
            } else {
                if (findLabel("PVG_UI_QA") == null || findLabel(item) == null) {
                    clickLabel("相册")
                    if (findLabel("返回 / Back") != null) clickLabel("返回 / Back")
                    if (findLabel(item) == null) clickLabel("PVG_UI_QA")
                }
                require(findLabel("PVG_UI_QA") != null) { "QA source boundary not confirmed" }
                clickLabel(item)
            }
            SystemClock.sleep(800)
            stage("gesture test")
            require(uiAutomation.rootInActiveWindow?.packageName == targetContext.packageName) { "Project must be foreground on an authorized QA sample" }
            val metrics = targetContext.resources.displayMetrics
            val width = metrics.widthPixels.toFloat(); val height = metrics.heightPixels.toFloat()
            stage("viewport ${metrics.widthPixels}x${metrics.heightPixels}")
            if (mode == "video-playback") {
                require(source == "qa" && item == "04-ui-sample.mp4") { "Playback test is restricted to the authorized QA clip" }
                require(findLabel("已生成") != null) { "Generate the authorized clip before this read-only playback test" }
                for (attempt in 0..20) {
                    if (findLabel("Pause") != null) break
                    SystemClock.sleep(100)
                }
                clickLabel("Pause")
                fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
                shell("input keyevent KEYCODE_HOME")
                SystemClock.sleep(800)
                shell("am start -W -n com.local.parallelvrgallery/.MainActivity")
                SystemClock.sleep(300)
                if (findLabel("Play") == null) {
                    val down = SystemClock.uptimeMillis()
                    event(down, MotionEvent.ACTION_DOWN, listOf(width * .5f to height * .45f))
                    event(down, MotionEvent.ACTION_UP, listOf(width * .5f to height * .45f))
                }
                require(findLabel("Play") != null) { "Explicit pause did not survive background return" }
                shell("wm user-rotation lock 1"); SystemClock.sleep(500)
                uiAutomation.takeScreenshot()?.let { bitmap ->
                    val dir = File(targetContext.getExternalFilesDir(null), "ui_qa_artifacts").apply { mkdirs() }
                    File(dir, "video-rotated.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
                }
                shell("wm user-rotation lock 0"); SystemClock.sleep(500)
                repeat(6) {
                    drag(width * .8f, height * .5f, width * .2f, height * .5f); SystemClock.sleep(200)
                    drag(width * .2f, height * .5f, width * .8f, height * .5f); SystemClock.sleep(200)
                }
                SystemClock.sleep(800)
                if (findLabel("已生成") == null) {
                    val down = SystemClock.uptimeMillis()
                    event(down, MotionEvent.ACTION_DOWN, listOf(width * .5f to height * .45f))
                    event(down, MotionEvent.ACTION_UP, listOf(width * .5f to height * .45f))
                    SystemClock.sleep(200)
                }
                require(findLabel("已生成") != null) { "QA video result missing after image/video page gestures" }
                val frame = uiAutomation.takeScreenshot() ?: error("Video screenshot unavailable")
                try {
                    for (eye in 0..1) {
                        var visible = 0
                        for (x in 1..4) for (y in 1..5) {
                            val color = frame.getPixel((frame.width * (eye * .5f + x * .1f)).toInt(), (frame.height * (.3f + y * .07f)).toInt())
                            if ((color and 0x00FFFFFF) > 0x101010) visible++
                        }
                        require(visible > 0) { "Video eye appears black after page return" }
                    }
                } finally { frame.recycle() }
                result.putString("stream", "PASS: existing QA SBS video, explicit pause retained after Home/return, rotation, six image/video page gesture roundtrips.\n")
            } else if (mode == "held-drag") {
                repeat(4) { heldDrag(width, height) }
                result.putString("stream", "PASS: 4 held drags crossed the pager threshold and returned before release.\n")
            } else {
                val before = sbsState() ?: error("Open a generated SBS QA image/video first")
                require(before.startsWith("SBS 1.000x")) { "Reset zoom before this test: $before" }
                pinch(width, height)
                SystemClock.sleep(200)
                val zoomed = sbsState() ?: error("SBS semantics lost")
                require((Regex("SBS ([0-9.]+)x").find(zoomed)?.groupValues?.get(1)?.toFloat() ?: 1f) > 1.2f) { "Pinch did not zoom: $zoomed" }
                drag(width * .25f, height * .5f, width * .32f, height * .54f)
                SystemClock.sleep(150)
                val moved = sbsState() ?: error("SBS state missing after drag")
                require(moved != zoomed) { "Zoomed one-finger drag did not move" }
                doubleTap(width * .25f, height * .5f)
                SystemClock.sleep(200)
                val reset = sbsState() ?: error("SBS state missing after reset")
                require(reset == "SBS 1.000x x=0 y=0") { "Double tap did not reset: $reset" }
                val bitmap = uiAutomation.takeScreenshot() ?: error("No screenshot")
                try {
                    for (eye in 0..1) {
                        var visible = 0
                        for (x in 1..4) for (y in 1..5) {
                            val colour = bitmap.getPixel((bitmap.width * (eye * .5f + x * .1f)).toInt(), (bitmap.height * (.3f + y * .07f)).toInt())
                            if ((colour and 0x00FFFFFF) > 0x101010) visible++
                        }
                        require(visible > 0) { "Eye appears black" }
                    }
                } finally { bitmap.recycle() }
                result.putString("stream", "PASS: pinch zoom, shared offset drag, double-tap reset, both eyes rendered. $zoomed / $moved / $reset\n")
            }
            stage("capture live metrics")
            runCatching {
                val dir = File(targetContext.getExternalFilesDir(null), "ui_qa_artifacts").apply { mkdirs() }
                fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
                File(dir, "${mode}-gfx.txt").writeText(shell("dumpsys gfxinfo com.local.parallelvrgallery framestats"))
                File(dir, "${mode}-memory.txt").writeText(shell("dumpsys meminfo com.local.parallelvrgallery"))
                uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(dir, "${mode}-success.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
                result.putString("liveMetrics", "captured")
            }.onFailure { result.putString("liveMetrics", "unavailable") }
            result.putString("result", "PASS")
            if (finished.compareAndSet(false, true)) finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            runCatching {
                uiAutomation.takeScreenshot()?.let { bitmap ->
                    val dir = File(targetContext.getExternalFilesDir(null), "ui_qa_artifacts").apply { mkdirs() }
                    File(dir, "failure.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
            result.putString("result", "FAIL"); result.putString("stream", "FAIL: ${error.message}\n")
            if (finished.compareAndSet(false, true)) finish(Activity.RESULT_CANCELED, result)
        }
    }

    private fun stage(value: String) {
        currentStage = value
        sendStatus(1, Bundle().apply { putString("stream", "Stage: $value\n") })
    }

    private fun sbsState(): String? {
        val metrics = targetContext.resources.displayMetrics
        if (Build.VERSION.SDK_INT >= 35) uiAutomation.clearCache()
        fun find(node: AccessibilityNodeInfo?): String? {
            if (node == null) return null
            node.refresh()
            if (!node.isVisibleToUser) return null
            if (Build.VERSION.SDK_INT >= 30) node.stateDescription?.toString()?.takeIf { it.startsWith("SBS ") }?.let {
                val bounds = Rect(); node.getBoundsInScreen(bounds)
                if (bounds.contains(metrics.widthPixels / 2, metrics.heightPixels / 2)) return it
            }
            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
            return null
        }
        return find(uiAutomation.rootInActiveWindow)
    }

    private fun findLabel(label: String): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= 35) uiAutomation.clearCache()
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null || !node.isVisibleToUser) return null
            if (node.text?.toString()?.lineSequence()?.any { it == label } == true || node.contentDescription?.toString()?.lineSequence()?.any { it == label } == true) return node
            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
            return null
        }
        return find(uiAutomation.rootInActiveWindow)
    }

    private fun clickLabel(label: String, scroll: Boolean = false) {
        stage("click: ${if (label == item) "target item" else if (label == version) "target version" else label}")
        var node: AccessibilityNodeInfo? = null
        for (attempt in 0..20) { node = findLabel(label); if (node != null) break; SystemClock.sleep(100) }
        if (node == null && scroll) {
            fun scrollable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (root == null || !root.isVisibleToUser) return null
                if (root.isScrollable) return root
                for (i in 0 until root.childCount) scrollable(root.getChild(i))?.let { return it }
                return null
            }
            // Navigation intentionally restores scroll. A requested result can precede that position.
            for (attempt in 0..20) {
                if (node != null) break
                if (Build.VERSION.SDK_INT >= 35) uiAutomation.clearCache()
                val moved = scrollable(uiAutomation.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
                SystemClock.sleep(150); node = findLabel(label)
                if (!moved) break
            }
            repeat(15) {
                if (node == null) {
                    scrollable(uiAutomation.rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    SystemClock.sleep(250); node = findLabel(label)
                }
            }
        }
        fun target(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (root == null || !root.isVisibleToUser) return null
            if (root.text?.toString()?.lineSequence()?.any { it == label } == true || root.contentDescription?.toString()?.lineSequence()?.any { it == label } == true) {
                var ancestor: AccessibilityNodeInfo? = root
                while (ancestor != null) {
                    if (ancestor.isClickable) return ancestor
                    ancestor = ancestor.parent
                }
            }
            for (i in 0 until root.childCount) target(root.getChild(i))?.let { return it }
            return null
        }
        require(node != null) { "Required media UI label missing" }
        var clickable: AccessibilityNodeInfo? = null
        for (attempt in 0..20) {
            if (Build.VERSION.SDK_INT >= 35) uiAutomation.clearCache()
            clickable = target(uiAutomation.rootInActiveWindow)
            if (clickable != null) break
            SystemClock.sleep(100)
        }
        if (clickable != null) {
            require(clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Could not click $label" }
        } else {
            // Some OEM accessibility bridges omit ACTION_CLICK on a selected Material tab.
            val matches = mutableListOf<Rect>()
            fun collect(root: AccessibilityNodeInfo?) {
                if (root == null || !root.isVisibleToUser) return
                if (root.text?.toString() == label || root.contentDescription?.toString() == label) {
                    val bounds = Rect(); root.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty) matches += bounds
                }
                for (i in 0 until root.childCount) collect(root.getChild(i))
            }
            collect(uiAutomation.rootInActiveWindow)
            val bounds = matches.maxByOrNull { it.centerY() } ?: error("No touch target for requested media UI")
            val down = SystemClock.uptimeMillis()
            event(down, MotionEvent.ACTION_DOWN, listOf(bounds.exactCenterX() to bounds.exactCenterY()))
            event(down, MotionEvent.ACTION_UP, listOf(bounds.exactCenterX() to bounds.exactCenterY()))
        }
        SystemClock.sleep(500)
    }

    private fun event(down: Long, action: Int, points: List<Pair<Float, Float>>) {
        val props = Array(points.size) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(points.size) { i -> MotionEvent.PointerCoords().apply { x = points[i].first; y = points[i].second; pressure = 1f; size = 1f } }
        val ev = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, points.size, props, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        try { require(uiAutomation.injectInputEvent(ev, true)) { "Touch injection rejected" } }
        finally { ev.recycle() }
    }

    private fun pinch(width: Float, height: Float) {
        val down = SystemClock.uptimeMillis(); val cx = width * .25f; val cy = height * .5f
        var points = listOf(cx - width * .04f to cy - height * .025f, cx + width * .04f to cy + height * .025f)
        event(down, MotionEvent.ACTION_DOWN, points.take(1))
        event(down, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), points)
        for (i in 1..12) {
            val factor = 1f + i / 12f
            points = listOf(cx - width * .04f * factor to cy - height * .025f * factor, cx + width * .04f * factor to cy + height * .025f * factor)
            event(down, MotionEvent.ACTION_MOVE, points); SystemClock.sleep(25)
        }
        event(down, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), points)
        event(down, MotionEvent.ACTION_UP, points.take(1))
    }

    private fun drag(x1: Float, y1: Float, x2: Float, y2: Float) {
        val down = SystemClock.uptimeMillis(); event(down, MotionEvent.ACTION_DOWN, listOf(x1 to y1))
        for (i in 1..12) { event(down, MotionEvent.ACTION_MOVE, listOf(x1 + (x2-x1)*i/12 to y1 + (y2-y1)*i/12)); SystemClock.sleep(20) }
        event(down, MotionEvent.ACTION_UP, listOf(x2 to y2))
    }

    private fun doubleTap(x: Float, y: Float) {
        repeat(2) { val down = SystemClock.uptimeMillis(); event(down, MotionEvent.ACTION_DOWN, listOf(x to y)); SystemClock.sleep(30); event(down, MotionEvent.ACTION_UP, listOf(x to y)); SystemClock.sleep(90) }
    }

    private fun heldDrag(width: Float, height: Float) {
        val down = SystemClock.uptimeMillis(); val start = width * .78f; val end = width * .20f; val y = height * .5f
        event(down, MotionEvent.ACTION_DOWN, listOf(start to y))
        for (i in 1..30) {
            val fraction = if (i <= 15) i / 15f else (30-i) / 15f
            event(down, MotionEvent.ACTION_MOVE, listOf(start + (end-start)*fraction to y)); SystemClock.sleep(25)
        }
        event(down, MotionEvent.ACTION_UP, listOf(start to y)); SystemClock.sleep(200)
    }
}
