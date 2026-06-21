package com.local.parallelvrgallery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
import kotlin.math.sqrt

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
    val bucketId: String = "",
    val bucketName: String = "",
    val relativePath: String = "",
    val forcedCacheKey: String? = null,
    val generatedVirtual: Boolean = false,
) {
    val cacheKey: String
        get() = forcedCacheKey ?: if (kind == MediaKind.IMAGE) "${id}_${size}_${modifiedTime}" else "VIDEO_${id}_${size}_${modifiedTime}"
}

typealias GalleryItem = PhotoItem

data class AlbumItem(
    val bucketId: String,
    val name: String,
    val relativePath: String,
    val coverUri: Uri,
    val coverKind: MediaKind,
    val count: Int,
    val latestModified: Long,
)

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
    val mirrors: List<String> = emptyList(),
    val sha256: String,
)

private val AvailableModels = listOf(
    ModelSpec(
        id = "depth_anything_v2_small_tflite",
        displayName = "Depth Anything V2 Small TFLite 518（稳定）",
        inputSize = 518,
        fileName = "depth_anything_v2.tflite",
        url = "https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite",
        mirrors = listOf(
            "https://gh-proxy.com/https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite",
            "https://ghproxy.net/https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite",
        ),
        sha256 = "B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F",
    ),
    ModelSpec(
        id = "qualcomm_depth_anything_v2_tflite",
        displayName = "Qualcomm Depth Anything V2 TFLite 518（实验）",
        inputSize = 518,
        fileName = "qualcomm_depth_anything_v2.tflite",
        url = "https://huggingface.co/qualcomm/Depth-Anything-V2/resolve/main/Depth-Anything-V2.tflite?download=true",
        mirrors = listOf(
            "https://hf-mirror.com/qualcomm/Depth-Anything-V2/resolve/main/Depth-Anything-V2.tflite?download=true",
        ),
        sha256 = "727E025EAB1DB3650C6FED86AA8D7932B994D8746E41A7A5773E663DE740859F",
    ),
)

private const val GENERATED_VR_PREFIX = "PVG_VR_"
private const val INITIAL_MEDIA_LIMIT = 1800
private const val ALBUM_PAGE_SIZE = 1200
private const val ALL_PAGE_SIZE = 1200
private const val VIDEO_ENCODER_VERSION = "encoderV2"

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
    val currentFrameMs: Long = 0L,
    val avgFrameMs: Long = 0L,
    val modelId: String = "",
    val cacheVersion: String = "",
    val modelThreads: Int = 0,
    val useGpu: Boolean = false,
    val runtimeInfo: String = "",
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)

