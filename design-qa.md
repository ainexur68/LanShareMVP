# LanShare 页面设计验收

## 本次范围

本次改动重构分享页和设备连接页的 Compose 布局，保留文件选择、设备发现、扫码与手动连接、接收确认和传输入口。没有新增 Activity。

合并最新 main 时保留了 #3 的文件选择语义：内部选择通过 `SharedFileSelection.merge(..., replaceExisting = false)` 追加并去重；新的外部 `ACTION_SEND/ACTION_SEND_MULTIPLE` 即使 URI 无效，也会清空旧选择，避免误发上次任务文件。

## 保留的最终视觉证据

以下设备截图和关键对比图来自本次 UI 实现的最终视觉复核：

- [分享页竖屏空状态](artifacts/design-qa/share-empty-tablet-final.png)
- [分享页竖屏单文件状态](artifacts/design-qa/share-selected-one-tablet-final.png)
- [分享页竖屏多文件状态](artifacts/design-qa/share-selected-two-tablet-final.png)
- [分享页横屏空状态](artifacts/design-qa/share-landscape-empty-tablet.png)
- [分享页横屏已选状态](artifacts/design-qa/share-landscape-selected-tablet.png)
- [连接页最终状态](artifacts/design-qa/tb321fu-connection-final.png)
- [分享页空状态设计对比](artifacts/design-qa/comparison-share-empty-final.png)
- [分享页已选状态设计对比](artifacts/design-qa/comparison-share-selected-final.png)
- [连接页设计对比](artifacts/design-qa/comparison-connect-revised.png)

这些截图记录的是 UI 分支合并前的设备视觉检查，不代替合并后代码的自动化验证。

## 当前验证状态

- 分享页、连接页及短高度横屏的布局状态已按上述设备截图检查。
- 合并后自动化结果以 GitHub Actions 的 `verify.yml` 新提交运行记录为准。
- 当前 revision 的双设备真实传输、断网续传恢复和 PHY110 本轮安装验证仍为 `BLOCKED / NOT VERIFIED`；不能用旧 APK 截图替代当前 revision 证据。
- 手工篡改临时文件后的 422 路径本轮未执行。
