# References and license review

## LocalSend 的使用范围

LocalShare 只参考 LocalSend 的公开协议和设计思路：局域网发现、接收端 HTTP 服务、分阶段准备/上传/取消，以及文件 SHA-256 元数据。本项目的 Kotlin/Android 实现、接口路径、状态机、续传规则和 UI 均为独立实现；当前不声称与官方 LocalSend 客户端协议兼容。

仓库审计结果：未发现 LocalSend 源码、二进制、资源、运行时依赖或逐字复制的文档段落。仓库中的 `LocalSend` 名称仅用于准确描述参考来源，不表示项目隶属、赞助或背书。

## 许可证核对（2026-09-19）

### LocalSend 应用

- 项目：https://github.com/localsend/localsend
- 官方许可证文件：https://github.com/localsend/localsend/blob/main/LICENSE
- 当前仓库页面标注：Apache-2.0。
- 结论：LocalShare 不分发 LocalSend 应用代码，因此不产生复制其源代码的再分发义务；保留本引用和非关联声明，便于溯源。

### LocalSend Protocol

- 项目：https://github.com/localsend/protocol
- 协议文档：https://github.com/localsend/protocol/blob/main/README.md
- 核对结论：当前项目页面未显示独立的 `LICENSE` 文件。LocalShare 不复制该仓库的文档原文或代码，只将公开协议行为作为设计参考，并在自己的 [`PROTOCOL.md`](../PROTOCOL.md) 与 [`docs/PROTOCOL.md`](PROTOCOL.md) 中明确自有协议版本、路径和扩展。
- 后续如果要直接复制协议文档段落、示例代码或其他文件，必须先重新核对该文件的版权/许可证，并在 `NOTICE` 与本文件中补充对应归属。

## 其他依赖

Apache-2.0 只覆盖 LocalShare 自己的原创代码和文档；AndroidX/Jetpack、Kotlin/Compose、JUnit 等 Gradle 依赖继续使用各自上游许可证。直接依赖声明位于 [`app/build.gradle.kts`](../app/build.gradle.kts)，分发 APK 或调整依赖时应按实际解析版本生成并附带完整的第三方许可证清单，见 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)。
