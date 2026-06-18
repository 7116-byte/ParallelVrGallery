# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 图库应用。它可以按“全部 / 相册 / 生成”浏览系统图片和视频，并在手机本地把图片或视频生成平行眼 SBS VR 缓存。

An Android gallery app for local parallel-eye SBS VR generation. It browses system photos/videos and keeps generated VR results in the app cache.

## v2.14 更新

- 双指改变列数的触发距离翻倍，减少轻微手势就跳列的情况。
- 相册主页标题和数量文字会随列数缩小：6/7/8 列逐级减小。

## v2.13 更新

- 双指缩放改为按整次手势累计跨度变化判断，手指离得远也能正常触发列数变化。
- 相册主页的相册列数和相册详情里的图片列数拆开保存，互不影响。

## v2.12 更新

- 全部、相册、生成三页的缩略图列数独立保存，互不影响。
- 每页初始列数改为 5，最小 1 列，最大 8 列。
- 双指缩放改为按列数离散变化，每次只增加或减少 1 列。
- 从 Viewer 返回后继续保留进入前的列数，避免列数恢复默认导致定位错位。

## v2.11 更新

- 双指缩放后的网格列数会保留到 Viewer 返回后，不再恢复默认列数导致定位错位。
- 首页顶部不再显示预加载档位，普通图库浏览更干净。
- 生成页图片 tab 改为按版本默认收起，展开后直接向下铺开图片。
- 生成页图片/视频 tab 都增加滚动条和回到顶部按钮。
- 视频 VR 生成改为按原视频 FPS 写入帧时间戳，修复先播封面+BGM、后播无声 3D 视频的问题。
- 视频缓存增加 `encoderV2` 标记，旧问题视频不会继续被识别为当前可用缓存。

## v2.10 更新

- 统一整理 `全部 / 相册 / 生成 / Viewer / 调试 / 设置` 的页面安全区、返回路径和状态刷新。
- 从生成页打开图片或视频后，返回会回到生成页原来的图片/视频子页；从独立管理页进入仍返回独立管理页。
- 生成页不再显示预加载说明；预加载信息只保留在“全部”和“相册详情”这类会触发图片预加载的页面。
- 底部入口和生成页图片/视频切换改为圆角点击反馈，减少方形闪烁。
- 图片生成完成后，生成页立即刷新版本统计和缩略图，不需要重启 App。
- 视频生成统计新增“当前帧耗时”，平均帧耗时只统计真实生成的帧，缓存命中帧不再污染统计。

## 功能

- 系统图库式首页：全部、相册、生成三页。
- 图片 VR：本地生成深度图和 SBS 平行眼图，支持缓存、调试页、调试包分享。
- 视频 VR：手动加入队列后逐帧生成 SBS 视频，并尽量保留原音频。
- 生成管理：图片/视频分栏，支持保存、删除、重新生成。
- 模型按需下载：APK 不内置大模型，首次生成前下载并校验。

## 构建

```powershell
$env:JAVA_HOME='D:\AndroidBuild\jdk17\jdk-17.0.19+10'
$env:ANDROID_SDK_ROOT='D:\Android\Sdk'
D:\AndroidBuild\gradle\gradle-8.10.2\bin\gradle.bat assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 模型资产

APK 不内置 `depth_anything_v2.tflite`。首次生成 VR 前，App 会从 GitHub Release 下载模型并校验 SHA-256。

覆盖安装升级通常不会清理 App 私有外部目录，所以本地已经下载过模型时不需要重新下载；卸载 App 会由系统清理该目录。

## 第三方项目与依赖

本项目是新的 Android App，没有复制旧 APK 的源码或资源。主要依赖和参考包括：

- Android SDK / Jetpack Compose / AndroidX / Material3：App 框架和 UI。
- TensorFlow Lite / TFLite GPU Delegate：本地深度模型推理。
- Depth-Anything-V2：深度估计模型来源。
- SamSeenX/ComfyUI_SSStereoscope：SBS 生成思路参考，包括深度平滑、相对视差和边缘填充。

更详细说明见 [NOTICE.md](NOTICE.md)。
