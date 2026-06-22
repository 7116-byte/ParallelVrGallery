# v2.25

- GPU 深度增加可信度校验：第一次 GPU 推理会和 CPU 参考深度做相似度对比。
- 如果 GPU 深度不是全黑、但和 CPU 参考差异过大，会自动回退 CPU，并在日志显示 `gpuFallback=validation`。
- 图片缓存版本升级到 depthV3，视频缓存版本升级到 encoderV7，隔离旧 GPU 深度结果。
- 用于验证视频乱跳是否来自 GPU 深度语义错乱：如果回退 CPU 后不跳，说明 GPU 深度不可信。
