# LanShare Protocol 0.2

默认发现 UDP 端口 `53317`；接收服务默认尝试 TCP `53317`，占用时依次尝试后续端口。

## Discovery

UDP multicast `224.0.0.167:53317`。字段参考 LocalSend discovery，但 `version=lanshare-0.2`，因此不宣称协议兼容。广播中的 `port` 是接收服务真实 TCP 端口，`token` 是当前服务生命周期内的 4 位数字访问口令。

除发现以外的 HTTP 请求都必须携带 `token` 查询参数；缺失或不匹配返回 `401`。

## Prepare

`POST /v1/prepare`

发送 sessionId、senderAlias、files[]：id/name/size/mime。0.1 客户端附带的可选 `sha256` 仍会被接收。
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

正文必须包含 sessionId、fileId、sha256。sha256 是发送端对实际发送字节流计算的摘要。

接收端先检查 size，再取得 SHA-256：正常路径复用 upload 写入时同步维护的摘要；摘要状态不可复用时回退完整读取 `.part`。随后：
- 一致：发布到 `Download/LanShare`，200。
- 不一致：保留为失败状态，不发布，422。

## Cancel

`POST /v1/cancel?sessionId=...`

MVP 只移除进程内 session；临时文件清理策略后续增强。

## QR Pairing

二维码只交换连接信息，不包含文件内容：

```text
lanshare://pair?v=1&host=192.168.1.21&port=53317&token=4821&fp=...&alias=Pixel
```

扫码端必须验证 scheme、版本、IPv4、端口、4 位数字连接码、设备标识和名称，再转换为 Peer。二维码不替代 HTTP token 校验。