data class GenerationTimings(
    val decodeMs: Long = 0L,
    val modelMs: Long = 0L,
    val depthPostMs: Long = 0L,
    val sbsMs: Long = 0L,
    val writeMs: Long = 0L,
    val totalMs: Long = 0L,
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
    val viewerOrigin: ViewerOrigin = ViewerOrigin.NORMAL,
    val manageViewerKeys: Set<String> = emptySet(),
    val viewerScopeKeys: Set<String> = emptySet(),
    val galleryAnchorIndex: Int = 0,
    val galleryScrollIndex: Int = 0,
    val galleryScrollOffset: Int = 0,
    val galleryAnchorSlot: Int = 0,
    val settingsOpen: Boolean = false,
    val manageOpen: Boolean = false,
    val homeTab: String = "all",
    val generatedTab: String = "images",
    val allColumns: Int = 5,
    val albumListColumns: Int = 5,
    val albumDetailColumns: Int = 5,
    val generatedColumns: Int = 5,
    val expandedGeneratedVersions: Set<String> = emptySet(),
    val allOffset: Int = 0,
    val allLoading: Boolean = false,
    val allExhausted: Boolean = false,
    val selectedAlbumId: String? = null,
    val albumOffsets: Map<String, Int> = emptyMap(),
    val albumLoading: Boolean = false,
    val albums: List<AlbumItem> = emptyList(),
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

private data class MediaSnapshot(
    val photos: List<PhotoItem>,
    val imageCount: Int,
    val videoCount: Int,
    val albums: List<AlbumItem>,
    val entries: Map<String, VrCacheEntry>,
    val managedItems: List<ManagedCacheItem>,
    val videoEntries: Map<String, VideoCacheEntry>,
    val cacheVersions: List<CacheVersionSummary>,
)

private sealed class TimelineCell {
    data class Header(val day: Long, val label: String) : TimelineCell()
    data class Media(val item: PhotoItem) : TimelineCell()
    data class Footer(val loaded: Int, val total: Int, val text: String? = null) : TimelineCell()
}

private enum class QueueSource {
    ALL,
    ALBUM,
    GENERATED,
}

enum class ViewerOrigin {
    NORMAL,
    GENERATED_TAB,
    MANAGE_MODAL,
}

private data class QueueContext(val source: QueueSource, val albumId: String?)

private data class QueueTag(
    val photoKey: String,
    val source: QueueSource,
    val albumId: String?,
    val priority: Int,
    val state: VrState,
    val updatedAt: Long,
)

private data class VideoQueueTag(
    val videoKey: String,
    val state: VideoVrState,
    val params: VideoGenerationParams?,
    val updatedAt: Long,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PhotoRepository(app)
    private val cache = VrCacheManager(app)
    private val videoCache = VideoVrCacheManager(app)
    private val videoNotifier = VideoGenerationNotifier(app)
    private val videoKeepAlive = VideoGenerationKeepAlive(app)
    private val modelManager = ModelManager(app)
    private val generator = VrGenerator(app, cache, modelManager)
    private val videoGenerator = VideoVrGenerator(app, videoCache, generator)
    private val settingsStore = SettingsStore(app)
    private val queueTags = QueueTagStore(app)
    private val videoQueueTags = VideoQueueTagStore(app)
    private val pending = PriorityQueue<QueuedJob>(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
    private val currentPending = PriorityQueue<QueuedJob>(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
    private val videoPending = PriorityQueue<VideoQueuedJob>(compareBy<VideoQueuedJob> { it.sequence })
    private val paused = linkedMapOf<String, QueuedJob>()
    private val pausedVideos = mutableSetOf<String>()
    private val pausedVideoParams = mutableMapOf<String, VideoGenerationParams>()
    private val activeVideoParams = mutableMapOf<String, VideoGenerationParams>()
    private val videoRunTokens = mutableMapOf<String, Long>()
    private var sequence = 0L
    private var videoSequence = 0L
    private var restoredQueueTags = false
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
            val snapshot = withContext(Dispatchers.IO) {
                val systemPhotos = repository.loadMedia(INITIAL_MEDIA_LIMIT)
                val imageItems = systemPhotos.filter { it.kind == MediaKind.IMAGE }
                val videoItems = systemPhotos.filter { it.kind == MediaKind.VIDEO }
                val photoByKey = imageItems.associateBy { it.cacheKey }
                val videoByKey = videoItems.associateBy { it.cacheKey }
                val managedItems = cache.allEntriesByKey(photoByKey)
                val entries = managedItems
                    .groupBy { it.entry.photoKey }
                    .mapValues { (_, items) -> items.maxBy { it.entry.createdAt }.entry }
                val videoPairs = videoCache.allEntriesByKey(videoByKey)
                val videoEntries = videoPairs.associate { it.first.cacheKey to it.second }
                val virtualItems = (managedItems.map { it.photoItem } + videoPairs.map { it.first })
                    .filter { it.generatedVirtual }
                    .distinctBy { it.cacheKey }
                val photos = systemPhotos + virtualItems
                MediaSnapshot(
                    photos = photos,
                    imageCount = imageItems.size,
                    videoCount = videoItems.size,
                    albums = buildAlbums(systemPhotos),
                    entries = entries,
                    managedItems = managedItems,
                    videoEntries = videoEntries,
                    cacheVersions = cache.summaries(),
                )
            }
            _uiState.update {
                val readyVideoStates = snapshot.videoEntries.keys.associateWith { VideoVrState.READY }
                val activeVideoStates = it.videoStates.filterValues { state -> state != VideoVrState.NORMAL } - readyVideoStates.keys
                val snapshotKeys = snapshot.photos.map { item -> item.cacheKey }.toSet()
                val activeVideoItems = it.videoJobs
                    .filter { job -> job.state != VideoVrState.READY || job.item.cacheKey !in snapshot.videoEntries }
                    .map { job -> if (job.item.cacheKey in snapshotKeys) job.item else job.item.copy(generatedVirtual = true) }
                val mergedPhotos = (snapshot.photos + activeVideoItems)
                    .distinctBy { item -> item.cacheKey }
                it.copy(
                    photos = mergedPhotos,
                    albums = snapshot.albums,
                    entries = snapshot.entries,
                    states = snapshot.entries.keys.associateWith { VrState.READY },
                    videoStates = activeVideoStates + readyVideoStates,
                    videoEntries = snapshot.videoEntries,
                    allOffset = mergedPhotos.count { !it.generatedVirtual },
                    allExhausted = mergedPhotos.count { !it.generatedVirtual } < INITIAL_MEDIA_LIMIT,
                    loading = false,
                    message = "已加载最近 ${mergedPhotos.count { !it.generatedVirtual }} 项媒体，生成库 ${snapshot.managedItems.size + snapshot.videoEntries.size} 项 / Loaded recent media and generated cache",
                    cacheVersions = snapshot.cacheVersions,
                    managedCacheItems = snapshot.managedItems,
                    modelStatus = modelStatusText(it.settings),
                )
            }
            if (!restoredQueueTags) {
                restoredQueueTags = true
                withContext(Dispatchers.IO) {
                    restoreQueueTags()
                    restoreVideoQueueTags()
                }
            }
            viewModelScope.launch {
                val fullAlbums = withContext(Dispatchers.IO) { repository.loadAlbums() }
                _uiState.update { it.copy(albums = fullAlbums) }
            }
        }
    }

    fun loadMoreAllMedia() {
        val state = _uiState.value
        if (state.allLoading || state.allExhausted) return
        viewModelScope.launch {
            val offset = _uiState.value.allOffset
            _uiState.update { it.copy(allLoading = true) }
            val page = withContext(Dispatchers.IO) { repository.loadMedia(ALL_PAGE_SIZE, offset) }
            _uiState.update { current ->
                val virtualItems = current.photos.filter { it.generatedVirtual }
                val systemItems = (current.photos.filterNot { it.generatedVirtual } + page)
                    .distinctBy { it.cacheKey }
                    .sortedByDescending { it.modifiedTime }
                current.copy(
                    photos = systemItems + virtualItems,
                    allOffset = offset + page.size,
                    allLoading = false,
                    allExhausted = page.size < ALL_PAGE_SIZE,
                    message = current.settings.language.t("全部页已加载 ${systemItems.size} 项", "All page loaded ${systemItems.size} items"),
                )
            }
        }
    }

    fun openPhoto(index: Int, firstVisibleIndex: Int = index, firstVisibleOffset: Int = 0) {
        openPhoto(index, firstVisibleIndex, firstVisibleOffset, emptySet())
    }

    fun openScopedPhoto(index: Int, scopeKeys: Set<String>, firstVisibleIndex: Int = index, firstVisibleOffset: Int = 0) {
        openPhoto(index, firstVisibleIndex, firstVisibleOffset, scopeKeys)
    }

    private fun openPhoto(index: Int, firstVisibleIndex: Int, firstVisibleOffset: Int, scopeKeys: Set<String>) {
        val item = _uiState.value.photos.getOrNull(index)
        _uiState.update {
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = firstVisibleIndex,
                galleryScrollOffset = firstVisibleOffset,
                galleryAnchorSlot = (index - firstVisibleIndex).coerceAtLeast(0),
                vrMode = true,
                viewerOrigin = ViewerOrigin.NORMAL,
                manageViewerKeys = emptySet(),
                viewerScopeKeys = scopeKeys,
                message = null,
            )
        }
        if (item?.kind == MediaKind.IMAGE) enqueueWindow(index, includeCurrent = true)
    }

    fun openGeneratedPhoto(index: Int, entry: VrCacheEntry? = null) {
        val photo = _uiState.value.photos.getOrNull(index)
        _uiState.update {
            val keys = it.managedCacheItems.map { item -> item.photoItem.cacheKey }.toSet()
            val origin = if (it.manageOpen) ViewerOrigin.MANAGE_MODAL else ViewerOrigin.GENERATED_TAB
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = max(0, index - it.galleryAnchorSlot),
                vrMode = true,
                manageOpen = false,
                homeTab = if (origin == ViewerOrigin.GENERATED_TAB) "generated" else it.homeTab,
                generatedTab = "images",
                viewerOrigin = origin,
                manageViewerKeys = keys + listOfNotNull(photo?.cacheKey),
                viewerScopeKeys = keys + listOfNotNull(photo?.cacheKey),
                entries = if (photo != null && entry != null) it.entries + (photo.cacheKey to entry) else it.entries,
                message = null,
            )
        }
    }

    fun openGeneratedVideo(index: Int) {
        _uiState.update {
            val keys = it.photos.filter { item ->
                item.kind == MediaKind.VIDEO &&
                    ((it.videoStates[item.cacheKey] ?: VideoVrState.NORMAL) != VideoVrState.NORMAL ||
                        it.videoEntries.containsKey(item.cacheKey) ||
                        it.videoJobs.any { job -> job.item.cacheKey == item.cacheKey })
            }.map { item -> item.cacheKey }.toSet()
            val origin = if (it.manageOpen) ViewerOrigin.MANAGE_MODAL else ViewerOrigin.GENERATED_TAB
            it.copy(
                selectedIndex = index,
                galleryAnchorIndex = index,
                galleryScrollIndex = max(0, index - it.galleryAnchorSlot),
                vrMode = true,
                manageOpen = false,
                homeTab = if (origin == ViewerOrigin.GENERATED_TAB) "generated" else it.homeTab,
                generatedTab = "videos",
                viewerOrigin = origin,
                manageViewerKeys = keys,
                viewerScopeKeys = keys,
                message = null,
            )
        }
    }

    fun closeViewer() {
        _uiState.update {
            val currentIndex = it.selectedIndex ?: it.galleryAnchorIndex
            val returnToManage = it.viewerOrigin == ViewerOrigin.MANAGE_MODAL
            val returnToGeneratedTab = it.viewerOrigin == ViewerOrigin.GENERATED_TAB
            it.copy(
                selectedIndex = null,
                debugIndex = null,
                manageOpen = returnToManage,
                homeTab = if (returnToGeneratedTab) "generated" else it.homeTab,
                viewerOrigin = ViewerOrigin.NORMAL,
                manageViewerKeys = emptySet(),
                viewerScopeKeys = emptySet(),
                galleryAnchorIndex = currentIndex,
            )
        }
        rebalanceQueueForActiveSource()
        startWorker()
    }

    fun setHomeTab(tab: String) {
        _uiState.update {
            it.copy(
                homeTab = tab,
                manageOpen = false,
            )
        }
        rebalanceQueueForActiveSource()
        startWorker()
    }

    fun setGeneratedTab(tab: String) {
        _uiState.update { it.copy(generatedTab = tab) }
    }

    fun setPageColumns(page: String, columns: Int) {
        val safeColumns = columns.coerceIn(1, 8)
        _uiState.update {
            when (page) {
                "all" -> it.copy(allColumns = safeColumns)
                "albumList" -> it.copy(albumListColumns = safeColumns)
                "albumDetail" -> it.copy(albumDetailColumns = safeColumns)
                "generated" -> it.copy(generatedColumns = safeColumns)
                else -> it
            }
        }
    }

    fun toggleGeneratedVersion(version: String) {
        _uiState.update {
            val expanded = it.expandedGeneratedVersions
            it.copy(expandedGeneratedVersions = if (version in expanded) expanded - version else expanded + version)
        }
    }

    fun openAlbum(bucketId: String) {
        _uiState.update { it.copy(homeTab = "albums", selectedAlbumId = bucketId, manageOpen = false) }
        loadAlbumPage(bucketId, reset = true)
        rebalanceQueueForActiveSource()
        startWorker()
    }

    fun closeAlbum() {
        _uiState.update { it.copy(selectedAlbumId = null) }
        rebalanceQueueForActiveSource()
    }

    fun loadMoreSelectedAlbum() {
        val state = _uiState.value
        val bucketId = state.selectedAlbumId ?: return
        if (state.albumLoading) return
        val album = state.albums.firstOrNull { it.bucketId == bucketId }
        val loaded = state.photos.count { !it.generatedVirtual && it.bucketId == bucketId }
        if (album != null && loaded >= album.count) return
        loadAlbumPage(bucketId, reset = false)
    }

    private fun loadAlbumPage(bucketId: String, reset: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.albumLoading && !reset) return@launch
            val offset = if (reset) 0 else current.albumOffsets[bucketId] ?: current.photos.count { !it.generatedVirtual && it.bucketId == bucketId }
            _uiState.update { it.copy(albumLoading = true) }
            val page = withContext(Dispatchers.IO) { repository.loadAlbumMedia(bucketId, ALBUM_PAGE_SIZE, offset) }
            _uiState.update { state ->
                val virtualItems = state.photos.filter { it.generatedVirtual }
                val otherSystemItems = if (reset) {
                    state.photos.filter { !it.generatedVirtual && it.bucketId != bucketId }
                } else {
                    state.photos.filterNot { it.generatedVirtual }
                }
                val mergedSystem = (otherSystemItems + page).distinctBy { it.cacheKey }.sortedByDescending { it.modifiedTime }
                state.copy(
                    photos = mergedSystem + virtualItems,
                    albumOffsets = state.albumOffsets + (bucketId to (offset + page.size)),
                    albumLoading = false,
                    message = state.settings.language.t("相册已加载 ${offset + page.size} 项", "Album loaded ${offset + page.size} items"),
                )
            }
        }
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
                    val current = "v2.19"
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
        queueTags.clearActive()
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
            items.forEach {
                nextVideoRunToken(it.cacheKey)
                synchronized(videoPending) { videoPending.removeAll { job -> job.item.cacheKey == it.cacheKey } }
                videoCache.delete(it)
                videoQueueTags.remove(it.cacheKey)
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            val photos = unique.mapNotNull { _uiState.value.photos.getOrNull(it) }.filter { it.kind == MediaKind.IMAGE }
            photos.forEach { cache.deletePhoto(it) }
            _uiState.update {
                val keys = photos.map { photo -> photo.cacheKey }.toSet()
                it.copy(
                    vrMode = true,
                    entries = it.entries - keys,
                    states = it.states + keys.associateWith { VrState.NORMAL },
                    cacheVersions = cache.summaries(),
                    managedCacheItems = cache.allEntries(it.photos),
                    message = "已清理缓存并加入 ${photos.size} 张图片重新生成队列",
                )
            }
            unique.forEachIndexed { order, index ->
                enqueuePhoto(index, priority = order + 1, force = true, current = false, context = QueueContext(QueueSource.GENERATED, null))
            }
            startWorker()
        }
    }

    fun regenerateVideos(indexes: List<Int>) {
        val unique = indexes.distinct()
        viewModelScope.launch(Dispatchers.IO) {
            val videos = unique.mapNotNull { _uiState.value.photos.getOrNull(it) }.filter { it.kind == MediaKind.VIDEO }
            videos.forEach {
                nextVideoRunToken(it.cacheKey)
                synchronized(videoPending) { videoPending.removeAll { job -> job.item.cacheKey == it.cacheKey } }
                videoCache.delete(it)
                videoQueueTags.remove(it.cacheKey)
                synchronized(pausedVideoParams) {
                    pausedVideoParams.remove(it.cacheKey)
                    activeVideoParams.remove(it.cacheKey)
                }
                synchronized(pausedVideos) { pausedVideos.remove(it.cacheKey) }
            }
            _uiState.update {
                val keys = videos.map { video -> video.cacheKey }.toSet()
                it.copy(
                    videoEntries = it.videoEntries - keys,
                    videoStates = it.videoStates + keys.associateWith { VideoVrState.NORMAL },
                    videoJobs = it.videoJobs.filterNot { job -> job.item.cacheKey in keys },
                    message = "已清理缓存并加入 ${videos.size} 个视频重新生成队列",
                )
            }
            unique.forEach { index -> _uiState.value.photos.getOrNull(index)?.let { enqueueVideo(it, force = true) } }
        }
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
        val state = _uiState.value
        val photos = state.photos
        if (!state.vrMode) return
        if (photos.isEmpty()) return
        val scopeIndices = if (state.viewerScopeKeys.isNotEmpty()) {
            photos.mapIndexedNotNull { photoIndex, item -> if (item.cacheKey in state.viewerScopeKeys) photoIndex else null }
        } else {
            photos.indices.toList()
        }
        val scopePosition = scopeIndices.indexOf(index)
        if (scopePosition < 0) return
        val currentVersion = state.settings.toParams().cacheVersion()
        val queueContext = activeQueueContext(state)
        val window = state.activePrefetchWindow.coerceAtMost(8)
        val desiredCount = window * 2
        val targets = mutableListOf<Pair<Int, Int>>()
        if (includeCurrent) {
            if (photos.getOrNull(index)?.kind == MediaKind.IMAGE) enqueuePhoto(index, priority = 0, force = false, current = true, context = queueContext)
        }
        var distance = 1
        while (targets.size < desiredCount && distance <= window && distance < scopeIndices.size) {
            val next = scopeIndices.getOrNull(scopePosition + distance)
            val prev = scopeIndices.getOrNull(scopePosition - distance)
            if (next != null && photos[next].kind == MediaKind.IMAGE && cache.findEntry(photos[next], currentVersion) == null) targets += next to distance * 2 - 1
            if (targets.size < desiredCount && prev != null && photos[prev].kind == MediaKind.IMAGE && cache.findEntry(photos[prev], currentVersion) == null) targets += prev to distance * 2
            distance++
        }

        synchronized(pending) {
            val keep = targets.map { photos[it.first].cacheKey }.toSet()
            val toPause = pending.filter { sameQueueSource(it, queueContext) && it.photo.cacheKey !in keep }
            pending.removeAll(toPause.toSet())
            toPause.forEach { job ->
                paused[job.photo.cacheKey] = job
                queueTags.upsert(job, VrState.PAUSED)
                markState(job.photo.cacheKey, VrState.PAUSED)
                upsertJob(job.photo, job.priority, VrState.PAUSED, 0f)
            }
        }
        targets.forEach { (targetIndex, priority) -> enqueuePhoto(targetIndex, priority, force = false, current = false, context = queueContext) }
        rebalanceQueueForActiveSource()
        startWorker()
    }

    private fun enqueuePhoto(index: Int, priority: Int, force: Boolean, current: Boolean = false, context: QueueContext? = null) {
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
        val queueContext = context ?: activeQueueContext(_uiState.value)
        val job = QueuedJob(photo, priority, sequence++, queueContext.source, queueContext.albumId)
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
        queueTags.upsert(job, VrState.QUEUED)
        markState(photo.cacheKey, VrState.QUEUED)
        addLog("${if (current) "current" else "queued"} ${photo.displayName} p=$priority source=${queueContext.source}${queueContext.albumId?.let { ":$it" } ?: ""}")
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

    private fun activeQueueContext(state: UiState): QueueContext = when {
        state.homeTab == "generated" || state.manageOpen -> QueueContext(QueueSource.GENERATED, null)
        state.homeTab == "albums" && state.selectedAlbumId != null -> QueueContext(QueueSource.ALBUM, state.selectedAlbumId)
        else -> QueueContext(QueueSource.ALL, null)
    }

    private fun sameQueueSource(job: QueuedJob, context: QueueContext): Boolean {
        return job.source == context.source && (job.source != QueueSource.ALBUM || job.albumId == context.albumId)
    }

    private fun jobAllowedInCurrentContext(job: QueuedJob, state: UiState): Boolean {
        if (state.homeTab == "generated" || state.manageOpen) return true
        return sameQueueSource(job, activeQueueContext(state))
    }

    private fun pollAllowedPendingJob(): QueuedJob? {
        val state = _uiState.value
        return synchronized(pending) {
            val job = pending
                .sortedWith(compareBy<QueuedJob> { it.priority }.thenBy { it.sequence })
                .firstOrNull { jobAllowedInCurrentContext(it, state) }
            if (job != null) pending.remove(job)
            job
        }
    }

    private fun rebalanceQueueForActiveSource() {
        val state = _uiState.value
        val disallowed = synchronized(pending) {
            val items = pending.filterNot { jobAllowedInCurrentContext(it, state) }
            pending.removeAll(items.toSet())
            items
        }
        if (disallowed.isNotEmpty()) {
            synchronized(paused) {
                disallowed.forEach { job ->
                    paused[job.photo.cacheKey] = job
                    queueTags.upsert(job, VrState.PAUSED)
                    markState(job.photo.cacheKey, VrState.PAUSED)
                    upsertJob(job.photo, job.priority, VrState.PAUSED, 0f)
                }
            }
        }
        val restored = synchronized(paused) {
            val items = paused.values.filter { jobAllowedInCurrentContext(it, state) }
            items.forEach { paused.remove(it.photo.cacheKey) }
            items
        }
        if (restored.isNotEmpty()) {
            synchronized(pending) { restored.forEach { pending.add(it) } }
            restored.forEach { job ->
                queueTags.upsert(job, VrState.QUEUED)
                markState(job.photo.cacheKey, VrState.QUEUED)
                upsertJob(job.photo, job.priority, VrState.QUEUED, 0f)
            }
        }
    }

    private fun restoreQueueTags() {
        val state = _uiState.value
        val photoByKey = state.photos.filter { it.kind == MediaKind.IMAGE }.associateBy { it.cacheKey }
        val restoredStates = mutableMapOf<String, VrState>()
        val pendingJobs = mutableListOf<QueuedJob>()
        val pausedJobs = mutableListOf<QueuedJob>()
        queueTags.load().forEach { tag ->
            val photo = photoByKey[tag.photoKey] ?: return@forEach
            if (cache.findEntry(photo, state.settings.toParams().cacheVersion()) != null) {
                queueTags.remove(tag.photoKey)
                restoredStates[tag.photoKey] = VrState.READY
                return@forEach
            }
            val restoredState = when (tag.state) {
                VrState.READY -> VrState.READY
                VrState.FAILED -> VrState.FAILED
                VrState.GENERATING -> VrState.QUEUED
                VrState.QUEUED -> VrState.QUEUED
                VrState.PAUSED -> VrState.PAUSED
                VrState.NORMAL -> VrState.QUEUED
            }
            restoredStates[tag.photoKey] = restoredState
            if (restoredState == VrState.FAILED || restoredState == VrState.READY) return@forEach
            val job = QueuedJob(photo, tag.priority, sequence++, tag.source, tag.albumId)
            if (jobAllowedInCurrentContext(job, state) && restoredState != VrState.PAUSED) {
                pendingJobs += job
            } else {
                pausedJobs += job
            }
        }
        synchronized(pending) { pendingJobs.forEach { pending.add(it) } }
        synchronized(paused) { pausedJobs.forEach { paused[it.photo.cacheKey] = it } }
        if (restoredStates.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    states = it.states + restoredStates,
                    message = it.settings.language.t("已恢复 ${restoredStates.size} 条生成标签", "Restored ${restoredStates.size} generation tags"),
                )
            }
            addLog("restored ${restoredStates.size} queue tags")
        }
        if (pendingJobs.isNotEmpty() && _uiState.value.vrMode) startWorker()
    }

    private fun restoreVideoQueueTags() {
        val state = _uiState.value
        val videoByKey = state.photos.filter { it.kind == MediaKind.VIDEO }.associateBy { it.cacheKey }
        val restoredStates = mutableMapOf<String, VideoVrState>()
        val restoredJobs = mutableListOf<VideoVrJob>()
        videoQueueTags.load().forEach { tag ->
            val item = videoByKey[tag.videoKey] ?: queuedVideoItemFromKey(tag.videoKey)
            val entry = videoCache.findEntry(item)
            if (entry != null) {
                videoQueueTags.remove(tag.videoKey)
                restoredStates[tag.videoKey] = VideoVrState.READY
                return@forEach
            }
            val restoredState = when (tag.state) {
                VideoVrState.READY -> VideoVrState.READY
                VideoVrState.FAILED -> VideoVrState.FAILED
                VideoVrState.NORMAL -> VideoVrState.PAUSED
                VideoVrState.QUEUED,
                VideoVrState.GENERATING,
                VideoVrState.PAUSED -> VideoVrState.PAUSED
            }
            restoredStates[tag.videoKey] = restoredState
            if (restoredState == VideoVrState.PAUSED) {
                synchronized(pausedVideos) { pausedVideos.add(tag.videoKey) }
                tag.params?.let { params ->
                    synchronized(pausedVideoParams) { pausedVideoParams[tag.videoKey] = params }
                }
            }
            if (restoredState != VideoVrState.READY) {
                restoredJobs += VideoVrJob(
                    item = item,
                    state = restoredState,
                    progress = if (restoredState == VideoVrState.FAILED) 1f else 0f,
                    startedAt = null,
                    finishedAt = null,
                    error = if (restoredState == VideoVrState.FAILED) "上次生成失败" else null,
                )
            }
        }
        if (restoredStates.isNotEmpty()) {
            _uiState.update {
                val restoredItems = restoredJobs.map { job -> job.item }
                it.copy(
                    photos = (it.photos + restoredItems).distinctBy { item -> item.cacheKey },
                    videoStates = it.videoStates + restoredStates,
                    videoJobs = (restoredJobs + it.videoJobs).distinctBy { job -> job.item.cacheKey }.take(100),
                    message = it.settings.language.t("已恢复 ${restoredStates.size} 条视频生成标签", "Restored ${restoredStates.size} video generation tags"),
                )
            }
            addLog("restored ${restoredStates.size} video queue tags")
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
            videoQueueTags.remove(item.cacheKey)
            _uiState.update { it.copy(videoStates = it.videoStates + (item.cacheKey to VideoVrState.READY)) }
            return
        }
        val existing = _uiState.value.videoStates[item.cacheKey]
        if (!force && (existing == VideoVrState.QUEUED || existing == VideoVrState.GENERATING)) return
        synchronized(videoPending) {
            videoPending.removeAll { it.item.cacheKey == item.cacheKey }
            val runToken = nextVideoRunToken(item.cacheKey)
            videoPending.add(VideoQueuedJob(item, params, videoSequence++, runToken))
        }
        upsertVideoJob(item, VideoVrState.QUEUED, 0f)
        videoQueueTags.upsert(item.cacheKey, VideoVrState.QUEUED, params)
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
        nextVideoRunToken(item.cacheKey)
        val paramsSnapshot = synchronized(pausedVideoParams) {
            activeVideoParams[item.cacheKey]?.let { pausedVideoParams[item.cacheKey] = it }
            pausedVideoParams[item.cacheKey]
        }
        _uiState.update {
            it.copy(
                videoStates = it.videoStates + (item.cacheKey to VideoVrState.PAUSED),
                message = "视频已暂停生成：${item.displayName}",
            )
        }
        videoQueueTags.upsert(item.cacheKey, VideoVrState.PAUSED, paramsSnapshot)
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
        if (!isCurrentVideoRun(next.item.cacheKey, next.runToken)) {
            addLog("video stale queued job ignored ${next.item.displayName}")
            return
        }
        synchronized(pausedVideoParams) { activeVideoParams[next.item.cacheKey] = next.params }
        val vrParams = next.params.toVrParams()
        val videoCacheVersion = next.params.cacheVersion()
        var lastRuntimeInfo = "pending"
        _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.GENERATING)) }
        videoQueueTags.upsert(next.item.cacheKey, VideoVrState.GENERATING, next.params)
        upsertVideoJob(next.item, VideoVrState.GENERATING, 0.01f, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = lastRuntimeInfo)
        videoNotifier.show(next.item, 0, 0, indeterminate = true, status = "准备生成")
        val started = SystemClock.uptimeMillis()
        var totalFrameTimeMs = 0L
        var measuredFrameCount = 0
        videoKeepAlive.acquire()
        val result = try {
            runCatching {
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
                    onRuntimeInfo = { runtime ->
                        if (runtime != lastRuntimeInfo && runtime.startsWith("tflite")) {
                            addLog("video runtime ${next.item.displayName}: $runtime")
                        }
                        lastRuntimeInfo = runtime
                        upsertVideoJob(next.item, VideoVrState.GENERATING, _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == next.item.cacheKey }?.progress ?: 0f, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = runtime)
                    },
                ) { progress, frame, total, fps, currentFrameMs ->
                    if (!isCurrentVideoRun(next.item.cacheKey, next.runToken)) {
                        throw VideoStaleException()
                    }
                    if (synchronized(pausedVideos) { pausedVideos.contains(next.item.cacheKey) }) {
                        throw VideoPausedException()
                    }
                    if (currentFrameMs > 0L) {
                        totalFrameTimeMs += currentFrameMs
                        measuredFrameCount++
                    }
                    val avgFrameMs = if (measuredFrameCount > 0) totalFrameTimeMs / measuredFrameCount else 0L
                    upsertVideoJob(next.item, VideoVrState.GENERATING, progress, frame, total, fps, currentFrameMs = currentFrameMs, avgFrameMs = avgFrameMs, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = lastRuntimeInfo)
                    videoNotifier.show(next.item, frame, total, indeterminate = false, status = "生成中")
                }
            }
        } finally {
            videoKeepAlive.release()
        }
        if (!isCurrentVideoRun(next.item.cacheKey, next.runToken)) {
            addLog("video stale result ignored ${next.item.displayName}")
            return
        }
        result.onSuccess { entry ->
            val elapsed = SystemClock.uptimeMillis() - started
            _uiState.update {
                it.copy(
                    videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.READY),
                    videoEntries = it.videoEntries + (next.item.cacheKey to entry),
                    modelProgress = null,
                    modelStatus = modelStatusText(it.settings),
                    message = "视频 VR 已生成：${next.item.displayName}（${elapsed}ms）",
                )
            }
            refreshGeneratedLibrary()
            upsertVideoJob(next.item, VideoVrState.READY, 1f, currentFrameMs = _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == next.item.cacheKey }?.currentFrameMs ?: 0L, avgFrameMs = _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == next.item.cacheKey }?.avgFrameMs ?: 0L, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = lastRuntimeInfo, finishedAt = System.currentTimeMillis())
            videoQueueTags.remove(next.item.cacheKey)
            synchronized(pausedVideoParams) {
                pausedVideoParams.remove(next.item.cacheKey)
                activeVideoParams.remove(next.item.cacheKey)
            }
            videoNotifier.show(next.item, 1, 1, indeterminate = false, status = "已完成")
            addLog("video ready ${next.item.displayName} ${entry.width}x${entry.height} ${elapsed}ms")
        }.onFailure { error ->
            if (error is VideoStaleException) {
                addLog("video stale failure ignored ${next.item.displayName}")
            } else if (error is VideoPausedException) {
                _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.PAUSED), modelProgress = null) }
                videoQueueTags.upsert(next.item.cacheKey, VideoVrState.PAUSED, next.params)
                synchronized(pausedVideoParams) {
                    pausedVideoParams[next.item.cacheKey] = next.params
                    activeVideoParams.remove(next.item.cacheKey)
                }
                upsertVideoJob(next.item, VideoVrState.PAUSED, _uiState.value.videoJobs.firstOrNull { it.item.cacheKey == next.item.cacheKey }?.progress ?: 0f, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = lastRuntimeInfo)
                addLog("video paused ${next.item.displayName}")
            } else {
                _uiState.update { it.copy(videoStates = it.videoStates + (next.item.cacheKey to VideoVrState.FAILED), modelProgress = null) }
                videoQueueTags.upsert(next.item.cacheKey, VideoVrState.FAILED, next.params)
                synchronized(pausedVideoParams) { activeVideoParams.remove(next.item.cacheKey) }
                upsertVideoJob(next.item, VideoVrState.FAILED, 1f, modelId = vrParams.depthModel, cacheVersion = videoCacheVersion, modelThreads = vrParams.modelThreads, useGpu = vrParams.useGpu, runtimeInfo = lastRuntimeInfo, finishedAt = System.currentTimeMillis(), error = error.message)
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
                rebalanceQueueForActiveSource()
                val next = pollAllowedPendingJob()
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
        queueTags.upsert(next, VrState.GENERATING)
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
            queueTags.remove(next.photo.cacheKey)
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
            queueTags.upsert(next, VrState.FAILED)
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
        return synchronized(pending) { pending.any { jobAllowedInCurrentContext(it, _uiState.value) } }
    }

    private fun markReady(key: String, entry: VrCacheEntry) {
        _uiState.update {
            it.copy(
                states = it.states + (key to VrState.READY),
                entries = it.entries + (key to entry),
            )
        }
        refreshGeneratedLibrary()
    }

    private fun markState(key: String, state: VrState) {
        _uiState.update { it.copy(states = it.states + (key to state)) }
    }

    private fun refreshGeneratedLibrary(message: String? = null) {
        val photos = _uiState.value.photos
        val imageEntries = photos
            .filter { it.kind == MediaKind.IMAGE }
            .mapNotNull { cache.findEntry(it) }
            .associateBy { it.photoKey }
        val managedItems = cache.allEntries(photos)
        val videoPairs = videoCache.allEntries(photos)
        val videoEntries = videoPairs.associate { it.first.cacheKey to it.second }
        val cacheVersions = cache.summaries()
        _uiState.update { current ->
            current.copy(
                entries = current.entries + imageEntries,
                states = current.states + imageEntries.keys.associateWith { VrState.READY },
                videoEntries = current.videoEntries + videoEntries,
                videoStates = current.videoStates + videoEntries.keys.associateWith { VideoVrState.READY },
                managedCacheItems = managedItems,
                cacheVersions = cacheVersions,
                message = message ?: current.message,
            )
        }
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
        currentFrameMs: Long = 0L,
        modelId: String = "",
        cacheVersion: String = "",
        modelThreads: Int = 0,
        useGpu: Boolean = false,
        runtimeInfo: String = "",
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
                currentFrameMs = currentFrameMs.takeIf { it > 0L } ?: previous?.currentFrameMs ?: 0L,
                avgFrameMs = avgFrameMs.takeIf { it > 0L } ?: previous?.avgFrameMs ?: 0L,
                modelId = modelId.ifBlank { previous?.modelId.orEmpty() },
                cacheVersion = cacheVersion.ifBlank { previous?.cacheVersion.orEmpty() },
                modelThreads = modelThreads.takeIf { it > 0 } ?: previous?.modelThreads ?: 0,
                useGpu = if (modelId.isNotBlank() || cacheVersion.isNotBlank()) useGpu else previous?.useGpu == true,
                runtimeInfo = runtimeInfo.ifBlank { previous?.runtimeInfo.orEmpty() },
                startedAt = previous?.startedAt ?: System.currentTimeMillis(),
                finishedAt = finishedAt,
                error = error,
            )
            current.copy(
                videoStates = current.videoStates + (item.cacheKey to state),
                videoJobs = (listOf(job) + existing).take(100),
            )
        }
    }

    private fun nextVideoRunToken(videoKey: String): Long = synchronized(videoRunTokens) {
        val next = (videoRunTokens[videoKey] ?: 0L) + 1L
        videoRunTokens[videoKey] = next
        next
    }

    private fun isCurrentVideoRun(videoKey: String, token: Long): Boolean = synchronized(videoRunTokens) {
        videoRunTokens[videoKey] == token
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

private data class QueuedJob(val photo: PhotoItem, val priority: Int, val sequence: Long, val source: QueueSource, val albumId: String?)

private data class VideoQueuedJob(val item: GalleryItem, val params: VideoGenerationParams, val sequence: Long, val runToken: Long)

private class VideoPausedException : RuntimeException("Video generation paused")
private class VideoStaleException : RuntimeException("Video generation superseded")

private fun AppLanguage.t(zh: String, en: String): String = if (this == AppLanguage.ZH) zh else en

private fun AppLanguage.pickMixed(text: String): String {
    val marker = " / "
    if (!text.contains(marker)) return text
    val parts = text.split(marker, limit = 2)
    return if (this == AppLanguage.ZH) parts.first() else parts.getOrElse(1) { parts.first() }
}

private fun queuedVideoItemFromKey(videoKey: String): GalleryItem {
    val rawKey = videoKey.removePrefix("VIDEO_")
    val parts = rawKey.split("_")
    val mediaId = parts.getOrNull(0)?.toLongOrNull()
    val size = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    val modified = parts.getOrNull(2)?.toLongOrNull() ?: 0L
    val id = mediaId ?: abs(videoKey.hashCode()).toLong()
    return GalleryItem(
        id = id,
        uri = mediaId?.let { ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it) } ?: Uri.EMPTY,
        displayName = mediaId?.let { "restored_video_$it.mp4" } ?: "restored_${videoKey.take(12)}.mp4",
        width = 0,
        height = 0,
        size = size,
        modifiedTime = modified,
        kind = MediaKind.VIDEO,
        forcedCacheKey = videoKey,
        generatedVirtual = true,
    )
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

private fun buildAlbums(items: List<GalleryItem>): List<AlbumItem> {
    return items.groupBy { item ->
        item.bucketId.ifBlank { item.relativePath.ifBlank { "bucket_${item.displayName.substringBeforeLast('.', item.displayName)}" } }
    }.mapNotNull { (bucketId, albumItems) ->
        val latest = albumItems.maxByOrNull { it.modifiedTime } ?: return@mapNotNull null
        AlbumItem(
            bucketId = bucketId,
            name = latest.bucketName.ifBlank {
                latest.relativePath.trim('/').substringAfterLast('/').ifBlank { "未命名相册" }
            },
            relativePath = latest.relativePath,
            coverUri = latest.uri,
            coverKind = latest.kind,
            count = albumItems.size,
            latestModified = latest.modifiedTime,
        )
    }.sortedByDescending { it.latestModified }
}

private fun dateGroupLabel(modifiedTimeSeconds: Long, lang: AppLanguage): String {
    if (modifiedTimeSeconds <= 0L) return lang.t("未知日期", "Unknown date")
    val now = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance().apply { timeInMillis = modifiedTimeSeconds * 1000L }
    val todayYear = now.get(java.util.Calendar.YEAR)
    val todayDay = now.get(java.util.Calendar.DAY_OF_YEAR)
    val targetYear = target.get(java.util.Calendar.YEAR)
    val targetDay = target.get(java.util.Calendar.DAY_OF_YEAR)
    return when {
        todayYear == targetYear && todayDay == targetDay -> lang.t("今天", "Today")
        todayYear == targetYear && todayDay - targetDay == 1 -> lang.t("昨天", "Yesterday")
        lang == AppLanguage.ZH -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(modifiedTimeSeconds * 1000L))
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(modifiedTimeSeconds * 1000L))
    }
}

