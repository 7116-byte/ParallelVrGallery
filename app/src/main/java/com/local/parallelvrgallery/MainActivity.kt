package com.local.parallelvrgallery

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.PriorityQueue
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    GalleryApp()
                }
            }
        }
    }
}

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val modifiedTime: Long,
) {
    val cacheKey: String
        get() = "${id}_${size}_${modifiedTime}"
}

enum class VrState {
    NORMAL,
    QUEUED,
    GENERATING,
    READY,
    FAILED,
}

data class VrCacheEntry(
    val photoKey: String,
    val version: String,
    val outputPath: String,
    val depthPath: String,
    val paramsPath: String,
    val logPath: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
)

data class VrGenerationParams(
    val depthModel: String = "depth_anything_v2.tflite",
    val outputMode: String = "SBS_PARALLEL",
    val depthScale: Float = 40f,
    val blurRadius: Int = 3,
    val fillRadius: Int = 10,
    val invertDepth: Boolean = false,
    val maxLongEdge: Int = 6000,
    val inpaintMode: String = "FOREGROUND_FILL",
    val quality: Int = 94,
)

enum class AppLanguage {
    ZH,
    EN,
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ZH,
    val modelId: String = "depth_anything_v2_small_tflite",
    val autoPrefetch: Boolean = true,
    val prefetchWindow: Int = 3,
    val depthScale: Float = 40f,
    val blurRadius: Int = 3,
    val fillRadius: Int = 10,
    val invertDepth: Boolean = false,
    val maxLongEdge: Int = 6000,
    val depthResolution: Int = 518,
) {
    fun toParams(): VrGenerationParams = VrGenerationParams(
        depthModel = modelId,
        depthScale = depthScale,
        blurRadius = blurRadius,
        fillRadius = fillRadius,
        invertDepth = invertDepth,
        maxLongEdge = maxLongEdge,
    )
}

data class ModelSpec(
    val id: String,
    val displayName: String,
    val inputSize: Int,
    val fileName: String,
    val url: String,
    val sha256: String,
)

private val AvailableModels = listOf(
    ModelSpec(
        id = "depth_anything_v2_small_tflite",
        displayName = "Depth Anything V2 - Small",
        inputSize = 518,
        fileName = "depth_anything_v2.tflite",
        url = "https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite",
        sha256 = "B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F",
    ),
)

private fun modelSpec(id: String): ModelSpec = AvailableModels.firstOrNull { it.id == id } ?: AvailableModels.first()

data class CacheVersionSummary(
    val version: String,
    val kind: String,
    val count: Int,
    val bytes: Long,
)

data class VrJob(
    val photoItem: PhotoItem,
    val priority: Int,
    val state: VrState,
    val progress: Float,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)

