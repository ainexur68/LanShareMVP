# 安全与存储

## MVP

- 同一 Wi-Fi 不等于可信，因此默认每次接收都确认。
- session/fileId/offset 都在服务端校验；ID 只允许安全字符并限制长度，prepare 还限制文件数量、重复 ID、文件名/mime 长度、单文件/总大小。
- upload 强制校验 Content-Length 不超过声明文件的剩余长度，offset 不匹配只返回 409，不会越界写入 `.part`。
- 每次前台服务启动生成新的 4 位数字短期访问口令，HTTP 请求必须携带匹配口令。
- 文件名净化，避免发布到 Downloads 时的路径穿越；临时目录使用经过校验的 sessionId/fileId，并额外检查 canonical parent 关系。
- 临时文件只放 app cache；hash 通过后才写 MediaStore Downloads。
- 不请求全盘存储权限。

文件选择语义：应用内文件选择会追加到当前列表并按 URI 去重；外部 `ACTION_SEND`/`ACTION_SEND_MULTIPLE` 表示新的分享任务，会替换旧列表（即使 Intent 没有有效 URI 也不会沿用旧文件）。

## 当前缺口

- HTTP 未加密，局域网攻击者理论上可窃听/篡改；SHA-256 只能检测内容与发送元数据是否一致，不能证明发送者身份。
- 访问口令通过 UDP 发现广播传递，主要用于减少误连，不应被视为加密或身份认证。
- 正式版需要 TLS、自签证书指纹或一次性配对码，以及 session 与来源 IP/设备指纹绑定。
