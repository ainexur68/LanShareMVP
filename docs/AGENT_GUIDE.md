# Agent 开发指南

1. 先读 `AGENTS.md`，再读本次修改涉及的 docs。
2. 先复现/建立证据，后修改；不要因为单测绿就宣称真实链路通过。
3. 网络/文件修改必须覆盖：正常、断网、重试、hash mismatch、取消。
4. 修改协议必须同步 `docs/PROTOCOL.md` 与 `docs/STATE_MACHINE.md`。
5. UI 修改至少考虑 compact portrait 与 expanded landscape。
6. 每次交付记录：changed files、build/test commands、真实运行证据、未验证项。
7. 不引入“获取真实文件路径”的 workaround，始终使用 ContentResolver 流。
8. 不把 `.part` 当成功文件；只有 SHA-256 通过才能 publish。
