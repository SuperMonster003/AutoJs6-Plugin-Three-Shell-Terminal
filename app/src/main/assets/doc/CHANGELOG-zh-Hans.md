******

### 发行历史

******

# v1.0.0

###### 2026/10/02

* `提示` P2 开发预览: 已实现 shell 会话, 存储访问, 带签名信任的 Node.js 集成与宿主会话控制. 终端界面, 脚本 API 与设置页将继续按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `three-shell-terminal` (engine `terminal`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.TERMINAL` 服务骨架
* `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 与 universal APK, 原生库按 16 KB 页对齐
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `新增` 自宿主终端迁入会话核心: 基于 pty 的 shell 会话与进程级注册表 (记录标题与退出码供 Binder 使用), 插件自有文件目录下的会话环境与目录布局, Node.js 启动器发现与 npm / corepack 安装器, 以及保持会话运行并提供 "关闭会话" 通知的前台服务 (渠道 `three.shell.terminal.sessions`)
* `新增` 存储访问解析 (`StorageAccess`): 以插件自身的权限状态判定 (API 30 以下为旧式运行时权限, API 30 起为 "所有文件访问权限"), 识别 `/sdcard`, `/storage/...` 等共享存储与自有 `Android/{data,obb,media}` 目录, 起始目录回退 `$HOME` 并给出 `STORAGE_PERMISSION_REQUIRED` 或 `DIRECTORY_INACCESSIBLE`, 以及打开所有文件访问开关的设置 Intent
* `新增` 带签名信任的 Node.js 集成 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): 仅当 Node.js Runtime 插件由 AutoJs6 官方插件密钥或本插件自身密钥签名时才使用, 设置开关在任何查找之前短路, 每种结果映射到契约的 `node-cli` 状态 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), 每次会话启动刷新 `usr/bin` 命令链接, 按摘要只解压一次 npm / corepack 归档并导出 npm / corepack 环境变量
* `新增` AutoJs6 可创建和控制最多 16 个终端会话, 每个会话支持最多 4 个实时输出监听, 并可读取最近输出和查询 shell 环境. 关闭 AutoJs6 后会话继续运行; 无效请求会返回具体原因.
* `新增` 包管理支持 npm init, 安装依赖或指定包, 读取并运行 package.json 脚本, 查看 Yarn / pnpm 命令与搜索 npm. 支持 npmjs, npmmirror 与自定义 HTTPS 镜像源, 以及忽略安装脚本. 清除终端数据会先关闭所有会话, 再清空 home / usr 并重建目录, 保留设置和外部工程. 菜单与设置页将在后续阶段接入.
* `修复` 系统限制后台活动时, 启动会话不再导致插件崩溃; 会话会在没有前台服务保护的情况下继续运行.
* `修复` 读取较长转录时保留最新文本, 并控制回复大小, 避免跨进程消息超限.
* `依赖` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作为终端仿真与 pty 原生库, 并在 `locks/vendored-aars.lock` 中锁定哈希
* `依赖` 附加 `common-plugin-api.aar` 与 `nodejs-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api` 与 `plugin-api/nodejs-api`, 宿主构建 6.8.0 / 5303, MPL 2.0) 作为共享插件契约与 Node.js 清单契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `terminal-api.aar` (AutoJs6 模块 `plugin-api/terminal-api`, 宿主构建 6.8.0 / 5304, MPL 2.0) 作为终端契约 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限与错误码), 插件身份常量改由它提供, 并在 `locks/host-api-aars.lock` 中锁定哈希
