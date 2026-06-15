package com.local.parallelvrgallery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Activity
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
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.VideoView
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
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

enum class MediaKind {
    IMAGE,
    VIDEO,
}

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val modifiedTime: Long,
    val kind: MediaKind = MediaKind.IMAGE,
) {
    val cacheKey: String
        get() = if (kind == MediaKind.IMAGE) "${id}_${size}_${modifiedTime}" else "VIDEO_${id}_${size}_${modifiedTime}"
}

typealias GalleryItem = PhotoItem

enum class VrState {
    NORMAL,
    PAUSED,
    QUEUED,
    GENERATING,
    READY,
    FAILED,
}

enum class VideoVrState {
    NORMAL,
    QUEUED,
    GENERATING,
    PAUSED,
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
    val modelThreads: Int = 4,
    val useGpu: Boolean = false,
    val inpaintMode: String = "FOREGROUND_FILL",
    val quality: Int = 94,
)

data class VideoGenerationParams(
    val vr: VrGenerationParams,
    val modelThreads: Int = 4,
    val useGpu: Boolean = false,
) {
    fun toVrParams(): VrGenerationParams = vr.copy(modelThreads = modelThreads, useGpu = useGpu)
}

enum class AppLanguage {
    ZH,
    EN,
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ZH,
    val imageModelId: String = "depth_anything_v2_small_tflite",
    val videoModelId: String = "depth_anything_v2_small_tflite",
    val autoPrefetch: Boolean = true,
    val prefetchWindow: Int = 2,
    val depthScale: Float = 40f,
    val blurRadius: Int = 3,
    val fillRadius: Int = 10,
    val invertDepth: Boolean = false,
    val maxLongEdge: Int = 6000,
    val depthResolution: Int = 518,
    val generationWorkers: Int = 1,
    val modelThreads: Int = 4,
    val useGpu: Boolean = false,
    val videoModelThreads: Int = 4,
    val videoUseGpu: Boolean = false,
) {
    fun toParams(): VrGenerationParams = VrGenerationParams(
        depthModel = imageModelId,
        depthScale = depthScale,
        blurRadius = blurRadius,
        fillRadius = fillRadius,
        invertDepth = invertDepth,
        maxLongEdge = maxLongEdge,
        modelThreads = modelThreads,
        useGpu = useGpu,
    )

    fun toVideoParams(): VideoGenerationParams = VideoGenerationParams(
        vr = toParams().copy(depthModel = videoModelId),
        modelThreads = videoModelThreads,
        useGpu = videoUseGpu,
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
        displayName = "Depth Anything V2 Small TFLite 518（稳定）",
        inputSize = 518,
        fileName = "depth_anything_v2.tflite",
        url = "https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite",
        sha256 = "B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F",
    ),
    ModelSpec(
        id = "qualcomm_depth_anything_v2_tflite",
        displayName = "Qualcomm Depth Anything V2 TFLite 518（实验）",
        inputSize = 518,
        fileName = "qualcomm_depth_anything_v2.tflite",
        url = "https://huggingface.co/qualcomm/Depth-Anything-V2/resolve/main/Depth-Anything-V2.tflite?download=true",
        sha256 = "727E025EAB1DB3650C6FED86AA8D7932B994D8746E41A7A5773E663DE740859F",
    ),
)

private const val GENERATED_VR_PREFIX = "PVG_VR_"

private object AppWorkScopes {
    val video = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

private fun modelSpec(id: String): ModelSpec = AvailableModels.firstOrNull { it.id == id } ?: AvailableModels.first()

data class CacheVersionSummary(
    val version: String,
    val kind: String,
    val count: Int,
    val bytes: Long,
)

data class ManagedCacheItem(
    val photoItem: PhotoItem,
    val entry: VrCacheEntry,
)

data class VideoCacheEntry(
    val videoKey: String,
    val outputPath: String,
    val logPath: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val fps: Int,
    val createdAt: Long,
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

data class VideoVrJob(
    val item: GalleryItem,
    val state: VideoVrState,
    val progress: Float,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val fps: Int = 30,
    val avgFrameMs: Long = 0L,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)

data class LastGenerationInfo(
    val relativeIndex: Int,
    val elapsedMs: Long,
)

data class UiState(
    val hasPermission: Boolean = false,
    val hasVideoPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val selectedIndex: Int? = null,
    val galleryAnchorIndex: Int = 0,
    val galleryScrollIndex: Int = 0,
    val galleryScrollOffset: Int = 0,
    val galleryAnchorSlot: Int = 0,
    val settingsOpen: Boolean = false,
    val manageOpen: Boolean = false,
    val vrMode: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val activePrefetchWindow: Int = 2,
    val states: Map<String, VrState> = emptyMap(),
    val entries: Map<String, VrCacheEntry> = emptyMap(),
    val jobs: List<VrJob> = emptyList(),
    val logs: List<String> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val debugIndex: Int? = null,
    val modelProgress: Float? = null,
    val modelStatus: String = "模型未下载 / Model not downloaded",
    val blockingMessage: String? = null,
    val cacheVersions: List<CacheVersionSummary> = emptyList(),
    val managedCacheItems: List<ManagedCacheItem> = emptyList(),
    val videoStates: Map<String, VideoVrState> = emptyMap(),
    val videoEntries: Map<String, VideoCacheEntry> = emptyMap(),
    val videoJobs: List<VideoVrJob> = emptyList(),
    val lastGeneration: LastGenerationInfo? = null,
    val recentGenerations: List<LastGenerationInfo> = emptyList(),
    val updateStatus: String? = null,
    val updateUrl: String? = null,
    val updateAvailable: Boolean = false,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PhotoRepository(app)
    private val cache = VrCacheManager(app)
    private val videoCache = VideoVrCacheManager(app)
    private val videoNotifier = VideoGenerationNotifier(app)
    private val modelManager = ModelManager(app)
    private val generator = VrGenerator(app, cache, modelManager)
    private val videoGenerator = VideoVrGenerator(app, videoCache, generator)
    private val settingsStore = SettingsStore(app)
    private val pending = PriorityQueue<QueuedJob>(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
    private val currentPending = PriorityQueue<QueuedJob>(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
    private val videoPending = PriorityQueue<VideoQueuedJob>(compareBy<VideoQueuedJob> { it.sequence })
    private val paused = linkedMapOf<String, QueuedJob>()
    private val pausedVideos = mutableSetOf<String>()
    private val pausedVideoParams = mutableMapOf<String, VideoGenerationParams>()
    private val activeVideoParams = mutableMapOf<String, VideoGenerationParams>()
    private var sequence = 0L
    private var videoSequence = 0L
    private val workers = mutableListOf<Job>()
    private val videoWorkers = mutableListOf<Job>()
    private var currentWorker: Job? = null
    private var activeKey: String? = null

    private val _uiState = MutableStateFlow(
        UiState(
            hasPermission = hasImagePermission(app),
            hasVideoPermission = hasVideoPermission(app),
            hasNotificationPermission = hasNotificationPermission(app),
            settings = settingsStore.load(),
        ),
    )
    val uiState: StateFlow<UiState> = _uiState

    init {
        if (_uiState.value.hasPermission) {
            loadPhotos()
        }
    }

    fun onPermissionChanged(imageGranted: Boolean, videoGranted: Boolean, notificationGranted: Boolean) {
        _uiState.update { it.copy(hasPermission = imageGranted, hasVideoPermission = videoGranted, hasNotificationPermission = notificationGranted) }
        if (imageGranted) loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            val photos = withContext(Dispatchers.IO) { repository.loadMedia() }
            val imageItems = photos.filter { it.kind == MediaKind.IMAGE }
            val videoItems = photos.filter { it.kind == MediaKind.VIDEO }
            val entries = withContext(Dispatchers.IO) { imageItems.mapNotNull { cache.findEntry(it) }.associateBy { it.photoKey } }
            val managedItems = withContext(Dispatchers.IO) { cache.allEntries(imageItems) }
            val videoEntries = withContext(Dispatchers.IO) { videoItems.mapNotNull { videoCache.findEntry(it) }.associateBy { it.videoKey } }
            _uiState.update {
                it.copy(
                    photos = photos,
                    entries = entries,
                    states = imageItems.associate { photo -> photo.cacheKey to if (entries.containsKey(photo.cacheKey)) VrState.READY else VrState.NORMAL },
                    videoStates = videoItems.associate { item -> item.cacheKey to if (videoEntries.containsKey(item.cacheKey)) VideoVrState.READY else VideoVrState.NORMAL },
                    videoEntries = videoEntries,
                    loading = false,
                    message = "已加载 ${imageItems.size} 张图片、${videoItems.size} 个视频 / ${imageItems.size} images, ${videoItems.size} videos loaded",
                    cacheVersions = cache.summaries(),
                    managedCacheItems = managedItems,
                    modelStatus = modelStatusText(it.settings),
                )
            }
        }
    }

    fun openPhoto(index: Int, firstVisibleIndex: Int = index, firstVisibleOffset: Int = 0) {
        val item = _uiState.value.photos.getOrNull(index)
        _uiState.update {
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = firstVisibleIndex,
                galleryScrollOffset = firstVisibleOffset,
                galleryAnchorSlot = (index - firstVisibleIndex).coerceAtLeast(0),
                vrMode = true,
                message = null,
            )
        }
        if (item?.kind == MediaKind.IMAGE) enqueueWindow(index, includeCurrent = true)
    }

    fun openGeneratedPhoto(index: Int, entry: VrCacheEntry? = null) {
        val photo = _uiState.value.photos.getOrNull(index)
        _uiState.update {
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = max(0, index - it.galleryAnchorSlot),
                vrMode = true,
                manageOpen = false,
                entries = if (photo != null && entry != null) it.entries + (photo.cacheKey to entry) else it.entries,
                message = null,
            )
        }
    }

    fun openGeneratedVideo(index: Int) {
        _uiState.update {
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = max(0, index - it.galleryAnchorSlot),
                vrMode = true,
                manageOpen = false,
                message = null,
            )
        }
    }

    fun closeViewer() {
        _uiState.update {
            val currentIndex = it.selectedIndex ?: it.galleryAnchorIndex
            it.copy(
                selectedIndex = null,
                debugIndex = null,
                galleryAnchorIndex = currentIndex,
                galleryScrollIndex = max(0, currentIndex - it.galleryAnchorSlot),
            )
        }
        enqueueWindow(_uiState.value.galleryAnchorIndex, includeCurrent = false)
    }

