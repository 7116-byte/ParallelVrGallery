# 3.1.00 非调试正式包设备安装（2026-10-31）

按用户要求，将普通版正式构建覆盖安装到已连接手机和平板。保留 3.1.00 / 30100，包名 com.local.parallelvrgallery；只改变构建类型，不修改业务算法、缓存版本、队列或应用依赖。

release 显式设置 debuggable=false、minifyEnabled=false，使用 .local/signing/debug.keystore 内的原历史证书。证书名称和构建类型是独立的；继续使用原证书是覆盖升级的必要条件，不能换用新密钥。

| 检查 | 结果 |
| --- | --- |
| 原证书 SHA-256 | c51c08290903e65d2e99fe35ceffd44b637f04cc5bbdd5ea9ecb8847d4ea7359 |
| 正式 APK SHA-256 | 7e52372d1add58988e89e1de2a2ef131150ef37a53e701739373bca58f217818 |
| 构建 | 离线 testReleaseUnitTest、assembleRelease、测试 APK 构建通过 |
| 正式构建 JUnit | 28 项，0 失败 / 错误 / 跳过 |
| 严格 lintRelease | 0 错误、11 警告、16 信息 |
| 手机、平板 | adb install -r 覆盖成功；设备内 APK SHA-256 均与上述正式包相同 |
| 调试标志 | APK badging 与两台设备运行时均确认 debuggable=false |
| 启动 | 两台同证书 instrumentation 探针均 PASS，未出现调试兼容弹窗 |
| 数据 | 两台 settings.xml SHA-256 均与安装前相同；vr_cache、video_vr_cache、models 的文件路径与长度清单摘要相同，首次安装时间保留 |

没有卸载、清空数据、删除缓存或测试媒体，也没有运行媒体生成。清单一致不是所有缓存内容逐字节重算哈希；私有设置文件实际做了完整 SHA-256。原始记录在 .local/verification/production-install-20261031/，设备标识、清单摘要和截图不提交。

平板首次安装前探针没有返回，已终止该测试进程并重试；重试快照、正式覆盖安装和正式包探针均通过。该次卡住不记作通过结果。

16 KB 专项静态检查仍按预期不合格：两份 TFLite 2.16.1 原生库的 LOAD 仍为 4096。此次没有通过 Manifest、系统属性或“不再显示”选项隐藏预警，正式包启动不再出现调试弹窗不等于 16 KB 问题已修复。详见 NATIVE_ALIGNMENT.md。

当前 GitHub v3.1.00 的公开资产仍是先前验证的 debug 构建（SHA-256 cd7f8163…），本次仅设备部署，没有替换旧 Release 资产。未来若发布正式构建作为更新，需使用新版本号及兼容资产名 app-debug.apk。

设备验证结束后停止桌面轮换，并按用户先前要求锁屏。