private fun localEpochDay(modifiedTimeSeconds: Long): Long {
    if (modifiedTimeSeconds <= 0L) return Long.MIN_VALUE
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = modifiedTimeSeconds * 1000L }
    val year = calendar.get(java.util.Calendar.YEAR)
    val day = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    return year.toLong() * 400L + day.toLong()
}

private fun jsonStringValue(text: String, key: String): String? {
    return Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]*)\"")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace("\\\"", "\"")
}

private fun jsonIntValue(text: String, key: String): Int? {
    return Regex("\"${Regex.escape(key)}\"\\s*:\\s*(\\d+)")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}

private fun jsonLongValue(text: String, key: String): Long? {
    return Regex("\"${Regex.escape(key)}\"\\s*:\\s*(\\d+)")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
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
    fun loadImages(): List<PhotoItem> = loadMedia(INITIAL_MEDIA_LIMIT).filter { it.kind == MediaKind.IMAGE }

    fun loadAlbums(): List<AlbumItem> = buildAlbums(loadMedia(null))

    fun loadAlbumMedia(bucketId: String, limit: Int, offset: Int): List<GalleryItem> {
        return queryFilesMedia(limit = limit, offset = offset, bucketId = bucketId)
    }

    fun loadMedia(limit: Int? = null, offset: Int = 0): List<GalleryItem> {
        return queryFilesMedia(limit = limit, offset = offset, bucketId = null)
    }

    private fun queryFilesMedia(limit: Int?, offset: Int, bucketId: String?): List<GalleryItem> {
        val result = mutableListOf<PhotoItem>()
        if (!hasImagePermission(context) && !hasVideoPermission(context)) return emptyList()
        val collection = MediaStore.Files.getContentUri("external")
        val idColumn = MediaStore.Files.FileColumns._ID
        val mediaTypeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
        val nameColumn = MediaStore.MediaColumns.DISPLAY_NAME
        val widthColumn = MediaStore.MediaColumns.WIDTH
        val heightColumn = MediaStore.MediaColumns.HEIGHT
        val sizeColumn = MediaStore.MediaColumns.SIZE
        val modifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
        val bucketIdColumn = MediaStore.Images.Media.BUCKET_ID
        val bucketNameColumn = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        val relativePathColumn = if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else null
        val projection = listOfNotNull(
            idColumn,
            mediaTypeColumn,
            nameColumn,
            widthColumn,
            heightColumn,
            sizeColumn,
            modifiedColumn,
            bucketIdColumn,
            bucketNameColumn,
            relativePathColumn,
        ).toTypedArray()
        val mediaTypes = mutableListOf<String>()
        if (hasImagePermission(context)) mediaTypes += MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
        if (hasVideoPermission(context)) mediaTypes += MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        val selectionParts = mutableListOf("${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${mediaTypes.joinToString(",") { "?" }})")
        val args = mutableListOf<String>().apply { addAll(mediaTypes) }
        if (bucketId != null) {
            selectionParts += "$bucketIdColumn=?"
            args += bucketId
        }
        val selection = selectionParts.joinToString(" AND ")
        val limitedSortOrder = if (limit != null) "$modifiedColumn DESC, $idColumn DESC LIMIT $limit OFFSET $offset" else "$modifiedColumn DESC, $idColumn DESC"
        var manualOffset = false
        val cursorResult = runCatching {
            context.contentResolver.query(collection, projection, selection, args.toTypedArray(), limitedSortOrder)
        }.getOrNull() ?: run {
            manualOffset = true
            context.contentResolver.query(collection, projection, selection, args.toTypedArray(), "$modifiedColumn DESC, $idColumn DESC")
        }
        cursorResult?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(idColumn)
            val mediaTypeCol = cursor.getColumnIndexOrThrow(mediaTypeColumn)
            val nameCol = cursor.getColumnIndexOrThrow(nameColumn)
            val widthCol = cursor.getColumnIndex(widthColumn)
            val heightCol = cursor.getColumnIndex(heightColumn)
            val sizeCol = cursor.getColumnIndex(sizeColumn)
            val modifiedCol = cursor.getColumnIndexOrThrow(modifiedColumn)
            val bucketIdCol = cursor.getColumnIndex(bucketIdColumn)
            val bucketNameCol = cursor.getColumnIndex(bucketNameColumn)
            val relativePathCol = relativePathColumn?.let { cursor.getColumnIndex(it) } ?: -1
            var skipped = 0
            while (cursor.moveToNext()) {
                if (manualOffset && limit != null && skipped < offset) {
                    skipped++
                    continue
                }
                if (limit != null && result.size >= limit) break
                val mediaType = cursor.getInt(mediaTypeCol)
                val kind = when (mediaType) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaKind.IMAGE
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.VIDEO
                    else -> continue
                }
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: "${kind.name.lowercase(Locale.US)}-$id"
                if (kind == MediaKind.IMAGE && (displayName.startsWith(GENERATED_VR_PREFIX) || displayName.startsWith("VR_"))) continue
                val itemUri = if (kind == MediaKind.IMAGE) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                }
                result += GalleryItem(
                    id = id,
                    uri = itemUri,
                    displayName = displayName,
                    width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                    height = if (heightCol >= 0) cursor.getInt(heightCol) else 0,
                    size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                    modifiedTime = cursor.getLong(modifiedCol),
                    kind = kind,
                    bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol).orEmpty() else "",
                    bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol).orEmpty() else "",
                    relativePath = if (relativePathCol >= 0) cursor.getString(relativePathCol).orEmpty() else "",
                )
            }
        }
        return result
    }

    private fun queryMedia(
        collection: Uri,
        idColumn: String,
        nameColumn: String,
        widthColumn: String,
        heightColumn: String,
        sizeColumn: String,
        modifiedColumn: String,
        bucketIdColumn: String,
        bucketNameColumn: String,
        relativePathColumn: String?,
        kind: MediaKind,
        limit: Int?,
        offset: Int = 0,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): List<GalleryItem> {
        val projection = listOfNotNull(idColumn, nameColumn, widthColumn, heightColumn, sizeColumn, modifiedColumn, bucketIdColumn, bucketNameColumn, relativePathColumn).toTypedArray()
        val result = mutableListOf<GalleryItem>()
        val limitedSortOrder = if (limit != null) "$modifiedColumn DESC LIMIT $limit OFFSET $offset" else "$modifiedColumn DESC"
        var manualOffset = false
        val cursorResult = runCatching {
            context.contentResolver.query(collection, projection, selection, selectionArgs, limitedSortOrder)
        }.getOrNull() ?: run {
            manualOffset = true
            context.contentResolver.query(collection, projection, selection, selectionArgs, "$modifiedColumn DESC")
        }
        cursorResult?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(idColumn)
            val nameCol = cursor.getColumnIndexOrThrow(nameColumn)
            val widthCol = cursor.getColumnIndexOrThrow(widthColumn)
            val heightCol = cursor.getColumnIndexOrThrow(heightColumn)
            val sizeCol = cursor.getColumnIndexOrThrow(sizeColumn)
            val modifiedCol = cursor.getColumnIndexOrThrow(modifiedColumn)
            val bucketIdCol = cursor.getColumnIndex(bucketIdColumn)
            val bucketNameCol = cursor.getColumnIndex(bucketNameColumn)
            val relativePathCol = relativePathColumn?.let { cursor.getColumnIndex(it) } ?: -1
            var skipped = 0
            while (cursor.moveToNext()) {
                if (manualOffset && limit != null && skipped < offset) {
                    skipped++
                    continue
                }
                if (limit != null && result.size >= limit) break
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: "${kind.name.lowercase(Locale.US)}-$id"
                if (kind == MediaKind.IMAGE && (displayName.startsWith(GENERATED_VR_PREFIX) || displayName.startsWith("VR_"))) continue
                val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol).orEmpty() else ""
                val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol).orEmpty() else ""
                val relativePath = if (relativePathCol >= 0) cursor.getString(relativePathCol).orEmpty() else ""
                result += GalleryItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = displayName,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    size = cursor.getLong(sizeCol),
                    modifiedTime = cursor.getLong(modifiedCol),
                    kind = kind,
                    bucketId = bucketId,
                    bucketName = bucketName,
                    relativePath = relativePath,
                )
            }
        }
        return result
    }
}

