# LanShare MVP

一个极简 Android 局域网文件传输 MVP：同一 APK 既能发送也能接收，重点解决“从系统分享菜单直接把文件发到同一局域网的另一台设备”。

## MVP 已实现范围

- Android `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 系统分享入口，直接消费 `content://` URI，不要求真实文件路径。
- 同一局域网 UDP Multicast 自动发现；发现失败可手动输入 IP。
- 接收端必须人工确认。
- 流式传输，不把整文件加载进内存。
- 中断续传：接收端 `.part` 临时文件长度即续传 offset；同一 session 重试时返回 offset。
- 完整性：发送前 SHA-256；接收完重新计算；不一致返回 422，不发布到 Download。
- 校验成功后写入 `Download/LanShare`。
- 前台 Service 保持接收服务器/发现服务存活。
- 单 APK 双角色。
- Compose 响应式 UI：窄屏单列；>=720dp 宽屏双栏，适配手机竖屏/车机横屏。
- 首版不包含音乐在线播放。

## 设计来源

网络流程借鉴 LocalSend 的成熟思路：局域网组播发现，以及 `prepare -> upload -> cancel` 的传输阶段；本项目没有复制 LocalSend 应用源码，而是实现了一个更小的 MVP 协议，并增加 `/status`、offset resume 和完整 SHA-256 验证。LocalSend Protocol v2.2 本身也定义了 `sha256` 校验失败使用 422；详见 `docs/REFERENCES.md`。

## 打开与运行

环境：Android Studio + Android SDK 35，JDK 17+。最低 Android 10（API 29）。

1. 用 Android Studio 打开根目录。
2. Gradle Sync。
3. 在两台 Android 10+ 设备上分别安装 `app`。
4. 两台设备接入同一 Wi-Fi，或设备 B 连接设备 A 的热点。
5. 打开双方 LanShare。
6. 在设备 A 的文件管理器/相册中长按文件 → 分享 → LanShare。
7. 选择设备 B；B 点击“接收”。
8. 成功后在 B 的 `Download/LanShare` 查看文件。

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

交付环境已执行：项目结构检查、关键 Manifest/协议端点/文档存在性检查、ZIP 完整性检查。

**未执行 Android Gradle 编译与真机双机验证**：当前交付容器没有 Android SDK、Gradle，也无法联网下载依赖。不要把本 ZIP 当作“已真机验收”。首次真机验收必须按 `docs/TEST_PLAN.md` 执行。

## 已知 MVP 限制

- 传输是 HTTP 明文，仅适合受信任的本地网络；后续应加入 TLS/设备配对。
- 续传依赖同一 `sessionId`；App/设备重启后的持久化恢复尚未实现。
- 当前每次 `PUT` 传输剩余全部字节；网络断开后重新发起同一 session 会从 `.part` 长度继续。
- 设备发现只实现 UDP Multicast + 手动 IP；未做子网扫描。
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
- `AGENTS.md`
