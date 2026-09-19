# ADR-0002: SHA-256 is a hard completion gate

Status: Accepted

发送方传输前计算整文件 SHA-256。接收端只在 byte count 达到 metadata size 后计算完整 hash；一致才 publish。

理由：网络 EOF、错误 offset、磁盘写入异常都不能被“HTTP 200/连接结束”误判为成功。
