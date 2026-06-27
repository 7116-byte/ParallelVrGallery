# v2.35

- 检查更新改为多源 fallback：优先 GitHub API，失败后尝试代理 API 和 Release 页面解析。
- APK 下载支持直连、`gh-proxy.com`、`ghproxy.net` 多线路重试。
- 所有更新源都失败时，App 会保留 Release 页面入口，方便手动下载。
