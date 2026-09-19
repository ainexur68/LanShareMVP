# LanShare MVP Protocol 0.1

默认 TCP/UDP 端口 `53317`。

## Discovery

UDP multicast `224.0.0.167:53317`。字段参考 LocalSend discovery，但 `version=lanshare-0.1`，因此不宣称协议兼容。

## Prepare

`POST /v1/prepare`

发送 sessionId、senderAlias、files[]：id/name/size/mime/sha256。
接收端人工确认后返回：

```json
{"sessionId":"...","offsets":{"file-id":12345}}
```

若同一 session 已存在，返回现有 offset，实现中断重连。

## Status

`GET /v1/status?sessionId=...`

返回每个 fileId 当前 `.part` 长度。

## Upload

`PUT /v1/upload?sessionId=...&fileId=...&offset=...`

接收端要求 offset == `.part` 当前长度，否则 409。正文为从 offset 开始的剩余二进制内容。

## Complete

`POST /v1/complete`

接收端先检查 size，再计算完整 SHA-256：
- 一致：发布到 `Download/LanShare`，200。
- 不一致：保留为失败状态，不发布，422。

## Cancel

`POST /v1/cancel?sessionId=...`

MVP 只移除进程内 session；临时文件清理策略后续增强。
