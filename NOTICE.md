# 第三方项目与依赖 / Third-Party Projects and Dependencies

本项目本身是一个新的 Android App。它没有复制旧 APK 的源码或资源；旧 APK 只用于早期可行性分析。

This is a new Android app. It does not copy source code or resources from the previous APK; that APK was only used for early feasibility analysis.

## Android / Google

- Android SDK, Android Gradle Plugin, Kotlin Android plugin, Jetpack Compose, AndroidX Activity/Core/Lifecycle/Material3.
- 用途 / Purpose: Android App 构建、原生 UI、相册权限、生命周期和 Compose 界面。
- 官方站点 / Official sites:
  - https://developer.android.com/
  - https://developer.android.com/compose

## TensorFlow Lite

- 依赖 / Dependency: `org.tensorflow:tensorflow-lite:2.16.1`
- 用途 / Purpose: 在手机本地运行深度估计 TFLite 模型。
- 官方站点 / Official site: https://www.tensorflow.org/lite

## Depth-Anything-V2 Model

- 模型文件 / Model file: `depth_anything_v2.tflite`
- 用途 / Purpose: 从普通图片估计深度图，再生成平行眼 SBS 图。
- 注意 / Note: 原始模型文件较大，没有提交进 Git；v0.2 起 APK 不再内置该模型，首次生成前会从 GitHub Release 下载并校验。
- 上游项目 / Upstream project: https://github.com/DepthAnything/Depth-Anything-V2

## Build Tools

- Gradle: https://gradle.org/
- Eclipse Temurin JDK: https://adoptium.net/temurin/
- GitHub CLI: https://cli.github.com/

## License Notes

请在分发 APK 或模型时同时遵守 Android SDK、TensorFlow Lite、Depth-Anything-V2 及其模型权重各自的许可证条款。

When redistributing the APK or model, comply with the license terms of Android SDK, TensorFlow Lite, Depth-Anything-V2, and the corresponding model weights.
