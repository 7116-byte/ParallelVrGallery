# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 图库应用：按“全部 / 相册 / 生成”浏览系统图片和视频，并在手机本地生成平行眼 SBS VR 缓存。

## v2.30 更新

- 视频生成新增“视频深度 worker 数”，默认 2 个独立深度模型会话，用于并行跑原始深度推理。
- 视频流水线调整为：解码 -> 多 depth worker 原始深度 -> 按帧序时序平滑/后处理/SBS -> 编码。
- 时序平滑仍严格按帧号顺序执行，避免多 worker 破坏连续帧稳定性。
- 视频缓存版本升级到 `encoderV12`，并在缓存名里加入 `dw1/dw2`，避免不同 worker 设置混用旧结果。
- 调试页和日志显示实际 depth worker 数，便于判断当前视频到底按 1 worker 还是 2 worker 生成。
- README 修复为 UTF-8，避免 GitHub 页面出现乱码。

## 功能

- 系统图库式首页：`全部 / 相册 / 生成` 三个入口。
- 全部页按时间浏览图片和视频；相册页按系统相册/文件夹浏览。
- 图片 VR：本地生成深度图和 SBS 平行眼图，支持缓存、调试页和调试包分享。
- 视频 VR：手动加入队列后逐帧生成 SBS 视频，并尽量保留原音频。
- 生成页：按图片/视频管理已生成、生成中、暂停和失败的缓存任务。
- 模型按需下载：APK 不内置大模型，首次生成前下载并校验 SHA-256。

## 诊断说明

如果视频调试页里显示：

```text
深度推理 360ms
SBS 39ms
缓存写入 21ms
编码提交 17ms
```

说明瓶颈主要在深度模型推理。v2.30 的 `视频深度 worker 数 = 2` 会尝试并行跑两个独立模型会话；如果显存或内存吃紧，可以在设置里降回 1。

如果调试页显示：

```text
GPU requested true
Force GPU true
Runtime ... delegateActive=true
```

说明 TFLite GPU delegate 已启用。若生成失败，可以关闭 GPU 回到 CPU 路线。

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

覆盖安装升级通常不会清理 App 私有外部目录，所以本地已下载过模型时不需要重新下载；卸载 App 会由系统清理该目录。

## 第三方项目与依赖

本项目是新的 Android App，没有复制旧 APK 的源码或资源。主要依赖和参考包括：

- Android SDK / Jetpack Compose / AndroidX / Material3：App 框架和 UI。
- TensorFlow Lite / TFLite GPU Delegate：本地深度模型推理。
- Depth Anything V2：深度估计模型来源。
- SamSeenX/ComfyUI_SSStereoscope：SBS 生成思路参考，包括深度平滑、相对视差和边缘填充。

更详细说明见 [NOTICE.md](NOTICE.md)。
