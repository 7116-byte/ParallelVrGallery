# 普通版开发

工程、Wrapper 与 Git 直接位于项目根目录。当前版本为 3.1.00 / 30100；包名始终为 com.local.parallelvrgallery。私密开发环境位于 .local/，路径按脚本位置计算，不依赖旧磁盘或全局 JDK/SDK/Gradle。

## 构建与验收

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Verify.ps1 -StrictLint
```

Verify.cmd 也可作为入口。验收执行离线 clean、JUnit、assembleDebug、Lint，以及 APK 包名/源码版本/原证书、稳定模型和 Git 完整性检查。JUnit 用例数和错误数读取实际 XML；无报告或零用例会失败。当前 Lint 错误白名单为空，不通过压制错误完成验收。

Build 默认离线。只有任务确实新增依赖时才明确使用 -Online。本次未升级应用依赖；新增的 JUnit 4.13.2 仅用于测试，来自已有离线依赖。

| 组件 | 已验证基线 / 位置 |
| --- | --- |
| JDK | Temurin 17.0.19+10，.local/toolchain/jdk17 |
| Android SDK | compile/target 35、min 26，.local/toolchain/android-sdk |
| Build Tools | 34.0.0（AGP 需要）及 35.0.0（验包） |
| Gradle | 8.10.2 Wrapper，.local/gradle-home |
| AGP / Kotlin | 8.7.3 / 2.0.21 |
| Compose / Material3 | 1.7.6 / 1.3.1 |
| TFLite / GPU | 2.16.1 |

构建临时设置进程环境并在退出时恢复；local.properties 自动生成且被忽略。Android 用户目录、TEMP/TMP 和 Gradle home 均在 .local/。若需在当前 PowerShell 使用项目 Git/Java/adb，可点加载 scripts/Use-Environment.ps1；不永久修改系统环境。

Android Studio 打开本根目录，JDK、SDK 和 Gradle user home 使用上表路径。当前验收以命令行脚本为准。

APK 输出 app/build/outputs/apk/debug/app-debug.apk，并复制到 .local/artifacts/app-debug.apk。每次日志、aapt、apksigner 和 JSON 保存到 .local/verification/ 下独立时间目录；latest-build、latest-lint、latest-acceptance.json 是最新索引，失败也会更新状态。

## 签名与发布

app/build.gradle.kts 显式使用 .local/signing/debug.keystore，缺少原密钥应失败，不能生成替代密钥。原证书 SHA-256：

C51C08290903E65D2E99FE35CEFFD44B637F04CC5BBDD5EA9ECB8847D4EA7359

历史更新器只识别 app-debug.apk，GitHub 稳定版 Release 保留该资产名。版本按“大版本.新功能.修复序号”，功能版 3.1.00 / 30100，后续修复从 3.1.01 / 30101 递增。构建版本来自源码；发布前还要查实际远端版本，不能重用已发布编号。

当次会话有发布授权时，在检查通过后提交、推送、打标签并发布。查询 Release 真实状态和 assets，避免超时后创建重复 Draft。公开下载后再次核对文件哈希和证书。使用现有凭据，不读取、打印或在脚本中存储 Token。

## 真机验收

覆盖安装只用 adb install -r；测试 APK 使用 -r -t，不卸载或清空数据。媒体生成、删除、替换等必须有具体范围授权。测试前记录设置和设备环境，结束恢复测试用字体/旋转/夜间模式等环境，私人截图和日志只留 .local/。

app/src/androidTest 的 ViewerSmokeInstrumentation 会重启目标进程。默认仅进入名为 PVG_UI_QA 的专用相册和明确样本；source=generated 模式仅打开指定已有生成版本和图片，不新建生成任务。此测试不能未经授权直接在任意设备运行。先准备已授权样本/缓存，再执行：

```powershell
. .\scripts\Use-Environment.ps1
.\gradlew.bat assembleDebugAndroidTest --offline
adb -s DEVICE_SERIAL install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s DEVICE_SERIAL shell am instrument -w -e mode sbs com.local.parallelvrgallery.test/com.local.parallelvrgallery.ViewerSmokeInstrumentation
```

mode=held-drag 检查持续按住并跨 Pager 临界点后返回；sbs 检查双指放大、放大后单指拖动、双击复位及左右眼画面。测试结果、UI截图和 gfxinfo/logcat 各自保留，不能用测试命令退出码代替读取 result=PASS/FAIL。复杂真实图片优先复用已有缓存进行只读验收。

mode=video-playback 仅打开专用相册已有生成的 04-ui-sample.mp4，检查明确暂停、回桌面/返回、旋转及图片/视频往返后的两眼画面；没有完成缓存时失败，不代为启动生成。运行完应恢复测试设备旋转状态。

UI规范见 UI_SPEC.md，源码导航和未定案问题见 AI_HANDOFF.md。本机恢复记录 docs/RESTORATION_ACCEPTANCE.md、verification/restoration-summary.json 以及原始备份证据为本地资料，故意不提交；私密归档与 Pro 资料仍不可随意清理。

16 KB 专项复查见 NATIVE_ALIGNMENT.md。运行 scripts/Check-NativeAlignment.ps1 可独立检查 APK ZIP 与原生库 ELF LOAD 对齐；当前 3.1.00 的 TFLite 库会使它返回 2（不合格）。该项尚未并入默认恢复验收，也不替代 RELRO 与 16 KB 运行时测试。
