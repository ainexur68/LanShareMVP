# 传输状态机

`IDLE -> PREPARING -> WAITING_APPROVAL -> TRANSFERRING -> VERIFYING -> COMPLETE`

失败分支：任一阶段 -> `FAILED`；用户/发送端取消 -> `CANCELLED`。

约束：
- `TRANSFERRING` 结束不等于成功。
- 只有 `VERIFYING` 的 SHA-256 一致后才能 `COMPLETE`。
- 接收端在确认前不创建正式 Downloads 文件。

UI 映射：

- `IDLE` 不显示传输浮层。
- 其他状态显示轻量传输浮层；点击后进入传输详情弹层。
- `TRANSFERRING` 展示当前文件字节进度、实时速度、ETA、已完成文件数/总文件数。
- `COMPLETE/FAILED/CANCELLED` 由用户在详情弹层明确关闭状态。