data class UiState(
    val hasPermission: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val selectedIndex: Int? = null,
    val galleryAnchorIndex: Int = 0,
    val settingsOpen: Boolean = false,
    val manageOpen: Boolean = false,
    val vrMode: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val activePrefetchWindow: Int = 3,
    val states: Map<String, VrState> = emptyMap(),
    val entries: Map<String, VrCacheEntry> = emptyMap(),
    val jobs: List<VrJob> = emptyList(),
    val logs: List<String> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val debugIndex: Int? = null,
    val modelProgress: Float? = null,
    val modelStatus: String = "模型未下载 / Model not downloaded",
    val cacheVersions: List<CacheVersionSummary> = emptyList(),
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PhotoRepository(app)
    private val cache = VrCacheManager(app)
    private val modelManager = ModelManager(app)
    private val generator = VrGenerator(app, cache, modelManager)
    private val settingsStore = SettingsStore(app)
    private val pending = PriorityQueue<QueuedJob>(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
    private var sequence = 0L
    private var worker: Job? = null
    private var activeKey: String? = null

    private val _uiState = MutableStateFlow(UiState(hasPermission = hasImagePermission(app), settings = settingsStore.load()))
    val uiState: StateFlow<UiState> = _uiState

    init {
        if (_uiState.value.hasPermission) {
            loadPhotos()
        }
    }

    fun onPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            val photos = withContext(Dispatchers.IO) { repository.loadImages() }
            val entries = withContext(Dispatchers.IO) { photos.mapNotNull { cache.findEntry(it) }.associateBy { it.photoKey } }
            _uiState.update {
                it.copy(
                    photos = photos,
                    entries = entries,
                    states = photos.associate { photo -> photo.cacheKey to if (entries.containsKey(photo.cacheKey)) VrState.READY else VrState.NORMAL },
                    loading = false,
                    message = "已加载 ${photos.size} 张图片 / ${photos.size} images loaded",
                    cacheVersions = cache.summaries(),
                    modelStatus = modelManager.statusText(it.settings.modelId),
                )
            }
        }
    }

    fun openPhoto(index: Int) {
        _uiState.update { it.copy(selectedIndex = index, galleryAnchorIndex = index, vrMode = false) }
    }

    fun closeViewer() {
        stopVr()
        _uiState.update { it.copy(selectedIndex = null, debugIndex = null, galleryAnchorIndex = it.selectedIndex ?: it.galleryAnchorIndex) }
    }

    fun openSettings() {
        _uiState.update { it.copy(settingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(settingsOpen = false) }
    }

    fun updateSettings(settings: AppSettings) {
        settingsStore.save(settings)
        _uiState.update {
            it.copy(
                settings = settings,
                activePrefetchWindow = if (settings.autoPrefetch) 3 else settings.prefetchWindow,
                modelStatus = modelManager.statusText(settings.modelId),
            )
        }
        _uiState.value.selectedIndex?.let { refreshWindow(it) }
    }

    fun onPagerIndexChanged(index: Int) {
        _uiState.update { it.copy(selectedIndex = index, galleryAnchorIndex = index, activePrefetchWindow = if (it.settings.autoPrefetch) 3 else it.settings.prefetchWindow) }
        if (_uiState.value.vrMode) {
            enqueueWindow(index, includeCurrent = true)
        }
    }

    fun requestVr(index: Int) {
        if (_uiState.value.vrMode) {
            stopVr()
        } else {
            _uiState.update { it.copy(vrMode = true, selectedIndex = index, message = null, activePrefetchWindow = if (it.settings.autoPrefetch) 3 else it.settings.prefetchWindow) }
            enqueueWindow(index, includeCurrent = true)
        }
    }

    fun stopVr() {
        synchronized(pending) { pending.clear() }
        _uiState.update { it.copy(vrMode = false, modelProgress = null) }
        addLog("vr stopped; generated caches kept")
    }

    fun retry(index: Int) {
        enqueuePhoto(index, priority = 0, force = true)
        startWorker()
    }

    fun exportDebug(context: Context, index: Int) {
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val entry = cache.findEntry(photo) ?: return@withContext null
                DebugExporter(context, cache).export(photo, entry)
            }
            if (result == null) {
                _uiState.update { it.copy(message = "当前图片还没有可导出的 VR 缓存 / No READY cache yet") }
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "导出调试包 / Export debug package"))
                _uiState.update { it.copy(message = "调试包已创建 / Debug package created: ${result.name}") }
            }
        }
    }

    fun openDebug(index: Int) {
        _uiState.update { it.copy(debugIndex = index) }
    }

    fun closeDebug() {
        _uiState.update { it.copy(debugIndex = null) }
    }

    fun openManage() {
        _uiState.update { it.copy(manageOpen = true, cacheVersions = cache.summaries()) }
    }

    fun closeManage() {
        _uiState.update { it.copy(manageOpen = false) }
    }

    fun deleteCacheVersion(version: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cache.deleteVersion(version)
            val photos = _uiState.value.photos
            val entries = photos.mapNotNull { cache.findEntry(it) }.associateBy { it.photoKey }
            _uiState.update {
                it.copy(
                    entries = entries,
                    states = photos.associate { photo -> photo.cacheKey to if (entries.containsKey(photo.cacheKey)) VrState.READY else VrState.NORMAL },
                    cacheVersions = cache.summaries(),
                    message = "已删除版本 / Deleted version: $version",
                )
            }
        }
    }

    fun saveGeneratedCopy(context: Context, index: Int) {
        val lang = _uiState.value.settings.language
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                val entry = cache.findEntry(photo) ?: return@withContext lang.t("当前图片还没有已生成的 VR 图", "No generated VR image for this photo")
                runCatching {
                    saveImageToGallery(context, File(entry.outputPath), "VR_${photo.displayName}")
                    lang.t("已保存到系统图库", "Saved to system gallery")
                }.getOrElse { error ->
                    lang.t("保存失败：${error.message}", "Save failed: ${error.message}")
                }
            }
            _uiState.update { it.copy(message = message) }
            loadPhotos()
        }
    }

    fun replaceOriginalWithGenerated(context: Context, index: Int) {
        val lang = _uiState.value.settings.language
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                val entry = cache.findEntry(photo) ?: return@withContext lang.t("当前图片还没有已生成的 VR 图", "No generated VR image for this photo")
                runCatching {
                    replaceOriginalImage(context, photo, File(entry.outputPath))
                    lang.t("已尝试替换原图并保留原时间", "Replaced original and kept its timestamp")
                }.getOrElse { error ->
                    lang.t("替换失败：系统可能不允许写入这张原图。${error.message}", "Replace failed: Android may not allow writing this original. ${error.message}")
                }
            }
            _uiState.update { it.copy(message = message) }
            loadPhotos()
        }
    }

    private fun refreshWindow(index: Int) {
        enqueueWindow(index, includeCurrent = false)
    }

    private fun enqueueWindow(index: Int, includeCurrent: Boolean) {
        val photos = _uiState.value.photos
        if (!_uiState.value.vrMode) return
        if (photos.isEmpty()) return
        val targets = mutableListOf<Pair<Int, Int>>()
        if (includeCurrent) targets += index to 0
        for (distance in 1.._uiState.value.activePrefetchWindow) {
            val next = index + distance
            val prev = index - distance
            if (next in photos.indices) targets += next to distance * 2 - 1
            if (prev in photos.indices) targets += prev to distance * 2
        }

        synchronized(pending) {
            val keep = targets.map { photos[it.first].cacheKey }.toSet()
            pending.removeAll { it.photo.cacheKey !in keep }
        }
        targets.forEach { (targetIndex, priority) -> enqueuePhoto(targetIndex, priority, force = false) }
        startWorker()
    }

    private fun enqueuePhoto(index: Int, priority: Int, force: Boolean) {
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        val currentVersion = _uiState.value.settings.toParams().cacheVersion()
        if (!force && cache.findEntry(photo, currentVersion) != null) {
            markState(photo.cacheKey, VrState.READY)
            return
        }
        if (!force && _uiState.value.states[photo.cacheKey] in listOf(VrState.QUEUED, VrState.GENERATING)) return
        synchronized(pending) {
            pending.add(QueuedJob(photo, priority, sequence++))
        }
        markState(photo.cacheKey, VrState.QUEUED)
        addLog("queued ${photo.displayName} p=$priority")
    }

    private fun startWorker() {
        if (worker?.isActive == true) return
        worker = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (!_uiState.value.vrMode) {
                    synchronized(pending) { pending.clear() }
                    break
                }
                val next = synchronized(pending) { pending.poll() }
                if (next == null) {
                    if (expandAutoPrefetchIfNeeded()) {
                        delay(50)
                        continue
                    }
                    break
                }
                activeKey = next.photo.cacheKey
                markState(next.photo.cacheKey, VrState.GENERATING)
                upsertJob(next.photo, next.priority, VrState.GENERATING, 0.1f)
                val result = runCatching {
                    generator.generate(
                        next.photo,
                        _uiState.value.settings.toParams(),
                        onModelProgress = { progress ->
                            _uiState.update {
                                it.copy(
                                    modelProgress = progress,
                                    modelStatus = if (progress < 1f) {
                                        "正在下载模型 / Downloading model ${(progress * 100f).roundToInt()}%"
                                    } else {
                                        "模型已就绪 / Model ready"
                                    },
                                )
                            }
                        },
                    ) { progress ->
                        upsertJob(next.photo, next.priority, VrState.GENERATING, progress)
                    }
                }
                result.onSuccess { entry ->
                    markReady(next.photo.cacheKey, entry)
                    upsertJob(next.photo, next.priority, VrState.READY, 1f, finishedAt = System.currentTimeMillis())
                    addLog("ready ${next.photo.displayName} ${entry.width}x${entry.height}")
                    _uiState.update { it.copy(modelProgress = null, modelStatus = modelManager.statusText(it.settings.modelId), cacheVersions = cache.summaries()) }
                }.onFailure { error ->
                    markState(next.photo.cacheKey, VrState.FAILED)
                    upsertJob(next.photo, next.priority, VrState.FAILED, 1f, finishedAt = System.currentTimeMillis(), error = error.message)
                    addLog("failed ${next.photo.displayName}: ${error.message}")
                }
                activeKey = null
                delay(50)
            }
        }
    }

    private fun expandAutoPrefetchIfNeeded(): Boolean {
        if (!_uiState.value.vrMode || !_uiState.value.settings.autoPrefetch) return false
        val selected = _uiState.value.selectedIndex ?: return false
        val next = when (_uiState.value.activePrefetchWindow) {
            3 -> 5
            5 -> 10
            else -> return false
        }
        _uiState.update { it.copy(activePrefetchWindow = next) }
        addLog("auto prefetch expanded to $next")
        enqueueWindow(selected, includeCurrent = false)
        return synchronized(pending) { pending.isNotEmpty() }
    }

    private fun markReady(key: String, entry: VrCacheEntry) {
        _uiState.update {
            it.copy(
                states = it.states + (key to VrState.READY),
                entries = it.entries + (key to entry),
            )
        }
    }

    private fun markState(key: String, state: VrState) {
        _uiState.update { it.copy(states = it.states + (key to state)) }
    }

    private fun upsertJob(
        photo: PhotoItem,
        priority: Int,
        state: VrState,
        progress: Float,
        finishedAt: Long? = null,
        error: String? = null,
    ) {
        _uiState.update { current ->
            val existing = current.jobs.filterNot { it.photoItem.cacheKey == photo.cacheKey }
            val job = VrJob(
                photoItem = photo,
                priority = priority,
                state = state,
                progress = progress,
                startedAt = System.currentTimeMillis(),
                finishedAt = finishedAt,
                error = error,
            )
            current.copy(jobs = (listOf(job) + existing).take(200))
        }
    }

    private fun addLog(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _uiState.update { it.copy(logs = (listOf("$stamp $line") + it.logs).take(200)) }
    }
}

