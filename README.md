# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 图库应用：按“全部 / 相册 / 生成”浏览系统图片和视频，并在手机本地生成平行眼 SBS VR 缓存。

## v2.28 更新

- 简化 GPU 设置：只保留“开启 GPU 加速”和“GPU 加速模式选择”。
- 开启 GPU 后默认强制 GPU，不再失败后偷偷回退 CPU；失败会直接失败或生成异常结果，并在日志中说明。
- GPU 模式默认改为已经验证有效的 `ACCURATE_UNSET`。
- 关闭 GPU 才会恢复 CPU 路线，避免用户不知道为什么突然变慢。
- 图片缓存版本升级到 `depthV6`，视频缓存版本升级到 `encoderV10`。

## v2.27 更新

- 设置页新增 `GPU 后端测试模式`：自动、精确模式、强制 OpenGL、强制 OpenCL、系统推荐。
- 设置页新增 `强制 GPU，不回退 CPU`：用于快速测试 GPU 后端，失败或输出异常也不会偷偷切 CPU。
- 强制 GPU 模式不会跑 CPU 参考校验，避免测试图片/视频时第一帧又慢回 CPU。
- 调试页显示 GPU mode、Force GPU；日志细化为 gpuMode、forceGpu、outputRange、flat、run failed 等信息。
- 图片缓存版本升级到 `depthV5`，视频缓存版本升级到 `encoderV9`，隔离旧自动回退缓存。

## v2.26 更新

- 恢复 GPU 加速尝试：不再只试一种 TFLite GPU 配置，而是依次尝试 `accurate-unset / accurate-opengl / accurate-opencl / best`。
- 默认先用禁止精度损失的 GPU 精确模式，目标是保留 GPU 速度，同时避免深度输出全黑或语义错乱。
- GPU 输出异常时先切换下一个 GPU 模式，所有 GPU 模式都失败后才回退 CPU。
- 调试日志会显示 `gpuMode`、`tflite gpu recovered mode=...`，用于判断哪种 GPU 后端真正可用。
- 图片缓存版本升级到 `depthV4`，视频缓存版本升级到 `encoderV8`，隔离旧 CPU 回退缓存。

## v2.25 更新

- GPU 深度增加可信度校验：第一次 GPU 推理会和 CPU 参考深度做相似度对比。
- 如果 GPU 深度不是全黑、但和 CPU 参考差异过大，会自动回退 CPU，并在日志显示 `gpuFallback=validation`。
- 图片缓存版本升级到 `depthV3`，视频缓存版本升级到 `encoderV7`，用于隔离旧 GPU 深度结果。
- 这个版本用于验证“视频乱跳是否来自 GPU 深度语义错乱”：如果回退 CPU 后不跳，说明 GPU 深度不可信。

## v2.24 更新

- 修复图片 VR 深度图可能全黑、导致图片没有 3D 视差的问题：如果 GPU 深度输出范围异常，会自动回退到 CPU 重跑该帧/图片。
- 图片缓存版本升级到 `depthV2`，之前已经生成的黑深度图缓存不会继续被当作 READY 结果。
- 视频按真实帧数生成时，FPS 改为优先使用 `帧数 / 原视频时长` 推导，避免 1000 多帧仍按 30fps 封装导致时间轴不匹配。
- 视频缓存版本升级到 `encoderV6`，需要重新生成视频以使用新的时间轴策略。

## v2.23 更新

- 视频取帧优先使用系统真实帧数和按帧序号解码，减少特定大幅运动视频中按时间随机取帧导致的重复帧/跳帧抽动。
- 视频生成日志会记录 `frameMode=index/time`、元数据帧数和原始 FPS，方便判断问题来自取帧还是深度生成。
- 图片 VR 生成复用同参数的 TFLite/GPU 深度模型会话，避免每张图片都重新创建 GPU Interpreter。

## 功能

- 系统图库式首页：全部、相册、生成三个入口。
- 全部页按时间浏览图片和视频；相册页按系统相册/文件夹浏览。
- 图片 VR：本地生成深度图和 SBS 平行眼图，支持缓存、调试页和调试包分享。
- 视频 VR：手动加入队列后逐帧生成 SBS 视频，并尽量保留原音频。
- 生成页：按图片/视频管理已生成、生成中、暂停和失败的缓存任务。
- 模型按需下载：APK 不内置大模型，首次生成前下载并校验 SHA-256。

## 诊断说明

如果图片调试页深度图全黑，v2.24 会自动尝试 CPU 回退，并在日志里写入 `gpuFallback=flatOutput`。

如果视频仍然在大幅运动处抽动，优先看视频日志：

- `frameMode=index`：系统支持按帧序号取帧。
- `frameMode=time`：设备或视频不支持按帧序号，只能按时间戳取帧。
- `metadataFrames` 和 `fps`：用于判断输出视频时间轴是否接近原视频。

如果调试页显示：

```text
GPU requested true
Runtime ... delegateActive=false
```

说明设置里请求了 GPU，但 TensorFlow Lite GPU delegate 实际没有启用，生成仍在 CPU 上运行。

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
- Depth-Anything-V2：深度估计模型来源。
- SamSeenX/ComfyUI_SSStereoscope：SBS 生成思路参考，包括深度平滑、相对视差和边缘填充。

更详细说明见 [NOTICE.md](NOTICE.md)。
