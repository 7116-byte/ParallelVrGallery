# v2.18

- 补全 TFLite GPU 诊断信息。
- Runtime 现在会显示设备信息、GPU CompatibilityList 结果、delegate 创建结果、Interpreter 创建结果。
- GPU delegate 创建失败或模型挂载 GPU 失败时，会显示异常摘要，并明确回退 CPU。
- 新增 `tensorflow-lite-gpu-api` 依赖，以使用官方兼容性推荐选项。

看调试页时重点看这一行：

```text
Runtime tflite runtime threads=... requestedGpu=true delegateActive=... device=... compat=... delegateCreate=... interpreterCreate=...
```

如果 `delegateActive=false`，后面的 `delegateCreate=failed:...` 或 `interpreterCreate=failed:...` 就是 GPU 不生效的直接原因。