private data class QueuedJob(val photo: PhotoItem, val priority: Int, val sequence: Long)

private fun AppLanguage.t(zh: String, en: String): String = if (this == AppLanguage.ZH) zh else en

private fun AppLanguage.pickMixed(text: String): String {
    val marker = " / "
    if (!text.contains(marker)) return text
    val parts = text.split(marker, limit = 2)
    return if (this == AppLanguage.ZH) parts.first() else parts.getOrElse(1) { parts.first() }
}

private fun VrState.label(lang: AppLanguage): String = when (this) {
    VrState.NORMAL -> lang.t("原图", "Normal")
    VrState.QUEUED -> lang.t("排队", "Queued")
    VrState.GENERATING -> lang.t("生成中", "Generating")
    VrState.READY -> lang.t("已生成", "Ready")
    VrState.FAILED -> lang.t("失败", "Failed")
}

private class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val migratedInvertDefault = prefs.getBoolean("migratedInvertDefaultOffV5", false)
        val invertDepth = if (migratedInvertDefault) {
            prefs.getBoolean("invertDepth", false)
        } else {
            prefs.edit()
                .putBoolean("invertDepth", false)
                .putBoolean("migratedInvertDefaultOffV5", true)
                .apply()
            false
        }
        return AppSettings(
            language = runCatching { AppLanguage.valueOf(prefs.getString("language", AppLanguage.ZH.name) ?: AppLanguage.ZH.name) }.getOrDefault(AppLanguage.ZH),
            modelId = prefs.getString("modelId", "depth_anything_v2_small_tflite") ?: "depth_anything_v2_small_tflite",
            autoPrefetch = prefs.getBoolean("autoPrefetch", true),
            prefetchWindow = prefs.getInt("prefetchWindow", 3),
            depthScale = prefs.getFloat("depthScale", 40f),
            blurRadius = prefs.getInt("blurRadius", 3),
            fillRadius = prefs.getInt("fillRadius", 10),
            invertDepth = invertDepth,
            maxLongEdge = prefs.getInt("maxLongEdge", 6000),
            depthResolution = 518,
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("language", settings.language.name)
            .putString("modelId", settings.modelId)
            .putBoolean("autoPrefetch", settings.autoPrefetch)
            .putInt("prefetchWindow", settings.prefetchWindow)
            .putFloat("depthScale", settings.depthScale)
            .putInt("blurRadius", settings.blurRadius)
            .putInt("fillRadius", settings.fillRadius)
            .putBoolean("invertDepth", settings.invertDepth)
            .putBoolean("migratedInvertDefaultOffV5", true)
            .putInt("maxLongEdge", settings.maxLongEdge)
            .apply()
    }
}

private class PhotoRepository(private val context: Context) {
    fun loadImages(): List<PhotoItem> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
        )
        val sort = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val result = mutableListOf<PhotoItem>()
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result += PhotoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getString(nameCol) ?: "image-$id",
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    size = cursor.getLong(sizeCol),
                    modifiedTime = cursor.getLong(modifiedCol),
                )
            }
        }
        return result
    }
}

private class VrCacheManager(private val context: Context) {
    val root: File = File(context.getExternalFilesDir(null), "vr_cache").also { it.mkdirs() }

    fun entryDir(photo: PhotoItem, version: String): File {
        return if (version == DEFAULT_VERSION) File(root, photo.cacheKey) else File(File(root, photo.cacheKey), version)
    }

    fun findEntry(photo: PhotoItem, version: String? = null): VrCacheEntry? {
        if (version != null) {
            return readEntry(photo.cacheKey, version, entryDir(photo, version))
        }
        return latestEntry(photo.cacheKey)
    }

    fun latestEntry(photoKey: String): VrCacheEntry? {
        val base = File(root, photoKey)
        val candidates = mutableListOf<Pair<String, File>>()
        candidates += DEFAULT_VERSION to base
        base.listFiles()?.filter { it.isDirectory }?.forEach { candidates += it.name to it }
        return candidates.mapNotNull { (version, dir) -> readEntry(photoKey, version, dir) }.maxByOrNull { it.createdAt }
    }

