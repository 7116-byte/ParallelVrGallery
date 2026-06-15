# v1.18

- 调试页重做为概览、运行时、预览、参数、日志、队列多区块，减少空白面积。
- 视频调试页显示模型、缓存版本、CPU 线程、GPU 请求、TFLite runtime、帧数、帧耗时、输出和错误。
- 图片调试页读取并展示 `params.json` 和 `job.log` 末尾。
- 视频逐帧生成会把 TFLite runtime 信息写入当前任务。
- GPU run 失败会明确显示 fallback CPU。
- 调试队列同时显示视频队列和图片队列，最近日志显示更多条。

已验证：`assembleDebug` 构建通过。
