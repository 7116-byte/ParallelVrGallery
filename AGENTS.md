# ParallelVrGallery 普通版接手规则

本目录是普通版正式开发根目录，源码与 .git 直接位于此处。当前代码版本 **3.1.00 / 30100**；重装恢复基线为 3.0.08 / 30008（4d90096cfcb57f841767a7c185aeefa47fdd13ac）。普通版与 Pro 保持独立。

先读 docs/development.md、docs/UI_SPEC.md、docs/AI_HANDOFF.md、当前版本验收和 .codex/AGENTS.md；本机若存在 docs/RESTORATION_ACCEPTANCE.md，也应读取。然后检查实际 Git 状态、源码及远端发布，保留原有修改。不要凭截图、旧发布文案或构建成功认定真机问题已修复。

- 默认离线构建：powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build.ps1。
- 一键验收：同样运行 Verify.ps1 -StrictLint。JUnit 结果必须读取实际 XML，不能把 NO-SOURCE 算通过；Pro 的历史测试不属于普通版。
- 工具、模型、离线依赖、原签名、原恢复记录和私人测试截图都在被 Git 忽略的 .local/。不上传这些资料，不读取或打印 Token。恢复记录及账号/磁盘证据仅留在本机。
- Gradle debug 签名显式绑定 .local/signing/debug.keystore。历史发布使用 debug APK，必须保留原证书，不能使用新系统随机生成的 key 或覆盖全局密钥。
- applicationId 保持 com.local.parallelvrgallery；原证书 SHA-256 为 c51c08290903e65d2e99fe35ceffd44b637f04cc5bbdd5ea9ecb8847d4ea7359。
- 更新器依赖 app-debug.apk 资产名。发布需有当次任务授权；核对包名、递增版本、证书、公开下载及 SHA-256。不得重建无历史仓库后强推。
- 真实媒体生成、删除、替换等测试必须说明范围并取得授权。覆盖安装不得卸载或清数据。私人图片即使获准查看，也不能自动扩大为批量生成或删除授权。
- 设备使用遵从用户最新要求：本次写代码期间每分钟回桌面 30 秒；工作完成或无法控制设备时，停止轮换并锁屏。不得在无人看管时长期停留静态页。

必须保留的业务边界：

1. 全部、当前相册、生成页当前版本各自限定浏览来源；相册边界不借全局列表。生成页 Viewer 只浏览已有结果，不触发普通图片预加载。
2. 当前图片独立最高优先级 currentPending；前后 worker 自动按 2 → 4 → 8 扩档，最多前八后八。自动模式保留原反向补槽，固定窗口不补位；切图重置窗口，运行中原生推理不强行中断。
3. 区分 paused、autoPausedKeys、sourcePausedKeys。新窗口可恢复自动暂停项，失败/手动/历史暂停不得全局自动复活。退出普通 Viewer 停止扩展预加载。
4. 图片与视频独立排队；Viewer decode 缓存不是生成预加载。不得为 UI 重写算法、改变缓存版本或队列持久化格式。
5. 保留强制 GPU 和图片/视频独立选项，不擅自引入 CPU fallback。双眼共用缩放和偏移；视频保留单 MediaPlayer、SurfaceTexture 和 OpenGL 双眼视口。
6. 系统深浅色、字号和安全边距统一由 StudioUi 管理；可视列数只限制布局，不改变媒体顺序、设置中的请求值或生成范围。

继续跟踪：

- 持续按住跨 Pager 临界点的顿挫，以及快速切视频黑屏，必须结合真实图片/视频、日志和帧数据验证。共享位图绘制和 settledPage 播放归属改进不能代替用户最终验收。
- jobAllowedInCurrentContext 在生成页按 GENERATED 匹配，与恢复 ALL/ALBUM 来源暂停任务的需求仍可能有差异；没有在 UI 改版中修改该规则。
- 3.0.08 的音轨 WrongConstant 已由显式 extractor→muxer 标志映射修复，配有回归测试。当前 Lint 错误白名单为空。
- 现有 Compose/TFLite 原生库仍有 16 KB ELF 对齐限制。当前设备出现系统兼容提示；4 KB 真机运行不代表 16 KB 设备兼容。不要为隐藏提示而压制检查或伪称修复。
- Qualcomm 实验模型无本地权重，历史下载源返回 404；Video Depth Anything 只有转换资料。稳定模型已保留，不得将原始 pth 冒充移动端模型。

UI-only navigation_v1 保存来源页、相册、生成版本、列数和滚动位置；冷启动不恢复 selectedIndex/vrMode，不因恢复界面自动开始生成。原 settings、queue_index、vr_cache、video_vr_cache 及缓存版本保持原规则。

Pro 的源码、bundle、API36、安装包、共享签名/模型/工具和技术资料仍保存在 .local/archives/supplemental/，继续由 Pro 使用。本次普通版迭代不构建、发布或清理 Pro，不删除旧备份。