private class QueueTagStore(context: Context) {
    private val root = File(context.getExternalFilesDir(null), "queue_index").also { it.mkdirs() }
    private val file = File(root, "image_queue.tsv")

    @Synchronized
    fun load(): List<QueueTag> {
        if (!file.exists()) return emptyList()
        return file.readLines(Charsets.UTF_8).mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 6) return@mapNotNull null
            val source = runCatching { QueueSource.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val state = runCatching { VrState.valueOf(parts[4]) }.getOrNull() ?: VrState.QUEUED
            QueueTag(
                photoKey = parts[0],
                source = source,
                albumId = parts[2].takeIf { it.isNotBlank() },
                priority = parts[3].toIntOrNull() ?: 99,
                state = state,
                updatedAt = parts[5].toLongOrNull() ?: 0L,
            )
        }.distinctBy { it.photoKey }
    }

    @Synchronized
    fun upsert(job: QueuedJob, state: VrState) {
        val next = load()
            .filterNot { it.photoKey == job.photo.cacheKey }
            .toMutableList()
        next += QueueTag(
            photoKey = job.photo.cacheKey,
            source = job.source,
            albumId = job.albumId,
            priority = job.priority,
            state = state,
            updatedAt = System.currentTimeMillis(),
        )
        write(next)
    }

    @Synchronized
    fun remove(photoKey: String) {
        write(load().filterNot { it.photoKey == photoKey })
    }

    @Synchronized
    fun clearActive() {
        write(load().filter { it.state == VrState.FAILED })
    }

    private fun write(tags: List<QueueTag>) {
        if (tags.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        file.writeText(
            tags.joinToString("\n") { tag ->
                listOf(
                    tag.photoKey.safeQueueField(),
                    tag.source.name,
                    tag.albumId.orEmpty().safeQueueField(),
                    tag.priority.toString(),
                    tag.state.name,
                    tag.updatedAt.toString(),
                ).joinToString("\t")
            },
            Charsets.UTF_8,
        )
    }

    private fun String.safeQueueField(): String = replace('\t', '_').replace('\n', '_').replace('\r', '_')
}

private class VideoQueueTagStore(context: Context) {
    private val root = File(context.getExternalFilesDir(null), "queue_index").also { it.mkdirs() }
    private val file = File(root, "video_queue.tsv")

