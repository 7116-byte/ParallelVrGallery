# v2.27

- 设置页新增 GPU 后端测试模式：自动、精确模式、强制 OpenGL、强制 OpenCL、系统推荐。
- 设置页新增“强制 GPU，不回退 CPU”：用于快速测试 GPU 后端，失败或输出异常也不会偷偷切 CPU。
- 强制 GPU 模式不会跑 CPU 参考校验，避免测试图片/视频时第一帧又慢回 CPU。
- 调试页显示 GPU mode、Force GPU；日志细化为 gpuMode、forceGpu、outputRange、flat、run failed 等信息。
- 图片缓存版本升级到 depthV5，视频缓存版本升级到 encoderV9，隔离旧自动回退缓存。
