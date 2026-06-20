# v2.16 功能

- 图片 VR 生成增加性能诊断计时：解码、模型推理、深度后处理、SBS 合成、写文件、总耗时。
- 调试页图片区域新增“性能诊断”，方便判断 3 秒一张主要慢在哪个阶段。
- 新生成图片会在 `params.json` 写入 `timings` 对象；旧缓存没有计时字段时显示 `-`，重新生成后可查看。
- `job.log` 同步写入更清晰的阶段耗时日志，例如 `decode xxxms`、`model inference xxxms`、`depth post xxxms`、`sbs xxxms`、`write xxxms`。
