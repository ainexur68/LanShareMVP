# Roadmap

## 0.1 MVP

系统分享、发现、确认、流式传输、进程内断点续传、SHA-256、Downloads、横竖屏。

## 0.2 Share UX + Pairing + Throughput

- 手机分享首屏、独立连接页、文件/传输详情弹层和宽屏双栏。
- QR 配对，自动发现与手动 IP 继续作为兜底。
- 流式 SHA-256、节流进度、速度、ETA 与文件计数。
- Docker/GitHub Actions 可复现构建。

## 0.3 Reliability

- session 持久化，App/设备重启仍可续传。
- temp file TTL/清理。
- 更细粒度 progress/取消。
- 大量文件并发策略与速率限制。

## 0.4 Security

- TLS + 自签证书 fingerprint。
- 首次配对/PIN。
- 来源 IP + session/token 绑定。

## 0.5 Media

- 音乐浏览与 HTTP Range 流式播放。
- 若目标车机支持，再评估 DLNA/UPnP。
