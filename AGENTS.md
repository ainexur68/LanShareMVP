# AGENTS.md — LocalShare Engineering Contract

## Mission

维护一个小而可靠的 Android LAN 文件传输应用。正确性优先于功能数量。

## Source of truth

- 产品边界：`docs/REQUIREMENTS.md`
- 协议：`docs/PROTOCOL.md`
- 状态机：`docs/STATE_MACHINE.md`
- 验收：`docs/TEST_PLAN.md`

冲突时先修正文档并说明决策，不允许代码与规范长期分叉。

## Non-negotiable invariants

1. Android 分享入口必须消费 `content://` URI；禁止假设真实路径存在。
2. 文件必须流式处理；禁止 `readBytes()` 读取用户大文件。
3. 接收端默认人工确认；不得静默自动接收。
4. 正式成功条件 = byte count 完整 + 接收端 SHA-256 == 发送端 SHA-256。
5. hash 未通过，不得写出“成功”状态，不得把临时文件发布为正式 Downloads 文件。
6. 续传 offset 必须由接收端事实决定，发送端不得凭本地猜测。
7. 真实设备链路没有跑过就写 `NOT VERIFIED`；禁止以 mock/unit test 冒充 E2E。
8. UI 不锁方向；compact portrait 和 expanded landscape 都是一级验收面。

## Required workflow

Explore -> state facts -> smallest design -> implement -> static/build tests -> real two-device test -> record evidence.

修改前至少读完整相关文件，不可只看首段代码就下结论。出现一层问题后继续追踪上下游，直到到达实际 I/O 或用户可见 postcondition。

## Verification gate

任何 PR/Agent 报告必须包含：
- `git diff --check`
- Android build 命令及结果
- 与改动对应的测试
- 若影响网络/传输/UI：真实设备证据或明确 `BLOCKED/NOT VERIFIED`

不得制造假测试、吞异常、用无意义断言换绿灯。
