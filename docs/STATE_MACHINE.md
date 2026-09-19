# 传输状态机

`IDLE -> PREPARING -> WAITING_APPROVAL -> TRANSFERRING -> VERIFYING -> COMPLETE`

失败分支：任一阶段 -> `FAILED`；用户/发送端取消 -> `CANCELLED`。

约束：
- `TRANSFERRING` 结束不等于成功。
- 只有 `VERIFYING` 的 SHA-256 一致后才能 `COMPLETE`。
- 接收端在确认前不创建正式 Downloads 文件。
