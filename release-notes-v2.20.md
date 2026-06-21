# v2.20

- 视频生成复用深度模型会话。
- 每个视频任务只创建一次 TFLite Interpreter / GPU Delegate，后续帧复用同一个会话，减少每帧重复初始化开销。
- 画质不变：模型、输入尺寸、深度平滑、SBS 合成算法和 cacheVersion 都不改变。
- Runtime 诊断新增 `reusedSession=true`，可确认是否走了复用路径。
- 重新生成视频仍会创建新的独立任务和新的模型会话；旧任务回调不会覆盖新任务状态。
