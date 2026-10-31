# 普通版技术交接：3.1.00

先检查 AGENTS.md、development.md、UI_SPEC.md、版本验收和实际 Git 状态。重装基线是 3.0.08 / 30008，当前 UI 迭代是 3.1.00 / 30100，普通版与 Pro 独立。原始恢复资料在本机 .local/archives/ 下，不能上传。

## 源码地图

- MainActivity.kt：MediaStore、GalleryViewModel、QueueSource/QueueTagStore、模型/图片/视频生成、缓存、播放器及页面入口。使用函数名检索，不依赖固定行号。
- StudioUi.kt：系统深浅色、通用栏/卡片/选项/开关、空状态、新手指南、只读任务面板、前后台播放判定和共享位图左右眼绘制。
- UiPolicy.kt：可视列数、版本比较、APK版本/签名集合判定、音轨标志映射和 UI 导航序列化；配有 JVM 回归测试。
- NavigationStore.kt：独立 navigation_v1 偏好，仅保存 UI 路由和位置，不改动原 settings 或队列数据。
- app/src/androidTest：需要具体媒体范围授权的真机手势测试。真实图片、截图及私密日志不进入 Git。

## 必须保持的生成规则

全部、当前相册和生成版本 Viewer 各自捕获来源范围。currentPending 是当前图片独立最高优先级；普通 pending 前后窗口自动 2→4→8，最多前八后八，自动模式才按原规则反向补槽。切图重置窗口，旧窗口未开始项自动暂停；不强行中断正在运行的单张原生推理。新窗口可接纳自动暂停项，手动暂停/失败项不能自动复活。

paused、autoPausedKeys、sourcePausedKeys 仍分别表达暂停与原因。图片与视频队列独立，视频不参加图片预加载。退出普通 Viewer 停止扩展生成预加载。Viewer 的原图/缓存 decode 预热与生成队列完全不同。

生成页 Viewer 只看已有结果；触边与翻页不新增普通图片生成。用户关于“进入生成页恢复来源切换暂停的 ALL/ALBUM 任务”的要求，与 jobAllowedInCurrentContext 当前仅匹配 GENERATED 的实现仍可能有差异。此处未在 UI 改版中改动，后续需要结合来源、队列标记和日志单独定案。

缓存仍为图片 depthV6、视频 encoderV12；持久化仍为原 settings、queue_index、vr_cache、video_vr_cache。强制 GPU 选择和图片/视频各自设置保留，不自动回退 CPU。GPU delegate 的生命周期与推理算法没有在本次 UI 工作中重写。

## UI 与生命周期

Gallery 的实际顶栏/浮动导航高度用于列表避让，列数随窗口和字号限制，不改请求值或媒体顺序。设置可滚动，选项自然换行。生成管理和操作确认沿用原调用，普通版“重新生成图片”仍会清理所选原图相关的已有生成缓存，确认文案必须准确，不能套用 Pro 只清一个版本的规则。

原图浏览入口仍默认开启图片 VR；新手引导已明确说明，自动化测试必须先确认来源。生成页和任务面板不会自动创建新任务。

SBS 图片直接用同一位图的左右源区域绘制，不再在主线程复制左右两张 Bitmap。缩放和 offset 仍由两眼共享，原跨中线/旋转/双击规则保留。视频仍用单 MediaPlayer/SurfaceTexture/OpenGL，两眼同步。当前播放归属在 settledPage 切换，且离开前台时暂停；显式暂停不会在返回前台时自动改为播放。载入和失败/重试状态现在可见。

navigation_v1 保存来源页、当前相册、生成版本、列数和滚动位置。冷启动返回对应来源页，不恢复 Viewer/vrMode，避免仅恢复界面就启动生成；图片/视频任务仍按原持久化恢复规则处理。不要将这描述成杀进程后原生推理会无损续跑。

## 验证与未解决项

音轨复制原有 WrongConstant 已通过显式 sync→key-frame 映射修复，不能直接转发 encrypted/partial extractor 标志。当前 Lint 错误白名单为空。JUnit 只计本工程实际 XML，不能计入 Pro 的历史测试。

持续按住跨 Pager 临界点、快切图片/视频、旋转、回桌面、杀进程与冷启动都需要真机证据。共享位图、前后台播放和状态恢复改善不代表所有设备的历史顿挫/黑屏已经解决。新的问题先抓 UI thread、Compose、队列和 IO 时间线，区分原图/SBS、来源和现有缓存，避免凭经验反复修改手势阈值。

现有 TFLite/Compose 原生库仍未全部满足 16 KB ELF 对齐。当前设备系统显示兼容提示；4 KB 页大小的运行验证不能代表 16 KB 兼容。本次不升级整套依赖或隐藏该限制。

稳定模型 SHA-256 为 B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F。Qualcomm 实验权重不存在且历史 URL 返回 404；Video Depth Anything 仍待移动端转换。不要把旧 pth 路径视为依赖。

发布继续保留原包名、原证书及 app-debug.apk 资产名。模型和手机缓存不打入 APK。Pro 的历史、API36、签名/模型共享副本及技术资料仍由 Pro 使用，本次不修改或清理它们。
