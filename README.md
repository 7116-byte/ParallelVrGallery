# 平行眼 VR 图库 / Parallel VR Gallery

本地 Android 图库应用，可从普通照片生成并缓存平行眼 SBS VR 图片。

Local Android gallery for generating and caching parallel-eye SBS VR images from normal photos.

## v1 功能 / What v1 does

- 读取系统相册图片 / Reads system photos with `MediaStore`.
- 使用 Compose 网格和全屏左右滑动浏览 / Browses photos in a Compose grid and full-screen pager.
- 使用 Depth-Anything-V2 TFLite 模型生成本地平行眼 SBS 图 / Generates a local parallel-eye SBS image.
- 当前图片前后缓存窗口可选 `3`、`5`、`10` / Configurable prefetch window.
- 生成结果缓存在 App 私有外部目录 / Caches generated output under the app private external files directory.
- 当前 READY 图片可导出调试包 / Exports a debug zip for the current READY image:
  - `source_preview.jpg`
  - `depth.png`
  - `vr_sbs.jpg`
  - `params.json`
  - `job.log`

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

Release APK 已经内置 `depth_anything_v2.tflite`。原始模型文件较大，不提交到 Git；如果要从源码构建，请把模型放到：

The release APK already bundles `depth_anything_v2.tflite`. The raw model is intentionally not committed to Git because it is a large binary; to build from source, place it at:

```text
app/src/main/assets/models/depth_anything_v2.tflite
```

## 第三方项目 / Third-party credits

本项目使用 Android/Jetpack Compose、TensorFlow Lite 和 Depth-Anything-V2 模型。详细说明见 [`NOTICE.md`](NOTICE.md)。

This project uses Android/Jetpack Compose, TensorFlow Lite, and the Depth-Anything-V2 model. See [`NOTICE.md`](NOTICE.md) for details.
