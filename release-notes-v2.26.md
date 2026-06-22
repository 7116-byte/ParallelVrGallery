# v2.26

- 恢复 GPU 加速尝试：依次尝试 accurate-unset、accurate-opengl、accurate-opencl、best 多种 TFLite GPU 配置。
- 默认先用禁止精度损失的 GPU 精确模式，目标是在恢复 GPU 速度的同时避免深度输出全黑或语义错乱。
- GPU 输出异常时先切换下一个 GPU 模式，所有 GPU 模式都失败后才回退 CPU。
- 调试日志新增 `gpuMode` 和 `tflite gpu recovered mode=...`，方便确认哪种 GPU 后端真正可用。
- 图片缓存版本升级到 depthV4，视频缓存版本升级到 encoderV8，隔离旧 CPU 回退缓存。