    fun summaries(): List<CacheVersionSummary> {
        val map = linkedMapOf<String, Pair<Int, Long>>()
        root.listFiles()?.filter { it.isDirectory }?.forEach { photoDir ->
            readEntry(photoDir.name, DEFAULT_VERSION, photoDir)?.let { entry ->
                val current = map[DEFAULT_VERSION] ?: (0 to 0L)
                map[DEFAULT_VERSION] = current.first + 1 to current.second + File(entry.outputPath).length()
            }
            photoDir.listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
                readEntry(photoDir.name, versionDir.name, versionDir)?.let { entry ->
                    val current = map[versionDir.name] ?: (0 to 0L)
                    map[versionDir.name] = current.first + 1 to current.second + File(entry.outputPath).length()
                }
            }
        }
        return map.map { (version, value) ->
            CacheVersionSummary(version = version, kind = "图片 / Images", count = value.first, bytes = value.second)
        }.sortedBy { it.version }
    }

    fun deleteVersion(version: String) {
        root.listFiles()?.filter { it.isDirectory }?.forEach { photoDir ->
            if (version == DEFAULT_VERSION) {
                listOf("vr_sbs.jpg", "depth.png", "params.json", "job.log", "source_preview.jpg").forEach {
                    File(photoDir, it).delete()
                }
            } else {
                File(photoDir, version).deleteRecursively()
            }
        }
    }

    private fun readEntry(photoKey: String, version: String, dir: File): VrCacheEntry? {
        val vr = File(dir, "vr_sbs.jpg")
        val depth = File(dir, "depth.png")
        val params = File(dir, "params.json")
        val log = File(dir, "job.log")
        if (!vr.exists() || !depth.exists() || !params.exists() || !log.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(vr.absolutePath, bounds)
        return VrCacheEntry(
            photoKey = photoKey,
            version = version,
            outputPath = vr.absolutePath,
            depthPath = depth.absolutePath,
            paramsPath = params.absolutePath,
            logPath = log.absolutePath,
            width = bounds.outWidth,
            height = bounds.outHeight,
            createdAt = vr.lastModified(),
        )
    }

    companion object {
        const val DEFAULT_VERSION = "default"
    }
}

private class ModelManager(private val context: Context) {
    private val modelsDir = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    fun statusText(modelId: String = AvailableModels.first().id): String {
        val spec = modelSpec(modelId)
        val modelFile = File(modelsDir, spec.fileName)
        return if (modelFile.exists() && modelFile.sha256().equals(spec.sha256, ignoreCase = true)) {
            "模型已就绪 / Model ready"
        } else {
            "模型未下载 / Model not downloaded"
        }
    }

    fun ensureModel(modelId: String, onProgress: (Float) -> Unit): File {
        val spec = modelSpec(modelId)
        val modelFile = File(modelsDir, spec.fileName)
        if (modelFile.exists() && modelFile.sha256().equals(spec.sha256, ignoreCase = true)) {
            onProgress(1f)
            return modelFile
        }

        val tmp = File(modelsDir, "${spec.fileName}.download")
        if (tmp.exists()) tmp.delete()
        val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("Model download failed: HTTP ${connection.responseCode}")
        }
        val total = connection.contentLengthLong.coerceAtLeast(1L)
        var readTotal = 0L
        connection.inputStream.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    readTotal += read
                    onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 0.99f))
                }
            }
        }
        val hash = tmp.sha256()
        if (!hash.equals(spec.sha256, ignoreCase = true)) {
            tmp.delete()
            error("Model SHA-256 mismatch: $hash")
        }
        if (modelFile.exists()) modelFile.delete()
        tmp.renameTo(modelFile)
        onProgress(1f)
        return modelFile
    }
}

