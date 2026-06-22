# v2.32

- 状态角标改为无背景文字变色：已生成绿色，失败红色，队列/生成/暂停黄色，未生成/原图白色。
- 生成页视频卡片状态文案缩短为一行小字，减少对缩略图遮挡。
- 底部 `全部 / 相册 / 生成` 和页面内分段按钮改为半透明样式，去掉底部实心遮挡块。
- 生成页图片/视频滚动位置会保存，切页、进 Viewer、进设置后返回仍回到原位置。
- 检查更新改为 App 内下载 APK，下载完成后显示安装按钮并拉起系统安装界面。
- 新增 `updates/` FileProvider 路径和安装未知应用权限声明。
- Video Depth Anything Small 权重已下载到本机临时目录并记录 SHA-256；由于当前机器缺少 Python/PyTorch/TensorFlow 转换链，本轮未上传不可运行模型。
- 新增 `tools/video_depth_anything/` 转换说明和 ONNX 导出脚本入口。

模型转换记录：

- 本机权重路径：`D:\AndroidBuild\model_conversion\VideoDepthAnything\video_depth_anything_vits.pth`
- SHA-256：`13379300B739E659F076A59D52E9801BD8D38C541A7E71F73BBCA4DCFB013609`
- 状态：未转换，未上传 GitHub 模型资产。
