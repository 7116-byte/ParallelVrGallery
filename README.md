# 平行眼 VR 图库 / Parallel VR Gallery

本地 Android 图库应用，可从普通照片生成并缓存平行眼 SBS VR 图片。

Local Android gallery for generating and caching parallel-eye SBS VR images from normal photos.

## v0.9 功能 / What v0.9 does

- 读取系统相册图片 / Reads system photos with `MediaStore`.
- 使用 Compose 网格和全屏左右滑动浏览 / Browses photos in a Compose grid and full-screen pager.
- 点开图片默认进入 VR 模式并开始生成当前图 / Opening a photo enters VR mode by default and starts generating the current image.
- 主界面支持双指缩放缩略图网格，并会改变每行列数 / The main grid supports pinch zoom and changes column count.
- 主界面缩放只响应双指，单指滑动和长按独立处理 / Grid zoom only responds to two fingers; one-finger scroll and long-press are handled separately.
- 从图片页返回主界面时会记住点开时的行位置 / Returning from the viewer keeps the viewed image near the row where it was opened.
- 主要界面切换带缩放淡入淡出动画 / Primary screens use scale and fade transitions.
- 主界面长按进入多选模式，支持批量保存已生成 VR 图或批量尝试替换原图 / Long-press enters multi-select mode for batch saving generated VR images or attempting original replacement.
- 保存生成图时只显示保存中弹窗，不重新扫描相册 / Saving generated images shows a lightweight saving dialog and does not rescan the gallery.
- 自动预加载会跳过已生成缓存并向外补位，保持队列围绕当前图继续填充 / Auto prefetch skips existing cache and fills farther slots around the current image.
- 设置页提供语言选择，默认中文 / The settings page includes language selection and defaults to Chinese.
- 设置页提供生成 worker 数、模型 CPU 线程数和 TFLite GPU Delegate 尝试开关 / Settings include generation worker count, model CPU thread count, and a TFLite GPU Delegate toggle.
- 首次生成前下载 Depth-Anything-V2 TFLite 模型 / Downloads the Depth-Anything-V2 TFLite model before first generation.
- 使用平滑深度图、相对视差和前景填充生成平行眼 SBS 图 / Generates SBS images with smoothed depth, relative disparity, and foreground filling.
- 当前图片前后缓存支持自动模式：先生成后1、前1到后3、前3，完成后若未切图自动扩到 `5`，再扩到 `10` / Auto prefetch starts at 3 each side, then expands to 5 and 10 if the user stays on the same image.
- 手动预加载窗口可选 `3`、`5`、`10`；队列顺序为后1、前1、后2、前2 / Manual prefetch supports `3`, `5`, and `10`; queue order is next1, previous1, next2, previous2.
- 生成结果缓存在 App 私有外部目录 / Caches generated output under the app private external files directory.
- 当前 READY 图片可导出调试包 / Exports a debug zip for the current READY image:
  - `source_preview.jpg`
  - `depth.png`
  - `vr_sbs.jpg`
  - `params.json`
  - `job.log`
- 主界面提供设置入口，可调整语言、预加载、深度强度、平滑、填充、输出尺寸和深度反转 / Main screen has a settings page for language, prefetch, depth scale, blur, fill, output size, and depth inversion.
- App 保存到系统图库的 VR 图片会带 `PVG_VR_` 标记，并从主图库列表中过滤 / VR images saved by the app are marked with `PVG_VR_` and filtered out of the main gallery.
- 主界面提供生成管理入口，图片缓存可按版本删除，视频入口已预留 / Main screen has a generated manager; image caches can be deleted by version, and the video entry is reserved.
- 生成管理页按图片/视频分页，图片页按版本显示缩略图区域，点击缩略图进入对应 VR 缓存 / Generated manager has image/video tabs; image versions show scrollable thumbnail areas that open the selected VR cache.
- 已生成 VR 图片会作为主界面缩略图显示 / Generated VR images are used as main-gallery thumbnails.
- VR 浏览页控件 3 秒后自动隐藏，只有单指点击会重新显示 / VR viewer controls auto-hide after 3 seconds and reappear only on single tap.
- VR 信息栏显示当前预加载窗口，不再显示图片文件名 / The VR info bar shows the current prefetch window instead of the file name.
- VR SBS 图贴合显示，中间覆盖一条细分隔线；同步双指缩放时左右半图互不遮挡 / VR SBS images are adjacent with a thin center overlay divider; synchronized pinch zoom keeps halves clipped.
- VR SBS 双指缩放带平滑动画 / VR SBS pinch zoom uses smoothed animation.
- 调试界面显示最近队列状态 / The debug screen shows recent queue state.
- 覆盖安装更新会保留已下载模型；卸载后重装通常需要重新下载 / Updating over an existing install keeps the downloaded model; uninstalling usually removes it.

## 构建 / Build

Open this folder in Android Studio, or use Gradle after setting `local.properties`:

```properties
sdk.dir=D:\\Android\\Sdk
```

Then run:

```powershell
.\gradlew.bat assembleDebug
```

当前工作区原本没有把 Android SDK/Gradle 放在 `PATH` 里，所以项目支持直接用 Android Studio 打开，或使用 D 盘 SDK 构建。

The current workspace originally had no Android SDK/Gradle on `PATH`, so the project is structured for Android Studio or a D-drive SDK install.

## 模型资产 / Model asset

Release APK 不再内置 `depth_anything_v2.tflite`，首次使用 VR 生成时会从 GitHub 模型资产 Release 下载并校验 SHA-256。原始模型文件较大，不提交到 Git；如果要离线调试，可手动把模型放到 App 外部私有目录。

The release APK no longer bundles `depth_anything_v2.tflite`. The app downloads it from the GitHub model asset release before first VR generation and verifies SHA-256. The raw model is intentionally not committed to Git.

```text
https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite
```

## 发布速度 / Release speed

模型文件单独放在 `model-assets-v1` Release 中，普通 APK Release 不再重复上传模型，因此后续发版只需要上传约 24MB 的 APK。

The model file is hosted once in the `model-assets-v1` release. Regular app releases no longer upload the model again, so later releases only upload an APK around 24MB.

目前真正的加速方式就是避免重复上传大模型；如果还嫌 GitHub Release 慢，可以后续增加镜像下载源，例如 Cloudflare R2、阿里云 OSS 或 jsDelivr，但 APK 仍以 GitHub Release 为主。

The main speedup is avoiding repeated model uploads. If GitHub Release is still slow, future builds can add mirror sources such as Cloudflare R2, Alibaba OSS, or jsDelivr while keeping GitHub Release as the primary channel.

## 第三方项目 / Third-party credits

本项目使用 Android/Jetpack Compose、TensorFlow Lite 和 Depth-Anything-V2 模型。详细说明见 [`NOTICE.md`](NOTICE.md)。

This project uses Android/Jetpack Compose, TensorFlow Lite, and the Depth-Anything-V2 model. See [`NOTICE.md`](NOTICE.md) for details.
