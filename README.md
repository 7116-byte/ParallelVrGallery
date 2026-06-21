# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 图库应用：按“全部 / 相册 / 生成”浏览系统图片和视频，并在手机本地生成平行眼 SBS VR 缓存。

## v2.22 更新

- 视频深度图增加轻量时序平滑，减少相邻帧深度忽深忽浅导致的画面抽动。
- 调试页新增视频单帧分段耗时：取帧、深度推理、时序平滑、深度后处理、SBS、缓存写入、编码提交。
- 视频缓存版本升级到 `encoderV4`，不会继续把旧版本未平滑的视频帧当作当前结果。
- 继续保留 v2.21 的真 3D 修复：深度输出使用 direct ByteBuffer，避免复用 GPU/TFLite 会话后视差丢失。

## 功能

- 系统图库式首页：全部、相册、生成三个入口。
- 全部页按时间浏览图片和视频；相册页按系统相册/文件夹浏览。
- 图片 VR：本地生成深度图和 SBS 平行眼图，支持缓存、调试页和调试包分享。
- 视频 VR：手动加入队列后逐帧生成 SBS 视频，并尽量保留原音频。
- 生成页：按图片/视频管理已生成、生成中、暂停和失败的缓存任务。
- 模型按需下载：APK 不内置大模型，首次生成前下载并校验 SHA-256。

## 诊断说明

如果调试页显示：

```text
GPU requested true
Runtime ... delegateActive=false
```

说明设置里请求了 GPU，但 TensorFlow Lite GPU delegate 实际没有启用，生成仍在 CPU 上运行。

如果视频“当前有效帧”很慢，可以看 v2.22 新增的分段耗时：

- `深度推理` 高：主要受模型、CPU/GPU delegate、线程数影响。
- `SBS` 高：主要受输出分辨率、深度强度、边缘填充影响。
- `缓存写入` 高：主要受图片质量和存储速度影响。
- `编码提交` 高：主要受硬件编码器和输出分辨率影响。

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