private class VrGenerator(
    private val context: Context,
    private val cache: VrCacheManager,
    private val modelManager: ModelManager,
) {
    fun generate(
        photo: PhotoItem,
        params: VrGenerationParams,
        onModelProgress: (Float) -> Unit,
        onProgress: (Float) -> Unit,
    ): VrCacheEntry {
        val version = params.cacheVersion()
        val dir = cache.entryDir(photo, version).also { it.mkdirs() }
        val log = StringBuilder()
        val start = System.currentTimeMillis()
        fun mark(message: String) {
            val elapsed = System.currentTimeMillis() - start
            log.append(elapsed).append("ms ").append(message).append('\n')
        }

        mark("start name=${photo.displayName} size=${photo.size} modified=${photo.modifiedTime}")
        val modelFile = modelManager.ensureModel(params.depthModel, onModelProgress)
        mark("model ready path=${modelFile.absolutePath} sha256=${modelFile.sha256()}")
        onProgress(0.12f)

        val original = decodeScaledBitmap(context, photo.uri, params.maxLongEdge)
        mark("decoded ${original.width}x${original.height}")
        if (max(photo.width, photo.height) > params.maxLongEdge) {
            mark("downsampled source because original long edge exceeded ${params.maxLongEdge}")
        }
        onProgress(0.25f)

        val depthStart = System.currentTimeMillis()
        val rawDepth = runDepthModel(original, modelFile)
        val depthSmall = smoothDepth(rawDepth, params.blurRadius, params.invertDepth)
        mark("depth model ${System.currentTimeMillis() - depthStart}ms")
        onProgress(0.55f)

        val depthBitmap = depthToBitmap(depthSmall)
        val depthPath = File(dir, "depth.png")
        FileOutputStream(depthPath).use { depthBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mark("depth saved ${depthBitmap.width}x${depthBitmap.height}")
        onProgress(0.7f)

        val sbsStart = System.currentTimeMillis()
        val vr = makeParallelSbs(original, depthSmall, params.depthScale, params.fillRadius)
        mark("sbs ${System.currentTimeMillis() - sbsStart}ms output=${vr.width}x${vr.height}")
        val vrPath = File(dir, "vr_sbs.jpg")
        FileOutputStream(vrPath).use { vr.compress(Bitmap.CompressFormat.JPEG, params.quality, it) }
        onProgress(0.9f)

        val paramsPath = File(dir, "params.json")
        paramsPath.writeText(params.toJson(photo, original, vr), Charsets.UTF_8)
        val logPath = File(dir, "job.log")
        mark("done total=${System.currentTimeMillis() - start}ms")
        logPath.writeText(log.toString(), Charsets.UTF_8)

        return VrCacheEntry(
            photoKey = photo.cacheKey,
            version = version,
            outputPath = vrPath.absolutePath,
            depthPath = depthPath.absolutePath,
            paramsPath = paramsPath.absolutePath,
            logPath = logPath.absolutePath,
            width = vr.width,
            height = vr.height,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun runDepthModel(bitmap: Bitmap, modelFile: File): FloatArray {
        val inputSize = 518
        val model = loadModelFile(modelFile)
        val interpreter = Interpreter(model, Interpreter.Options().setNumThreads(4))
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            input.putFloat(Color.red(pixel) / 255f)
            input.putFloat(Color.green(pixel) / 255f)
            input.putFloat(Color.blue(pixel) / 255f)
        }
        val output = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(1) } } }
        interpreter.run(input, output)
        interpreter.close()
        val flat = FloatArray(inputSize * inputSize)
        var minValue = Float.MAX_VALUE
        var maxValue = -Float.MAX_VALUE
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val value = output[0][y][x][0]
                flat[y * inputSize + x] = value
                minValue = min(minValue, value)
                maxValue = max(maxValue, value)
            }
        }
        val range = max(0.0001f, maxValue - minValue)
        for (i in flat.indices) {
            flat[i] = (flat[i] - minValue) / range
        }
        return flat
    }

    private fun depthToBitmap(depth: FloatArray): Bitmap {
        val size = 518
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (i in depth.indices) {
            val value = (depth[i].coerceIn(0f, 1f) * 255f).roundToInt()
            pixels[i] = Color.rgb(value, value, value)
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }

    private fun smoothDepth(depth: FloatArray, radius: Int, invert: Boolean): FloatArray {
        val size = 518
        if (radius <= 0) {
            return if (invert) FloatArray(depth.size) { 1f - depth[it] } else depth.copyOf()
        }
        val r = radius.coerceAtLeast(1) / 2
        val horizontal = FloatArray(depth.size)
        val output = FloatArray(depth.size)
        for (y in 0 until size) {
            var sum = 0f
            for (x in 0 until size) {
                sum += depth[y * size + x]
                if (x > r) sum -= depth[y * size + x - r - 1]
                val count = min(x + r + 1, size) - max(0, x - r)
                horizontal[y * size + x] = sum / count.toFloat()
            }
        }
        for (x in 0 until size) {
            var sum = 0f
            for (y in 0 until size) {
                sum += horizontal[y * size + x]
                if (y > r) sum -= horizontal[(y - r - 1) * size + x]
                val count = min(y + r + 1, size) - max(0, y - r)
                val value = sum / count.toFloat()
                output[y * size + x] = if (invert) 1f - value else value
            }
        }
        return output
    }

    private fun makeParallelSbs(source: Bitmap, depth: FloatArray, depthScale: Float, fillRadius: Int): Bitmap {
        val w = source.width
        val h = source.height
        val left = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val right = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(w * h)
        val leftPixels = IntArray(w * h)
        val rightPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)
        srcPixels.copyInto(leftPixels)
        srcPixels.copyInto(rightPixels)

        val fill = fillRadius.coerceIn(1, 32)
        val depthScaling = depthScale / w.toFloat()

        for (x in w - 1 downTo 0) {
            for (y in 0 until h) {
                val dx = (x * 517 / max(1, w - 1)).coerceIn(0, 517)
                val dy = (y * 517 / max(1, h - 1)).coerceIn(0, 517)
                val d = depth[dy * 518 + dx]
                val shift = (d.coerceIn(0f, 1f) * 255f * depthScaling).toInt().coerceIn(0, w - 1)
                val color = srcPixels[y * w + x]
                for (offset in 0 until fill) {
                    val leftX = (x + shift + offset).coerceIn(0, w - 1)
                    leftPixels[y * w + leftX] = color
                }
            }
        }
        left.setPixels(leftPixels, 0, w, 0, 0, w, h)
        right.setPixels(rightPixels, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w * 2, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(left, 0f, 0f, null)
        canvas.drawBitmap(right, w.toFloat(), 0f, null)
        return out
    }
}

private class DebugExporter(
    private val context: Context,
    private val cache: VrCacheManager,
) {
    fun export(photo: PhotoItem, entry: VrCacheEntry): File {
        val exportDir = File(context.getExternalFilesDir(null), "debug_exports").also { it.mkdirs() }
        val safeName = photo.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val zip = File(exportDir, "${photo.cacheKey}_$safeName.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out ->
            val preview = File(cache.entryDir(photo, entry.version), "source_preview.jpg")
            decodeScaledBitmap(context, photo.uri, 1600).also { bitmap ->
                FileOutputStream(preview).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
            out.addFile("source_preview.jpg", preview)
            out.addFile("depth.png", File(entry.depthPath))
            out.addFile("vr_sbs.jpg", File(entry.outputPath))
            out.addFile("params.json", File(entry.paramsPath))
            out.addFile("job.log", File(entry.logPath))
        }
        return zip
    }
}

private sealed class AppScreen {
    data object Gallery : AppScreen()
    data object Settings : AppScreen()
    data object Manage : AppScreen()
    data object Viewer : AppScreen()
    data object Debug : AppScreen()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GalleryApp(viewModel: GalleryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permission = imagePermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionChanged(granted)
    }

    LaunchedEffect(Unit) {
        if (!state.hasPermission) {
            launcher.launch(permission)
        }
    }

    if (!state.hasPermission) {
        PermissionScreen(state.settings.language) { launcher.launch(permission) }
        return
    }

    val screen = when {
        state.manageOpen -> AppScreen.Manage
        state.settingsOpen -> AppScreen.Settings
        state.debugIndex != null -> AppScreen.Debug
        state.selectedIndex != null -> AppScreen.Viewer
        else -> AppScreen.Gallery
    }
    BackHandler(enabled = screen is AppScreen.Debug) { viewModel.closeDebug() }
    BackHandler(enabled = screen is AppScreen.Viewer) { viewModel.closeViewer() }
    BackHandler(enabled = screen is AppScreen.Manage) { viewModel.closeManage() }
    BackHandler(enabled = screen is AppScreen.Settings) { viewModel.closeSettings() }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.96f))
        },
        label = "screenTransition",
    ) { target ->
        when (target) {
            AppScreen.Manage -> ManageScreen(
                state = state,
                onBack = viewModel::closeManage,
                onDeleteVersion = viewModel::deleteCacheVersion,
            )
            AppScreen.Settings -> SettingsScreen(
                settings = state.settings,
                modelStatus = state.modelStatus,
                onBack = viewModel::closeSettings,
                onChange = viewModel::updateSettings,
            )
            AppScreen.Debug -> DebugScreen(
                state = state,
                index = state.debugIndex ?: state.galleryAnchorIndex,
                onBack = viewModel::closeDebug,
                onShare = { viewModel.exportDebug(context, state.debugIndex ?: state.galleryAnchorIndex) },
            )
            AppScreen.Viewer -> ViewerScreen(
                state = state,
                startIndex = state.selectedIndex ?: state.galleryAnchorIndex,
                onClose = viewModel::closeViewer,
                onIndexChanged = viewModel::onPagerIndexChanged,
                onVr = viewModel::requestVr,
                onRetry = viewModel::retry,
                onOpenDebug = viewModel::openDebug,
            )
            AppScreen.Gallery -> GalleryScreen(
                state = state,
                onRefresh = viewModel::loadPhotos,
                onOpen = viewModel::openPhoto,
                onSettings = viewModel::openSettings,
                onManage = viewModel::openManage,
                onSaveGenerated = { viewModel.saveGeneratedCopy(context, it) },
                onReplaceOriginal = { viewModel.replaceOriginalWithGenerated(context, it) },
            )
        }
    }
}