    fun openSettings() {
        _uiState.update { it.copy(settingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(settingsOpen = false) }
    }

    fun checkForUpdates() {
        val lang = _uiState.value.settings.language
        _uiState.update { it.copy(updateStatus = lang.t("正在检查更新...", "Checking for updates..."), updateUrl = null, updateAvailable = false) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = (URL("https://api.github.com/repos/7116-byte/ParallelVrGallery/releases/latest").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 12000
                        readTimeout = 12000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }.mapCatching { body ->
                    val latest = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1) ?: "unknown"
                    val downloadUrl = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]*app-debug\\.apk)\"").find(body)?.groupValues?.getOrNull(1)
                    val pageUrl = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
                    val url = downloadUrl ?: pageUrl
                    val current = "v1.17"
                    if (latest == current) {
                        Triple(lang.t("已是最新版本：$current", "Already up to date: $current"), null, false)
                    } else {
                        Triple(lang.t("发现新版本：$latest，当前：$current", "New version: $latest, current: $current"), url, url != null)
                    }
                }.getOrElse { error ->
                    Triple(lang.t("检查更新失败：${error.message}", "Update check failed: ${error.message}"), null, false)
                }
            }
            _uiState.update { it.copy(updateStatus = result.first, updateUrl = result.second, updateAvailable = result.third) }
        }
    }

    fun updateSettings(settings: AppSettings) {
        settingsStore.save(settings)
        _uiState.update {
            it.copy(
                settings = settings,
                activePrefetchWindow = if (settings.autoPrefetch) 2 else settings.prefetchWindow,
                modelStatus = modelStatusText(settings),
            )
        }
        _uiState.value.selectedIndex?.let { refreshWindow(it) }
        startWorker()
    }

    fun onPagerIndexChanged(index: Int) {
        _uiState.update { it.copy(selectedIndex = index, galleryAnchorIndex = index, activePrefetchWindow = if (it.settings.autoPrefetch) 2 else it.settings.prefetchWindow) }
        if (_uiState.value.vrMode && _uiState.value.photos.getOrNull(index)?.kind == MediaKind.IMAGE) {
            enqueueWindow(index, includeCurrent = true)
        }
    }

    fun requestVr(index: Int) {
        val item = _uiState.value.photos.getOrNull(index) ?: return
        if (item.kind == MediaKind.VIDEO) {
            when (_uiState.value.videoStates[item.cacheKey] ?: VideoVrState.NORMAL) {
                VideoVrState.QUEUED,
                VideoVrState.GENERATING -> pauseVideo(item)
                VideoVrState.PAUSED -> enqueueVideo(item, force = true)
                VideoVrState.FAILED -> enqueueVideo(item, force = true)
                else -> enqueueVideo(item)
            }
            return
        }
        if (_uiState.value.vrMode) {
            stopVr()
        } else {
            _uiState.update { it.copy(vrMode = true, selectedIndex = index, message = null, activePrefetchWindow = if (it.settings.autoPrefetch) 2 else it.settings.prefetchWindow) }
            enqueueWindow(index, includeCurrent = true)
        }
    }

    fun stopVr() {
        synchronized(pending) { pending.clear() }
        synchronized(currentPending) { currentPending.clear() }
        synchronized(paused) { paused.clear() }
        _uiState.update { it.copy(vrMode = false, modelProgress = null) }
        addLog("vr stopped; generated caches kept")
    }

    fun retry(index: Int) {
        val item = _uiState.value.photos.getOrNull(index) ?: return
        if (item.kind == MediaKind.VIDEO) {
            enqueueVideo(item, force = true)
        } else {
            enqueuePhoto(index, priority = 0, force = true, current = true)
            startCurrentWorker()
        }
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
                    managedCacheItems = cache.allEntries(photos),
                    message = "已删除版本 / Deleted version: $version",
                )
            }
        }
    }

    fun saveGeneratedCopy(context: Context, index: Int) {
        saveGeneratedCopies(context, listOf(index))
    }

    fun saveGeneratedCopies(context: Context, indexes: List<Int>) {
        val lang = _uiState.value.settings.language
        _uiState.update { it.copy(blockingMessage = lang.t("保存中", "Saving")) }
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                var saved = 0
                var failed = 0
                indexes.distinct().forEach { index ->
                    val photo = _uiState.value.photos.getOrNull(index) ?: return@forEach
                    val entry = cache.findEntry(photo) ?: return@forEach
                    runCatching {
                        saveImageToGallery(context, File(entry.outputPath), "${GENERATED_VR_PREFIX}${photo.cacheKey}_${photo.displayName}")
                        saved++
                    }.onFailure { failed++ }
                }
                when {
                    saved == 0 -> lang.t("没有可保存的已生成 VR 图", "No generated VR images to save")
                    failed == 0 -> lang.t("已保存 $saved 张到系统图库", "Saved $saved images to system gallery")
                    else -> lang.t("已保存 $saved 张，失败 $failed 张", "Saved $saved images, failed $failed")
                }
            }
            _uiState.update { it.copy(blockingMessage = null, message = message) }
        }
    }

    fun saveGeneratedVideo(context: Context, index: Int) {
        saveGeneratedVideos(context, listOf(index))
    }

    fun saveGeneratedVideos(context: Context, indexes: List<Int>) {
        val lang = _uiState.value.settings.language
        _uiState.update { it.copy(blockingMessage = lang.t("保存中", "Saving")) }
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                var saved = 0
                var failed = 0
                indexes.distinct().forEach { index ->
                    val item = _uiState.value.photos.getOrNull(index) ?: return@forEach
                    val entry = _uiState.value.videoEntries[item.cacheKey] ?: videoCache.findEntry(item) ?: return@forEach
                    runCatching {
                        saveVideoToGallery(context, File(entry.outputPath), "${GENERATED_VR_PREFIX}${item.cacheKey}_${item.displayName}")
                        saved++
                    }.onFailure { failed++ }
                }
                when {
                    saved == 0 -> lang.t("没有可保存的已生成 VR 视频", "No generated VR videos to save")
                    failed == 0 -> lang.t("已保存 $saved 个视频到系统相册", "Saved $saved videos")
                    else -> lang.t("已保存 $saved 个视频，失败 $failed 个", "Saved $saved videos, failed $failed")
                }
            }
            _uiState.update { it.copy(blockingMessage = null, message = message) }
        }
    }

    fun deleteGeneratedVideo(index: Int) {
        deleteGeneratedVideos(listOf(index))
    }

    fun deleteGeneratedVideos(indexes: List<Int>) {
        val items = indexes.distinct().mapNotNull { _uiState.value.photos.getOrNull(it) }.filter { it.kind == MediaKind.VIDEO }
        viewModelScope.launch(Dispatchers.IO) {
            items.forEach { videoCache.delete(it) }
            _uiState.update {
                val keys = items.map { item -> item.cacheKey }.toSet()
                it.copy(
                    videoEntries = it.videoEntries - keys,
                    videoStates = it.videoStates + keys.associateWith { VideoVrState.NORMAL },
                    videoJobs = it.videoJobs.filterNot { job -> job.item.cacheKey in keys },
                    message = "已删除 ${items.size} 个视频 VR 缓存",
                )
            }
        }
    }

    fun deleteGeneratedImageEntries(entries: List<ManagedCacheItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { cache.deleteEntry(it.entry) }
            val photos = _uiState.value.photos
            val currentEntries = photos.mapNotNull { cache.findEntry(it) }.associateBy { it.photoKey }
            _uiState.update {
                it.copy(
                    entries = currentEntries,
                    states = photos.associate { photo -> photo.cacheKey to if (currentEntries.containsKey(photo.cacheKey)) VrState.READY else VrState.NORMAL },
                    cacheVersions = cache.summaries(),
                    managedCacheItems = cache.allEntries(photos),
                    message = "已删除 ${entries.size} 张图片 VR 缓存",
                )
            }
        }
    }

    fun regenerateImages(indexes: List<Int>) {
        val unique = indexes.distinct()
        if (unique.isEmpty()) return
        _uiState.update { it.copy(vrMode = true, message = "已加入 ${unique.size} 张图片重新生成队列") }
        unique.forEachIndexed { order, index -> enqueuePhoto(index, priority = order + 1, force = true, current = false) }
        startWorker()
    }

    fun regenerateVideos(indexes: List<Int>) {
        val unique = indexes.distinct()
        unique.forEach { index -> _uiState.value.photos.getOrNull(index)?.let { enqueueVideo(it, force = true) } }
        _uiState.update { it.copy(message = "已加入 ${unique.size} 个视频重新生成队列") }
    }

    fun replaceOriginalWithGenerated(context: Context, index: Int) {
        replaceOriginalsWithGenerated(context, listOf(index))
    }

    fun replaceOriginalsWithGenerated(context: Context, indexes: List<Int>) {
        val lang = _uiState.value.settings.language
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                var replaced = 0
                var failed = 0
                indexes.distinct().forEach { index ->
                    val photo = _uiState.value.photos.getOrNull(index) ?: return@forEach
                    val entry = cache.findEntry(photo) ?: return@forEach
                    runCatching {
                        replaceOriginalImage(context, photo, File(entry.outputPath))
                        replaced++
                    }.onFailure { failed++ }
                }
                when {
                    replaced == 0 -> lang.t("替换失败：系统可能不允许写入这些原图", "Replace failed: Android may not allow writing these originals")
                    failed == 0 -> lang.t("已替换 $replaced 张并尝试保留原时间", "Replaced $replaced images and tried to keep timestamps")
                    else -> lang.t("已替换 $replaced 张，失败 $failed 张", "Replaced $replaced images, failed $failed")
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
        val currentVersion = _uiState.value.settings.toParams().cacheVersion()
        val window = _uiState.value.activePrefetchWindow.coerceAtMost(8)
        val desiredCount = window * 2
        val targets = mutableListOf<Pair<Int, Int>>()
        if (includeCurrent) {
            if (photos.getOrNull(index)?.kind == MediaKind.IMAGE) enqueuePhoto(index, priority = 0, force = false, current = true)
        }
        var distance = 1
        while (targets.size < desiredCount && distance <= window && distance < photos.size) {
            val next = index + distance
            val prev = index - distance
            if (next in photos.indices && photos[next].kind == MediaKind.IMAGE && cache.findEntry(photos[next], currentVersion) == null) targets += next to distance * 2 - 1
            if (targets.size < desiredCount && prev in photos.indices && photos[prev].kind == MediaKind.IMAGE && cache.findEntry(photos[prev], currentVersion) == null) targets += prev to distance * 2
            distance++
        }

        synchronized(pending) {
            val keep = targets.map { photos[it.first].cacheKey }.toSet()
            val toPause = pending.filter { it.photo.cacheKey !in keep }
            pending.removeAll(toPause.toSet())
            toPause.forEach { job ->
                paused[job.photo.cacheKey] = job
                markState(job.photo.cacheKey, VrState.PAUSED)
                upsertJob(job.photo, job.priority, VrState.PAUSED, 0f)
            }
        }
        targets.forEach { (targetIndex, priority) -> enqueuePhoto(targetIndex, priority, force = false, current = false) }
        startWorker()
    }

    private fun enqueuePhoto(index: Int, priority: Int, force: Boolean, current: Boolean = false) {
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        if (photo.kind != MediaKind.IMAGE) return
        val currentVersion = _uiState.value.settings.toParams().cacheVersion()
        if (!force && cache.findEntry(photo, currentVersion) != null) {
            markState(photo.cacheKey, VrState.READY)
            return
        }
        val existingState = _uiState.value.states[photo.cacheKey]
        if (!force && existingState == VrState.GENERATING) return
        if (!force && !current && existingState == VrState.QUEUED) return
        val job = QueuedJob(photo, priority, sequence++)
        synchronized(pending) { pending.removeAll { it.photo.cacheKey == photo.cacheKey } }
        synchronized(paused) { paused.remove(photo.cacheKey) }
        if (current) {
            synchronized(currentPending) {
                currentPending.removeAll { it.photo.cacheKey == photo.cacheKey }
                currentPending.add(job)
            }
            startCurrentWorker()
        } else {
            synchronized(pending) { pending.add(job) }
        }
        markState(photo.cacheKey, VrState.QUEUED)
        addLog("${if (current) "current" else "queued"} ${photo.displayName} p=$priority")
    }

    private fun startWorker() {
        workers.removeAll { !it.isActive }
        val desired = _uiState.value.settings.generationWorkers.coerceIn(1, 3)
        repeat((desired - workers.size).coerceAtLeast(0)) {
            workers += viewModelScope.launch(Dispatchers.IO) {
                workerLoop()
            }
        }
    }

    private fun startCurrentWorker() {
        if (currentWorker?.isActive == true) return
        currentWorker = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (!_uiState.value.vrMode) {
                    synchronized(currentPending) { currentPending.clear() }
                    break
                }
                val next = synchronized(currentPending) { currentPending.poll() } ?: break
                processJob(next)
                delay(25)
            }
        }
    }

    private fun enqueueVideo(item: GalleryItem, force: Boolean = false) {
        if (item.kind != MediaKind.VIDEO) return
        val params = synchronized(pausedVideoParams) {
            if (force) pausedVideoParams.remove(item.cacheKey) else null
        } ?: _uiState.value.settings.toVideoParams()
        synchronized(pausedVideos) { pausedVideos.remove(item.cacheKey) }
        if (!force && videoCache.findEntry(item) != null) {
            _uiState.update { it.copy(videoStates = it.videoStates + (item.cacheKey to VideoVrState.READY)) }
            return
        }
        val existing = _uiState.value.videoStates[item.cacheKey]
        if (!force && (existing == VideoVrState.QUEUED || existing == VideoVrState.GENERATING)) return
        synchronized(videoPending) {
            videoPending.removeAll { it.item.cacheKey == item.cacheKey }
            videoPending.add(VideoQueuedJob(item, params, videoSequence++))
        }
        upsertVideoJob(item, VideoVrState.QUEUED, 0f)
        _uiState.update { it.copy(videoStates = it.videoStates + (item.cacheKey to VideoVrState.QUEUED), message = "视频已加入 VR 队列：${item.displayName}") }
        addLog("video queued ${item.displayName}")
        startVideoWorkers()
    }

    private fun pauseVideo(item: GalleryItem) {
        if (item.kind != MediaKind.VIDEO) return
        synchronized(videoPending) {
            videoPending.removeAll { it.item.cacheKey == item.cacheKey }
        }
        synchronized(pausedVideos) { pausedVideos.add(item.cacheKey) }
        synchronized(pausedVideoParams) {
            activeVideoParams[item.cacheKey]?.let { pausedVideoParams[item.cacheKey] = it }
        }
        _uiState.update {
            it.copy(
                videoStates = it.videoStates + (item.cacheKey to VideoVrState.PAUSED),
                message = "视频已暂停生成：${item.displayName}",
            )
        }
        upsertVideoJob(item, VideoVrState.PAUSED, _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == item.cacheKey }?.progress ?: 0f)
        addLog("video paused ${item.displayName}")
    }

    private fun startVideoWorkers() {
        videoWorkers.removeAll { !it.isActive }
        repeat((3 - videoWorkers.size).coerceAtLeast(0)) {
            videoWorkers += AppWorkScopes.video.launch {
                while (true) {
                    val next = synchronized(videoPending) { videoPending.poll() } ?: break
                    processVideoJob(next)
                    delay(100)
                }
            }
        }
    }

    private fun processVideoJob(next: VideoQueuedJob) {
        synchronized(pausedVideoParams) { activeVideoParams[next.item.cacheKey] = next.params }
        _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.GENERATING)) }
        upsertVideoJob(next.item, VideoVrState.GENERATING, 0.01f)
        videoNotifier.show(next.item, 0, 0, indeterminate = true, status = "准备生成")
        val started = System.currentTimeMillis()
        val recentFrameTimes = mutableListOf<Long>()
        var lastFrameAt: Long? = null
        val result = runCatching {
            videoGenerator.generate(
                next.item,
                next.params,
                onModelProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            modelProgress = progress,
                            modelStatus = if (progress < 1f) "正在下载模型 / Downloading model ${(progress * 100f).roundToInt()}%" else "模型已就绪 / Model ready",
                        )
                    }
                },
            ) { progress, frame, total, fps ->
                if (synchronized(pausedVideos) { pausedVideos.contains(next.item.cacheKey) }) {
                    throw VideoPausedException()
                }
                val now = System.currentTimeMillis()
                lastFrameAt?.let { previous ->
                    if (frame > 1) {
                        recentFrameTimes += (now - previous).coerceAtLeast(0L)
                        if (recentFrameTimes.size > 3) recentFrameTimes.removeAt(0)
                    }
                }
                lastFrameAt = now
                val avgFrameMs = if (recentFrameTimes.isNotEmpty()) recentFrameTimes.average().roundToInt().toLong() else 0L
                upsertVideoJob(next.item, VideoVrState.GENERATING, progress, frame, total, fps, avgFrameMs = avgFrameMs)
                videoNotifier.show(next.item, frame, total, indeterminate = false, status = "生成中")
            }
        }
        result.onSuccess { entry ->
            val elapsed = System.currentTimeMillis() - started
            _uiState.update {
                it.copy(
                    videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.READY),
                    videoEntries = it.videoEntries + (next.item.cacheKey to entry),
                    modelProgress = null,
                    modelStatus = modelStatusText(it.settings),
                    message = "视频 VR 已生成：${next.item.displayName}（${elapsed}ms）",
                )
            }
            upsertVideoJob(next.item, VideoVrState.READY, 1f, finishedAt = System.currentTimeMillis())
            synchronized(pausedVideoParams) {
                pausedVideoParams.remove(next.item.cacheKey)
                activeVideoParams.remove(next.item.cacheKey)
            }
            videoNotifier.show(next.item, 1, 1, indeterminate = false, status = "已完成")
            addLog("video ready ${next.item.displayName} ${entry.width}x${entry.height} ${elapsed}ms")
        }.onFailure { error ->
            if (error is VideoPausedException) {
                _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.PAUSED), modelProgress = null) }
                synchronized(pausedVideoParams) {
                    pausedVideoParams[next.item.cacheKey] = next.params
                    activeVideoParams.remove(next.item.cacheKey)
                }
                upsertVideoJob(next.item, VideoVrState.PAUSED, _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == next.item.cacheKey }?.progress ?: 0f)
                addLog("video paused ${next.item.displayName}")
            } else {
                _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.FAILED), modelProgress = null) }
                synchronized(pausedVideoParams) { activeVideoParams.remove(next.item.cacheKey) }
                upsertVideoJob(next.item, VideoVrState.FAILED, 1f, finishedAt = System.currentTimeMillis(), error = error.message)
                videoNotifier.failed(next.item, error.message ?: "生成失败")
                addLog("video failed ${next.item.displayName}: ${error.message}")
            }
        }
    }

    private suspend fun workerLoop() {
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
                processJob(next)
                delay(50)
            }
    }

    private fun processJob(next: QueuedJob) {
        activeKey = next.photo.cacheKey
        markState(next.photo.cacheKey, VrState.GENERATING)
        upsertJob(next.photo, next.priority, VrState.GENERATING, 0.1f)
        val started = System.currentTimeMillis()
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
            val selected = _uiState.value.selectedIndex ?: _uiState.value.galleryAnchorIndex
            val doneIndex = _uiState.value.photos.indexOfFirst { it.cacheKey == next.photo.cacheKey }
            val generationInfo = LastGenerationInfo(doneIndex - selected, System.currentTimeMillis() - started)
            _uiState.update {
                it.copy(
                    modelProgress = null,
                    modelStatus = modelStatusText(it.settings),
                    cacheVersions = cache.summaries(),
                    lastGeneration = generationInfo,
                    recentGenerations = (listOf(generationInfo) + it.recentGenerations).take(3),
                )
            }
        }.onFailure { error ->
            markState(next.photo.cacheKey, VrState.FAILED)
            upsertJob(next.photo, next.priority, VrState.FAILED, 1f, finishedAt = System.currentTimeMillis(), error = error.message)
            addLog("failed ${next.photo.displayName}: ${error.message}")
        }
        activeKey = null
    }

    private fun expandAutoPrefetchIfNeeded(): Boolean {
        if (!_uiState.value.vrMode || !_uiState.value.settings.autoPrefetch) return false
        val selected = _uiState.value.selectedIndex ?: _uiState.value.galleryAnchorIndex
        val next = when (_uiState.value.activePrefetchWindow) {
            2 -> 4
            4 -> 8
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

    private fun upsertVideoJob(
        item: GalleryItem,
        state: VideoVrState,
        progress: Float,
        currentFrame: Int = 0,
        totalFrames: Int = 0,
        fps: Int = 30,
        avgFrameMs: Long = 0L,
        finishedAt: Long? = null,
        error: String? = null,
    ) {
        _uiState.update { current ->
            val previous = current.videoJobs.firstOrNull { it.item.cacheKey == item.cacheKey }
            val existing = current.videoJobs.filterNot { it.item.cacheKey == item.cacheKey }
            val job = VideoVrJob(
                item = item,
                state = state,
                progress = progress,
                currentFrame = currentFrame.takeIf { it > 0 } ?: previous?.currentFrame ?: 0,
                totalFrames = totalFrames.takeIf { it > 0 } ?: previous?.totalFrames ?: 0,
                fps = fps.takeIf { it > 0 } ?: previous?.fps ?: 30,
                avgFrameMs = avgFrameMs.takeIf { it > 0L } ?: previous?.avgFrameMs ?: 0L,
                startedAt = previous?.startedAt ?: System.currentTimeMillis(),
                finishedAt = finishedAt,
                error = error,
            )
            current.copy(videoJobs = (listOf(job) + existing).take(100))
        }
    }

    private fun addLog(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _uiState.update { it.copy(logs = (listOf("$stamp $line") + it.logs).take(200)) }
    }

    private fun modelStatusText(settings: AppSettings): String {
        val lang = settings.language
        return listOf(
            "${lang.t("图片模型", "Image model")}：${lang.pickMixed(modelManager.statusText(settings.imageModelId))}",
            "${lang.t("视频模型", "Video model")}：${lang.pickMixed(modelManager.statusText(settings.videoModelId))}",
        ).joinToString("\n")
    }
}

private data class QueuedJob(val photo: PhotoItem, val priority: Int, val sequence: Long)

private data class VideoQueuedJob(val item: GalleryItem, val params: VideoGenerationParams, val sequence: Long)

private class VideoPausedException : RuntimeException("Video generation paused")

private fun AppLanguage.t(zh: String, en: String): String = if (this == AppLanguage.ZH) zh else en

private fun AppLanguage.pickMixed(text: String): String {
    val marker = " / "
    if (!text.contains(marker)) return text
    val parts = text.split(marker, limit = 2)
    return if (this == AppLanguage.ZH) parts.first() else parts.getOrElse(1) { parts.first() }
}

private fun VrState.label(lang: AppLanguage): String = when (this) {
    VrState.NORMAL -> lang.t("原图", "Normal")
    VrState.PAUSED -> lang.t("暂停", "Paused")
    VrState.QUEUED -> lang.t("队列中", "Queued")
    VrState.GENERATING -> lang.t("生成中", "Generating")
    VrState.READY -> lang.t("已生成", "Ready")
    VrState.FAILED -> lang.t("失败", "Failed")
}

private fun VideoVrState.label(lang: AppLanguage): String = when (this) {
    VideoVrState.NORMAL -> lang.t("未生成", "Normal")
    VideoVrState.QUEUED -> lang.t("队列中", "Queued")
    VideoVrState.GENERATING -> lang.t("生成中", "Generating")
    VideoVrState.PAUSED -> lang.t("已暂停", "Paused")
    VideoVrState.READY -> lang.t("已生成", "Ready")
    VideoVrState.FAILED -> lang.t("失败", "Failed")
}

private fun imageSideCount(state: UiState, index: Int, offsetStep: Int, states: Set<VrState>): Int {
    var count = 0
    var cursor = index + offsetStep
    while (cursor in state.photos.indices) {
        val item = state.photos[cursor]
        if (item.kind != MediaKind.IMAGE) {
            cursor += offsetStep
            continue
        }
        val itemState = state.states[item.cacheKey] ?: VrState.NORMAL
        if (itemState !in states) break
        count++
        cursor += offsetStep
    }
    return count
}

private fun imageLoadedLine(state: UiState, index: Int, lang: AppLanguage): String {
    val prev = imageSideCount(state, index, -1, setOf(VrState.READY))
    val next = imageSideCount(state, index, 1, setOf(VrState.READY))
    return lang.t("已加载：前$prev 后$next", "Loaded: prev $prev next $next")
}

private fun imageQueueLine(state: UiState, index: Int, lang: AppLanguage): String {
    val queueStates = setOf(VrState.QUEUED, VrState.GENERATING, VrState.PAUSED)
    val prev = imageWindowCount(state, index, -1, queueStates)
    val next = imageWindowCount(state, index, 1, queueStates)
    return lang.t("队列中：前$prev 后$next", "Queued: prev $prev next $next")
}

private fun imageWindowCount(state: UiState, index: Int, offsetStep: Int, states: Set<VrState>): Int {
    var count = 0
    var seenImages = 0
    var cursor = index + offsetStep
    val window = state.activePrefetchWindow.coerceAtMost(8)
    while (cursor in state.photos.indices && seenImages < window) {
        val item = state.photos[cursor]
        if (item.kind == MediaKind.IMAGE) {
            seenImages++
            val itemState = state.states[item.cacheKey] ?: VrState.NORMAL
            if (itemState in states) count++
        }
        cursor += offsetStep
    }
    return count
}

private fun imageRecentGenerationLines(state: UiState, lang: AppLanguage): List<String> {
    return state.recentGenerations.take(3).map {
        val side = when {
            it.relativeIndex < 0 -> lang.t("前${abs(it.relativeIndex)}", "prev ${abs(it.relativeIndex)}")
            it.relativeIndex > 0 -> lang.t("后${it.relativeIndex}", "next ${it.relativeIndex}")
            else -> lang.t("当前", "current")
        }
        lang.t("${side}已生成（${it.elapsedMs}ms）", "$side ready (${it.elapsedMs}ms)")
    }
}

private fun imagePrefetchSummary(state: UiState, index: Int, lang: AppLanguage): String {
    fun countSide(offsetStep: Int, states: Set<VrState>): Int {
        var count = 0
        var cursor = index + offsetStep
        while (cursor in state.photos.indices) {
            val item = state.photos[cursor]
            if (item.kind != MediaKind.IMAGE) {
                cursor += offsetStep
                continue
            }
            val itemState = state.states[item.cacheKey] ?: VrState.NORMAL
            if (itemState !in states) break
            count++
            cursor += offsetStep
        }
        return count
    }
    val readyPrev = countSide(-1, setOf(VrState.READY))
    val readyNext = countSide(1, setOf(VrState.READY))
    val queuedPrev = countSide(-1, setOf(VrState.QUEUED, VrState.GENERATING, VrState.PAUSED))
    val queuedNext = countSide(1, setOf(VrState.QUEUED, VrState.GENERATING, VrState.PAUSED))
    val last = state.lastGeneration?.let {
        val side = when {
            it.relativeIndex < 0 -> lang.t("前${abs(it.relativeIndex)}", "prev ${abs(it.relativeIndex)}")
            it.relativeIndex > 0 -> lang.t("后${it.relativeIndex}", "next ${it.relativeIndex}")
            else -> lang.t("当前", "current")
        }
        "  ${side}${lang.t("已生成", " ready")}（${it.elapsedMs}ms）"
    }.orEmpty()
    return lang.t(
        "已加载：前$readyPrev 后$readyNext  队列中：前$queuedPrev 后$queuedNext$last",
        "Ready: prev $readyPrev next $readyNext  Queued: prev $queuedPrev next $queuedNext$last",
    )
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
        val oldModelId = prefs.getString("modelId", "depth_anything_v2_small_tflite") ?: "depth_anything_v2_small_tflite"
        return AppSettings(
            language = runCatching { AppLanguage.valueOf(prefs.getString("language", AppLanguage.ZH.name) ?: AppLanguage.ZH.name) }.getOrDefault(AppLanguage.ZH),
            imageModelId = prefs.getString("imageModelId", oldModelId) ?: oldModelId,
            videoModelId = prefs.getString("videoModelId", oldModelId) ?: oldModelId,
            autoPrefetch = prefs.getBoolean("autoPrefetch", true),
            prefetchWindow = prefs.getInt("prefetchWindow", 2),
            depthScale = prefs.getFloat("depthScale", 40f),
            blurRadius = prefs.getInt("blurRadius", 3),
            fillRadius = prefs.getInt("fillRadius", 10),
            invertDepth = invertDepth,
            maxLongEdge = prefs.getInt("maxLongEdge", 6000),
            depthResolution = 518,
            generationWorkers = prefs.getInt("generationWorkers", 1),
            modelThreads = prefs.getInt("modelThreads", 4),
            useGpu = prefs.getBoolean("useGpu", false),
            videoModelThreads = prefs.getInt("videoModelThreads", 4),
            videoUseGpu = prefs.getBoolean("videoUseGpu", false),
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("language", settings.language.name)
            .putString("modelId", settings.imageModelId)
            .putString("imageModelId", settings.imageModelId)
            .putString("videoModelId", settings.videoModelId)
            .putBoolean("autoPrefetch", settings.autoPrefetch)
            .putInt("prefetchWindow", settings.prefetchWindow)
            .putFloat("depthScale", settings.depthScale)
            .putInt("blurRadius", settings.blurRadius)
            .putInt("fillRadius", settings.fillRadius)
            .putBoolean("invertDepth", settings.invertDepth)
            .putBoolean("migratedInvertDefaultOffV5", true)
            .putInt("maxLongEdge", settings.maxLongEdge)
            .putInt("generationWorkers", settings.generationWorkers)
            .putInt("modelThreads", settings.modelThreads)
            .putBoolean("useGpu", settings.useGpu)
            .putInt("videoModelThreads", settings.videoModelThreads)
            .putBoolean("videoUseGpu", settings.videoUseGpu)
            .apply()
    }
}

private class PhotoRepository(private val context: Context) {
    fun loadImages(): List<PhotoItem> = loadMedia()

    fun loadMedia(): List<GalleryItem> {
        val result = mutableListOf<PhotoItem>()
        if (hasImagePermission(context)) {
            result += queryMedia(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                idColumn = MediaStore.Images.Media._ID,
                nameColumn = MediaStore.Images.Media.DISPLAY_NAME,
                widthColumn = MediaStore.Images.Media.WIDTH,
                heightColumn = MediaStore.Images.Media.HEIGHT,
                sizeColumn = MediaStore.Images.Media.SIZE,
                modifiedColumn = MediaStore.Images.Media.DATE_MODIFIED,
                kind = MediaKind.IMAGE,
            )
        }
        if (hasVideoPermission(context)) {
            result += queryMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                idColumn = MediaStore.Video.Media._ID,
                nameColumn = MediaStore.Video.Media.DISPLAY_NAME,
                widthColumn = MediaStore.Video.Media.WIDTH,
                heightColumn = MediaStore.Video.Media.HEIGHT,
                sizeColumn = MediaStore.Video.Media.SIZE,
                modifiedColumn = MediaStore.Video.Media.DATE_MODIFIED,
                kind = MediaKind.VIDEO,
            )
        }
        return result.sortedByDescending { it.modifiedTime }
    }

    private fun queryMedia(
        collection: Uri,
        idColumn: String,
        nameColumn: String,
        widthColumn: String,
        heightColumn: String,
        sizeColumn: String,
        modifiedColumn: String,
        kind: MediaKind,
    ): List<GalleryItem> {
        val projection = arrayOf(idColumn, nameColumn, widthColumn, heightColumn, sizeColumn, modifiedColumn)
        val result = mutableListOf<GalleryItem>()
        context.contentResolver.query(collection, projection, null, null, "$modifiedColumn DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(idColumn)
            val nameCol = cursor.getColumnIndexOrThrow(nameColumn)
            val widthCol = cursor.getColumnIndexOrThrow(widthColumn)
            val heightCol = cursor.getColumnIndexOrThrow(heightColumn)
            val sizeCol = cursor.getColumnIndexOrThrow(sizeColumn)
            val modifiedCol = cursor.getColumnIndexOrThrow(modifiedColumn)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: "${kind.name.lowercase(Locale.US)}-$id"
                if (kind == MediaKind.IMAGE && (displayName.startsWith(GENERATED_VR_PREFIX) || displayName.startsWith("VR_"))) continue
                result += GalleryItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = displayName,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    size = cursor.getLong(sizeCol),
                    modifiedTime = cursor.getLong(modifiedCol),
                    kind = kind,
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

    fun allEntries(photos: List<PhotoItem>): List<ManagedCacheItem> {
        return photos.flatMap { photo ->
            val entries = mutableListOf<VrCacheEntry>()
            readEntry(photo.cacheKey, DEFAULT_VERSION, entryDir(photo, DEFAULT_VERSION))?.let { entries += it }
            File(root, photo.cacheKey).listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
                readEntry(photo.cacheKey, versionDir.name, versionDir)?.let { entries += it }
            }
            entries.map { ManagedCacheItem(photo, it) }
        }.sortedByDescending { it.entry.createdAt }
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

    fun deleteEntry(entry: VrCacheEntry) {
        val photoDir = File(root, entry.photoKey)
        if (entry.version == DEFAULT_VERSION) {
            listOf("vr_sbs.jpg", "depth.png", "params.json", "job.log", "source_preview.jpg").forEach {
                File(photoDir, it).delete()
            }
        } else {
            File(photoDir, entry.version).deleteRecursively()
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

private class VideoVrCacheManager(private val context: Context) {
    val root: File = File(context.getExternalFilesDir(null), "video_vr_cache").also { it.mkdirs() }

    fun entryDir(item: GalleryItem): File = File(root, item.cacheKey).also { it.mkdirs() }

    fun entryDir(item: GalleryItem, version: String): File = File(entryDir(item), version).also { it.mkdirs() }

    fun findEntry(item: GalleryItem): VideoCacheEntry? {
        val base = entryDir(item)
        val entries = mutableListOf<VideoCacheEntry>()
        readEntry(item.cacheKey, base)?.let { entries += it }
        base.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            readEntry(item.cacheKey, dir)?.let { entries += it }
        }
        return entries.maxByOrNull { it.createdAt }
    }

    fun allEntries(items: List<GalleryItem>): List<Pair<GalleryItem, VideoCacheEntry>> {
        return items.filter { it.kind == MediaKind.VIDEO }.mapNotNull { item ->
            findEntry(item)?.let { item to it }
        }.sortedByDescending { it.second.createdAt }
    }

    fun delete(item: GalleryItem) {
        File(root, item.cacheKey).deleteRecursively()
    }

    private fun readEntry(videoKey: String, dir: File): VideoCacheEntry? {
        val output = File(dir, "vr_sbs.mp4")
        val log = File(dir, "job.log")
        val meta = File(dir, "video_params.txt")
        if (!output.exists() || !log.exists() || !meta.exists()) return null
        val values = meta.readLines().associate {
            val parts = it.split("=", limit = 2)
            parts.first() to parts.getOrElse(1) { "" }
        }
        return VideoCacheEntry(
            videoKey = videoKey,
            outputPath = output.absolutePath,
            logPath = log.absolutePath,
            width = values["width"]?.toIntOrNull() ?: 0,
            height = values["height"]?.toIntOrNull() ?: 0,
            durationMs = values["durationMs"]?.toLongOrNull() ?: 0L,
            fps = values["fps"]?.toIntOrNull() ?: 30,
            createdAt = output.lastModified(),
        )
    }
}

private class VideoGenerationNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "视频 VR 生成", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    fun show(item: GalleryItem, frame: Int, total: Int, indeterminate: Boolean, status: String) {
        if (!canNotify()) return
        val progress = if (total > 0) ((frame.toFloat() / total.toFloat()) * 100f).roundToInt().coerceIn(0, 100) else 0
        val text = if (total > 0) "$progress%  $frame / $total  $status" else status
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("视频 VR 生成：$text")
            .setContentText(item.displayName)
            .setSubText(if (total > 0) "$progress%" else null)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${item.displayName}"))
            .setOnlyAlertOnce(true)
            .setOngoing(status != "已完成")
            .setProgress(if (total > 0) total else 100, frame.coerceAtLeast(0), indeterminate)
            .build()
        manager.notify(notificationId(item), notification)
    }

    fun failed(item: GalleryItem, message: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("视频 VR 生成失败")
            .setContentText("${item.displayName}：$message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${item.displayName}\n$message"))
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .build()
        manager.notify(notificationId(item), notification)
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationId(item: GalleryItem): Int = (item.cacheKey.hashCode() and 0x7fffffff).coerceAtLeast(1)

    companion object {
        private const val CHANNEL_ID = "video_vr_generation"
    }
}

private class ModelManager(private val context: Context) {
    private val modelsDir = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    fun statusText(modelId: String = AvailableModels.first().id): String {
        val spec = modelSpec(modelId)
        val modelFile = File(modelsDir, spec.fileName)
        return if (modelFile.exists()) {
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
        val rawDepth = runDepthModel(original, modelFile, params.modelThreads, params.useGpu) { runtime ->
            mark(runtime)
        }
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

    fun generateSbsBitmap(
        source: Bitmap,
        params: VrGenerationParams,
        onModelProgress: (Float) -> Unit = {},
    ): Bitmap {
        val modelFile = modelManager.ensureModel(params.depthModel, onModelProgress)
        val working = if (max(source.width, source.height) > params.maxLongEdge) {
            val scale = params.maxLongEdge.toFloat() / max(source.width, source.height).toFloat()
            Bitmap.createScaledBitmap(source, max(1, (source.width * scale).roundToInt()), max(1, (source.height * scale).roundToInt()), true)
        } else {
            source
        }
        val rawDepth = runDepthModel(working, modelFile, params.modelThreads, params.useGpu)
        val depthSmall = smoothDepth(rawDepth, params.blurRadius, params.invertDepth)
        return makeParallelSbs(working, depthSmall, params.depthScale, params.fillRadius)
    }

    private fun runDepthModel(
        bitmap: Bitmap,
        modelFile: File,
        modelThreads: Int,
        useGpu: Boolean,
        onRuntimeInfo: ((String) -> Unit)? = null,
    ): FloatArray {
        val inputSize = 518
        val model = loadModelFile(modelFile)
        var gpuDelegate: GpuDelegate? = null
        val options = Interpreter.Options().setNumThreads(modelThreads.coerceIn(1, 8))
        if (useGpu) {
            runCatching {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
            }.onFailure {
                gpuDelegate?.close()
                gpuDelegate = null
            }
        }
        var delegateActive = false
        val interpreter = runCatching { Interpreter(model, options) }.onSuccess {
            delegateActive = gpuDelegate != null
        }.getOrElse {
            gpuDelegate?.close()
            gpuDelegate = null
            onRuntimeInfo?.invoke("tflite gpu delegate fallback: ${it.message}")
            Interpreter(model, Interpreter.Options().setNumThreads(modelThreads.coerceIn(1, 8)))
        }
        onRuntimeInfo?.invoke("tflite runtime threads=${modelThreads.coerceIn(1, 8)} requestedGpu=$useGpu delegateActive=$delegateActive")
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
        runCatching {
            interpreter.run(input, output)
        }.onFailure { error ->
            interpreter.close()
            gpuDelegate?.close()
            if (!useGpu) throw error
            val cpuInterpreter = Interpreter(model, Interpreter.Options().setNumThreads(modelThreads.coerceIn(1, 8)))
            input.rewind()
            cpuInterpreter.run(input, output)
            cpuInterpreter.close()
        }
        interpreter.close()
        gpuDelegate?.close()
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

private class VideoVrGenerator(
    private val context: Context,
    private val cache: VideoVrCacheManager,
    private val frameGenerator: VrGenerator,
) {
    fun generate(
        item: GalleryItem,
        params: VideoGenerationParams,
        onModelProgress: (Float) -> Unit,
        onProgress: (Float, Int, Int, Int) -> Unit,
    ): VideoCacheEntry {
        val vrParams = params.toVrParams()
        val version = vrParams.cacheVersion()
        val dir = cache.entryDir(item, version)
        val framesDir = File(dir, "frames").also { it.mkdirs() }
        val output = File(dir, "vr_sbs.mp4")
        val logPath = File(dir, "job.log")
        val metaPath = File(dir, "video_params.txt")
        if (output.exists()) output.delete()
        val log = StringBuilder()
        val started = System.currentTimeMillis()
        fun mark(line: String) {
            log.append(System.currentTimeMillis() - started).append("ms ").append(line).append('\n')
            logPath.writeText(log.toString(), Charsets.UTF_8)
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, item.uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.roundToInt()?.coerceIn(1, 60) ?: 30
        val totalFrames = ((durationMs / 1000f) * fps).roundToInt().coerceAtLeast(1)
        mark("start video=${item.displayName} durationMs=$durationMs fps=$fps frames=$totalFrames version=$version model=${vrParams.depthModel} threads=${vrParams.modelThreads} useGpu=${vrParams.useGpu}")

        val first = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST) ?: error("Unable to decode first video frame")
        val firstFrameCache = File(framesDir, "frame_000000.jpg")
        val firstSbs = if (firstFrameCache.exists()) {
            BitmapFactory.decodeFile(firstFrameCache.absolutePath) ?: frameGenerator.generateSbsBitmap(first.copy(Bitmap.Config.ARGB_8888, false), vrParams, onModelProgress)
        } else {
            frameGenerator.generateSbsBitmap(first.copy(Bitmap.Config.ARGB_8888, false), vrParams, onModelProgress).also { bitmap ->
                FileOutputStream(firstFrameCache).use { bitmap.compress(Bitmap.CompressFormat.JPEG, vrParams.quality, it) }
            }
        }
        val width = even(firstSbs.width)
        val height = even(firstSbs.height)
        first.recycle()
        mark("first frame sbs=${firstSbs.width}x${firstSbs.height} encode=${width}x${height}")

        encodeVideo(
            item = item,
            retriever = retriever,
            firstSbs = firstSbs,
            framesDir = framesDir,
            output = output,
            width = width,
            height = height,
            fps = fps,
            totalFrames = totalFrames,
            params = vrParams,
            onModelProgress = onModelProgress,
            onProgress = onProgress,
            mark = ::mark,
        )
        retriever.release()

        metaPath.writeText(
            listOf(
                "width=$width",
                "height=$height",
                "durationMs=$durationMs",
                "fps=$fps",
                "source=${item.displayName}",
                "depthModel=${vrParams.depthModel}",
                "cacheVersion=$version",
                "modelThreads=${vrParams.modelThreads}",
                "useGpu=${vrParams.useGpu}",
                "maxLongEdge=${vrParams.maxLongEdge}",
            ).joinToString("\n"),
            Charsets.UTF_8,
        )
        mark("done output=${output.absolutePath}")
        return cache.findEntry(item) ?: error("Video output cache missing")
    }

    private fun encodeVideo(
        item: GalleryItem,
        retriever: MediaMetadataRetriever,
        firstSbs: Bitmap,
        framesDir: File,
        output: File,
        width: Int,
        height: Int,
        fps: Int,
        totalFrames: Int,
        params: VrGenerationParams,
        onModelProgress: (Float) -> Unit,
        onProgress: (Float, Int, Int, Int) -> Unit,
        mark: (String) -> Unit,
    ) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, (width * height * fps * 0.08f).roundToInt().coerceAtLeast(2_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val audio = prepareAudioExtractor(item)
        val audioTrack = audio?.format?.let { muxer.addTrack(it) } ?: -1
        var videoTrack = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        fun drain(end: Boolean) {
            if (end) codec.signalEndOfInputStream()
            while (true) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!end) return
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        videoTrack = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val encoded = codec.getOutputBuffer(outputIndex)
                        if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrack, encoded, bufferInfo)
                        }
                        val flags = bufferInfo.flags
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                    }
                }
            }
        }

        try {
            fun cachedOrGenerateFrame(frame: Int): Bitmap? {
                val frameCache = File(framesDir, "frame_${frame.toString().padStart(6, '0')}.jpg")
                BitmapFactory.decodeFile(frameCache.absolutePath)?.let { return it }
                val timeUs = frame * 1_000_000L / fps
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: return null
                val generated = frameGenerator.generateSbsBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false), params, onModelProgress)
                FileOutputStream(frameCache).use { generated.compress(Bitmap.CompressFormat.JPEG, params.quality, it) }
                bitmap.recycle()
                return generated
            }

            for (frame in 0 until totalFrames) {
                val sbs = if (frame == 0) {
                    firstSbs
                } else {
                    cachedOrGenerateFrame(frame) ?: continue
                }
                drawBitmapToSurface(surface, sbs, width, height)
                if (frame != 0) sbs.recycle()
                drain(end = false)
                onProgress((frame + 1).toFloat() / totalFrames.toFloat(), frame + 1, totalFrames, fps)
            }
            drain(end = true)
            if (audio != null && audioTrack >= 0 && muxerStarted) {
                copyAudio(audio.extractor, muxer, audioTrack)
                mark("audio copied")
            }
        } finally {
            firstSbs.recycle()
            surface.release()
            codec.stop()
            codec.release()
            audio?.extractor?.release()
            runCatching { muxer.stop() }
            muxer.release()
        }
    }

    private data class AudioSource(val extractor: MediaExtractor, val format: MediaFormat)

    private fun prepareAudioExtractor(item: GalleryItem): AudioSource? {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, item.uri, null)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                return AudioSource(extractor, format)
            }
        }
        extractor.release()
        return null
    }

    private fun copyAudio(extractor: MediaExtractor, muxer: MediaMuxer, track: Int) {
        val buffer = ByteBuffer.allocateDirect(512 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.set(0, size, extractor.sampleTime.coerceAtLeast(0L), extractor.sampleFlags)
            muxer.writeSampleData(track, buffer, info)
            extractor.advance()
        }
    }

    private fun drawBitmapToSurface(surface: Surface, bitmap: Bitmap, width: Int, height: Int) {
        val canvas = surface.lockCanvas(null)
        try {
            canvas.drawColor(Color.BLACK)
            val scaled = if (bitmap.width == width && bitmap.height == height) bitmap else Bitmap.createScaledBitmap(bitmap, width, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled !== bitmap) scaled.recycle()
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun even(value: Int): Int = if (value % 2 == 0) value else value - 1
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
    var pendingReplaceIndexes by remember { mutableStateOf<List<Int>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        viewModel.onPermissionChanged(
            imageGranted = grants[imagePermission()] == true || hasImagePermission(context),
            videoGranted = grants[videoPermission()] == true || hasVideoPermission(context),
            notificationGranted = grants[notificationPermission()] == true || hasNotificationPermission(context),
        )
    }
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingReplaceIndexes.isNotEmpty()) {
            viewModel.replaceOriginalsWithGenerated(context, pendingReplaceIndexes)
        }
        pendingReplaceIndexes = emptyList()
    }

    LaunchedEffect(state.hasPermission, state.hasVideoPermission, state.hasNotificationPermission) {
        if (!state.hasPermission) {
            launcher.launch(mediaPermissions())
        } else if (!state.hasVideoPermission && Build.VERSION.SDK_INT >= 33) {
            launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
        } else if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= 33) {
            launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    if (!state.hasPermission) {
        PermissionScreen(state.settings.language) { launcher.launch(mediaPermissions()) }
        return
    }

    state.blockingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(message) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(state.settings.language.t("请稍等", "Please wait"))
                }
            },
        )
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
                onOpenGenerated = viewModel::openGeneratedPhoto,
                onOpenGeneratedVideo = viewModel::openGeneratedVideo,
                onSaveVideo = { viewModel.saveGeneratedVideo(context, it) },
                onDeleteVideo = viewModel::deleteGeneratedVideo,
                onSaveVideos = { viewModel.saveGeneratedVideos(context, it) },
                onDeleteVideos = viewModel::deleteGeneratedVideos,
                onRegenerateVideos = viewModel::regenerateVideos,
                onSaveImages = { viewModel.saveGeneratedCopies(context, it) },
                onDeleteImages = viewModel::deleteGeneratedImageEntries,
                onRegenerateImages = viewModel::regenerateImages,
            )
            AppScreen.Settings -> SettingsScreen(
                settings = state.settings,
                updateStatus = state.updateStatus,
                updateUrl = state.updateUrl,
                updateAvailable = state.updateAvailable,
                onBack = viewModel::closeSettings,
                onChange = viewModel::updateSettings,
                onCheckUpdate = viewModel::checkForUpdates,
                onOpenUpdate = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
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
                onSaveGenerated = { viewModel.saveGeneratedCopies(context, it) },
                onReplaceOriginal = { indexes ->
                    if (Build.VERSION.SDK_INT >= 30) {
                        val uris = indexes.mapNotNull { state.photos.getOrNull(it)?.uri }
                        if (uris.isNotEmpty()) {
                            pendingReplaceIndexes = indexes
                            val request = MediaStore.createWriteRequest(context.contentResolver, uris)
                            replaceLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                        }
                    } else {
                        viewModel.replaceOriginalsWithGenerated(context, indexes)
                    }
                },
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
        Text(lang.t("授权读取图片和视频后，可以浏览系统相册，并在本地生成平行眼 SBS VR 缓存。", "Allow image and video access to browse your gallery and build local parallel-eye VR cache."))
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrant) { Text(lang.t("授权图片和视频访问", "Grant access")) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onOpen: (Int, Int, Int) -> Unit,
    onSettings: () -> Unit,
    onManage: () -> Unit,
    onSaveGenerated: (List<Int>) -> Unit,
    onReplaceOriginal: (List<Int>) -> Unit,
) {
    val lang = state.settings.language
    var tileSize by rememberSaveable { mutableStateOf(112f) }
    var lastPinchAt by remember { mutableStateOf(0L) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedIndexes = remember(selectedKeys, state.photos) {
        state.photos.mapIndexedNotNull { index, photo -> if (photo.cacheKey in selectedKeys) index else null }
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = state.galleryScrollIndex.coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = state.galleryScrollOffset.coerceAtLeast(0),
    )
    LaunchedEffect(state.galleryScrollIndex, state.galleryScrollOffset, state.photos.size) {
        if (state.galleryScrollIndex in state.photos.indices) {
            gridState.scrollToItem(state.galleryScrollIndex, state.galleryScrollOffset)
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedKeys.isEmpty()) {
                        Text(lang.t("平行眼 VR 图库", "Parallel VR Gallery"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onManage) { Text(lang.t("管理", "Manage")) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onSettings) { Text(lang.t("设置", "Settings")) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onRefresh) { Text(lang.t("刷新", "Refresh")) }
                    } else {
                        Text(lang.t("已选择 ${selectedKeys.size} 项", "${selectedKeys.size} selected"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { selectedKeys = emptySet() }) { Text(lang.t("取消", "Cancel")) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            onSaveGenerated(selectedIndexes)
                            selectedKeys = emptySet()
                        }) { Text(lang.t("保存", "Save")) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            onReplaceOriginal(selectedIndexes)
                            selectedKeys = emptySet()
                        }) { Text(lang.t("替换", "Replace")) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val prefetch = if (state.settings.autoPrefetch) lang.t("自动：2 -> 4 -> 8", "Auto: 2 -> 4 -> 8") else lang.t("前后各 ${state.settings.prefetchWindow} 张", "${state.settings.prefetchWindow} each side")
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
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val centroid = pressed.centroid()
                                    val previousCentroid = pressed.previousCentroid()
                                    val previousSpan = pressed.previousSpan(previousCentroid).coerceAtLeast(1f)
                                    val zoom = (pressed.span(centroid) / previousSpan).coerceIn(0.85f, 1.18f)
                                    if (abs(zoom - 1f) > 0.01f) {
                                        lastPinchAt = System.currentTimeMillis()
                                        tileSize = (tileSize * zoom).coerceIn(72f, 220f)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
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
                        selected = photo.cacheKey in selectedKeys,
                        onClick = {
                            if (selectedKeys.isNotEmpty()) {
                                selectedKeys = if (photo.cacheKey in selectedKeys) selectedKeys - photo.cacheKey else selectedKeys + photo.cacheKey
                            } else {
                                onOpen(index, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
                            }
                        },
                        onLongClick = {
                            if (System.currentTimeMillis() - lastPinchAt > 700L) {
                                selectedKeys = selectedKeys + photo.cacheKey
                            }
                        },
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
    selected: Boolean,
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
        val displayUri = if (photo.kind == MediaKind.IMAGE) entry?.let { Uri.fromFile(File(it.outputPath)) } ?: photo.uri else photo.uri
        AsyncMediaThumbnail(photo.kind, displayUri, 420, ContentScale.Crop, Modifier.fillMaxSize())
        if (photo.kind == MediaKind.VIDEO) {
            Text(
                text = "▶",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x6638d8b4)))
            Text(
                text = "✓",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopEnd).background(androidx.compose.ui.graphics.Color(0xcc111315)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            text = state.label(lang),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(5.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManageScreen(
    state: UiState,
    onBack: () -> Unit,
    onDeleteVersion: (String) -> Unit,
    onOpenGenerated: (Int, VrCacheEntry) -> Unit,
    onOpenGeneratedVideo: (Int) -> Unit,
    onSaveVideo: (Int) -> Unit,
    onDeleteVideo: (Int) -> Unit,
    onSaveVideos: (List<Int>) -> Unit,
    onDeleteVideos: (List<Int>) -> Unit,
    onRegenerateVideos: (List<Int>) -> Unit,
    onSaveImages: (List<Int>) -> Unit,
    onDeleteImages: (List<ManagedCacheItem>) -> Unit,
    onRegenerateImages: (List<Int>) -> Unit,
) {
    val lang = state.settings.language
    var tab by remember { mutableStateOf("images") }
    var selectedImageKeys by remember { mutableStateOf(setOf<String>()) }
    var selectedVideoKeys by remember { mutableStateOf(setOf<String>()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xfff5f6f7))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text(lang.t("返回", "Back")) }
            Spacer(Modifier.width(12.dp))
            Text(lang.t("生成管理", "Generated Manager"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow {
            listOf("images" to lang.t("图片", "Images"), "videos" to lang.t("视频", "Videos")).forEachIndexed { index, option ->
                SegmentedButton(
                    selected = tab == option.first,
                    onClick = { tab = option.first },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(option.second) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (tab == "videos") {
            val videos = state.photos.filter { item ->
                item.kind == MediaKind.VIDEO &&
                    ((state.videoStates[item.cacheKey] ?: VideoVrState.NORMAL) != VideoVrState.NORMAL ||
                        state.videoEntries.containsKey(item.cacheKey) ||
                        state.videoJobs.any { it.item.cacheKey == item.cacheKey })
            }
            if (selectedVideoKeys.isNotEmpty()) {
                val selectedIndexes = videos.mapNotNull { item -> state.photos.indexOfFirst { it.cacheKey == item.cacheKey }.takeIf { it >= 0 && item.cacheKey in selectedVideoKeys } }
                ManageSelectionActions(
                    count = selectedVideoKeys.size,
                    lang = lang,
                    onClear = { selectedVideoKeys = emptySet() },
                    onSave = { onSaveVideos(selectedIndexes); selectedVideoKeys = emptySet() },
                    onRegenerate = { onRegenerateVideos(selectedIndexes); selectedVideoKeys = emptySet() },
                    onDelete = { onDeleteVideos(selectedIndexes); selectedVideoKeys = emptySet() },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (videos.isEmpty()) {
                Text(lang.t("暂无生成中或已生成的视频", "No generating or generated videos"))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(videos, key = { it.cacheKey }) { item ->
                        val index = state.photos.indexOfFirst { it.cacheKey == item.cacheKey }
                        val videoState = state.videoStates[item.cacheKey] ?: VideoVrState.NORMAL
                        val job = state.videoJobs.firstOrNull { it.item.cacheKey == item.cacheKey }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color(0xff16191c)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .combinedClickable(
                                        enabled = index >= 0,
                                        onClick = {
                                            if (selectedVideoKeys.isNotEmpty()) {
                                                selectedVideoKeys = if (item.cacheKey in selectedVideoKeys) selectedVideoKeys - item.cacheKey else selectedVideoKeys + item.cacheKey
                                            } else {
                                                onOpenGeneratedVideo(index)
                                            }
                                        },
                                        onLongClick = { selectedVideoKeys = selectedVideoKeys + item.cacheKey },
                                    ),
                            ) {
                                AsyncMediaThumbnail(MediaKind.VIDEO, state.videoEntries[item.cacheKey]?.let { Uri.fromFile(File(it.outputPath)) } ?: item.uri, 420, ContentScale.Crop, Modifier.fillMaxSize())
                                if (item.cacheKey in selectedVideoKeys) SelectionBadge()
                                Text(
                                    text = "${videoState.label(lang)} ${(job?.progress?.times(100f) ?: 0f).roundToInt()}%",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomStart).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(5.dp),
                                )
                            }
                            Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { onSaveVideo(index) }, enabled = videoState == VideoVrState.READY && index >= 0, modifier = Modifier.weight(1f)) {
                                    Text(lang.t("保存", "Save"), style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(onClick = { onDeleteVideo(index) }, enabled = videoState == VideoVrState.READY && index >= 0, modifier = Modifier.weight(1f)) {
                                    Text(lang.t("删除", "Delete"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (selectedImageKeys.isNotEmpty()) {
                val selectedItems = state.managedCacheItems.filter { "${it.entry.photoKey}|${it.entry.version}" in selectedImageKeys }
                val selectedIndexes = selectedItems.mapNotNull { item -> state.photos.indexOfFirst { it.cacheKey == item.photoItem.cacheKey }.takeIf { it >= 0 } }.distinct()
                ManageSelectionActions(
                    count = selectedImageKeys.size,
                    lang = lang,
                    onClear = { selectedImageKeys = emptySet() },
                    onSave = { onSaveImages(selectedIndexes); selectedImageKeys = emptySet() },
                    onRegenerate = { onRegenerateImages(selectedIndexes); selectedImageKeys = emptySet() },
                    onDelete = { onDeleteImages(selectedItems); selectedImageKeys = emptySet() },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.cacheVersions.isEmpty()) {
                Text(lang.t("暂无已生成图片", "No generated images yet"))
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    state.cacheVersions.forEach { summary ->
                        val items = state.managedCacheItems.filter { it.entry.version == summary.version }
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(lang.t("版本：${summary.version}", "Version: ${summary.version}"), fontWeight = FontWeight.Bold)
                                    Text(lang.t("${summary.count} 张  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB", "${summary.count} images  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB"), style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { onDeleteVersion(summary.version) }) {
                                    Text(lang.t("删除", "Delete"))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(86.dp),
                                modifier = Modifier.fillMaxWidth().height(260.dp).background(androidx.compose.ui.graphics.Color(0xffffffff)).padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(items, key = { "${it.entry.photoKey}_${it.entry.version}" }) { item ->
                                    val index = state.photos.indexOfFirst { it.cacheKey == item.photoItem.cacheKey }
                                    val selectionKey = "${item.entry.photoKey}|${item.entry.version}"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(androidx.compose.ui.graphics.Color(0xff16191c))
                                            .combinedClickable(
                                                enabled = index >= 0,
                                                onClick = {
                                                    if (selectedImageKeys.isNotEmpty()) {
                                                        selectedImageKeys = if (selectionKey in selectedImageKeys) selectedImageKeys - selectionKey else selectedImageKeys + selectionKey
                                                    } else {
                                                        onOpenGenerated(index, item.entry)
                                                    }
                                                },
                                                onLongClick = { selectedImageKeys = selectedImageKeys + selectionKey },
                                            ),
                                    ) {
                                        AsyncBitmapImage(Uri.fromFile(File(item.entry.outputPath)), 360, ContentScale.Crop, Modifier.fillMaxSize())
                                        if (selectionKey in selectedImageKeys) SelectionBadge()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageSelectionActions(
    count: Int,
    lang: AppLanguage,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(lang.t("已选 $count", "$count selected"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onClear) { Text(lang.t("取消", "Cancel")) }
        OutlinedButton(onClick = onSave) { Text(lang.t("保存", "Save")) }
        OutlinedButton(onClick = onRegenerate) { Text(lang.t("重新生成", "Regenerate")) }
        Button(onClick = onDelete) { Text(lang.t("删除", "Delete")) }
    }
}

@Composable
private fun SelectionBadge() {
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x6638d8b4))) {
        Text(
            text = "✓",
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.TopEnd).background(androidx.compose.ui.graphics.Color(0xcc111315)).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    updateStatus: String?,
    updateUrl: String?,
    updateAvailable: Boolean,
    onBack: () -> Unit,
    onChange: (AppSettings) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenUpdate: (String) -> Unit,
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

        SettingsSectionTitle(lang.t("公共设置", "General"))
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

        OutlinedButton(onClick = onCheckUpdate) {
            Text(lang.t("检查更新", "Check for updates"))
        }
        updateStatus?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        if (updateAvailable) updateUrl?.let { url ->
            Spacer(Modifier.height(6.dp))
            Button(onClick = { onOpenUpdate(url) }) {
                Text(lang.t("下载更新", "Download update"))
            }
        }
        Spacer(Modifier.height(16.dp))

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

        SettingsSectionTitle(lang.t("图片生成设置", "Image generation"))
        ModelPicker(
            title = lang.t("图片模型选择", "Image model"),
            selectedModelId = settings.imageModelId,
            lang = lang,
            onSelect = { spec -> onChange(settings.copy(imageModelId = spec.id, depthResolution = spec.inputSize)) },
        )
        Text(lang.t("实验模型首次使用会单独下载；如果模型输入输出不兼容，会在日志里显示失败原因。", "Experimental models download separately on first use. Shape incompatibility is reported in logs."), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        Text(lang.t("预加载", "Prefetch"), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow {
            val options = listOf("auto", "2", "4", "8")
            options.forEachIndexed { index, option ->
                val selected = if (option == "auto") settings.autoPrefetch else !settings.autoPrefetch && settings.prefetchWindow == option.toInt()
                SegmentedButton(
                    selected = selected,
                    onClick = {
                        if (option == "auto") onChange(settings.copy(autoPrefetch = true, prefetchWindow = 2))
                        else onChange(settings.copy(autoPrefetch = false, prefetchWindow = option.toInt()))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(if (option == "auto") lang.t("自动", "Auto") else option) }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingInt(lang.t("后台 worker 数", "Background workers"), settings.generationWorkers, listOf(1, 2, 3)) {
            onChange(settings.copy(generationWorkers = it))
        }
        SettingInt(lang.t("模型 CPU 线程", "Model CPU threads"), settings.modelThreads, listOf(1, 2, 4, 8)) {
            onChange(settings.copy(modelThreads = it))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.useGpu,
                onCheckedChange = { onChange(settings.copy(useGpu = it)) },
            )
            Text(lang.t("尝试 GPU 加速", "Try GPU acceleration"))
        }
        Text(lang.t("GPU 会尝试使用 TFLite GPU Delegate；不兼容时自动回退 CPU。", "GPU uses TFLite GPU Delegate when compatible and falls back to CPU if needed."), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        SettingsSectionTitle(lang.t("视频生成设置", "Video generation"))
        ModelPicker(
            title = lang.t("视频模型选择", "Video model"),
            selectedModelId = settings.videoModelId,
            lang = lang,
            onSelect = { spec -> onChange(settings.copy(videoModelId = spec.id)) },
        )
        SettingInt(lang.t("视频模型 CPU 线程", "Video model CPU threads"), settings.videoModelThreads, listOf(1, 2, 4, 8)) {
            onChange(settings.copy(videoModelThreads = it))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.videoUseGpu,
                onCheckedChange = { onChange(settings.copy(videoUseGpu = it)) },
            )
            Text(lang.t("视频生成尝试 GPU 加速", "Try GPU for video generation"))
        }
        Text(lang.t("视频设置只影响视频 VR 生成；图片仍使用上面的图片模型设置。", "Video settings only affect video VR generation; images use the image settings above."), style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))
        Text(lang.t("深度图分辨率", "Depth resolution"), fontWeight = FontWeight.Bold)
        val imageSpec = modelSpec(settings.imageModelId)
        val videoSpec = modelSpec(settings.videoModelId)
        Text(lang.t("图片 ${imageSpec.inputSize} x ${imageSpec.inputSize}；视频 ${videoSpec.inputSize} x ${videoSpec.inputSize}（由所选模型固定）", "Image ${imageSpec.inputSize} x ${imageSpec.inputSize}; video ${videoSpec.inputSize} x ${videoSpec.inputSize}. Fixed by selected models."), style = MaterialTheme.typography.bodySmall)
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
private fun SettingsSectionTitle(title: String) {
    HorizontalDivider(Modifier.padding(vertical = 10.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ModelPicker(
    title: String,
    selectedModelId: String,
    lang: AppLanguage,
    onSelect: (ModelSpec) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AvailableModels.forEach { spec ->
            val selected = selectedModelId == spec.id
            val buttonModifier = Modifier.fillMaxWidth()
            if (selected) {
                Button(onClick = { onSelect(spec) }, modifier = buttonModifier) {
                    Text(spec.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                OutlinedButton(onClick = { onSelect(spec) }, modifier = buttonModifier) {
                    Text(spec.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    Text(
        lang.t("当前选择会在首次生成前自动下载。", "The selected model downloads automatically before first generation."),
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(10.dp))
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
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = state.photos[page]
            if (photo.kind == MediaKind.VIDEO) {
                val generated = state.videoEntries[photo.cacheKey]
                val job = state.videoJobs.firstOrNull { it.item.cacheKey == photo.cacheKey }
                val videoState = state.videoStates[photo.cacheKey] ?: VideoVrState.NORMAL
                VideoPlayer(
                    uri = generated?.let { Uri.fromFile(File(it.outputPath)) } ?: photo.uri,
                    modifier = Modifier.fillMaxSize(),
                    controlsVisible = controlsVisible,
                    statusText = "状态：${videoState.label(lang)}  ${job?.currentFrame ?: 0}/${job?.totalFrames ?: 0}  帧生成${job?.avgFrameMs ?: 0}ms",
                    onSingleTap = { controlsVisible = !controlsVisible },
                )
            } else {
                val entry = state.entries[photo.cacheKey]
                val vrState = state.states[photo.cacheKey] ?: VrState.NORMAL
                if (state.vrMode && entry != null) {
                    SyncSbsZoomImage(Uri.fromFile(File(entry.outputPath)), Modifier.fillMaxSize()) {
                        controlsVisible = !controlsVisible
                    }
                } else {
                    AsyncBitmapImage(
                        photo.uri,
                        4096,
                        ContentScale.Fit,
                        Modifier
                            .fillMaxSize()
                            .pointerInput(photo.uri) {
                                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                            },
                    )
                    if (state.vrMode && vrState != VrState.READY) {
                        StatusOverlay(vrState, lang, onRetry = { onRetry(page) })
                    }
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
                val currentItem = state.photos.getOrNull(pagerState.currentPage)
                Text(
                    text = currentItem?.displayName.orEmpty(),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onVr(pagerState.currentPage) }) {
                    Text(
                        if (currentItem?.kind == MediaKind.VIDEO) {
                            when (state.videoStates[currentItem.cacheKey] ?: VideoVrState.NORMAL) {
                                VideoVrState.NORMAL -> lang.t("加入 VR 队列", "Add to VR queue")
                                VideoVrState.QUEUED -> lang.t("暂停生成", "Pause")
                                VideoVrState.GENERATING -> lang.t("暂停生成", "Pause")
                                VideoVrState.PAUSED -> lang.t("继续生成", "Resume")
                                VideoVrState.READY -> lang.t("已生成", "Ready")
                                VideoVrState.FAILED -> lang.t("重试视频 VR", "Retry video VR")
                            }
                        } else if (state.vrMode) {
                            lang.t("关闭 VR", "VR Off")
                        } else {
                            lang.t("开启 VR", "VR On")
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onOpenDebug(pagerState.currentPage) }) { Text(lang.t("调试", "Debug")) }
            }
            val current = state.photos.getOrNull(pagerState.currentPage)
            if (current?.kind != MediaKind.VIDEO) Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0x99000000)).padding(10.dp),
            ) {
                if (current != null) {
                    Text(
                        text = imageLoadedLine(state, pagerState.currentPage, lang),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = imageQueueLine(state, pagerState.currentPage, lang),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val recentLines = imageRecentGenerationLines(state, lang)
                    repeat(3) { lineIndex ->
                        Text(
                            text = recentLines.getOrNull(lineIndex).orEmpty(),
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
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
    val isVideo = photo?.kind == MediaKind.VIDEO
    val videoEntry = photo?.let { state.videoEntries[it.cacheKey] }
    val videoJob = photo?.let { p -> state.videoJobs.firstOrNull { it.item.cacheKey == p.cacheKey } }

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
            Button(onClick = onShare, enabled = !isVideo && entry != null) { Text(lang.t("分享调试包", "Share debug package")) }
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
        if (isVideo) {
            Column(Modifier.weight(1f).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xff202326)).padding(10.dp)) {
                Text("视频队列 / Video queue", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                Text("State: ${photo?.let { state.videoStates[it.cacheKey] } ?: VideoVrState.NORMAL}", color = androidx.compose.ui.graphics.Color.White)
                Text("Frame: ${videoJob?.currentFrame ?: 0}/${videoJob?.totalFrames ?: 0}", color = androidx.compose.ui.graphics.Color.White)
                Text("FPS: ${videoJob?.fps ?: videoEntry?.fps ?: 30}", color = androidx.compose.ui.graphics.Color.White)
                Text("Progress: ${((videoJob?.progress ?: 0f) * 100f).roundToInt()}%", color = androidx.compose.ui.graphics.Color.White)
                Text("Output: ${videoEntry?.outputPath ?: "-"}", color = androidx.compose.ui.graphics.Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Error: ${videoJob?.error ?: "-"}", color = androidx.compose.ui.graphics.Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DebugImagePanel(lang.t("原图", "Source"), photo?.uri, Modifier.weight(1f), lang)
                DebugImagePanel(lang.t("深度图", "Depth"), entry?.let { Uri.fromFile(File(it.depthPath)) }, Modifier.weight(1f), lang)
                DebugImagePanel("VR SBS", entry?.let { Uri.fromFile(File(it.outputPath)) }, Modifier.weight(1f), lang)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(androidx.compose.ui.graphics.Color(0xff202326))
                .padding(8.dp),
        ) {
            Text(lang.t("日志", "Logs"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            Text("${lang.t("模型", "Model")}: ${lang.pickMixed(state.modelStatus)}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            job?.let {
                Text("Job: ${it.state} progress=${(it.progress * 100f).roundToInt()}% error=${it.error.orEmpty()}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Text(lang.t("队列", "Queue"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            state.jobs.take(5).forEach {
                Text(
                    "${it.state.label(lang)} ${(it.progress * 100f).roundToInt()}%  ${it.photoItem.displayName}",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
private fun SyncSbsZoomImage(uri: Uri, modifier: Modifier = Modifier, onTap: () -> Unit) {
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
    val animatedScale by animateFloatAsState(targetValue = scale, label = "sbsScale")
    val animatedOffsetX by animateFloatAsState(targetValue = offset.x, label = "sbsOffsetX")
    val animatedOffsetY by animateFloatAsState(targetValue = offset.y, label = "sbsOffsetY")
    val imageModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationX = animatedOffsetX
            translationY = animatedOffsetY
        }

    Box(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.Black)
            .pointerInput(uri) {
                awaitPointerEventScope {
                    while (true) {
                        val firstEvent = awaitPointerEvent()
                        if (firstEvent.changes.none { it.pressed }) continue
                        var multiTouch = false
                        var moved = false
                        var firstPosition: Offset? = null
                        var lastCentroid: Offset? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (firstPosition == null) firstPosition = pressed.firstOrNull()?.position
                            if (pressed.isEmpty()) {
                                if (!multiTouch && !moved) onTap()
                                break
                            }
                            if (pressed.size >= 2) {
                                multiTouch = true
                                val centroid = pressed.centroid()
                                val previousCentroid = lastCentroid ?: pressed.previousCentroid()
                                val previousSpan = pressed.previousSpan(previousCentroid).coerceAtLeast(1f)
                                val zoom = (pressed.span(centroid) / previousSpan).coerceIn(0.85f, 1.18f)
                                val pan = lastCentroid?.let { centroid - it } ?: Offset.Zero
                                val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                scale = nextScale
                                offset = if (nextScale == 1f) Offset.Zero else offset + pan
                                lastCentroid = centroid
                                event.changes.forEach { it.consume() }
                            } else {
                                val start = firstPosition
                                val current = pressed.first().position
                                if (start != null && (current - start).getDistance() > 18f) moved = true
                            }
                        }
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize().clipToBounds().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.CenterEnd) {
                Image(left, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Fit)
            }
            Box(Modifier.weight(1f).fillMaxSize().clipToBounds().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.CenterStart) {
                Image(right, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Fit)
            }
        }
        Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99ffffff)))
    }
}

private fun List<PointerInputChange>.centroid(): Offset {
    if (isEmpty()) return Offset.Zero
    val x = sumOf { it.position.x.toDouble() }.toFloat() / size
    val y = sumOf { it.position.y.toDouble() }.toFloat() / size
    return Offset(x, y)
}

private fun List<PointerInputChange>.previousCentroid(): Offset {
    if (isEmpty()) return Offset.Zero
    val x = sumOf { it.previousPosition.x.toDouble() }.toFloat() / size
    val y = sumOf { it.previousPosition.y.toDouble() }.toFloat() / size
    return Offset(x, y)
}

private fun List<PointerInputChange>.span(center: Offset): Float {
    if (isEmpty()) return 1f
    return map { (it.position - center).getDistance() }.average().toFloat()
}

private fun List<PointerInputChange>.previousSpan(center: Offset): Float {
    if (isEmpty()) return 1f
    return map { (it.previousPosition - center).getDistance() }.average().toFloat()
}

@Composable
private fun WindowSelector(value: Int, onChange: (Int) -> Unit) {
    val options = listOf(2, 4, 8)
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

@Composable
private fun AsyncMediaThumbnail(kind: MediaKind, uri: Uri, maxSide: Int, contentScale: ContentScale, modifier: Modifier = Modifier) {
    if (kind == MediaKind.IMAGE) {
        AsyncBitmapImage(uri, maxSide, contentScale, modifier)
        return
    }
    val context = LocalContext.current
    var bitmap by remember(uri, maxSide) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxSide) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(uri, Size(maxSide, maxSide), null)
                } else {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    frame
                }
            }.getOrNull()
        }
    }
    if (bitmap == null) {
        Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(26.dp))
        }
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    controlsVisible: Boolean,
    statusText: String,
    onSingleTap: () -> Unit,
) {
    val context = LocalContext.current
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var longPressRunnable by remember { mutableStateOf<Runnable?>(null) }
    var longPressActive by remember { mutableStateOf(false) }
    var progress by remember(uri) { mutableStateOf(0f) }
    var positionText by remember(uri) { mutableStateOf("00:00 / 00:00") }
    var hint by remember(uri) { mutableStateOf<String?>(null) }
    var isPlaying by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri, videoView) {
        while (true) {
            val view = videoView
            val duration = view?.duration?.takeIf { it > 0 } ?: 0
            val position = view?.currentPosition ?: 0
            isPlaying = view?.isPlaying == true
            progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            positionText = "${formatDuration(position)} / ${formatDuration(duration)}"
            delay(350)
        }
    }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(900)
            hint = null
        }
    }

    Box(modifier.background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView(
            factory = {
                VideoView(context).apply {
                    setVideoURI(uri)
                    setOnPreparedListener { player ->
                        mediaPlayer = player
                        player.isLooping = true
                        start()
                        isPlaying = true
                    }
                    videoView = this
                }
            },
            update = { view ->
                if (view.tag != uri) {
                    view.tag = uri
                    view.setVideoURI(uri)
                    view.start()
                }
                videoView = view
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(uri) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                            val view = videoView
                            longPressActive = false
                            val runnable = Runnable {
                                longPressActive = true
                                hint = "2 倍速"
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= 23) mediaPlayer?.let { it.playbackParams = it.playbackParams.setSpeed(2f) }
                                }
                            }
                            longPressRunnable = runnable
                            view?.postDelayed(runnable, 450L)
                            var moved = false
                            val start = down.position
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isNotEmpty() && (pressed.first().position - start).getDistance() > 18f) moved = true
                                if (pressed.isEmpty()) break
                            }
                            longPressRunnable?.let { view?.removeCallbacks(it) }
                            longPressRunnable = null
                            if (longPressActive) {
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= 23) mediaPlayer?.let { it.playbackParams = it.playbackParams.setSpeed(1f) }
                                }
                                hint = "恢复正常速度"
                                longPressActive = false
                                continue
                            }
                            if (!moved) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapAt < 280L) {
                                    val player = mediaPlayer
                                    val duration = (player?.duration ?: view?.duration ?: 0).coerceAtLeast(0)
                                    val current = (player?.currentPosition ?: view?.currentPosition ?: 0).coerceAtLeast(0)
                                    val target = (current + 5_000).let { if (duration > 0) it.coerceAtMost(duration) else it }
                                    runCatching {
                                        if (Build.VERSION.SDK_INT >= 26 && player != null) {
                                            player.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
                                        } else {
                                            view?.seekTo(target)
                                        }
                                    }
                                    progress = if (duration > 0) (target.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else progress
                                    positionText = "${formatDuration(target)} / ${formatDuration(duration)}"
                                    hint = "快进 5 秒"
                                    lastTapAt = 0L
                                } else {
                                    lastTapAt = now
                                    onSingleTap()
                                }
                            }
                        }
                    }
                },
        )
        hint?.let {
            Text(
                text = it,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(androidx.compose.ui.graphics.Color(0xaa000000))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val view = videoView
                            if (view?.isPlaying == true) {
                                view.pause()
                                isPlaying = false
                            } else {
                                view?.start()
                                isPlaying = true
                            }
                        },
                    ) {
                        Text(if (isPlaying) "暂停" else "播放")
                    }
                    Text(
                        text = statusText,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(positionText, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = progress,
                    onValueChange = { value ->
                        val view = videoView
                        val duration = (mediaPlayer?.duration ?: view?.duration ?: 0).coerceAtLeast(0)
                        progress = value.coerceIn(0f, 1f)
                        if (duration > 0) {
                            val target = (duration * progress).roundToInt().coerceIn(0, duration)
                            runCatching {
                                if (Build.VERSION.SDK_INT >= 26 && mediaPlayer != null) {
                                    mediaPlayer?.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
                                } else {
                                    view?.seekTo(target)
                                }
                            }
                            positionText = "${formatDuration(target)} / ${formatDuration(duration)}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

private fun hasImagePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, imagePermission()) == PackageManager.PERMISSION_GRANTED
}

private fun hasVideoPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, videoPermission()) == PackageManager.PERMISSION_GRANTED
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun imagePermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun videoPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun notificationPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun mediaPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
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

private fun saveVideoToGallery(context: Context, source: File, displayName: String): Uri {
    val safeName = displayName.ifBlank { "parallel_vr_${System.currentTimeMillis()}.mp4" }
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, safeName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
        put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ParallelVrGallery")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
    runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Unable to open video output")
        if (Build.VERSION.SDK_INT >= 29) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
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
          "modelThreads": $modelThreads,
          "useGpu": $useGpu,
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
    val gpu = if (useGpu) "gpu1" else "gpu0"
    return "${depthModel}_s${scale}_b${blurRadius}_f${fillRadius}_${invert}_m${maxLongEdge}_t${modelThreads}_$gpu"
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
