# Parallel VR Gallery

本地 Android 平行眼 VR 图库。图片可本地生成并缓存 SBS VR 图；v0.11 开始系统图库会同时加载图片和视频。

## v0.11 功能 / What v0.11 adds

- 系统图库混排图片和视频；视频缩略图带播放标记。
- 图片自动预加载仍只处理图片，遇到视频会跳过并继续向外补位。
- 图片 VR 页顶部显示当前文件名，底部显示已加载、队列中和最近生成耗时。
- 视频页使用播放器浏览；按钮为“加入 VR 队列”，不会自动参与图片预加载。
- 视频 VR 使用独立队列，最多 3 个视频任务并行，不占用图片当前图 worker 或后台预加载 worker。
- 视频生成按源 FPS 尽量抽帧，逐帧复用图片 SBS 生成算法，输出 H.264 MP4，并尝试保留原音频轨。
- 生成管理页的视频分页显示视频任务/缓存缩略图，READY 视频可播放、保存到系统相册或删除缓存。
- 调试页对视频显示队列状态、帧进度、FPS、输出路径和错误日志。

## Build

```powershell
$env:JAVA_HOME='D:\AndroidBuild\jdk17\jdk-17.0.19+10'
$env:ANDROID_SDK_ROOT='D:\Android\Sdk'
D:\AndroidBuild\gradle\gradle-8.10.2\bin\gradle.bat assembleDebug
```

## Model asset

APK 不内置 `depth_anything_v2.tflite`。首次生成 VR 前会从 GitHub Release 下载模型并校验 SHA-256：

```text
https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite
```

## Third-party credits

本项目使用 Android / Jetpack Compose、TensorFlow Lite、TFLite GPU Delegate 和 Depth-Anything-V2 模型。详见 `NOTICE.md`。
