# ADR-0001: LocalSend-inspired protocol, not direct app fork

Status: Accepted

选择：借鉴 LocalSend 的本地 REST + multicast + prepare/upload 分阶段思想，但 MVP 自己实现 Android/Kotlin 协议层。

原因：需求只需要极简分享，直接 fork 完整 LocalSend 会携带大量不需要的跨平台/产品能力；同时我们需要明确加入 offset resume 与自己的 UI 状态机。

代价：0.1 不保证与 LocalSend 官方客户端互操作。后续如有价值，可实现兼容层。
