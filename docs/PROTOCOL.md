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

接收端在展示人工确认前完成元数据校验：

- `sessionId`（prepare 未提供时由接收端生成）和 `fileId` 只能包含 ASCII 字母、数字、`_`、`-`，长度为 1–128；显式传入的非法值返回 `400`。
- `files` 必须包含 1–256 个元素，`fileId` 必须唯一。
- `name` 最长 255 个字符，`mime` 最长 255 个字符；空白文件名、控制字符和非字符串字段返回 `400`。
- 单文件最大 100 GiB，单次 prepare 的总声明大小最大 500 GiB；`size` 必须是非负整数。
- 元数据 JSON 的 `Content-Length` 必须存在且不超过 4 MiB；超出返回 `413`。上述校验失败时不会创建接收 session。

## Status

`GET /v1/status?sessionId=...`

返回每个 fileId 当前 `.part` 长度。

## Upload

`PUT /v1/upload?sessionId=...&fileId=...&offset=...`

接收端要求：

- `sessionId`、`fileId` 和 `offset` 必须存在且格式合法；`offset` 必须位于 `0..size`。
- `Content-Length` 必须存在、非负，且不超过 `size - offset`；文件仍有剩余字节时不能发送零长度正文。
- `offset` 必须等于 `.part` 当前长度，否则返回 `409` 和当前 offset。

正文为从 offset 开始的剩余二进制内容。声明长度超过文件剩余长度、负数或正文提前结束均返回 `400`，服务端不会按声明大小之外继续写入。

## Complete

`POST /v1/complete`

正文必须包含 sessionId、fileId、sha256。sha256 是发送端对实际发送字节流计算的摘要。

接收端先检查 size，再取得 SHA-256：正常路径复用 upload 写入时同步维护的摘要；摘要状态不可复用时回退完整读取 `.part`。随后：
- 一致：发布到 `Download/LanShare`，200。
- 不一致：保留为失败状态，不发布，422。

`complete` 中的 `sessionId`、`fileId` 必须使用同样的安全格式；`sha256` 若提供必须是 64 位十六进制字符串。为兼容早期客户端，若 prepare 已提供摘要，complete 可以省略该字段并复用 prepare 元数据中的摘要。

## Cancel

`POST /v1/cancel?sessionId=...`

MVP 只移除进程内 session；临时文件清理策略后续增强。

## QR Pairing

二维码只交换连接信息，不包含文件内容：

```text
lanshare://pair?v=1&host=192.168.1.21&port=53317&token=4821&fp=...&alias=Pixel
```

扫码端必须验证 scheme、版本、IPv4、端口、4 位数字连接码、设备标识和名称，再转换为 Peer。二维码不替代 HTTP token 校验。
