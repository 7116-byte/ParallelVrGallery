# 第三方项目与依赖 / Third-Party Projects and Dependencies

本项目是一个新的 Android App，没有复制旧 APK 的源码或资源。

This is a new Android app. It does not copy source code or resources from the previous APK.

## Android / Google

- Android SDK, Android Gradle Plugin, Kotlin Android plugin
- Jetpack Compose, AndroidX Activity/Core/Lifecycle, Material3
- 用途：Android 构建、原生 UI、MediaStore 相册权限、生命周期和界面组件
- 官方网站：
  - https://developer.android.com/
  - https://developer.android.com/compose

## TensorFlow Lite

- 依赖：`org.tensorflow:tensorflow-lite:2.16.1`
- 依赖：`org.tensorflow:tensorflow-lite-gpu:2.16.1`
- 用途：在手机本地运行深度估计 TFLite 模型，并尝试使用 GPU delegate 加速
- 官方网站：https://www.tensorflow.org/lite

## Depth-Anything-V2

- 模型用途：从普通图片或视频帧估计深度图，再生成平行眼 SBS 图像
- 上游项目：https://github.com/DepthAnything/Depth-Anything-V2
- 说明：模型文件不提交到 Git；App 首次生成前从 Release 资产下载并校验

## SamSeenX/ComfyUI_SSStereoscope

- 项目：https://github.com/SamSeenX/ComfyUI_SSStereoscope
- 用途：SBS 生成算法思路参考
- 参考点：深度图平滑、相对视差、按列位移、边缘填充和调试输出思路
- 说明：本项目没有直接复制 ComfyUI 节点代码，而是在 Android/Kotlin 中按相同思路实现本地生成流程

## Build Tools

- Gradle：https://gradle.org/
- Eclipse Temurin JDK：https://adoptium.net/temurin/
- GitHub CLI：https://cli.github.com/

## License Notes

分发 APK、模型或派生版本时，请同时遵守 Android SDK、TensorFlow Lite、Depth-Anything-V2、模型权重以及参考项目各自的许可证条款。

When redistributing the APK, model, or derived builds, comply with the license terms of Android SDK, TensorFlow Lite, Depth-Anything-V2, the model weights, and referenced projects.
