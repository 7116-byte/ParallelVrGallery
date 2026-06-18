# 平行眼 VR 图库 / Parallel VR Gallery

本地 Android 平行眼 VR 图库。App 可以浏览系统图片和视频，在手机本地生成并缓存平行眼 SBS VR 图片；视频可作为播放器浏览，也可以手动加入 VR 生成队列。

## v2.0 修复

- 相册详情改为按 `bucketId` 分页加载，点进相册先加载一页，继续下滑自动加载后续页，不再受首页最近 1800 项限制。
- 相册列表仍会后台全量聚合，数量保持完整；相册详情按页查询当前相册内容。
- 版本号提升到 `2.0`。

## v1.24 修复

- 修复全部页下滑几屏后可能闪退的问题：时间线日期 header key 改为稳定 epoch-day，避免跨年份同名日期导致 LazyGrid key 冲突。
- 图片和视频缩略图统一优先使用系统 `loadThumbnail`，避免下滑时大量完整图片解码造成内存压力。
- 缩略图占位去掉大量进度圈，降低列表滚动时的组合和绘制压力。
- 相册页启动时先显示最近媒体的临时聚合，后台再全量聚合系统相册并刷新，避免相册永远只有少数几个。

## v1.23 修复

- 修复 v1.21 首页改造后，大图库打开会卡住的问题：移除未选择状态下的全量 `indexOfFirst` 扫描，避免几万张媒体触发近似 N² 主线程计算。
- 首次打开只从 MediaStore 读取最近 1800 项媒体，避免启动阶段等待几万条记录；生成页仍可独立显示内部缓存。
- 全部/相册时间线改为渐进渲染，首屏先显示 1200 项，继续下滑自动加载更多，避免一次性把几万项交给 Compose。
- 生成页开始改为 App 内部生成库优先：图片/视频缓存即使不在本次系统相册扫描结果里，也会从 `params.json` / `video_params.txt` 恢复成生成页专用虚拟项。
- 内部生成库虚拟项不会出现在 `全部` 和 `相册` 页，避免污染普通图库浏览。

## v1.22 修复

- 修复几万张图片/视频图库加载时首页卡住的问题。
- 媒体加载阶段不再逐张图片/视频查找 VR 缓存，改为只扫描已有缓存目录后反查媒体项。
- UI 状态不再为每个普通未生成媒体创建 `NORMAL` 状态记录，只保留 READY/任务状态，减少大图库内存和主线程压力。
- `全部` 日期时间线改为线性生成 header/item，不再用 `groupBy` 复制大列表。
- 进入相册详情时不再复用全部页的大滚动位置，避免相册项较少时滚动越界。

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
