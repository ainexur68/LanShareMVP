# MVP 真机验收计划

必须至少使用两台 Android 10+ 真机；“编译通过”不能代替真实链路验收。

## A. 基础

- [ ] `:app:assembleDebug` PASS。
- [ ] 两台设备均安装、启动，无 crash。
- [ ] 手机竖屏 UI 无遮挡。
- [ ] 横屏/车机宽屏呈双栏，无关键操作被裁切。

## B. 分享入口

- [ ] 文件管理器长按单文件 -> 分享 -> LanShare，文件名/大小正确。
- [ ] 多文件 ACTION_SEND_MULTIPLE 正确列出。
- [ ] 不依赖 filesystem path；来自相册/Downloads provider 的 content URI 可读。

## C. 发现与确认

- [ ] 同 Wi-Fi 自动发现。
- [ ] 手机热点场景自动发现；若失败，手动 IP 可发送。
- [ ] 接收端拒绝后发送端明确失败。
- [ ] 未确认前不写正式 Download 文件。

## D. 正常传输

- [ ] 10 KB、10 MB、500 MB 文件均成功。
- [ ] 接收文件位于 `Download/LanShare`。
- [ ] 发送端原文件 SHA-256 == 接收端最终文件 SHA-256。

## E. 断点续传

- [ ] 传输 500 MB 文件至 30%-70% 时关闭 Wi-Fi 10 秒。
- [ ] 恢复网络后重试同一 session，不从 0 开始，服务端 offset > 0。
- [ ] 最终 hash 一致。

## F. 失败安全

- [ ] 人工篡改 `.part` 后 complete 返回 422，Downloads 不出现“成功文件”。
- [ ] 空间不足时明确失败，不显示成功。
- [ ] 对方 App 被杀/断网，发送端不假成功。
- [ ] 文件重名时 MediaStore 正常生成可用项，不覆盖未知原文件。

## 证据要求

每一条 PASS 保存：设备型号/Android 版本、操作步骤、日志或截图、最终 hash。Agent 不得用单测绿替代真实传输证据。
