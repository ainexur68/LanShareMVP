# LanShare 页面设计验收

## 本轮分享页状态切换追加

本轮按最新两张分享页设计稿重构了同一个 `SharePane`：文件数量为 0 时渲染紧凑的“文件 → 设备”分组面板，选择文件后在同一 Compose 页面内切换为文件确认列表；没有新增 Activity，也没有复制第二个分享页面。

源视觉目标：

- `C:\Users\k\AppData\Local\Temp\codex-clipboard-5c80c68f-71c3-4a20-abd0-bad966a40a0b.png`（未选择状态，943×1668）
- `C:\Users\k\AppData\Local\Temp\codex-clipboard-b08fc4b7-cf77-4cc0-a2bb-966714a812d7.png`（已选择状态，943×1668）

本轮 TB321FU 实现证据：

- [share-empty-tablet-final.png](artifacts/design-qa/share-empty-tablet-final.png)：最终 APK 的竖屏未选择状态，1600×2560，400 dpi。
- [share-selected-two-tablet-final.png](artifacts/design-qa/share-selected-two-tablet-final.png)：最终 APK 的竖屏两文件确认状态，1600×2560，400 dpi。
- [share-landscape-empty-tablet.png](artifacts/design-qa/share-landscape-empty-tablet.png)：横屏空状态双栏，2560×1600，400 dpi。
- [share-landscape-selected-tablet.png](artifacts/design-qa/share-landscape-selected-tablet.png)：横屏已选择状态双栏，2560×1600，400 dpi。
- [comparison-share-empty-final.png](artifacts/design-qa/comparison-share-empty-final.png)：源稿与最终 APK 竖屏实现同一组合输入。
- [comparison-share-selected-final.png](artifacts/design-qa/comparison-share-selected-final.png)：源稿与最终 APK 竖屏实现同一组合输入。

源稿为 943×1668 的手机比例，TB321FU 为 1600×2560 的平板比例；组合图按相同高度缩放保留纵横比，未把设备比例差异误判为页面溢出。横屏证据单独检查了左右双栏、小高度下的按钮和二维码区域。

## 结果范围

本轮实现了三张设计稿对应的 Compose 页面：分享首页、连接页、待发送文件弹层；保留 `content://` 选择、设备发现、二维码、手动连接、人工接收和流式传输逻辑。

源设计稿均为 941×1672：

- `C:\Users\k\AppData\Local\Temp\codex-clipboard-bf63367b-3de2-411d-aa05-05036a24940b.png`：分享首页
- `C:\Users\k\AppData\Local\Temp\codex-clipboard-ca6aa225-e543-4358-8ceb-80baba1e01cc.png`：连接页
- `C:\Users\k\AppData\Local\Temp\codex-clipboard-aa22dc73-0496-49d3-8d7d-1ba38d84a65a.png`：待发送文件弹层
- `C:\Users\k\AppData\Local\Temp\codex-clipboard-e21150dc-ff9e-456c-95e3-9357c0d3b5be.png`：连接页收敛稿（本轮连接页目标）

设备实现截图为 PHY110 1440×3168（Android 15、override density 560 dpi）和 TB321FU 横屏 2560×1600（Android 16、400 dpi）。比较图已统一缩放到 470px 面板宽度，保留各自纵横比；比较输入见：

- [comparison-main.png](artifacts/design-qa/comparison-main.png)
- [comparison-connect.png](artifacts/design-qa/comparison-connect.png)
- [comparison-dialog.png](artifacts/design-qa/comparison-dialog.png)

## 视觉证据

