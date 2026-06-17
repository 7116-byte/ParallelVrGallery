# 平行眼 VR 图库 / Parallel VR Gallery

本地 Android 平行眼 VR 图库。App 可以浏览系统图片和视频，在手机本地生成并缓存平行眼 SBS VR 图片；视频可作为播放器浏览，也可以手动加入 VR 生成队列。

## v1.21 功能

- 首页改为系统图库式结构：底部 `全部 / 相册 / 生成` 三个入口。
- `全部` 按日期分组显示系统图片和视频，视频带播放标记。
- `相册` 按系统 MediaStore 文件夹聚合，显示封面、名称和数量；进入相册后只浏览该相册内容。
- `生成` 直接替代旧“生成管理”，只显示已生成、生成中、暂停或失败的图片/视频 VR 缓存任务。
- Viewer 的左右滑动范围会跟随来源：从全部进入就浏览全部，从相册进入就浏览该相册，从生成进入就只浏览生成集合。
- 生成页仍支持图片/视频分页、多选、保存、重新生成和删除。

## 构建

```powershell
$env:JAVA_HOME='D:\AndroidBuild\jdk17\jdk-17.0.19+10'
$env:ANDROID_SDK_ROOT='D:\Android\Sdk'
D:\AndroidBuild\gradle\gradle-8.10.2\bin\gradle.bat assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 模型资产

APK 不内置 `depth_anything_v2.tflite`。首次生成 VR 前，App 会从 GitHub Release 下载模型并校验 SHA-256：

```text
https://github.com/7116-byte/ParallelVrGallery/releases/download/model-assets-v1/depth_anything_v2.tflite
```

已下载模型保存在 App 私有外部目录。覆盖安装升级通常不会清除该目录，因此本地已有模型时不需要重复下载；卸载 App 会由系统清理私有目录。

## 第三方项目

本项目使用 Android、Jetpack Compose、TensorFlow Lite、TFLite GPU Delegate，以及 Depth-Anything-V2 模型。SBS 生成思路参考了 `SamSeenX/ComfyUI_SSStereoscope` 中的深度平滑、相对视差和边缘填充做法。详细说明见 `NOTICE.md`。