    @Synchronized
    fun load(): List<VideoQueueTag> {
        if (!file.exists()) return emptyList()
        return file.readLines(Charsets.UTF_8).mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            val state = runCatching { VideoVrState.valueOf(parts[1]) }.getOrNull() ?: VideoVrState.PAUSED
            VideoQueueTag(
                videoKey = parts[0],
                state = state,
                updatedAt = parts[2].toLongOrNull() ?: 0L,
                params = parts.getOrNull(3)?.let { decodeVideoParams(it) },
            )
        }.distinctBy { it.videoKey }
    }

    @Synchronized
    fun upsert(videoKey: String, state: VideoVrState, params: VideoGenerationParams? = null) {
        val next = load()
            .filterNot { it.videoKey == videoKey }
            .toMutableList()
        val previous = load().firstOrNull { it.videoKey == videoKey }
        next += VideoQueueTag(
            videoKey = videoKey,
            state = state,
            params = params ?: previous?.params,
            updatedAt = System.currentTimeMillis(),
        )
        write(next)
    }

    @Synchronized
    fun remove(videoKey: String) {
        write(load().filterNot { it.videoKey == videoKey })
    }

    private fun write(tags: List<VideoQueueTag>) {
        if (tags.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        file.writeText(
            tags.joinToString("\n") { tag ->
                listOf(
                    tag.videoKey.safeQueueField(),
                    tag.state.name,
                    tag.updatedAt.toString(),
                    tag.params?.encodeForQueue().orEmpty(),
                ).joinToString("\t")
            },
            Charsets.UTF_8,
        )
    }

    private fun VideoGenerationParams.encodeForQueue(): String {
        return listOf(
            vr.depthModel,
            vr.outputMode,
            vr.depthScale.toString(),
            vr.blurRadius.toString(),
            vr.fillRadius.toString(),
            vr.invertDepth.toString(),
            vr.maxLongEdge.toString(),
            vr.inpaintMode,
            vr.quality.toString(),
            modelThreads.toString(),
            useGpu.toString(),
            cacheVersion(),
        ).joinToString(",") { it.replace(",", "_").replace('\t', '_').replace('\n', '_').replace('\r', '_') }
    }

    private fun decodeVideoParams(text: String): VideoGenerationParams? {
        val parts = text.split(',')
        if (parts.size < 11) return null
        return runCatching {
            VideoGenerationParams(
                vr = VrGenerationParams(
                    depthModel = parts[0],
                    outputMode = parts[1],
                    depthScale = parts[2].toFloat(),
                    blurRadius = parts[3].toInt(),
                    fillRadius = parts[4].toInt(),
                    invertDepth = parts[5].toBooleanStrictOrNull() ?: false,
                    maxLongEdge = parts[6].toInt(),
                    modelThreads = parts[9].toInt(),
                    useGpu = parts[10].toBooleanStrictOrNull() ?: false,
                    inpaintMode = parts[7],
                    quality = parts[8].toInt(),
                ),
                modelThreads = parts[9].toInt(),
                useGpu = parts[10].toBooleanStrictOrNull() ?: false,
            )
        }.getOrNull()
    }

    private fun String.safeQueueField(): String = replace('\t', '_').replace('\n', '_').replace('\r', '_')
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
        return allEntriesByKey(photos.associateBy { it.cacheKey })
    }

    fun allEntriesByKey(photoByKey: Map<String, PhotoItem>): List<ManagedCacheItem> {
        return root.listFiles()?.asSequence()
            ?.filter { it.isDirectory }
            ?.flatMap { photoDir ->
                val entries = mutableListOf<VrCacheEntry>()
                readEntry(photoDir.name, DEFAULT_VERSION, photoDir)?.let { entries += it }
                photoDir.listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
                    readEntry(photoDir.name, versionDir.name, versionDir)?.let { entries += it }
                }
                val latest = entries.maxByOrNull { it.createdAt } ?: return@flatMap emptySequence()
                val photo = photoByKey[photoDir.name] ?: virtualPhotoItem(photoDir.name, latest)
                entries.asSequence().map { ManagedCacheItem(photo, it) }
            }
            ?.sortedByDescending { it.entry.createdAt }
            ?.toList()
            ?: emptyList()
    }

    fun allEntriesSlow(photos: List<PhotoItem>): List<ManagedCacheItem> {
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

    fun deletePhoto(photo: PhotoItem) {
        File(root, photo.cacheKey).deleteRecursively()
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

    private fun virtualPhotoItem(photoKey: String, entry: VrCacheEntry): PhotoItem {
        val paramsText = runCatching { File(entry.paramsPath).readText(Charsets.UTF_8) }.getOrDefault("")
        val displayName = jsonStringValue(paramsText, "displayName") ?: "generated_$photoKey.jpg"
        val sourceWidth = jsonIntValue(paramsText, "sourceWidth") ?: max(1, entry.width / 2)
        val sourceHeight = jsonIntValue(paramsText, "sourceHeight") ?: entry.height
        val parts = photoKey.split("_")
        val id = parts.getOrNull(0)?.toLongOrNull() ?: abs(photoKey.hashCode()).toLong()
        val size = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val modified = parts.getOrNull(2)?.toLongOrNull() ?: (entry.createdAt / 1000L)
        val uri = parts.getOrNull(0)?.toLongOrNull()?.let { ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it) }
            ?: Uri.fromFile(File(entry.outputPath))
        return PhotoItem(
            id = id,
            uri = uri,
            displayName = displayName,
            width = sourceWidth,
            height = sourceHeight,
            size = size,
            modifiedTime = modified,
            kind = MediaKind.IMAGE,
            forcedCacheKey = photoKey,
            generatedVirtual = true,
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
        return allEntriesByKey(items.filter { it.kind == MediaKind.VIDEO }.associateBy { it.cacheKey })
    }

    fun allEntriesByKey(videoByKey: Map<String, GalleryItem>): List<Pair<GalleryItem, VideoCacheEntry>> {
        return root.listFiles()?.asSequence()
            ?.filter { it.isDirectory }
            ?.mapNotNull { videoDir ->
                val entries = mutableListOf<VideoCacheEntry>()
                readEntry(videoDir.name, videoDir)?.let { entries += it }
                videoDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                    readEntry(videoDir.name, dir)?.let { entries += it }
                }
                val latest = entries.maxByOrNull { it.createdAt } ?: return@mapNotNull null
                val item = videoByKey[videoDir.name] ?: virtualVideoItem(videoDir.name, latest)
                item to latest
            }
            ?.sortedByDescending { it.second.createdAt }
            ?.toList()
            ?: emptyList()
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
        if (values["encoderVersion"] != VIDEO_ENCODER_VERSION) return null
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

    private fun virtualVideoItem(videoKey: String, entry: VideoCacheEntry): GalleryItem {
        val values = runCatching {
            File(entry.logPath).parentFile?.let { File(it, "video_params.txt") }?.readLines(Charsets.UTF_8)?.associate { line ->
                val parts = line.split("=", limit = 2)
                parts.first() to parts.getOrElse(1) { "" }
            }
        }.getOrNull().orEmpty()
        val displayName = values["source"]?.takeIf { it.isNotBlank() } ?: "generated_$videoKey.mp4"
        val rawKey = videoKey.removePrefix("VIDEO_")
        val parts = rawKey.split("_")
        val id = parts.getOrNull(0)?.toLongOrNull() ?: abs(videoKey.hashCode()).toLong()
        val size = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val modified = parts.getOrNull(2)?.toLongOrNull() ?: (entry.createdAt / 1000L)
        val uri = parts.getOrNull(0)?.toLongOrNull()?.let { ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it) }
            ?: Uri.fromFile(File(entry.outputPath))
        return GalleryItem(
            id = id,
            uri = uri,
            displayName = displayName,
            width = entry.width,
            height = entry.height,
            size = size,
            modifiedTime = modified,
            kind = MediaKind.VIDEO,
            forcedCacheKey = videoKey,
            generatedVirtual = true,
        )
    }
}

class VideoGenerationForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(
            SERVICE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("视频 VR 生成运行中")
                .setContentText("切到后台或锁屏时继续生成")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .build(),
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "视频 VR 后台生成", NotificationManager.IMPORTANCE_LOW))
        }
    }

    companion object {
        const val ACTION_START = "com.local.parallelvrgallery.video.START"
        const val ACTION_STOP = "com.local.parallelvrgallery.video.STOP"
        private const val CHANNEL_ID = "video_vr_foreground"
        private const val SERVICE_NOTIFICATION_ID = 9117
    }
}

private class VideoGenerationKeepAlive(context: Context) {
    private val appContext = context.applicationContext
    private val wakeLock = (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ParallelVrGallery:VideoGeneration")
        .apply { setReferenceCounted(false) }
    private var activeCount = 0

    @Synchronized
    fun acquire() {
        activeCount++
        if (activeCount == 1) {
            val intent = Intent(appContext, VideoGenerationForegroundService::class.java)
                .setAction(VideoGenerationForegroundService.ACTION_START)
            runCatching {
                if (Build.VERSION.SDK_INT >= 26) {
                    ContextCompat.startForegroundService(appContext, intent)
                } else {
                    appContext.startService(intent)
                }
            }
            runCatching {
                if (!wakeLock.isHeld) wakeLock.acquire(12 * 60 * 60 * 1000L)
            }
        }
    }

    @Synchronized
    fun release() {
        if (activeCount > 0) activeCount--
        if (activeCount == 0) {
            runCatching { if (wakeLock.isHeld) wakeLock.release() }
            runCatching { appContext.stopService(Intent(appContext, VideoGenerationForegroundService::class.java)) }
        }
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
        val urls = (listOf(spec.url) + spec.mirrors).distinct()
        val errors = mutableListOf<String>()
        for ((urlIndex, modelUrl) in urls.withIndex()) {
            if (tmp.exists()) tmp.delete()
            runCatching {
                val connection = (URL(modelUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                }
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
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
                            val sourceBase = urlIndex.toFloat() / urls.size.toFloat()
                            val sourceSpan = 1f / urls.size.toFloat()
                            onProgress((sourceBase + (readTotal.toFloat() / total.toFloat()) * sourceSpan).coerceIn(0f, 0.99f))
                        }
                    }
                }
                val hash = tmp.sha256()
                if (!hash.equals(spec.sha256, ignoreCase = true)) {
                    tmp.delete()
                    error("SHA-256 mismatch: $hash")
                }
                if (modelFile.exists()) modelFile.delete()
                tmp.renameTo(modelFile)
                onProgress(1f)
                return modelFile
            }.onFailure { error ->
                errors += "${modelUrl.substringBefore("?")}: ${error.message}"
            }
        }
        if (tmp.exists()) tmp.delete()
        error("模型下载失败，所有下载源均不可用 / Model download failed: ${errors.joinToString(" | ")}")
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

        val decodeMaxLongEdge = memorySafeMaxLongEdge(photo.width, photo.height, params.maxLongEdge)
        if (decodeMaxLongEdge < params.maxLongEdge) {
            mark("memory capped maxLongEdge ${params.maxLongEdge} -> $decodeMaxLongEdge")
        }
        val decodeStart = System.currentTimeMillis()
        var original = decodeScaledBitmap(context, photo.uri, decodeMaxLongEdge)
        val decodeMs = System.currentTimeMillis() - decodeStart
        mark("decode ${decodeMs}ms size=${original.width}x${original.height}")
        if (max(photo.width, photo.height) > decodeMaxLongEdge) {
            mark("downsampled source because original long edge exceeded $decodeMaxLongEdge")
        }
        onProgress(0.25f)

        val depthStart = System.currentTimeMillis()
        val rawDepth = runDepthModel(original, modelFile, params.modelThreads, params.useGpu) { runtime ->
            mark(runtime)
        }
        val modelMs = System.currentTimeMillis() - depthStart
        mark("model inference ${modelMs}ms")
        val depthPostStart = System.currentTimeMillis()
        val depthSmall = smoothDepth(rawDepth, params.blurRadius, params.invertDepth)
        onProgress(0.55f)

        val depthBitmap = depthToBitmap(depthSmall)
        val depthPath = File(dir, "depth.png")
        FileOutputStream(depthPath).use { depthBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val depthPostMs = System.currentTimeMillis() - depthPostStart
        mark("depth post ${depthPostMs}ms output=${depthBitmap.width}x${depthBitmap.height}")
        onProgress(0.7f)

        val sbsStart = System.currentTimeMillis()
        val vr = runCatching {
            makeParallelSbs(original, depthSmall, params.depthScale, params.fillRadius)
        }.getOrElse { error ->
            if (error !is OutOfMemoryError || max(original.width, original.height) <= 960) throw error
            val retryLongEdge = max(960, (max(original.width, original.height) * 0.72f).roundToInt())
            val scale = retryLongEdge.toFloat() / max(original.width, original.height).toFloat()
            mark("sbs oom retry maxLongEdge=$retryLongEdge error=${error.message}")
            val reduced = Bitmap.createScaledBitmap(
                original,
                max(1, (original.width * scale).roundToInt()),
                max(1, (original.height * scale).roundToInt()),
                true,
            )
            original.recycle()
            original = reduced
            makeParallelSbs(original, depthSmall, params.depthScale, params.fillRadius)
        }
        val sbsMs = System.currentTimeMillis() - sbsStart
        mark("sbs ${sbsMs}ms output=${vr.width}x${vr.height}")
        val writeStart = System.currentTimeMillis()
        val vrPath = File(dir, "vr_sbs.jpg")
        FileOutputStream(vrPath).use { vr.compress(Bitmap.CompressFormat.JPEG, params.quality, it) }
        onProgress(0.9f)
        val outputWidth = vr.width
        val outputHeight = vr.height

        val paramsPath = File(dir, "params.json")
        val logPath = File(dir, "job.log")
        val totalBeforeFinalLog = System.currentTimeMillis() - start
        val timings = GenerationTimings(
            decodeMs = decodeMs,
            modelMs = modelMs,
            depthPostMs = depthPostMs,
            sbsMs = sbsMs,
            writeMs = 0L,
            totalMs = totalBeforeFinalLog,
        )
        paramsPath.writeText(params.toJson(photo, original, vr, timings), Charsets.UTF_8)
        val writeMs = System.currentTimeMillis() - writeStart
        val finalTimings = timings.copy(writeMs = writeMs, totalMs = System.currentTimeMillis() - start)
        paramsPath.writeText(params.toJson(photo, original, vr, finalTimings), Charsets.UTF_8)
        mark("write ${writeMs}ms")
        mark("done total=${finalTimings.totalMs}ms")
        logPath.writeText(log.toString(), Charsets.UTF_8)
        depthBitmap.recycle()
        original.recycle()
        vr.recycle()

        return VrCacheEntry(
            photoKey = photo.cacheKey,
            version = version,
            outputPath = vrPath.absolutePath,
            depthPath = depthPath.absolutePath,
            paramsPath = paramsPath.absolutePath,
            logPath = logPath.absolutePath,
            width = outputWidth,
            height = outputHeight,
            createdAt = System.currentTimeMillis(),
        )
    }

    fun generateSbsBitmap(
        source: Bitmap,
        params: VrGenerationParams,
        onModelProgress: (Float) -> Unit = {},
        onRuntimeInfo: (String) -> Unit = {},
    ): Bitmap {
        val modelFile = modelManager.ensureModel(params.depthModel, onModelProgress)
        val safeMaxLongEdge = memorySafeMaxLongEdge(source.width, source.height, params.maxLongEdge)
        val working = if (max(source.width, source.height) > safeMaxLongEdge) {
            val scale = safeMaxLongEdge.toFloat() / max(source.width, source.height).toFloat()
            Bitmap.createScaledBitmap(source, max(1, (source.width * scale).roundToInt()), max(1, (source.height * scale).roundToInt()), true)
        } else {
            source
        }
        val rawDepth = runDepthModel(working, modelFile, params.modelThreads, params.useGpu, onRuntimeInfo)
        val depthSmall = smoothDepth(rawDepth, params.blurRadius, params.invertDepth)
        return makeParallelSbs(working, depthSmall, params.depthScale, params.fillRadius)
    }