- [phy110-share-final.png](artifacts/design-qa/phy110-share-final.png)：真实多文件队列、已发现 TB321FU、发送 CTA
- [phy110-connect-camera.png](artifacts/design-qa/phy110-connect-camera.png)：相机预览、扫码框/扫描线、手电筒入口、本机二维码
- [tb321fu-connection-revised.png](artifacts/design-qa/tb321fu-connection-revised.png)：TB321FU 横屏双栏下的默认关闭相机状态；扫码卡与二维码卡同尺寸
- [tb321fu-connection-final.png](artifacts/design-qa/tb321fu-connection-final.png)：最终 APK 重装后的默认关闭相机状态
- [tb321fu-connection-camera-revised.png](artifacts/design-qa/tb321fu-connection-camera-revised.png)：单击扫码卡后的真实 CameraX 预览状态
- [tb321fu-connection-toggled-off.png](artifacts/design-qa/tb321fu-connection-toggled-off.png)：再次单击扫码卡后的关闭状态
- [comparison-connect-revised.png](artifacts/design-qa/comparison-connect-revised.png)：新连接页设计稿与 TB321FU 横屏实现的组合比对；右侧为 expanded 响应式状态
- [phy110-files-dialog-height-fixed.png](artifacts/design-qa/phy110-files-dialog-height-fixed.png)：文件类型图标、内部列表和完整底部汇总栏
- [tb321fu-landscape-final.png](artifacts/design-qa/tb321fu-landscape-final.png)：横屏分享/连接双栏
- [tb321fu-incoming-approval.png](artifacts/design-qa/tb321fu-incoming-approval.png)：人工接收确认
- [phy110-reject-approval.png](artifacts/design-qa/phy110-reject-approval.png)：拒绝确认
- [phy110-send-complete.png](artifacts/design-qa/phy110-send-complete.png)：发送端 3/3、SHA-256 校验通过
- [tb321fu-receive-complete.png](artifacts/design-qa/tb321fu-receive-complete.png)：接收端已验证并保存
- [phy110-incoming-approval-reverse.png](artifacts/design-qa/phy110-incoming-approval-reverse.png)：反向人工接收确认
- [phy110-receive-complete.png](artifacts/design-qa/phy110-receive-complete.png)：反向接收完成
- [share-empty-tablet-revised-clean.png](artifacts/design-qa/share-empty-tablet-revised-clean.png)：本轮空状态，真实设备、无通知授权弹层干扰
- [share-selected-one-tablet-revised.png](artifacts/design-qa/share-selected-one-tablet-revised.png)：本轮单文件紧凑确认状态
- [share-selected-two-tablet-revised.png](artifacts/design-qa/share-selected-two-tablet-revised.png)：本轮两文件、类型图标、删除按钮和汇总
- [share-empty-tablet-final.png](artifacts/design-qa/share-empty-tablet-final.png)：最终 APK 空状态
- [share-selected-one-tablet-final.png](artifacts/design-qa/share-selected-one-tablet-final.png)：最终 APK 单文件状态
- [share-selected-two-tablet-final.png](artifacts/design-qa/share-selected-two-tablet-final.png)：最终 APK 两文件状态
- [share-empty-after-delete-tablet.png](artifacts/design-qa/share-empty-after-delete-tablet.png)：删除最后一个文件后恢复空状态
- [share-landscape-stable-empty.png](artifacts/design-qa/share-landscape-stable-empty.png)：本轮修订横屏空状态，分享内容限宽并居中
- [share-landscape-stable-selected.png](artifacts/design-qa/share-landscape-stable-selected.png)：本轮修订横屏单文件状态，设备区与按钮间保留留白
- [share-portrait-stable-empty.png](artifacts/design-qa/share-portrait-stable-empty.png)：本轮修订竖屏空状态
- [share-portrait-stable-selected.png](artifacts/design-qa/share-portrait-stable-selected.png)：本轮修订竖屏单文件状态
- [share-short-expanded-empty.png](artifacts/design-qa/share-short-expanded-empty.png)：短高度 expanded 横屏兜底；内容区可滚动，footer 不与内容重叠
- [share-short-expanded-empty-scrolled.png](artifacts/design-qa/share-short-expanded-empty-scrolled.png)：短高度横屏上滑后设备行可访问，发送按钮仍固定

状态差异已标注：设计稿使用 Pixel 9 和 1.8 GB 演示数据；设备证据使用 TB321FU/PHY110 和三个真实本地测试文件，页面结构和交互状态相同。

## 功能验收

- `content://` 多选：通过 Android `OpenMultipleDocuments` 选择 3 个文件，追加选择逻辑保留已有队列。
- 文件类型：图片、PDF、视频分别显示独立图标和颜色。
- 分享页状态：重复选择同一 URI 后仍保持 `已选 2 个文件`，删除两个文件后 UI 回到 `选择文件 / 尚未选择文件`；文件列表只在确认面板内部滚动。
- 分享页设备刷新：点击“重新扫描设备”会清空旧设备并触发 DiscoveryManager 立即重新广播；可用设备重新出现后恢复选中态。
- 分享页发送：有文件且设备 token 有效时按钮进入启用态，仍调用 `TransferClient.send`；没有文件时按钮保持禁用并带 Material 无障碍 disabled 语义。
- 扫码页：默认关闭相机；单击扫码窗口打开，再次单击窗口关闭；CameraX/ML Kit 预览、扫描框、扫描线和手电筒入口均已触达（TB321FU）。
- 连接页布局：移除“扫描二维码”“我的二维码”等占位标题；扫码窗口与二维码卡共用同一 `cardSize`，按可用高度自适应且不再撑满页面；保留设备标识和“手动连接”。
- 手机 PHY110 → 平板 TB321FU：人工点击“接收”，3/3 完成，接收端显示 `已验证并保存到 Download/LanShare`。
- 平板 TB321FU → 手机 PHY110：人工点击“接收”，3/3 完成，接收端显示 `已验证并保存到 Download/LanShare`。
- 拒绝路径：人工点击“拒绝”后发送端显示 `对方拒绝或超时 (403)`，未发布正式文件。
- SHA-256：两方向三文件逐文件一致，完整输出见 [hash-evidence.txt](artifacts/design-qa/hash-evidence.txt)。
- 设备信息见 [device-info.txt](artifacts/design-qa/device-info.txt)。

## 断网续传状态

