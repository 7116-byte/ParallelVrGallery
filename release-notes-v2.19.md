# v2.19

- 修复升级或刷新后，未完成的视频生成任务可能从生成页消失的问题。
- 视频队列恢复不再依赖首页最近加载窗口；如果源视频不在当前已加载媒体列表中，会通过持久化的 `VIDEO_id_size_modified` 构造虚拟视频项。
- 生成页现在可以继续显示暂停、生成中、失败的视频任务，方便继续生成或管理。
- 修复重新生成同一视频时，旧任务失败回调覆盖新任务状态，导致“显示失败但实际仍在生成”的状态错乱。

GPU 诊断说明：

- `delegateActive=true` 表示 GPU delegate 已经实际启用。
- `compat=false` 表示 TFLite 官方兼容表未认证/不推荐该设备，但如果 `delegateCreate=ok interpreterCreate=ok delegateActive=true`，说明当前模型仍然成功跑进 GPU。