    private fun memorySafeMaxLongEdge(width: Int, height: Int, requested: Int): Int {
        val longEdge = max(width, height).takeIf { it > 0 } ?: requested
        val shortEdge = min(width, height).takeIf { it > 0 } ?: longEdge
        val ratio = shortEdge.toFloat() / longEdge.toFloat()
        val heap = Runtime.getRuntime()
        val heapAvailable = heap.maxMemory() - (heap.totalMemory() - heap.freeMemory())
        val budget = min(160L * 1024L * 1024L, max(48L * 1024L * 1024L, (heapAvailable * 55L) / 100L))
        val bytesPerSourcePixel = 18f
        val requestedPixels = requested.toFloat() * requested.toFloat() * ratio
        val requestedBytes = requestedPixels * bytesPerSourcePixel
        if (requestedBytes <= budget) return requested
        val safe = sqrt(budget.toDouble() / (ratio.toDouble() * bytesPerSourcePixel.toDouble())).roundToInt()
        return safe.coerceIn(768, requested)
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
        val gpuDiagnostics = mutableListOf<String>()
        gpuDiagnostics += "device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT}"
        val options = Interpreter.Options().setNumThreads(modelThreads.coerceIn(1, 8))
        if (!useGpu) {
            gpuDiagnostics += "gpu=disabled"
        } else {
            var compatibility: CompatibilityList? = null
            val compatibilityResult = runCatching {
                CompatibilityList().also { compatibility = it }
            }
            val compatible = compatibilityResult.getOrNull()?.isDelegateSupportedOnThisDevice
            gpuDiagnostics += "compat=${compatible ?: "unknown"}"
            compatibilityResult.exceptionOrNull()?.let { error ->
                gpuDiagnostics += "compatError=${error.shortMessage()}"
            }
            runCatching {
                val delegateOptions = if (compatible == true) {
                    compatibility?.bestOptionsForThisDevice
                } else {
                    null
                }
                gpuDelegate = if (delegateOptions != null) GpuDelegate(delegateOptions) else GpuDelegate()
                options.addDelegate(gpuDelegate)
                gpuDiagnostics += "delegateCreate=ok"
            }.onFailure { error ->
                gpuDiagnostics += "delegateCreate=failed:${error.shortMessage()}"
                gpuDelegate?.close()
                gpuDelegate = null
            }.also {
                runCatching { compatibility?.close() }
            }
        }
        var delegateActive = false
        val interpreter = runCatching { Interpreter(model, options) }.onSuccess {
            delegateActive = gpuDelegate != null
            gpuDiagnostics += "interpreterCreate=ok"
        }.getOrElse {
            gpuDiagnostics += "interpreterCreate=failed:${it.shortMessage()}"
            gpuDelegate?.close()
            gpuDelegate = null
            onRuntimeInfo?.invoke("tflite gpu delegate fallback: ${it.shortMessage()}")
            Interpreter(model, Interpreter.Options().setNumThreads(modelThreads.coerceIn(1, 8)))
        }
        val threadCount = modelThreads.coerceIn(1, 8)
        onRuntimeInfo?.invoke("tflite runtime threads=$threadCount requestedGpu=$useGpu delegateActive=$delegateActive ${gpuDiagnostics.joinToString(" ")}")
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
            onRuntimeInfo?.invoke("tflite gpu run failed; fallback cpu threads=$threadCount error=${error.message}")
            cpuInterpreter.run(input, output)
            cpuInterpreter.close()
            delegateActive = false
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
        val fill = fillRadius.coerceIn(1, 32)
        val depthScaling = depthScale / w.toFloat()
        val out = Bitmap.createBitmap(w * 2, h, Bitmap.Config.ARGB_8888)
        val srcRow = IntArray(w)
        val leftRow = IntArray(w)
        val dxForX = IntArray(w) { x -> (x * 517 / max(1, w - 1)).coerceIn(0, 517) }

        for (y in 0 until h) {
            source.getPixels(srcRow, 0, w, 0, y, w, 1)
            srcRow.copyInto(leftRow)
            val depthBase = (y * 517 / max(1, h - 1)).coerceIn(0, 517) * 518
            for (x in w - 1 downTo 0) {
                val d = depth[depthBase + dxForX[x]]
                val shift = (d.coerceIn(0f, 1f) * 255f * depthScaling).toInt().coerceIn(0, w - 1)
                val color = srcRow[x]
                for (offset in 0 until fill) {
                    val leftX = (x + shift + offset).coerceIn(0, w - 1)
                    leftRow[leftX] = color
                }
            }
            out.setPixels(leftRow, 0, w, 0, y, w, 1)
            out.setPixels(srcRow, 0, w, w, y, w, 1)
        }
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
        onRuntimeInfo: (String) -> Unit = {},
        onProgress: (Float, Int, Int, Int, Long) -> Unit,
    ): VideoCacheEntry {
        val vrParams = params.toVrParams()
        val version = params.cacheVersion()
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
            BitmapFactory.decodeFile(firstFrameCache.absolutePath)?.also { onRuntimeInfo("frame cache hit frame=0 version=$version") }
                ?: frameGenerator.generateSbsBitmap(first.copy(Bitmap.Config.ARGB_8888, false), vrParams, onModelProgress, onRuntimeInfo)
        } else {
            frameGenerator.generateSbsBitmap(first.copy(Bitmap.Config.ARGB_8888, false), vrParams, onModelProgress, onRuntimeInfo).also { bitmap ->
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
            version = version,
            params = vrParams,
            onModelProgress = onModelProgress,
            onRuntimeInfo = onRuntimeInfo,
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
                "encoderVersion=$VIDEO_ENCODER_VERSION",
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
        version: String,
        params: VrGenerationParams,
        onModelProgress: (Float) -> Unit,
        onRuntimeInfo: (String) -> Unit,
        onProgress: (Float, Int, Int, Int, Long) -> Unit,
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

        var renderer: InputSurfaceRenderer? = null
        try {
            val activeRenderer = InputSurfaceRenderer(surface, width, height)
            renderer = activeRenderer
            data class FrameResult(val bitmap: Bitmap, val generatedMs: Long)

            fun cachedOrGenerateFrame(frame: Int): FrameResult? {
                val frameCache = File(framesDir, "frame_${frame.toString().padStart(6, '0')}.jpg")
                BitmapFactory.decodeFile(frameCache.absolutePath)?.let {
                    onRuntimeInfo("frame cache hit frame=$frame version=$version")
                    return FrameResult(it, 0L)
                }
                val started = SystemClock.uptimeMillis()
                val timeUs = frame * 1_000_000L / fps
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: return null
                val generated = frameGenerator.generateSbsBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false), params, onModelProgress, onRuntimeInfo)
                FileOutputStream(frameCache).use { generated.compress(Bitmap.CompressFormat.JPEG, params.quality, it) }
                bitmap.recycle()
                return FrameResult(generated, SystemClock.uptimeMillis() - started)
            }

            for (frame in 0 until totalFrames) {
                val frameResult = if (frame == 0) {
                    FrameResult(firstSbs, 0L)
                } else {
                    cachedOrGenerateFrame(frame) ?: continue
                }
                val sbs = frameResult.bitmap
                val presentationTimeUs = frame * 1_000_000L / fps
                activeRenderer.draw(sbs, presentationTimeUs)
                if (frame != 0) sbs.recycle()
                drain(end = false)
                onProgress((frame + 1).toFloat() / totalFrames.toFloat(), frame + 1, totalFrames, fps, frameResult.generatedMs)
            }
            drain(end = true)
            if (audio != null && audioTrack >= 0 && muxerStarted) {
                copyAudio(audio.extractor, muxer, audioTrack)
                mark("audio copied")
            }
        } finally {
            firstSbs.recycle()
            renderer?.release()
            runCatching { codec.stop() }
            codec.release()
            surface.release()
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

private class InputSurfaceRenderer(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
) {
    private val eglDisplay: EGLDisplay
    private val eglContext: EGLContext
    private val eglSurface: EGLSurface
    private val program: Int
    private val textureId: Int
    private val positionBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to initialize EGL" }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) { "Unable to choose EGL config" }
        val eglConfig = configs[0] ?: error("No EGL config")
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            eglConfig,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0,
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context" }
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window surface" }
        makeCurrent()

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        textureId = createTexture()
        positionBuffer = floatBufferOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )
        texCoordBuffer = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
        )
    }

    fun draw(bitmap: Bitmap, presentationTimeUs: Long) {
        makeCurrent()
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        GLES20.glUniform1i(textureHandle, 0)

        positionBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeUs * 1000L)
        check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "Unable to swap EGL buffers" }
    }

    fun release() {
        makeCurrent()
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        GLES20.glDeleteProgram(program)
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }

    private fun makeCurrent() {
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "Unable to make EGL context current" }
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        check(linkStatus[0] == GLES20.GL_TRUE) { "Unable to link GL program: ${GLES20.glGetProgramInfoLog(program)}" }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "Unable to compile GL shader: ${GLES20.glGetShaderInfoLog(shader)}" }
        return shader
    }

    private fun floatBufferOf(vararg values: Float): FloatBuffer {
        return ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
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
    BackHandler(enabled = screen is AppScreen.Gallery && state.homeTab == "albums" && state.selectedAlbumId != null) {
        viewModel.closeAlbum()
    }

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
                selectedTab = state.generatedTab,
                onTabChange = viewModel::setGeneratedTab,
                onToggleVersion = viewModel::toggleGeneratedVersion,
                onSetGeneratedColumns = { viewModel.setPageColumns("generated", it) },
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
                onOpenScoped = viewModel::openScopedPhoto,
                onSettings = viewModel::openSettings,
                onSetTab = viewModel::setHomeTab,
                onSetGeneratedTab = viewModel::setGeneratedTab,
                onSetPageColumns = viewModel::setPageColumns,
                onToggleGeneratedVersion = viewModel::toggleGeneratedVersion,
                onOpenAlbum = viewModel::openAlbum,
                onCloseAlbum = viewModel::closeAlbum,
                onLoadMoreAll = viewModel::loadMoreAllMedia,
                onLoadMoreAlbum = viewModel::loadMoreSelectedAlbum,
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
    onOpenScoped: (Int, Set<String>, Int, Int) -> Unit,
    onSettings: () -> Unit,
    onSetTab: (String) -> Unit,
    onSetGeneratedTab: (String) -> Unit,
    onSetPageColumns: (String, Int) -> Unit,
    onToggleGeneratedVersion: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onCloseAlbum: () -> Unit,
    onLoadMoreAll: () -> Unit,
    onLoadMoreAlbum: () -> Unit,
    onSaveGenerated: (List<Int>) -> Unit,
    onReplaceOriginal: (List<Int>) -> Unit,
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
    var lastPinchAt by remember { mutableStateOf(0L) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val systemItems = remember(state.photos) { state.photos.filterNot { it.generatedVirtual } }
    val albumItems = remember(systemItems, state.selectedAlbumId) {
        state.selectedAlbumId?.let { bucketId -> systemItems.filter { it.bucketId == bucketId } } ?: emptyList()
    }
    val visibleItems = remember(systemItems, albumItems, state.homeTab, state.selectedAlbumId) {
        if (state.homeTab == "albums" && state.selectedAlbumId != null) albumItems else systemItems
    }
    val photoIndexByKey = remember(state.photos) { state.photos.mapIndexed { index, item -> item.cacheKey to index }.toMap() }
    val visibleScopeKeys = remember(visibleItems, state.homeTab, state.selectedAlbumId) {
        if (state.homeTab == "generated") emptySet() else visibleItems.map { it.cacheKey }.toSet()
    }
    val selectedIndexes = remember(selectedKeys, photoIndexByKey) {
        if (selectedKeys.isEmpty()) emptyList() else selectedKeys.mapNotNull { photoIndexByKey[it] }
    }
    val allGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = state.galleryScrollIndex.coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = state.galleryScrollOffset.coerceAtLeast(0),
    )
    val albumListGridState = rememberLazyGridState()
    val albumDetailGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = state.galleryScrollIndex.coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = state.galleryScrollOffset.coerceAtLeast(0),
    )
    val gridState = if (state.homeTab == "albums" && state.selectedAlbumId != null) albumDetailGridState else allGridState
    val timelineColumns = if (state.homeTab == "albums" && state.selectedAlbumId != null) state.albumDetailColumns else state.allColumns

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xfff7f8f9))) {
        Scaffold(
            topBar = {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedKeys.isEmpty()) {
                        val title = when {
                            state.homeTab == "albums" && state.selectedAlbumId != null -> state.albums.firstOrNull { it.bucketId == state.selectedAlbumId }?.name ?: lang.t("相册", "Album")
                            state.homeTab == "albums" -> lang.t("相册", "Albums")
                            state.homeTab == "generated" -> lang.t("生成", "Generated")
                            else -> lang.t("全部", "All")
                        }
                        if (state.homeTab == "albums" && state.selectedAlbumId != null) {
                            OutlinedButton(onClick = onCloseAlbum) { Text(lang.t("返回", "Back")) }
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            } else if (state.homeTab == "generated") {
                Box(Modifier.fillMaxSize().padding(padding).padding(bottom = 78.dp)) {
                    ManageScreen(
                        state = state,
                        embedded = true,
                        onBack = {},
                        selectedTab = state.generatedTab,
                        onTabChange = onSetGeneratedTab,
                        onToggleVersion = onToggleGeneratedVersion,
                        onSetGeneratedColumns = { onSetPageColumns("generated", it) },
                        onDeleteVersion = onDeleteVersion,
                        onOpenGenerated = onOpenGenerated,
                        onOpenGeneratedVideo = onOpenGeneratedVideo,
                        onSaveVideo = onSaveVideo,
                        onDeleteVideo = onDeleteVideo,
                        onSaveVideos = onSaveVideos,
                        onDeleteVideos = onDeleteVideos,
                        onRegenerateVideos = onRegenerateVideos,
                        onSaveImages = onSaveImages,
                        onDeleteImages = onDeleteImages,
                        onRegenerateImages = onRegenerateImages,
                    )
                }
            } else if (state.homeTab == "albums" && state.selectedAlbumId == null) {
                Box(Modifier.fillMaxSize().padding(padding).padding(bottom = 78.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.albumListColumns),
                        state = albumListGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .discreteColumnPinch(
                                columns = state.albumListColumns,
                                onColumns = { onSetPageColumns("albumList", it) },
                                onPinchActivity = { lastPinchAt = System.currentTimeMillis() },
                            ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.albums, key = { it.bucketId }) { album ->
                            AlbumTile(album = album, lang = lang, columns = state.albumListColumns, onClick = { onOpenAlbum(album.bucketId) })
                        }
                    }
                    GridScrollbar(albumListGridState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
                    GridToTopButton(albumListGridState, lang, Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 18.dp))
                }
            } else {
                TimelineGrid(
                    items = visibleItems,
                    state = state,
                    lang = lang,
                    gridState = gridState,
                    columns = timelineColumns,
                    selectedKeys = selectedKeys,
                    onColumns = { nextColumns ->
                        lastPinchAt = System.currentTimeMillis()
                        onSetPageColumns(if (state.homeTab == "albums") "albumDetail" else "all", nextColumns)
                    },
                    onItemClick = { photo ->
                        if (selectedKeys.isNotEmpty()) {
                            selectedKeys = if (photo.cacheKey in selectedKeys) selectedKeys - photo.cacheKey else selectedKeys + photo.cacheKey
                        } else {
                            val index = state.photos.indexOfFirst { it.cacheKey == photo.cacheKey }
                            if (index >= 0) {
                                if (state.homeTab == "albums" || state.homeTab == "all") {
                                    onOpenScoped(index, visibleScopeKeys, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
                                } else {
                                    onOpen(index, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
                                }
                            }
                        }
                    },
                    onItemLongClick = { photo ->
                        if (System.currentTimeMillis() - lastPinchAt > 700L) {
                    selectedKeys = selectedKeys + photo.cacheKey
                        }
                    },
                    onNearEnd = {
                        when {
                            state.homeTab == "albums" && state.selectedAlbumId != null -> onLoadMoreAlbum()
                            state.homeTab == "all" -> onLoadMoreAll()
                        }
                    },
                    footerText = if (state.homeTab == "albums" && state.selectedAlbumId != null) {
                        val total = state.albums.firstOrNull { it.bucketId == state.selectedAlbumId }?.count ?: visibleItems.size
                        val loaded = visibleItems.size
                        when {
                            state.albumLoading -> lang.t("正在加载更多...", "Loading more...")
                            loaded < total -> lang.t("已加载 $loaded/$total，继续下滑加载更多", "Loaded $loaded/$total; scroll for more")
                            else -> lang.t("已加载全部 $loaded 项", "Loaded all $loaded items")
                        }
                    } else if (state.homeTab == "all") {
                        when {
                            state.allLoading -> lang.t("正在预取更多...", "Prefetching more...")
                            !state.allExhausted -> lang.t("已加载 ${visibleItems.size} 项，继续下滑加载更多", "Loaded ${visibleItems.size} items; scroll for more")
                            else -> lang.t("已加载全部 ${visibleItems.size} 项", "Loaded all ${visibleItems.size} items")
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 78.dp),
                )
            }
        }
        HomeBottomNav(
            current = state.homeTab,
            lang = lang,
            onSelect = {
                selectedKeys = emptySet()
                onSetTab(it)
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineGrid(
    items: List<PhotoItem>,
    state: UiState,
    lang: AppLanguage,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    columns: Int,
    selectedKeys: Set<String>,
    onColumns: (Int) -> Unit,
    onItemClick: (PhotoItem) -> Unit,
    onItemLongClick: (PhotoItem) -> Unit,
    onNearEnd: () -> Unit,
    footerText: String?,
    modifier: Modifier = Modifier,
) {
    val restoreRenderLimit = min(items.size, max(1200, gridState.firstVisibleItemIndex + 400))
    var renderLimit by remember(items.firstOrNull()?.cacheKey) { mutableStateOf(restoreRenderLimit) }
    LaunchedEffect(items.size) {
        renderLimit = renderLimit.coerceAtLeast(restoreRenderLimit).coerceAtMost(items.size)
    }
    LaunchedEffect(items.size, gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (lastVisible > renderLimit - 80 && renderLimit < items.size) {
                    renderLimit = min(items.size, renderLimit + 1200)
                }
                if (lastVisible > renderLimit - 80) {
                    onNearEnd()
                }
            }
    }
    val renderedItems = remember(items, renderLimit) { items.take(renderLimit) }
    val cells = remember(renderedItems, lang, renderLimit, items.size) {
        val result = mutableListOf<TimelineCell>()
        var lastDay = Long.MIN_VALUE
        renderedItems.forEach { item ->
            val day = localEpochDay(item.modifiedTime)
            if (day != lastDay) {
                val label = dateGroupLabel(item.modifiedTime, lang)
                result += TimelineCell.Header(day, label)
                lastDay = day
            }
            result += TimelineCell.Media(item)
        }
        if (footerText != null || renderLimit < items.size) {
            result += TimelineCell.Footer(renderLimit, items.size, footerText)
        }
        result
    }
    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .discreteColumnPinch(columns = columns, onColumns = onColumns),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(
                items = cells,
                key = { cell ->
                    when (cell) {
                        is TimelineCell.Header -> "header_${cell.day}"
                        is TimelineCell.Media -> cell.item.cacheKey
                        is TimelineCell.Footer -> "footer_${cell.loaded}_${cell.total}_${cell.text.orEmpty()}"
                    }
                },
                span = { cell -> if (cell is TimelineCell.Media) GridItemSpan(1) else GridItemSpan(maxLineSpan) },
            ) { cell ->
                when (cell) {
                    is TimelineCell.Header -> Text(
                        text = cell.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                    is TimelineCell.Media -> {
                        val photo = cell.item
                        val imageState = state.states[photo.cacheKey] ?: VrState.NORMAL
                        val statusText = if (photo.kind == MediaKind.VIDEO) {
                            (state.videoStates[photo.cacheKey] ?: VideoVrState.NORMAL).label(lang)
                        } else {
                            imageState.label(lang)
                        }
                        PhotoTile(
                            photo = photo,
                            statusText = statusText,
                            entry = state.entries[photo.cacheKey],
                            lang = lang,
                            columns = columns,
                            selected = photo.cacheKey in selectedKeys,
                            onClick = { onItemClick(photo) },
                            onLongClick = { onItemLongClick(photo) },
                        )
                    }
                    is TimelineCell.Footer -> Text(
                        text = cell.text ?: lang.t("已显示 ${cell.loaded}/${cell.total}，继续下滑自动加载更多", "Showing ${cell.loaded}/${cell.total}; keep scrolling to load more"),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xff777777),
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                    )
                }
            }
        }
        GridScrollbar(gridState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
        GridToTopButton(gridState, lang, Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 18.dp))
    }
}

@Composable
private fun GridToTopButton(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    lang: AppLanguage,
    modifier: Modifier = Modifier,
) {
    if (gridState.firstVisibleItemIndex <= 18) return
    val scope = rememberCoroutineScope()
    Button(
        onClick = { scope.launch { gridState.animateScrollToItem(0) } },
        modifier = modifier,
    ) {
        Text(lang.t("顶部", "Top"))
    }
}

@Composable
private fun AlbumTile(album: AlbumItem, lang: AppLanguage, columns: Int, onClick: () -> Unit) {
    val reduction = columnFontReduction(columns)
    val titleSize = (16 - reduction).coerceAtLeast(9).sp
    val countSize = (12 - reduction).coerceAtLeast(8).sp
    val textGap = if (columns >= 7) 4.dp else 8.dp
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .background(androidx.compose.ui.graphics.Color(0xffe8eaed), RoundedCornerShape(18.dp)),
        ) {
            AsyncMediaThumbnail(album.coverKind, album.coverUri, 420, ContentScale.Crop, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(textGap))
        Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = titleSize)
        Text(lang.t("${album.count} 项", "${album.count} items"), color = androidx.compose.ui.graphics.Color(0xff777777), fontSize = countSize, maxLines = 1)
    }
}

@Composable
private fun GridScrollbar(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = gridState.layoutInfo
    val total = layoutInfo.totalItemsCount
    val visible = layoutInfo.visibleItemsInfo
    if (total <= 0 || visible.isEmpty()) return
    val first = visible.first().index
    val visibleCount = visible.size.coerceAtLeast(1)
    val thumbFraction = (visibleCount.toFloat() / total.toFloat()).coerceIn(0.06f, 1f)
    val topFraction = (first.toFloat() / total.toFloat()).coerceIn(0f, 1f - thumbFraction)
    val scope = rememberCoroutineScope()
    var trackHeight by remember { mutableStateOf(1) }
    fun scrollToTrackPosition(y: Float) {
        val fraction = (y / trackHeight.toFloat()).coerceIn(0f, 1f)
        val targetIndex = (fraction * (total - 1).toFloat()).roundToInt().coerceIn(0, total - 1)
        scope.launch { gridState.scrollToItem(targetIndex) }
    }
    Box(
        modifier
            .width(22.dp)
            .fillMaxSize()
            .padding(vertical = 12.dp)
            .onSizeChanged { trackHeight = it.height.coerceAtLeast(1) }
            .pointerInput(total, trackHeight) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> scrollToTrackPosition(offset.y) },
                    onVerticalDrag = { change, _ ->
                        scrollToTrackPosition(change.position.y)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(androidx.compose.ui.graphics.Color(0x22000000), RoundedCornerShape(3.dp)),
        )
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight(thumbFraction)
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = size.height * topFraction / thumbFraction }
                .background(androidx.compose.ui.graphics.Color(0x99000000), RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun HomeBottomNav(current: String, lang: AppLanguage, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(34.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "all" to lang.t("全部", "All"),
            "albums" to lang.t("相册", "Albums"),
            "generated" to lang.t("生成", "Generated"),
        ).forEach { (key, label) ->
            val selected = key == current
            Surface(
                color = if (selected) androidx.compose.ui.graphics.Color(0xff6650a4) else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onSelect(key) },
            ) {
                Text(
                    text = label,
                    color = if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xff333333),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RoundedPillTabs(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(28.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (key, label) ->
            val active = key == selected
            Surface(
                color = if (active) androidx.compose.ui.graphics.Color(0xfff0eaff) else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelect(key) },
            ) {
                Text(
                    text = label,
                    color = if (active) androidx.compose.ui.graphics.Color(0xff3f2a78) else androidx.compose.ui.graphics.Color(0xff333333),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

private fun Modifier.discreteColumnPinch(
    columns: Int,
    onColumns: (Int) -> Unit,
    onPinchActivity: () -> Unit = {},
): Modifier = pointerInput(columns) {
    val stepThresholdPx = 72f
    var lastStepAt = 0L
    var baselineSpan: Float? = null
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                val centroid = pressed.centroid()
                val currentSpan = pressed.span(centroid)
                val startSpan = baselineSpan ?: currentSpan.also { baselineSpan = it }
                val delta = currentSpan - startSpan
                val now = System.currentTimeMillis()
                val next = when {
                    delta > stepThresholdPx -> columns - 1
                    delta < -stepThresholdPx -> columns + 1
                    else -> columns
                }.coerceIn(1, 8)
                if (next != columns && now - lastStepAt > 140L) {
                    onPinchActivity()
                    onColumns(next)
                    baselineSpan = currentSpan
                    lastStepAt = now
                }
                event.changes.forEach { it.consume() }
            } else {
                baselineSpan = null
            }
        }
    }
}

private fun columnFontReduction(columns: Int): Int = ((columns - 5).coerceAtLeast(0) * 2).coerceAtMost(6)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    photo: PhotoItem,
    statusText: String,
    entry: VrCacheEntry?,
    lang: AppLanguage,
    columns: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val compact = columns >= 7
    val labelSize = (14 - columnFontReduction(columns)).coerceAtLeast(8).sp
    val playSize = (20 - columnFontReduction(columns)).coerceAtLeast(10).sp
    val labelPaddingH = if (compact) 3.dp else 5.dp
    val labelPaddingV = if (compact) 1.dp else 2.dp
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
        AsyncMediaThumbnail(photo.kind, photo.uri, 420, ContentScale.Crop, Modifier.fillMaxSize())
        if (photo.kind == MediaKind.VIDEO) {
            Text(
                text = "▶",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = playSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(androidx.compose.ui.graphics.Color(0x99000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 1.dp else 4.dp),
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
            text = statusText,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = labelSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(androidx.compose.ui.graphics.Color(0xaa000000), RoundedCornerShape(3.dp))
                .padding(horizontal = labelPaddingH, vertical = labelPaddingV),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManageScreen(
    state: UiState,
    embedded: Boolean = false,
    onBack: () -> Unit,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    onToggleVersion: (String) -> Unit,
    onSetGeneratedColumns: (Int) -> Unit,
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
    val tab = selectedTab
    var selectedImageKeys by remember { mutableStateOf(setOf<String>()) }
    var selectedVideoKeys by remember { mutableStateOf(setOf<String>()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xfff5f6f7))
            .then(if (embedded) Modifier else Modifier.statusBarsPadding())
            .padding(if (embedded) 0.dp else 16.dp),
    ) {
        if (!embedded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text(lang.t("返回", "Back")) }
                Spacer(Modifier.width(12.dp))
                Text(lang.t("生成管理", "Generated Manager"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
        RoundedPillTabs(
            options = listOf("images" to lang.t("图片", "Images"), "videos" to lang.t("视频", "Videos")),
            selected = tab,
            onSelect = onTabChange,
        )
        Spacer(Modifier.height(8.dp))
        if (tab == "videos") {
            val videoGridState = rememberLazyGridState()
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
                Box(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.generatedColumns),
                        state = videoGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.White)
                            .discreteColumnPinch(columns = state.generatedColumns, onColumns = onSetGeneratedColumns)
                            .padding(8.dp),
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
                                        text = "${videoState.label(lang)} ${(job?.progress?.times(100f) ?: 0f).roundToInt()}%  当前${job?.currentFrameMs ?: 0}ms 平均${job?.avgFrameMs ?: 0}ms",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.BottomStart).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(5.dp),
                                    )
                                }
                            }
                        }
                    }
                    GridScrollbar(videoGridState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
                    GridToTopButton(videoGridState, lang, Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 18.dp))
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
                val imageGridState = rememberLazyGridState()
                Box(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.generatedColumns),
                        state = imageGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.White)
                            .discreteColumnPinch(columns = state.generatedColumns, onColumns = onSetGeneratedColumns)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.cacheVersions.forEach { summary ->
                            val items = state.managedCacheItems.filter { it.entry.version == summary.version }
                            val expanded = summary.version in state.expandedGeneratedVersions
                            item(
                                key = "version_header_${summary.version}",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(androidx.compose.ui.graphics.Color(0xfff5f6f7), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lang.t("版本：${summary.version}", "Version: ${summary.version}"),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        OutlinedButton(onClick = { onToggleVersion(summary.version) }) {
                                            Text(if (expanded) lang.t("收起", "Collapse") else lang.t("展开", "Expand"))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lang.t("${summary.count} 张  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB", "${summary.count} images  ${(summary.bytes / 1024f / 1024f).roundToInt()} MB"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color(0xff555555),
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedButton(onClick = { onDeleteVersion(summary.version) }) {
                                            Text(lang.t("删除", "Delete"))
                                        }
                                    }
                                }
                            }
                            if (expanded) {
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
                                        GeneratedSbsThumbnail(File(item.entry.outputPath), 360, ContentScale.Crop, Modifier.fillMaxSize(), lang)
                                        if (selectionKey in selectedImageKeys) SelectionBadge()
                                    }
                                }
                            }
                        }
                    }
                    GridScrollbar(imageGridState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
                    GridToTopButton(imageGridState, lang, Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 18.dp))
                }
            }
        }
    }
}

