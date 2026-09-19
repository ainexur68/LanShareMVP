# LanShare MVP

一个极简 Android 局域网文件传输 MVP：同一 APK 既能发送也能接收，重点解决“从系统分享菜单直接把文件发到同一局域网的另一台设备”。

## MVP 已实现范围

- Android `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 系统分享入口，直接消费 `content://` URI，不要求真实文件路径。
- App 内支持一次选择多个文件，和系统分享入口共用同一发送队列。
- 同一局域网 UDP Multicast 自动发现；发现失败可手动输入 IP。
- 发现广播携带接收端动态服务端口和 4 位数字短期访问口令；端口占用时自动尝试备用端口。
- 接收端必须人工确认。
- 流式传输，不把整文件加载进内存。
- 中断续传：接收端 `.part` 临时文件长度即续传 offset；同一 session 重试时返回 offset。
- 完整性：发送前 SHA-256；接收完重新计算；不一致返回 422，不发布到 Download。
- 校验成功后写入 `Download/LanShare`。
- 接收面板可调用系统文件管理器打开 `Download/LanShare`，无直接打开能力时回退到目录选择器。
- 前台 Service 保持接收服务器/发现服务存活。
- 单 APK 双角色。
- Compose 响应式 UI：窄屏单列；>=720dp 宽屏双栏，适配手机竖屏/车机横屏。
- LanDrop 风格的卡片化发送/接收状态 UI、手动地址和 4 位数字口令入口。
- 首版不包含音乐在线播放。

## 设计来源

网络流程借鉴 LocalSend 的成熟思路：局域网组播发现，以及 `prepare -> upload -> cancel` 的传输阶段；本项目没有复制 LocalSend 应用源码，而是实现了一个更小的 MVP 协议，并增加 `/status`、offset resume 和完整 SHA-256 验证。LocalSend Protocol v2.2 本身也定义了 `sha256` 校验失败使用 422；详见 `docs/REFERENCES.md`。

## 开源许可与第三方声明

本项目的原创代码和文档采用 Apache License 2.0，详见 [`LICENSE`](LICENSE) 和 [`NOTICE`](NOTICE)。

LocalSend 仅作为公开协议/设计思路参考；本仓库没有复制 LocalSend 源码、二进制、资源或文档原文，也没有引入 LocalSend 运行时依赖。LocalSend 应用仓库当前标注为 Apache-2.0；详细的来源、范围和许可证核对记录见 [`docs/REFERENCES.md`](docs/REFERENCES.md)。Gradle 依赖仍分别遵循各自上游许可证，见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

## 打开与运行

环境：Android Studio + Android SDK 35，JDK 17+。最低 Android 10（API 29）。

1. 用 Android Studio 打开根目录。
2. Gradle Sync。
3. 在两台 Android 10+ 设备上分别安装 `app`。
4. 两台设备接入同一 Wi-Fi，或设备 B 连接设备 A 的热点。
5. 打开双方 LanShare。
6. 在设备 A 的文件管理器/相册中长按文件 → 分享 → LanShare。
7. 选择设备 B；B 点击“接收”。
8. B 可点击“打开接收目录”，调用系统文件管理器查看 `Download/LanShare`。

### 命令行

项目附带自举版 `gradlew` / `gradlew.bat`。首次使用需要互联网下载 Gradle 8.10.2 和 Maven 依赖：

```bash
./gradlew :app:assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 当前验证状态

融合后已执行：`python scripts/static_verify.py`、`:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug`。

当前环境没有两台 Android 真机，因此尚未完成真实双机局域网传输验收；首次真机验收仍需按 `docs/TEST_PLAN.md` 执行。

## 已知 MVP 限制

- 传输是 HTTP 明文，仅适合受信任的本地网络；后续应加入 TLS/设备配对。
- 续传依赖同一 `sessionId`；App/设备重启后的持久化恢复尚未实现。
- 当前每次 `PUT` 传输剩余全部字节；网络断开后重新发起同一 session 会从 `.part` 长度继续。
- 设备发现只实现 UDP Multicast + 手动 IP/端口/4 位数字口令；未做子网扫描。
- 访问口令只在当前前台服务生命周期内有效；HTTP 仍为明文传输，不能替代 TLS。
- UI 是 Apple 风格取向（留白、圆角、克制层级），没有复制 Apple 专有资源。

## 文档

- `docs/REQUIREMENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/PROTOCOL.md`
- `docs/STATE_MACHINE.md`
- `docs/SECURITY_STORAGE.md`
- `docs/TEST_PLAN.md`
- `docs/ROADMAP.md`
- `docs/AGENT_GUIDE.md`
- `docs/REFERENCES.md`
- `LICENSE`
- `NOTICE`
- `THIRD_PARTY_NOTICES.md`
- `AGENTS.md`
