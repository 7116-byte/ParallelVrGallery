# v2.36

- 恢复图片 VR 当前图最高优先级生成：当前 Viewer 图片进入 `currentPending`，前后图片只作为预加载队列。
- 修复 `State=QUEUED` 但调试页 `Job=-` 的问题，当前图入队时同步写入 job 状态。
- 恢复图片 VR 按钮语义：VR 模式下显示 `关闭 VR`，`QUEUED` 只表示排队等待生成。
