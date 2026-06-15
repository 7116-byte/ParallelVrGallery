# 平行眼 VR 图库 / Parallel VR Gallery

本地 Android 平行眼 VR 图库。图片可本地生成并缓存 SBS VR 图；视频可作为播放器浏览，并可手动加入视频 VR 生成队列。

## v0.11.4 修复

- 修复视频双击快进“识别了但原地跳转”的问题，改用底层播放器 seek。
- 视频页底部合并为一条控制区：播放/暂停、生成状态、时间和进度条放在一起，不再被额外信息层挡住。
- 视频进度条增加播放/暂停按钮。
- 检查更新结果提供“下载更新”按钮，会直接打开 GitHub APK 下载链接或 Release 页面。
- Android 13 及以上会补申请通知权限，视频生成通知才能正常出现在通知栏。

## v0.11.3 修复

- 视频 VR 生成会在通知栏显示进度，包含当前帧、总帧数和百分比；完成或失败也会更新通知。
- 视频页增加视频进度条和时间，跟随信息控件一起显示/隐藏。
- 视频页双击快进 5 秒、长按 2 倍速播放时会显示屏幕提示。
- 生成管理的视频页不再显示所有系统视频，只显示已加入队列、生成中、失败或已生成的视频。

## v0.11.2 修复

- 继续清理 App 内残留乱码，补齐主界面、设置、管理、调试、重试等位置。
- 视频播放页支持：
  - 单击显示/隐藏信息控件
  - 双击快进 5 秒
  - 长按期间 2 倍速播放，松手恢复正常速度
- 设置页新增“检查更新”，会查询 GitHub 最新 Release 并显示当前版本和最新版本。

## v0.11.1 修复

- 修复 App 内残留乱码，按钮、状态、设置页、管理页、调试页恢复中文优先显示。
- 修复覆盖安装升级后不会主动申请视频权限的问题：已有图片权限但缺少视频权限时，会单独申请 `READ_MEDIA_VIDEO`。
- 修复 GitHub README 偏英文的问题，说明文档恢复中文优先。
- VR 图片页底部信息改成固定结构：
  - 第一行：`已加载：前N 后M`
  - 第二行：`队列中：前N 后M`
  - 第三到第五行：最近三条生成结果，例如 `后2已生成（1234ms）`

## v0.11 功能

- 系统图库混排图片和视频；视频缩略图带播放标记。
- 图片自动预加载仍只处理图片，遇到视频会跳过并继续向外补位。
- 图片 VR 页顶部显示当前文件名。
- 视频页使用播放器浏览；按钮为“加入 VR 队列”，不会自动参与图片预加载。
- 视频 VR 使用独立队列，最多 3 个视频任务并行，不占用图片当前图 worker 或后台预加载 worker。
- 视频生成按源 FPS 尽量抽帧，逐帧复用图片 SBS 生成算法，输出 H.264 MP4，并尝试保留原音频轨。
- 生成管理页的视频分页显示视频任务/缓存缩略图，READY 视频可播放、保存到系统相册或删除缓存。
- 调试页对视频显示队列状态、帧进度、FPS、输出路径和错误日志。

## 构建

```powershell
$env:JAVA_HOME='D:\AndroidBuild\jdk17\jdk-17.0.19+10'
$env:ANDROID_SDK_ROOT='D:\Android\Sdk'
D:\AndroidBuild\gradle\gradle-8.10.2\bin\gradle.bat assembleDebug
```

## 模型资产

APK 不内置 `depth_anything_v2.tflite`。首次生成 VR 前会从 GitHub Release 下载模型并校验 SHA-256：

```text
https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite
```

## 第三方项目

本项目使用 Android / Jetpack Compose、TensorFlow Lite、TFLite GPU Delegate 和 Depth-Anything-V2 模型。详见 `NOTICE.md`。
