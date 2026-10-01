3-Shell Terminal 接管 AutoJs6 的内置终端: 主页抽屉的 "终端" 开关, 文件管理器目录菜单与项目工具栏的 "在终端中打开", 以及脚本侧用于打开, 驱动与监听终端会话的全局对象 `terminal`. 每个会话都是一个在 pty 中运行的系统 shell (`/system/bin/sh`), 离开界面后继续在后台运行.

P2 开发预览: 已实现 shell 会话, 存储访问, 带签名信任的 Node.js 集成与宿主会话控制. 终端界面, 脚本 API 与设置页将继续按 ROADMAP.md 的阶段推进. AutoJs6 6.8.0 (build 5304+).

### 使用方法

1. 在安装了 AutoJs6 构建 5304 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 安装与设备 ABI 对应的插件 APK (或 universal).
2. 打开 AutoJs6 插件中心, 确认 `3-Shell Terminal` 已被识别并启用它.
3. 在 AutoJs6 主页抽屉打开 "终端", 在文件管理器中对目录选择 "在终端中打开", 或在脚本中调用 `terminal.open(...)`. 需要进入共享存储 (如 `/sdcard`) 下的目录时, 按插件提示授予 "所有文件访问权限".

### Node.js 命令

- 需要 Node.js Runtime 插件 1.5.0 或更高版本; 插件读取其清单契约, 校验签名, 启动器与 npm / corepack 归档后, 在每次新建会话时把命令链接进 `PATH`. 未安装或校验失败时终端仍可用, 只是不含这些命令.
- Android 禁止执行应用写出的文件: `node_modules/.bin/*` 与 npm 包自带的原生可执行文件会以 `EACCES` 失败, 请改用 `node <入口文件>` 或 `npx`; 原生扩展 (`.node`) 不可加载.
- corepack 默认使用内置的 pnpm 11.x 与 Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`), 显式指定版本时按指定版本下载; npm 镜像源可在设置中切换为 npmmirror 或自定义 https 地址.

安装指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
