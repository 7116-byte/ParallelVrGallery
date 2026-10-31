# 平行眼 VR 图库 / Parallel VR Gallery

一个本地 Android 创作工具：浏览系统图片与视频，在设备上生成平行眼 SBS 立体结果，并按来源和参数版本管理缓存。

## 3.1.00

- 跟随系统深浅色的统一界面，覆盖图库、相册、生成、Viewer、设置、任务、诊断与弹窗。
- 根据窗口和系统字号调整网格；设置选项自动换行，减少固定留白，浮动操作槽为末行内容留出空间。
- 权限说明、新手指南、只读任务面板及持续可找到的操作状态。
- 双眼共享同一位图绘制，保留同步缩放/拖动与双击复位；视频按当前已停稳页面和前后台状态播放。
- UI来源、生成版本、列数和位置可恢复；冷启动不自动重开 Viewer 或启动生成。
- 修复音轨标志映射，更新下载增加同证书校验。原包名、原证书和更新资产名 app-debug.apk 保持兼容。

图片当前优先级、来源边界、2→4→8 前后预加载、暂停原因、独立视频队列、强制 GPU 及原缓存/设置格式保持原规则。部分历史流畅度与设备兼容问题仍需持续真机确认，详见版本验收与技术交接。

## 开发

先读 [AGENTS.md](AGENTS.md)、[开发环境](docs/development.md)、[UI规范](docs/UI_SPEC.md) 和 [技术交接](docs/AI_HANDOFF.md)。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Verify.ps1 -StrictLint
```

脚本使用根目录 .local/ 中已恢复的 JDK、SDK、离线依赖及原签名；这些私密资产不在 Git 中。APK 输出 app/build/outputs/apk/debug/app-debug.apk。APK 不内置深度模型，首次生成按需下载并校验；原始媒体保持在系统图库中。

覆盖升级必须使用原证书。真实媒体生成、删除或替换测试需要明确范围授权；不能用卸载/清数据作为升级验收。

第三方框架、模型及算法参考见 [NOTICE.md](NOTICE.md)。Git 历史保留此前的技术说明与发布记录。Pro 为独立项目，不属于本普通版的构建或验收范围。
