# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 图库应用：按“全部 / 相册 / 生成”浏览系统图片和视频，并在手机本地生成平行眼 SBS VR 缓存。

## v2.19 更新

- 修复升级或刷新后，未完成的视频生成任务如果不在首页最近加载窗口内，会从生成页消失的问题。
- 恢复视频队列标签时，会根据持久化的 `VIDEO_id_size_modified` 构造虚拟视频项，确保生成页仍能看到暂停/生成中任务。
- 修复视频重新生成时，旧任务失败回调覆盖新任务状态，导致页面显示失败但后台仍在生成的问题。

## v2.18 更新

- 视频/图片生成的 TFLite Runtime 诊断补全 GPU 细节：设备信息、CompatibilityList 兼容性、delegate 创建结果、Interpreter 创建结果。
- GPU delegate 创建失败或 Interpreter 挂载失败时，不再只显示 `delegateActive=false`，会显示具体异常摘要和 CPU 回退原因。
- 新增 `tensorflow-lite-gpu-api` 依赖，用于获取官方兼容性推荐选项。

## v2.17 更新

- 调试页视频耗时改为“当前有效帧 / 平均有效帧”，避免锁屏、后台等待或唤醒时间污染平均帧耗时。
- 视频生成期间启动前台服务并申请部分唤醒锁，减少切后台或锁屏后停止生成的问题。
- 生成页视频卡片不再直接显示“保存 / 删除”按钮，管理操作继续通过长按多选完成。
- 检查更新的当前版本更新为 `v2.17`。

## 诊断说明

如果调试页显示：

```text
GPU requested true
Runtime tflite runtime threads=4 requestedGpu=true delegateActive=false
```

说明设置里请求了 GPU，但 TensorFlow Lite GPU delegate 实际没有启用，生成仍在 CPU 上运行。此时单帧 3 秒左右主要来自 CPU 模型推理、深度后处理和 SBS 合成链路。

## 功能

- 系统图库式首页：全部、相册、生成三个入口。
- 全部页按时间浏览图片和视频；相册页按系统相册/文件夹浏览。
- 图片 VR：本地生成深度图和 SBS 平行眼图，支持缓存、调试页和调试包分享。
- 视频 VR：手动加入队列后逐帧生成 SBS 视频，并尽量保留原音频。
- 生成页：按图片/视频管理已生成、生成中、暂停和失败的缓存任务。
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

覆盖安装升级通常不会清理 App 私有外部目录，所以本地已下载过模型时不需要重新下载；卸载 App 会由系统清理该目录。

## 第三方项目与依赖

本项目是新的 Android App，没有复制旧 APK 的源码或资源。主要依赖和参考包括：

- Android SDK / Jetpack Compose / AndroidX / Material3：App 框架和 UI。
- TensorFlow Lite / TFLite GPU Delegate：本地深度模型推理。
- Depth-Anything-V2：深度估计模型来源。
- SamSeenX/ComfyUI_SSStereoscope：SBS 生成思路参考，包括深度平滑、相对视差和边缘填充。

更详细说明见 [NOTICE.md](NOTICE.md)。
