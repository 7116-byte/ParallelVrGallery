# 16 KB 兼容提示复查（2026-10-31）

对连接手机和已安装的普通版 3.1.00 / 30100 实际检查。此次只诊断，没有升级依赖、替换 APK、生成媒体、修改系统兼容开关或隐藏提示。

## 实测

- 手机 API 37、arm64-v8a，`getconf PAGE_SIZE` 和启动页大小属性均为 **4096**。并非正在运行 16 KB 内核。
- 已安装 APK 拉取后的 SHA-256 为 `cd7f816384633bfdb842390f5ff2a57abc37ee21db7c8031a17c06bc62c1599d`，与公开发布件相同。
- `zipalign -c -P 16 -v 4` 通过：APK ZIP 对齐没有失败。
- `dumpsys package` 显示 DEBUGGABLE 和 `pageSizeCompat=4`；AOSP 中该位表示 ELF 未对齐。
- 真机系统弹窗明确报告 ELF 对齐失败；TFLite 两库为“LOAD 区段未对齐”，graphics-path 为“未知错误”。没有选择“不再显示”。

| arm64 原生库 | 来源（按库内容 SHA-256 与离线 AAR 核对） | 最小 PT_LOAD p_align | 判断 |
| --- | --- | --- | --- |
| libtensorflowlite_jni.so | org.tensorflow:tensorflow-lite:2.16.1 | 4096 / 0x1000 | 确认未达到 16 KB LOAD 对齐 |
| libtensorflowlite_gpu_jni.so | org.tensorflow:tensorflow-lite-gpu:2.16.1 | 4096 / 0x1000 | 确认未达到 16 KB LOAD 对齐 |
| libandroidx.graphics.path.so | androidx.graphics:graphics-path:1.0.1 | 16384 / 0x4000 | LOAD 对齐通过；系统“未知错误”不能直接证明它也存在 LOAD 对齐缺陷 |

x86_64 的两份 TFLite 原生库同样为 4096；graphics-path 同样为 16384。32 位库也记录在原始 JSON 中，但不能用其结果冒充 64 位设备运行验收。

系统在 4 KB 手机显示此警告符合 Android 的调试应用预警行为。官方说明从 Android 16 QPR2 开始，对通过 ADB 安装的可调试应用，即使在 4 KB 设备也会显示 16 KB 兼容预警。改成不可调试构建或隐藏弹窗并不会改变这些原生库。

## 证据与复查入口

本机原始资料位于 `.local/verification/page-sizes-20261031/`：installed.apk、launch.xml、zipalign.txt、native/result.json。XML 包含实际系统提示；JSON 包含每个 ABI、LOAD 区段的 offset、virtualAddress、alignment 和原生库 SHA-256。私人设备资料不提交。

重复静态检查：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Check-NativeAlignment.ps1
```

可用 `-ApkPath` 指定待验 APK，`-EvidenceDirectory` 指定本机输出。检查器只读 APK，同时执行官方 zipalign 检查和 ELF LOAD 检查；ZIP 或任一 64 位库 LOAD 不合格返回 2。它不认证完整 16 KB 兼容性，不替代 RELRO、分配器、运行时及真机测试。当前 3.1.00 得到预期的不合格结果。

## 修复边界

已确认至少需要处理 TFLite CPU 与 GPU 的预编译原生库。仅再次 zipalign、升级本项目 NDK 配置、压缩 so 或改 Manifest，不能重新链接依赖 AAR 内的现成 so。

下一步应针对性评估兼容的原生依赖版本，或按原版本源码重新构建 CPU/GPU 库；保留现有模型、强制 GPU 各模式、无 CPU fallback、图片/视频独立队列及缓存规则。不能为此盲目升级整套 Compose/Gradle/TFLite 依赖。

graphics-path 的“未知错误”暂不定案。其 LOAD 已通过；RELRO 与具体系统诊断仍需单独验证。尤其不能把系统列出的全部库都简单判成 4 KB LOAD 对齐。

修复后的 APK 仍须保持原包名、原证书，检查 ZIP、全部相关 ABI 的 ELF LOAD/RELRO，并在实际 `PAGE_SIZE=16384` 的测试环境验收 UI、CPU/GPU 推理、视频及模型加载。本次手机只有 4 KB，不能完成这一最终验证。当前问题定位完成，兼容问题尚未修复。

官方依据：

- [Android 16 KB 支持与验证方法](https://developer.android.com/guide/practices/page-sizes)
- [Android 16 QPR2：4 KB 设备上的调试应用预警](https://android-developers.googleblog.com/2025/08/android-16-qpr2-beta-1-is-here.html)
- [AOSP ApplicationInfo 的页大小兼容标志](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/content/pm/ApplicationInfo.java)
