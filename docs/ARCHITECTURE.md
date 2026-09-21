# 架构

- `MainActivity`: 只负责系统分享 Intent、文件选择、通知权限与 Compose 入口。
- `ui/*`: 不滚动的分享首页、连接页、宽屏双栏、详情弹层、传输悬浮卡和 CameraX 扫码视图。
- `PairingPayload` / `QrCodeGenerator`: 版本化连接 URI 的严格解析与本机二维码生成。
- `TransferService`: 前台服务，生命周期托管；启动时生成访问口令并为 TCP 服务选择可用端口。
- `DiscoveryManager`: UDP 224.0.0.167:53317 发现，LocalSend 思路；手动 IP 兜底在 UI。
- `TransferServer`: 原生 `ServerSocket` 的最小 HTTP server。
- `TransferClient`: `HttpURLConnection` 客户端，负责 prepare/upload/status/complete。
- `ReceiveSession`: `.part` 文件、offset、MediaStore 发布。
- `FileUtil`: metadata、SHA-256、文件名净化。
- `AppState`: MVP 进程内 UI 状态。
- `TransferProgress`: 150 ms 进度节流、吞吐率与 ETA 快照。

## 关键原则

- content URI first：不依赖绝对路径。
- 流式传输：1 MiB buffer；正常路径发送/接收时同步维护 SHA-256，避免传输前后重复整文件扫描。
- fail closed：hash 不匹配绝不发布为正式文件。
- 临时与正式文件分离：cache `.part` -> hash -> MediaStore Downloads。
- “传输完成”与“验证完成”是两个不同状态。
- 接收服务端口和访问口令通过发现广播同步；手动连接时由用户输入同一组信息。
- 接收目录通过 `ACTION_VIEW` 优先调用系统文件管理器，必要时回退到 `ACTION_OPEN_DOCUMENT_TREE`。
- 手机分享主页面本身不滚动，详情列表在独立弹层内部滚动；宽屏直接展示分享/连接双栏。
