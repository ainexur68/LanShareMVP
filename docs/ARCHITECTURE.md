# 架构

- `MainActivity`: Compose UI、系统分享 Intent、App 内多文件选择、手动地址/访问口令、用户确认。
- `TransferService`: 前台服务，生命周期托管；启动时生成访问口令并为 TCP 服务选择可用端口。
- `DiscoveryManager`: UDP 224.0.0.167:53317 发现，LocalSend 思路；手动 IP 兜底在 UI。
- `TransferServer`: 原生 `ServerSocket` 的最小 HTTP server。
- `TransferClient`: `HttpURLConnection` 客户端，负责 prepare/upload/status/complete。
- `ReceiveSession`: `.part` 文件、offset、MediaStore 发布。
- `FileUtil`: metadata、SHA-256、文件名净化。
- `AppState`: MVP 进程内 UI 状态。

## 关键原则

- content URI first：不依赖绝对路径。
- 流式传输：1 MiB buffer。
- fail closed：hash 不匹配绝不发布为正式文件。
- 临时与正式文件分离：cache `.part` -> hash -> MediaStore Downloads。
- “传输完成”与“验证完成”是两个不同状态。
- 接收服务端口和访问口令通过发现广播同步；手动连接时由用户输入同一组信息。