@Composable
private fun GeneratedSbsThumbnail(file: File, maxSide: Int, contentScale: ContentScale, modifier: Modifier = Modifier, lang: AppLanguage) {
    var bitmap by remember(file.absolutePath, maxSide) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(file.absolutePath, maxSide) { mutableStateOf(false) }
    LaunchedEffect(file.absolutePath, maxSide) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { decodeSbsLeftPreview(file, maxSide) }.getOrNull()
        }
        bitmap = loaded
        failed = loaded == null
    }
    when {
        bitmap != null -> Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
        failed -> Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)), contentAlignment = Alignment.Center) {
            Text(lang.t("暂无预览", "No preview"), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
        }
        else -> Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(22.dp))
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

    val viewerItems = remember(state.photos, state.viewerScopeKeys, startIndex) {
        val indexed = state.photos.mapIndexed { index, item -> index to item }
        if (state.viewerScopeKeys.isNotEmpty()) {
            indexed.filter { (_, item) -> item.cacheKey in state.viewerScopeKeys }
                .ifEmpty { indexed.filter { it.first == startIndex } }
        } else {
            indexed
        }
    }
    val initialPage = viewerItems.indexOfFirst { it.first == startIndex }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { viewerItems.size }
    LaunchedEffect(pagerState.currentPage) {
        viewerItems.getOrNull(pagerState.currentPage)?.first?.let(onIndexChanged)
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
            val (sourceIndex, photo) = viewerItems.getOrNull(page) ?: return@HorizontalPager
            if (photo.kind == MediaKind.VIDEO) {
                val generated = state.videoEntries[photo.cacheKey]
                val job = state.videoJobs.firstOrNull { it.item.cacheKey == photo.cacheKey }
                val videoState = state.videoStates[photo.cacheKey] ?: VideoVrState.NORMAL
                VideoPlayer(
                    uri = generated?.let { Uri.fromFile(File(it.outputPath)) } ?: photo.uri,
                    modifier = Modifier.fillMaxSize(),
                    active = page == pagerState.currentPage,
                    controlsVisible = controlsVisible,
                    statusText = "状态：${videoState.label(lang)}  ${job?.currentFrame ?: 0}/${job?.totalFrames ?: 0}  当前帧${job?.currentFrameMs ?: 0}ms  平均帧${job?.avgFrameMs ?: 0}ms",
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
                        StatusOverlay(vrState, lang, onRetry = { onRetry(sourceIndex) })
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
                val currentPair = viewerItems.getOrNull(pagerState.currentPage)
                val currentIndex = currentPair?.first ?: startIndex
                val currentItem = currentPair?.second
                Text(
                    text = currentItem?.displayName.orEmpty(),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onVr(currentIndex) }) {
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
                OutlinedButton(onClick = { onOpenDebug(currentIndex) }) { Text(lang.t("调试", "Debug")) }
            }
            val current = viewerItems.getOrNull(pagerState.currentPage)?.second
            if (current?.kind != MediaKind.VIDEO) Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Color(0x99000000)).padding(10.dp),
            ) {
                if (current != null) {
                    Text(
                        text = imageLoadedLine(state, viewerItems.getOrNull(pagerState.currentPage)?.first ?: startIndex, lang),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = imageQueueLine(state, viewerItems.getOrNull(pagerState.currentPage)?.first ?: startIndex, lang),
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
    val imageLogLines = remember(entry?.logPath, entry?.createdAt) { entry?.logPath?.let { readLastLines(File(it), 16) } ?: emptyList() }
    val imageParamsLines = remember(entry?.paramsPath, entry?.createdAt) { entry?.paramsPath?.let { readLastLines(File(it), 16) } ?: emptyList() }
    val imageTimings = remember(entry?.paramsPath, entry?.createdAt) { entry?.paramsPath?.let { readImageTimings(File(it)) } }
    val videoLogLines = remember(videoEntry?.logPath, videoEntry?.createdAt) { videoEntry?.logPath?.let { readLastLines(File(it), 18) } ?: emptyList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xff111315))
            .statusBarsPadding()
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
            text = "${photo?.displayName.orEmpty()}  ${lang.t("状态", "State")}: ${if (isVideo) photo?.let { state.videoStates[it.cacheKey] } ?: VideoVrState.NORMAL else vrState}",
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (isVideo) {
                DebugSection(lang.t("视频运行状态", "Video runtime")) {
                    DebugLine("State", "${photo?.let { state.videoStates[it.cacheKey] } ?: VideoVrState.NORMAL}")
                    DebugLine("Frame", "${videoJob?.currentFrame ?: 0}/${videoJob?.totalFrames ?: 0}")
                    DebugLine("Progress", "${((videoJob?.progress ?: 0f) * 100f).roundToInt()}%")
                    DebugLine("FPS", "${videoJob?.fps ?: videoEntry?.fps ?: 30}")
                    DebugLine(lang.t("当前有效帧", "Current active frame"), "${videoJob?.currentFrameMs ?: 0}ms")
                    DebugLine(lang.t("平均有效帧", "Avg active frame"), "${videoJob?.avgFrameMs ?: 0}ms")
                    DebugLine("Model", videoJob?.modelId?.ifBlank { state.settings.videoModelId } ?: state.settings.videoModelId)
                    DebugLine("Cache", videoJob?.cacheVersion?.ifBlank { "-" } ?: "-")
                    DebugLine("Threads", "${videoJob?.modelThreads ?: state.settings.videoModelThreads}")
                    DebugLine("GPU requested", "${videoJob?.useGpu ?: state.settings.videoUseGpu}")
                    DebugLine("Runtime", videoJob?.runtimeInfo?.ifBlank { "-" } ?: "-")
                    DebugLine("Output", videoEntry?.outputPath ?: "-")
                    DebugLine("Error", videoJob?.error ?: "-")
                }
                DebugSection(lang.t("视频生成日志", "Video log")) {
                    (videoLogLines.ifEmpty { state.logs.filter { it.contains(photo?.displayName.orEmpty()) || it.contains("video runtime") }.take(12) }).forEach {
                        DebugMonoLine(it)
                    }
                }
            } else {
                DebugSection(lang.t("图片运行状态", "Image runtime")) {
                    DebugLine("State", "$vrState")
                    DebugLine("Job", job?.let { "${it.state} ${(it.progress * 100f).roundToInt()}%" } ?: "-")
                    DebugLine("Cache", entry?.version ?: "-")
                    DebugLine("Output", entry?.let { "${it.width}x${it.height}" } ?: "-")
                    DebugLine("Model", state.settings.imageModelId)
                    DebugLine("Threads", "${state.settings.modelThreads}")
                    DebugLine("GPU requested", "${state.settings.useGpu}")
                    DebugLine("Error", job?.error ?: "-")
                }
                DebugSection(lang.t("性能诊断", "Performance")) {
                    DebugLine(lang.t("解码", "Decode"), imageTimings?.decodeMs?.let { "${it}ms" } ?: "-")
                    DebugLine(lang.t("模型推理", "Model"), imageTimings?.modelMs?.let { "${it}ms" } ?: "-")
                    DebugLine(lang.t("深度后处理", "Depth post"), imageTimings?.depthPostMs?.let { "${it}ms" } ?: "-")
                    DebugLine(lang.t("SBS 合成", "SBS"), imageTimings?.sbsMs?.let { "${it}ms" } ?: "-")
                    DebugLine(lang.t("写文件", "Write"), imageTimings?.writeMs?.let { "${it}ms" } ?: "-")
                    DebugLine(lang.t("总耗时", "Total"), imageTimings?.totalMs?.let { "${it}ms" } ?: "-")
                }
                Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebugImagePanel(lang.t("原图", "Source"), photo?.uri, Modifier.weight(1f), lang)
                    DebugImagePanel(lang.t("深度图", "Depth"), entry?.let { Uri.fromFile(File(it.depthPath)) }, Modifier.weight(1f), lang)
                    DebugImagePanel("VR SBS", entry?.let { Uri.fromFile(File(it.outputPath)) }, Modifier.weight(1f), lang)
                }
                Spacer(Modifier.height(8.dp))
                DebugSection(lang.t("图片参数", "Image params")) {
                    imageParamsLines.forEach { DebugMonoLine(it) }
                }
                DebugSection(lang.t("图片生成日志", "Image log")) {
                    imageLogLines.forEach { DebugMonoLine(it) }
                }
            }
            DebugSection(lang.t("队列", "Queue")) {
                state.videoJobs.take(4).forEach {
                    DebugMonoLine("${it.state.label(lang)} ${(it.progress * 100f).roundToInt()}% f=${it.currentFrame}/${it.totalFrames} ${it.item.displayName}")
                }
                state.jobs.take(4).forEach {
                    DebugMonoLine("${it.state.label(lang)} ${(it.progress * 100f).roundToInt()}% ${it.photoItem.displayName}")
                }
            }
            DebugSection(lang.t("最近日志", "Recent app logs")) {
                state.logs.take(10).forEach { DebugMonoLine(it) }
            }
        }
    }
}