已实际触发一次接收端 Wi-Fi 中断：接收端生成了 15,822,296 字节的 `.part` 临时文件，证明传输没有从正式 Downloads 文件开始写入；无线调试链路恢复后，接收服务进程未保持可查询状态，发送端最终显示 `无法从接收端获取续传偏移`。因此断网续传本轮记为 `NOT VERIFIED/BLOCKED`，没有把该路径计为通过，也没有把 `.part` 当作成功文件。

## 本轮视觉对比结论

- 字体/层级：页面标题继续使用连接页同一 `titleLarge` 令牌；主文字、次文字和操作文字的权重与颜色层级清晰，长文件名由单行省略保护删除按钮。
- 间距/布局：空状态为单个两行分组面板；确认状态按文件数量计算面板高度，文件多时仅 `LazyColumn` 内部滚动；TB321FU 竖屏和横屏截图均未出现遮挡或按钮被挤出。
- 颜色/令牌：主题统一为白底、`#07183D` 深海军蓝、`#1677FF` 主蓝、`#F3F7FD` 模块底色和轻描边；未引入渐变或大面积虚线框。
- 图标/资产：文件类型、设备、发送、扫码和帮助图标使用 Material Icons；未用插画或占位图片替代功能元素。
- 文案/内容：移除旧版 `LanShare`、营销副标题及“扫描二维码/我的二维码”占位文字；保留真实设备名、同一局域网标识、重新扫描、添加、删除和发送入口。

组合对比中唯一的可预期差异是源稿使用 PHY110/演示文件，而最终设备使用 TB321FU/真实本地文件，且源稿是手机比例、实现证据是平板比例；这些是状态和窗口差异，不是裁剪或重复页面。

本轮 TB321FU 视觉状态未发现可操作的 P0/P1/P2 级设计偏差；PHY110 不能连接导致整体验收仍保留 `BLOCKED`，不把旧设备截图当作本轮证据。

## 自动化门禁

- `python scripts/static_verify.py`：通过（`STATIC_VERIFY_PASS`）。
- `git diff --check`：通过；仅有 Git 的 LF→CRLF 工作区提示。
- `JAVA_HOME=G:\dev-tools\jdk-21` + Gradle wrapper 8.10.2 + Windows ROOT/TLS 进程参数：`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --refresh-dependencies --no-daemon` 通过，`BUILD SUCCESSFUL`。
- APK：`app/build/outputs/apk/debug/app-debug.apk`；SHA-256 `E55A1986A192646CD06D2E49D048E6CFC353C82F284BF290B3CACD30FA4267DE`；本轮修订 APK 已安装到 TB321FU。PHY110 当前无线 ADB 地址拒绝连接，未能安装本轮修订 APK，不能以旧截图替代本轮竖屏证据。

## 迭代记录

1. 首轮截图发现副标题折行，改为单行省略并重新安装验证。
2. 首轮弹层发现底部汇总被 Compose `LazyColumn` 测量挤出窗口；先添加失败回归测试，再按文件数计算弹层/列表高度，最终截图确认汇总栏完整可见。
3. 添加文件类型回归测试、独立网络插画资源和横屏双栏布局。
4. 本轮连接页对照新收敛稿：两张卡统一尺寸、缩小并增加留白；相机初始化固定为关闭，点击卡片切换；TB321FU 横屏复验发现 QR 底部标识裁切后，改为按卡片尺寸比例缩放二维码并重新截图确认完整可见。
5. 本轮分享页第一次 TB321FU 截图发现单文件确认面板按最大高度撑开；将面板改为按文件数量计算高度（多文件时仅列表内部滚动），重新安装后单文件和两文件截图确认空白收敛、设备和发送按钮仍可见。
6. 本轮复现分享按钮上移：已选状态原先把按钮放在设备行之后、剩余空间放在按钮下方；改为内容区占剩余空间、空/已选共用固定底部操作区，并为摘要保留固定槽位。横屏空/已选按钮 bounds 均为 `[208,1324][1048,1464]`，竖屏空/已选均为 `[90,2284][1510,2424]`；横屏分享内容宽度为 840px（336dp），设备区与按钮间留白保持明显。
7. 代码审查发现短高度 expanded 横屏的潜在溢出；增加 `520dp` 以下内容区滚动兜底，并在 720px 高度模拟窗口验证 ScrollView 可滚动、设备行可通过上滑访问、footer 不重叠。

## 未验证项

- 断网后同一 session 的真实续传恢复：`BLOCKED/NOT VERIFIED`，原因是测试期间无线调试与接收服务同时断开，恢复后无法查询接收端 offset。
- 手工篡改 `.part` 后的 422 路径：本轮未执行。
- PHY110 上本轮分享页修订的竖屏截图、发送端验证和双机回归：`BLOCKED/NOT VERIFIED`，无线 ADB 地址 `192.168.31.203:34379` 当前拒绝连接，mDNS 也未发现 PHY110；不能用旧 APK/旧截图代替本轮证据。

blocked
