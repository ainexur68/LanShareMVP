# Roadmap

## 0.1 MVP

系统分享、发现、确认、流式传输、进程内断点续传、SHA-256、Downloads、横竖屏。

## 0.2 Reliability

- session 持久化，App/设备重启仍可续传。
- temp file TTL/清理。
- 更细粒度 progress/取消。
- 大量文件并发策略与速率限制。

## 0.3 Security

- TLS + 自签证书 fingerprint。
- 首次配对/PIN。
- 来源 IP + session/token 绑定。

## 0.4 Media

- 音乐浏览与 HTTP Range 流式播放。
- 若目标车机支持，再评估 DLNA/UPnP。
