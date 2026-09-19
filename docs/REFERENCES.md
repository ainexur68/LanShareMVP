# References

本项目只参考公开协议/设计思想，不复制 LocalSend 应用实现代码。

- LocalSend Protocol: https://github.com/localsend/protocol
- LocalSend: https://github.com/localsend/localsend

交付时核对到的 LocalSend Protocol v2.2 关键点：REST、本地服务器、UDP multicast discovery、prepare-upload/upload/cancel；v2.2 changelog 增加上传 SHA-256 不匹配时 422。LanShare MVP 在此基础上设计自己的 `/status` + offset resume 扩展，因此当前不声称可与官方 LocalSend 互操作。
