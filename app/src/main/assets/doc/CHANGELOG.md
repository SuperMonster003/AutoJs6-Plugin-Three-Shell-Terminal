******

### 发行历史

******

# v1.0.0

###### 2026/10/05

* `提示` P5 本地开发预览: 已实现终端界面, 多会话管理, 脚本 API (输出监听, 交互输入与退出码等待), 独立设置页, 关于 / 版本历史 / 检查更新, 以及启动器图标. 脚本 API 需使用包含 P4 实现的 AutoJs6 构建; 插件中心的设置入口需支持 TERMINAL_SETTINGS 的构建.
* `新增` 插件标识 `three-shell-terminal` (engine `terminal`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.TERMINAL` 服务骨架
* `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 与 universal APK, 原生库按 16 KB 页对齐
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `新增` 自宿主终端迁入会话核心: 基于 pty 的 shell 会话与进程级注册表 (记录标题与退出码供 Binder 使用), 插件自有文件目录下的会话环境与目录布局, Node.js 启动器发现与 npm / corepack 安装器, 以及保持会话运行并提供 "关闭会话" 通知的前台服务 (渠道 `three.shell.terminal.sessions`)
* `新增` 存储访问解析 (`StorageAccess`): 以插件自身的权限状态判定 (API 30 以下为旧式运行时权限, API 30 起为 "所有文件访问权限"), 识别 `/sdcard`, `/storage/...` 等共享存储与自有 `Android/{data,obb,media}` 目录, 起始目录回退 `$HOME` 并给出 `STORAGE_PERMISSION_REQUIRED` 或 `DIRECTORY_INACCESSIBLE`, 以及打开所有文件访问开关的设置 Intent
* `新增` 带签名信任的 Node.js 集成 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): 仅当 Node.js Runtime 插件由 AutoJs6 官方插件密钥或本插件自身密钥签名时才使用, 设置开关在任何查找之前短路, 每种结果映射到契约的 `node-cli` 状态 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), 每次会话启动刷新 `usr/bin` 命令链接, 按摘要只解压一次 npm / corepack 归档并导出 npm / corepack 环境变量
* `新增` AutoJs6 可创建和控制最多 16 个终端会话, 每个会话支持最多 4 个实时输出监听, 并可读取最近输出和查询 shell 环境. 关闭 AutoJs6 后会话继续运行; 无效请求会返回具体原因.
* `新增` 包管理支持 npm init, 安装依赖或指定包, 读取并运行 package.json 脚本, 查看 Yarn / pnpm 命令与搜索 npm. 支持 npmjs, npmmirror 与自定义 HTTPS 镜像源, 以及忽略安装脚本. 清除终端数据会先关闭所有会话, 再清空 home / usr 并重建目录, 保留设置和外部工程. 菜单与设置页将在后续阶段接入.
* `新增` 终端界面 (`TerminalActivity`): 宿主终端界面迁入插件自有的 Material 3 主题, 含快捷键栏 (Esc / Tab / Ctrl / Alt / 方向 / 翻页), 双指缩放字号, 长按选择并复制文本, 会话 / 文本 / 包管理 / 设置 / 帮助菜单, 显示 shell 当前目录并可点按复制的工具栏副标题, 以及说明 Node.js 运行时缺失 / 不受信任 / 版本过旧 / 已停用并提供安装 / 更新 / 启用 / 详情动作的 Node.js 横幅; 无法进入共享存储目录时出现存储横幅并提供 "授予" 与 "重新进入目录"; 界面通过宿主设置提供者跟随 AutoJs6 的语言, 夜间模式与主题色, 无宿主时回退到系统值与共用的 `#FFDEAD` 颜色
* `新增` 会话管理器: 含状态 / 控制 / 会话 / 设置四个可收起分组的对话框, 列出全部运行中的会话及其目录, PID 与运行时长, 可打开或关闭单个会话, 新建会话, 关闭全部, 查看会话详情并复制, 并提供字号, npm 镜像源与 ignore-scripts 设置; 它与宿主 `onSessionsChanged` 使用同一会话注册表, 可从终端菜单, 会话通知 (点击) 进入, 宿主的 `manager=true` 入口则经透明的 `TerminalManagerActivity` 承载, 关闭后不会留下终端界面
* `新增` 宿主入口与启动器: 导出的 `TERMINAL_OPEN` 入口 Activity 受 `org.autojs.permission.PLUGIN` 签名权限保护, 对可识别的调用方校验权限持有与签名一致, 按契约上限校验 `directory` / `sessionId` / `newSession` / `command` / `manager` extras 并转发到自有任务中的终端界面或覆盖在调用方之上的会话管理器; `LauncherActivity` (图标 alias 的目标) 恢复最近会话或在主目录新建会话; 从宿主进入的终端按返回键回到宿主, 从启动器进入的回到桌面, 终端任务随之离开最近任务, 启动请求不会被重放
* `新增` 启动器图标选择: 私有启动器转发器之上的四个 `MAIN / LAUNCHER` activity alias (自适应亮色, 自适应暗色, 自适应自动, 透明背景), 默认启用自动图标; 选择由 PackageManager 以唯一启用的 alias 持久化, 每次界面启动与包更新后自动修复, 固定或动态快捷方式随之迁移到所选 alias
* `新增` 关于, 版本历史与检查更新: 关于页显示已安装版本, 构建号与构建日期, 开发者, 插件许可证 (MPL 2.0), 并在第三方组件分组中离线打开内置的 jackpal Android-Terminal-Emulator 许可证与声明 (Apache-2.0), AutoJs6 插件 API 声明 (MPL 2.0) 与依赖库声明; 文档页不使用 WebView, 按当前语言渲染内置更新日志 (回退英文); 手动 "检查更新" 仅经 HTTPS 访问 GitHub Releases API, 可取消, 有超时, 结果复用 12 小时, 可忽略指定版本, 并提供打开发行页或内置版本历史. 终端溢出菜单新增 "关于"
* `新增` 独立设置页, 分外观, 终端与信息三组: 外观含语言, 夜间模式, 主题色与启动器图标 (默认均跟随 AutoJs6, 主题色提供 16 个预设与 HEX / rgb() 输入); 终端含字号, npm 镜像源, 忽略安装脚本, Node.js 集成开关, 环境探测页, 全部文件访问与清除数据; 信息含检查更新, 版本历史与关于. 选择对话框先选后确定, 更改立即生效. 终端溢出菜单的 "设置" 直接进入该页 (原快捷设置子菜单移除), AutoJs6 插件中心的 "设置" 经 TERMINAL_SETTINGS 进入, 插件能力声明新增 settings
* `修复` 系统限制后台活动时, 启动会话不再导致插件崩溃; 会话会在没有前台服务保护的情况下继续运行.
* `修复` 读取较长转录时保留最新文本, 并控制回复大小, 避免跨进程消息超限.
* `修复` 脚本读取或回放终端输出时去除屏幕填充的尾部空行, 保留提示符空格和后续输出的行边界
* `修复` 会话从准备中进入运行中时, 宿主查询偶尔找不到该会话, 导致可见执行无法打开终端的问题
* `优化` 启动器与插件中心图标按统一视觉尺寸标准调整, 插件中心采用透明背景和黑白或中性灰阶图案
* `优化` 插件中心图标采用统一工作台调整后的尺寸, 位置, 亮暗图稿与圆形底色, 保留可重建原稿和参数
* `优化` Android 系统应用信息图标与图标工作台共用图稿和亮暗底色, 保留插件中心透明图稿及现有启动器选项
* `依赖` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作为终端仿真与 pty 原生库, 并在 `locks/vendored-aars.lock` 中锁定哈希
* `依赖` 附加 `common-plugin-api.aar` 与 `nodejs-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api` 与 `plugin-api/nodejs-api`, 宿主构建 6.8.0 / 5303, MPL 2.0) 作为共享插件契约与 Node.js 清单契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `terminal-api.aar` (AutoJs6 模块 `plugin-api/terminal-api`, 宿主构建 6.8.0 / 5304, MPL 2.0) 作为终端契约 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限与错误码), 插件身份常量改由它提供, 并在 `locks/host-api-aars.lock` 中锁定哈希
