package com.local.parallelvrgallery

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

internal val StudioDarkColors = darkColorScheme(
    primary = Color(0xFFC9B8FF), onPrimary = Color(0xFF302054),
    primaryContainer = Color(0xFF453366), onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFA5D9CD), onSecondary = Color(0xFF123830),
    secondaryContainer = Color(0xFF244E43), onSecondaryContainer = Color(0xFFC2F0E3),
    background = Color(0xFF141318), onBackground = Color(0xFFE9E3EF),
    surface = Color(0xFF1C1B22), onSurface = Color(0xFFE9E3EF),
    surfaceVariant = Color(0xFF2B2933), onSurfaceVariant = Color(0xFFC4BECB),
    surfaceContainer = Color(0xFF24222B), surfaceContainerHigh = Color(0xFF302D38),
    outline = Color(0xFF827A8E), outlineVariant = Color(0xFF45404F),
)

private val StudioLightColors = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDDFF), onPrimaryContainer = Color(0xFF302054),
    secondary = Color(0xFF30665A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEDE2), onSecondaryContainer = Color(0xFF12382D),
    background = Color(0xFFF8F7FB), onBackground = Color(0xFF24212B),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF24212B),
    surfaceVariant = Color(0xFFEDE8F2), onSurfaceVariant = Color(0xFF615B6B),
    surfaceContainer = Color(0xFFF0ECF5), surfaceContainerHigh = Color(0xFFE9E3EF),
    outline = Color(0xFF7B7386), outlineVariant = Color(0xFFD7D0E0),
)

@Composable
internal fun StudioTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) StudioDarkColors else StudioLightColors,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}

@Composable
internal fun studioColumns(requested: Int, cards: Boolean = false): Int {
    val config = LocalConfiguration.current
    val scale = LocalDensity.current.fontScale
    return boundedColumns(requested, config.screenWidthDp, scale, cards)
}

@Composable
internal fun StudioIcon(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = label) }
}

@Composable
internal fun StudioTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) StudioIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回 / Back", onBack)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
internal fun StudioEmptyState(title: String, detail: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.padding(18.dp).size(28.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
internal fun StudioSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
internal fun StudioToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun foregroundPlayback(requested: Boolean): Boolean {
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return requested && resumed
}

/** Draw one half of a shared bitmap. No per-eye Bitmap.createBitmap allocation during a drag. */
@Composable
internal fun SbsEye(bitmap: ImageBitmap, right: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val half = bitmap.width / 2
        val eyeWidth = if (right) bitmap.width - half else half
        val fit = minOf(size.width / eyeWidth.coerceAtLeast(1), size.height / bitmap.height)
        val target = IntSize((eyeWidth * fit).roundToInt().coerceAtLeast(1), (bitmap.height * fit).roundToInt().coerceAtLeast(1))
        drawImage(bitmap, srcOffset = IntOffset(if (right) half else 0, 0), srcSize = IntSize(eyeWidth, bitmap.height),
            dstOffset = IntOffset(((size.width - target.width) / 2).roundToInt(), ((size.height - target.height) / 2).roundToInt()), dstSize = target)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioOptions(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (key, label) ->
            FilterChip(selected = key == selected, onClick = { onSelect(key) }, label = { Text(label) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
internal fun StudioGuide(lang: AppLanguage, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(lang.t("从平面，到立体", "From gallery to stereo")) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(lang.t("1 · 选择来源\n全部与相册各自浏览。生成页按参数版本保存已有结果。", "1 · Choose a source\nAll and each album keep their own browsing scope. Generated results are grouped by version."))
                Text(lang.t("2 · 打开图片\n普通图片默认开启 VR，并按设置预加载同一来源的前后图片。视频需要手动加入独立队列。", "2 · Open an image\nImage VR starts automatically, with neighbours from the same source. Video generation requires a separate explicit request."))
                Text(lang.t("3 · 双眼一起移动\n双指缩放，放大后单指拖动；双击复位。1× 时左右滑动切页。", "3 · Move both eyes together\nPinch to zoom, drag when enlarged, double tap to reset. At 1× swipe to change pages."))
                Text(lang.t("你的媒体留在设备上。首次生成会下载模型；开启强制 GPU 后不会自动回退 CPU。保存、替换与删除前请核对操作范围。", "Media stays on your device. The first generation downloads a model. Forced GPU never silently falls back to CPU. Review the scope before saving, replacing or deleting."))
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(lang.t("开始使用", "Get started")) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudioTasks(state: UiState, pauseReason: (String) -> String, onDismiss: () -> Unit) {
    val lang = state.settings.language
    val restoredImages = state.photos.filter { photo -> photo.kind == MediaKind.IMAGE &&
        state.states[photo.cacheKey] in setOf(VrState.QUEUED, VrState.PAUSED, VrState.FAILED) && state.jobs.none { it.photoItem.cacheKey == photo.cacheKey } }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lang.t("任务与状态", "Tasks & status"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            state.modelProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
            Text(lang.pickMixed(state.modelStatus), style = MaterialTheme.typography.bodySmall)
            state.updateStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(lang.t("图片与视频独立排队；此面板只查看状态，不启动或恢复任务。", "Image and video queues are separate. This panel only displays status; it never starts or resumes tasks."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.jobs.isEmpty() && state.videoJobs.isEmpty() && restoredImages.isEmpty()) {
                StudioEmptyState(lang.t("暂时没有任务", "No tasks yet"), lang.t("从同一相册打开图片开始；生成结果在「生成」中查看。", "Open an image from an album to start. Find existing results in Generated."))
            }
            Text(lang.t("图片 · ${state.jobs.size + restoredImages.size}", "Images · ${state.jobs.size + restoredImages.size}"), fontWeight = FontWeight.SemiBold)
            restoredImages.forEach { photo ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(photo.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${state.states[photo.cacheKey]?.label(lang)} · ${lang.t("持久化队列标签", "restored queue tag")}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            state.jobs.forEach { job ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(job.photoItem.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${job.state.label(lang)} · ${job.source ?: "—"}", style = MaterialTheme.typography.labelMedium)
                        if (job.state == VrState.PAUSED) Text(when (pauseReason(job.photoItem.cacheKey)) {
                            "source" -> lang.t("暂停原因：来源切换", "Paused: source changed")
                            "window" -> lang.t("暂停原因：预加载窗口切换", "Paused: prefetch window changed")
                            "manual_or_restored" -> lang.t("暂停原因：手动或上次恢复", "Paused: manual or restored")
                            else -> lang.t("暂停原因：请查看诊断记录", "Paused: see diagnostics")
                        }, style = MaterialTheme.typography.bodySmall)
                        if (job.state == VrState.GENERATING) LinearProgressIndicator(progress = { job.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        job.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            Text(lang.t("视频 · ${state.videoJobs.size}", "Videos · ${state.videoJobs.size}"), fontWeight = FontWeight.SemiBold)
            state.videoJobs.forEach { job ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(job.item.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${job.state.label(lang)} · ${(job.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        if (job.state == VideoVrState.GENERATING) LinearProgressIndicator(progress = { job.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        job.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            state.message?.let { Text(lang.pickMixed(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