@Composable
private fun PermissionScreen(lang: AppLanguage, onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(lang.t("平行眼 VR 图库", "Parallel VR Gallery"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(lang.t("授权读取图片后，可以浏览系统相册，并在本地生成平行眼 SBS VR 缓存。", "Allow image access to browse your gallery and build local parallel-eye VR cache."))
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrant) { Text(lang.t("授权图片访问", "Grant access")) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onOpen: (Int) -> Unit,
    onSettings: () -> Unit,
    onManage: () -> Unit,
    onSaveGenerated: (Int) -> Unit,
    onReplaceOriginal: (Int) -> Unit,
) {
    val lang = state.settings.language
    var tileSize by rememberSaveable { mutableStateOf(112f) }
    var selectedActionIndex by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = state.galleryAnchorIndex.coerceAtLeast(0))
    LaunchedEffect(state.galleryAnchorIndex, state.photos.size) {
        if (state.galleryAnchorIndex in state.photos.indices) {
            gridState.scrollToItem(state.galleryAnchorIndex)
        }
    }

    selectedActionIndex?.let { index ->
        val photo = state.photos.getOrNull(index)
        val isReady = photo?.let { state.states[it.cacheKey] == VrState.READY && state.entries.containsKey(it.cacheKey) } == true
        AlertDialog(
            onDismissRequest = { selectedActionIndex = null },
            title = { Text(lang.t("处理图片", "Process image")) },
            text = {
                Text(
                    if (isReady) {
                        photo?.displayName.orEmpty()
                    } else {
                        lang.t("这张图还没有生成好的 VR 图，先开启 VR 生成后再保存或替换。", "This photo has no generated VR image yet. Generate VR before saving or replacing.")
                    },
                )
            },
            confirmButton = {
                Button(
                    enabled = isReady,
                    onClick = {
                        selectedActionIndex = null
                        onSaveGenerated(index)
                    },
                ) { Text(lang.t("保存", "Save")) }
            },
            dismissButton = {
                Row {
                    OutlinedButton(onClick = { selectedActionIndex = null }) { Text(lang.t("取消", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = isReady,
                        onClick = {
                            selectedActionIndex = null
                            onReplaceOriginal(index)
                        },
                    ) { Text(lang.t("替换", "Replace")) }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lang.t("平行眼 VR 图库", "Parallel VR Gallery"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onManage) { Text(lang.t("管理", "Manage")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onSettings) { Text(lang.t("设置", "Settings")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onRefresh) { Text(lang.t("刷新", "Refresh")) }
                }
                Spacer(Modifier.height(10.dp))
                val prefetch = if (state.settings.autoPrefetch) lang.t("自动：3→5→10", "Auto: 3→5→10") else lang.t("前后各 ${state.settings.prefetchWindow} 张", "${state.settings.prefetchWindow} each side")
                Text(lang.t("预加载：$prefetch", "Prefetch: $prefetch"), style = MaterialTheme.typography.bodySmall)
                state.message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(lang.pickMixed(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(tileSize.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (abs(zoom - 1f) > 0.01f) {
                                tileSize = (tileSize * zoom).coerceIn(72f, 220f)
                            }
                        }
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.photos, key = { it.cacheKey }) { photo ->
                    val index = state.photos.indexOf(photo)
                    PhotoTile(
                        photo = photo,
                        state = state.states[photo.cacheKey] ?: VrState.NORMAL,
                        entry = state.entries[photo.cacheKey],
                        lang = lang,
                        onClick = { onOpen(index) },
                        onLongClick = { selectedActionIndex = index },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    photo: PhotoItem,
    state: VrState,
    entry: VrCacheEntry?,
    lang: AppLanguage,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(androidx.compose.ui.graphics.Color(0xff16191c))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        val displayUri = entry?.let { Uri.fromFile(File(it.outputPath)) } ?: photo.uri
        AsyncBitmapImage(displayUri, 420, ContentScale.Crop, Modifier.fillMaxSize())
        Text(
            text = state.label(lang),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(5.dp),
        )
    }
}

@Composable
private fun ManageScreen(
    state: UiState,
    onBack: () -> Unit,
    onDeleteVersion: (String) -> Unit,
) {
    val lang = state.settings.language
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xfff5f6f7))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text(lang.t("返回", "Back")) }
            Spacer(Modifier.width(12.dp))
            Text(lang.t("生成管理", "Generated Manager"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(lang.t("图片", "Images"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (state.cacheVersions.isEmpty()) {
            Text(lang.t("暂无已生成图片", "No generated images yet"))
        } else {
            state.cacheVersions.forEach { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lang.t("版本：${summary.version}", "Version: ${summary.version}"), fontWeight = FontWeight.Bold)
                        Text(lang.t("${summary.count} 张  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB", "${summary.count} images  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB"), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onDeleteVersion(summary.version) }) {
                        Text(lang.t("删除", "Delete"))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(lang.t("视频", "Videos"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(lang.t("视频生成入口已预留，当前版本暂未实现。", "Video generation entry is reserved; not implemented in this version."), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    modelStatus: String,
    onBack: () -> Unit,
    onChange: (AppSettings) -> Unit,
) {
    val lang = settings.language
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xfff5f6f7))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text(lang.t("返回", "Back")) }
            Spacer(Modifier.width(12.dp))
            Text(lang.t("设置", "Settings"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(lang.t("语言", "Language"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow {
            listOf(AppLanguage.ZH to "中文", AppLanguage.EN to "English").forEachIndexed { index, option ->
                SegmentedButton(
                    selected = settings.language == option.first,
                    onClick = { onChange(settings.copy(language = option.first)) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(option.second) }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(lang.t("模型", "Model"), fontWeight = FontWeight.Bold)
        Text(lang.pickMixed(modelStatus), style = MaterialTheme.typography.bodySmall)
        Text(lang.t("覆盖安装更新会保留已下载模型；卸载后重装通常需要重新下载。", "Updating over the existing app keeps the downloaded model; uninstalling usually removes it."), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        Text(lang.t("模型选择", "Model selection"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow {
            AvailableModels.forEachIndexed { index, spec ->
                SegmentedButton(
                    selected = settings.modelId == spec.id,
                    onClick = { onChange(settings.copy(modelId = spec.id, depthResolution = spec.inputSize)) },
                    shape = SegmentedButtonDefaults.itemShape(index, AvailableModels.size),
                ) { Text(spec.displayName) }
            }
        }
        Text(lang.t("目前只内置已验证模型；新增模型时会出现在这里。", "Only the verified model is available now; additional models will appear here."), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        Text(lang.t("预加载", "Prefetch"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow {
            val options = listOf("auto", "3", "5", "10")
            options.forEachIndexed { index, option ->
                val selected = if (option == "auto") settings.autoPrefetch else !settings.autoPrefetch && settings.prefetchWindow == option.toInt()
                SegmentedButton(
                    selected = selected,
                    onClick = {
                        if (option == "auto") onChange(settings.copy(autoPrefetch = true, prefetchWindow = 3))
                        else onChange(settings.copy(autoPrefetch = false, prefetchWindow = option.toInt()))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(if (option == "auto") lang.t("自动", "Auto") else option) }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingFloat(lang.t("深度强度", "Depth scale"), settings.depthScale, listOf(10f, 20f, 30f, 40f, 50f, 60f, 80f)) {
            onChange(settings.copy(depthScale = it))
        }
        SettingInt(lang.t("深度平滑", "Blur radius"), settings.blurRadius, listOf(0, 1, 3, 5, 9, 15, 25)) {
            onChange(settings.copy(blurRadius = it))
        }
        SettingInt(lang.t("边缘填充", "Fill radius"), settings.fillRadius, listOf(0, 3, 5, 10, 15, 20, 30)) {
            onChange(settings.copy(fillRadius = it))
        }
        SettingInt(lang.t("输出最大长边", "Max output long edge"), settings.maxLongEdge, listOf(1280, 1920, 2048, 3072, 4096, 6000)) {
            onChange(settings.copy(maxLongEdge = it))
        }

        Spacer(Modifier.height(12.dp))
        Text(lang.t("深度图分辨率", "Depth resolution"), fontWeight = FontWeight.Bold)
        val spec = modelSpec(settings.modelId)
        Text(lang.t("${spec.inputSize} x ${spec.inputSize}（由当前模型固定；选择其他模型后这里会随模型变化）", "${spec.inputSize} x ${spec.inputSize}. Fixed by the selected model; choosing another model changes this value."), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.invertDepth,
                onCheckedChange = { onChange(settings.copy(invertDepth = it)) },
            )
            Text(lang.t("反转深度", "Invert depth"))
        }
    }
}

@Composable
private fun SettingInt(title: String, value: Int, options: List<Int>, onChange: (Int) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = value == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(option.toString()) }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun SettingFloat(title: String, value: Float, options: List<Float>, onChange: (Float) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = abs(value - option) < 0.01f,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(option.roundToInt().toString()) }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun ViewerScreen(
    state: UiState,
    startIndex: Int,
    onClose: () -> Unit,
    onIndexChanged: (Int) -> Unit,
    onVr: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onOpenDebug: (Int) -> Unit,
) {
    val lang = state.settings.language
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? ComponentActivity)?.window
        if (Build.VERSION.SDK_INT >= 30) {
            window?.insetsController?.hide(WindowInsets.Type.systemBars())
            window?.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= 30) {
                window?.insetsController?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = startIndex) { state.photos.size }
    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
    }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, state.vrMode, pagerState.currentPage) {
        if (state.vrMode && controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .pointerInput(state.vrMode) {
                detectTapGestures {
                    controlsVisible = true
                }
            },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = state.photos[page]
            val entry = state.entries[photo.cacheKey]
            val vrState = state.states[photo.cacheKey] ?: VrState.NORMAL
            if (state.vrMode && entry != null) {
                SyncSbsZoomImage(Uri.fromFile(File(entry.outputPath)), Modifier.fillMaxSize()) {
                    controlsVisible = true
                }
            } else {
                AsyncBitmapImage(photo.uri, 4096, ContentScale.Fit, Modifier.fillMaxSize())
                if (state.vrMode && vrState != VrState.READY) {
                    StatusOverlay(vrState, lang, onRetry = { onRetry(page) })
                }
            }
        }
        if (controlsVisible || !state.vrMode) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0x99000000)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onClose) { Text(lang.t("返回", "Back")) }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.photos.getOrNull(pagerState.currentPage)?.displayName.orEmpty(),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onVr(pagerState.currentPage) }) {
                    Text(if (state.vrMode) lang.t("关闭 VR", "VR Off") else lang.t("开启 VR", "VR On"))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onOpenDebug(pagerState.currentPage) }) { Text(lang.t("调试", "Debug")) }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0x99000000)).padding(10.dp),
            ) {
                val current = state.photos.getOrNull(pagerState.currentPage)
                if (current != null) {
                    Text(
                        text = "${lang.t("状态", "State")}: ${(state.states[current.cacheKey] ?: VrState.NORMAL).label(lang)}   ${pagerState.currentPage + 1}/${state.photos.size}",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                state.modelProgress?.let {
                    Text(lang.pickMixed(state.modelStatus), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall)
                }
                state.logs.take(3).forEach {
                    Text(it, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StatusOverlay(state: VrState, lang: AppLanguage, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x66000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state == VrState.GENERATING || state == VrState.QUEUED) {
            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.height(12.dp))
            Text(state.label(lang), color = androidx.compose.ui.graphics.Color.White)
        } else if (state == VrState.FAILED) {
            Text(lang.t("生成失败", "Generation failed"), color = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text(lang.t("重试", "Retry")) }
        }
    }
}

@Composable
private fun DebugScreen(
    state: UiState,
    index: Int,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    val lang = state.settings.language
    val photo = state.photos.getOrNull(index)
    val entry = photo?.let { state.entries[it.cacheKey] }
    val vrState = photo?.let { state.states[it.cacheKey] } ?: VrState.NORMAL
    val job = photo?.let { p -> state.jobs.firstOrNull { it.photoItem.cacheKey == p.cacheKey } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xff111315))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text(lang.t("返回", "Back")) }
            Spacer(Modifier.width(8.dp))
            Text(
                text = lang.t("调试", "Debug"),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onShare, enabled = entry != null) { Text(lang.t("分享调试包", "Share debug package")) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${photo?.displayName.orEmpty()}  ${lang.t("状态", "State")}: $vrState",
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebugImagePanel(lang.t("原图", "Source"), photo?.uri, Modifier.weight(1f), lang)
            DebugImagePanel(lang.t("深度图", "Depth"), entry?.let { Uri.fromFile(File(it.depthPath)) }, Modifier.weight(1f), lang)
            DebugImagePanel("VR SBS", entry?.let { Uri.fromFile(File(it.outputPath)) }, Modifier.weight(1f), lang)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(androidx.compose.ui.graphics.Color(0xff202326))
                .padding(8.dp),
        ) {
            Text(lang.t("日志", "Logs"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            Text("${lang.t("模型", "Model")}: ${lang.pickMixed(state.modelStatus)}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            job?.let {
                Text("Job: ${it.state} progress=${(it.progress * 100f).roundToInt()}% error=${it.error.orEmpty()}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
            state.logs.take(6).forEach {
                Text(it, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DebugImagePanel(title: String, uri: Uri?, modifier: Modifier = Modifier, lang: AppLanguage = AppLanguage.ZH) {
    Column(modifier) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        if (uri == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xff202326)),
                contentAlignment = Alignment.Center,
            ) {
                Text(lang.t("暂无", "None"), color = androidx.compose.ui.graphics.Color.White)
            }
        } else {
            AsyncBitmapImage(uri, 2048, ContentScale.Fit, Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
        }
    }
}

@Composable
private fun SyncSbsZoomImage(uri: Uri, modifier: Modifier = Modifier, onInteract: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(uri) { mutableStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) { runCatching { decodeScaledBitmap(context, uri, 4096) }.getOrNull() }
        scale = 1f
        offset = Offset.Zero
    }
    if (bitmap == null) {
        Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(26.dp))
        }
        return
    }

    val source = bitmap!!
    val halfWidth = (source.width / 2).coerceAtLeast(1)
    val left = remember(source) { Bitmap.createBitmap(source, 0, 0, halfWidth, source.height).asImageBitmap() }
    val right = remember(source) { Bitmap.createBitmap(source, halfWidth, 0, source.width - halfWidth, source.height).asImageBitmap() }
    val imageModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        }

    Row(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.Black)
            .pointerInput(uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onInteract()
                    val nextScale = (scale * zoom).coerceIn(1f, 8f)
                    scale = nextScale
                    offset = if (nextScale == 1f) Offset.Zero else offset + pan
                }
            },
    ) {
        Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.Center) {
            Image(left, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Fit)
        }
        Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.Center) {
            Image(right, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun WindowSelector(value: Int, onChange: (Int) -> Unit) {
    val options = listOf(3, 5, 10)
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = value == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text("$option")
            }
        }
    }
}

@Composable
private fun AsyncBitmapImage(uri: Uri, maxSide: Int, contentScale: ContentScale, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri, maxSide) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxSide) {
        bitmap = withContext(Dispatchers.IO) { runCatching { decodeScaledBitmap(context, uri, maxSide) }.getOrNull() }
    }
    if (bitmap == null) {
        Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(26.dp))
        }
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

private fun hasImagePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, imagePermission()) == PackageManager.PERMISSION_GRANTED
}

private fun imagePermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxLongEdge: Int): Bitmap {
    if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = min(1f, maxLongEdge.toFloat() / max(w, h).toFloat())
            decoder.setTargetSize(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }.copy(Bitmap.Config.ARGB_8888, false)
    }

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
    var sample = 1
    while (max(options.outWidth / sample, options.outHeight / sample) > maxLongEdge) sample *= 2
    val decode = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input, null, decode) ?: error("Unable to decode image")
    }.copy(Bitmap.Config.ARGB_8888, false)
}

private fun saveImageToGallery(context: Context, source: File, displayName: String): Uri {
    val safeName = displayName.ifBlank { "parallel_vr_${System.currentTimeMillis()}.jpg" }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
        put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ParallelVrGallery")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
    runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Unable to open gallery output")
        if (Build.VERSION.SDK_INT >= 29) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
    }.onFailure { error ->
        resolver.delete(uri, null, null)
        throw error
    }
    return uri
}

private fun replaceOriginalImage(context: Context, photo: PhotoItem, source: File) {
    val resolver = context.contentResolver
    resolver.openOutputStream(photo.uri, "w")?.use { output ->
        source.inputStream().use { input -> input.copyTo(output) }
    } ?: error("Unable to open original image for writing")
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, photo.displayName)
        put(MediaStore.Images.Media.DATE_MODIFIED, photo.modifiedTime)
    }
    resolver.update(photo.uri, values, null, null)
}

private fun loadModelFile(file: File): ByteBuffer {
    FileInputStream(file).use { input ->
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }
}

private fun VrGenerationParams.toJson(photo: PhotoItem, source: Bitmap, output: Bitmap): String {
    return """
        {
          "photoKey": "${photo.cacheKey}",
          "cacheVersion": "${cacheVersion()}",
          "displayName": "${photo.displayName.replace("\"", "\\\"")}",
          "sourceWidth": ${source.width},
          "sourceHeight": ${source.height},
          "outputWidth": ${output.width},
          "outputHeight": ${output.height},
          "depthModel": "$depthModel",
          "outputMode": "$outputMode",
          "depthScale": $depthScale,
          "blurRadius": $blurRadius,
          "fillRadius": $fillRadius,
          "invertDepth": $invertDepth,
          "maxLongEdge": $maxLongEdge,
          "inpaintMode": "$inpaintMode",
          "quality": $quality,
          "modelSha256": "B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F",
          "modelSource": "https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite"
        }
    """.trimIndent()
}

private fun VrGenerationParams.cacheVersion(): String {
    val scale = depthScale.roundToInt()
    val invert = if (invertDepth) "inv1" else "inv0"
    return "${depthModel}_s${scale}_b${blurRadius}_f${fillRadius}_${invert}_m${maxLongEdge}"
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02X".format(it) }
}

private fun ZipOutputStream.addFile(name: String, file: File) {
    putNextEntry(ZipEntry(name))
    file.inputStream().use { it.copyTo(this) }
    closeEntry()
}