@Composable
private fun DebugSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(androidx.compose.ui.graphics.Color(0xff202326))
            .padding(10.dp),
    ) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = androidx.compose.ui.graphics.Color(0xffc8cdd2), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(96.dp))
        Text(value, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DebugMonoLine(value: String) {
    Text(
        value,
        color = androidx.compose.ui.graphics.Color.White,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun readLastLines(file: File, count: Int): List<String> {
    return runCatching {
        if (!file.exists()) emptyList() else file.readLines(Charsets.UTF_8).takeLast(count)
    }.getOrDefault(emptyList())
}

private fun readImageTimings(file: File): GenerationTimings? {
    return runCatching {
        if (!file.exists()) return@runCatching null
        val text = file.readText(Charsets.UTF_8)
        val decode = jsonLongValue(text, "decodeMs") ?: return@runCatching null
        GenerationTimings(
            decodeMs = decode,
            modelMs = jsonLongValue(text, "modelMs") ?: 0L,
            depthPostMs = jsonLongValue(text, "depthPostMs") ?: 0L,
            sbsMs = jsonLongValue(text, "sbsMs") ?: 0L,
            writeMs = jsonLongValue(text, "writeMs") ?: 0L,
            totalMs = jsonLongValue(text, "totalMs") ?: 0L,
        )
    }.getOrNull()
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
    val context = LocalContext.current
    val safeSide = maxSide.coerceIn(96, 320)
    var bitmap by remember(uri, maxSide) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxSide) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(uri, Size(safeSide, safeSide), null)
                } else if (kind == MediaKind.IMAGE) {
                    decodeScaledBitmap(context, uri, safeSide)
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
        Box(modifier.background(androidx.compose.ui.graphics.Color(0xff202326)))
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    active: Boolean,
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

    LaunchedEffect(uri, videoView, active) {
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

    LaunchedEffect(uri, videoView, active) {
        val view = videoView ?: return@LaunchedEffect
        if (active) {
            if (!view.isPlaying) view.start()
        } else {
            if (view.isPlaying) view.pause()
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
                        if (active) {
                            start()
                            isPlaying = true
                        } else {
                            pause()
                            isPlaying = false
                        }
                    }
                    videoView = this
                }
            },
            update = { view ->
                if (view.tag != uri) {
                    view.tag = uri
                    view.setVideoURI(uri)
                } else if (!active) {
                    if (view.isPlaying) view.pause()
                }
                videoView = view
            },
            modifier = Modifier.fillMaxSize(),
        )
        DisposableEffect(uri) {
            onDispose {
                runCatching { videoView?.stopPlayback() }
                mediaPlayer = null
                videoView = null
            }
        }
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

private fun decodeSbsLeftPreview(file: File, maxLongEdge: Int): Bitmap {
    require(file.exists()) { "Generated file missing: ${file.absolutePath}" }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    require(width > 0 && height > 0) { "Invalid generated image bounds: ${file.absolutePath}" }
    val leftWidth = (width / 2).coerceAtLeast(1)
    var sample = 1
    while (max(leftWidth / sample, height / sample) > maxLongEdge.coerceAtLeast(96)) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    FileInputStream(file).use { input ->
        val decoder = BitmapRegionDecoder.newInstance(input, false)
            ?: error("Unable to open generated preview decoder: ${file.absolutePath}")
        return try {
            decoder.decodeRegion(Rect(0, 0, leftWidth, height), options)
                ?: error("Unable to decode generated preview: ${file.absolutePath}")
        } finally {
            decoder.recycle()
        }
    }
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

private fun VrGenerationParams.toJson(photo: PhotoItem, source: Bitmap, output: Bitmap, timings: GenerationTimings): String {
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
          "timings": {
            "decodeMs": ${timings.decodeMs},
            "modelMs": ${timings.modelMs},
            "depthPostMs": ${timings.depthPostMs},
            "sbsMs": ${timings.sbsMs},
            "writeMs": ${timings.writeMs},
            "totalMs": ${timings.totalMs}
          },
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

private fun VideoGenerationParams.cacheVersion(): String {
    return "${toVrParams().cacheVersion()}_$VIDEO_ENCODER_VERSION"
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
}

private fun Throwable.shortMessage(): String {
    val type = this::class.java.simpleName.ifBlank { "Error" }
    val message = message?.replace('\n', ' ')?.replace('\r', ' ')?.take(180)
    return if (message.isNullOrBlank()) type else "$type:$message"
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
