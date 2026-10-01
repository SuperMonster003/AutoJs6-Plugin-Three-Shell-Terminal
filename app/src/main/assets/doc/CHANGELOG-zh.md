******

### 发行历史

******

# v1.0.0

###### 2026/10/01

* `提示` P0 开发预览: 仓库骨架, 可被 AutoJs6 插件中心识别的插件身份, 以及 pty / 存储 / Node.js 启动器 spike. Binder 契约, 会话核心, 终端界面, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `three-shell-terminal` (engine `terminal`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.TERMINAL` 服务骨架
* `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 与 universal APK, 原生库按 16 KB 页对齐
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `新增` 自宿主终端迁入会话核心: 基于 pty 的 shell 会话与进程级注册表 (记录标题与退出码供 Binder 使用), 插件自有文件目录下的会话环境与目录布局, Node.js 启动器发现与 npm / corepack 安装器, 以及保持会话运行并提供 "关闭会话" 通知的前台服务 (渠道 `three.shell.terminal.sessions`)
* `依赖` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作为终端仿真与 pty 原生库, 并在 `locks/vendored-aars.lock` 中锁定哈希
* `依赖` 附加 `common-plugin-api.aar` 与 `nodejs-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api` 与 `plugin-api/nodejs-api`, 宿主构建 6.8.0 / 5303, MPL 2.0) 作为共享插件契约与 Node.js 清单契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `terminal-api.aar` (AutoJs6 模块 `plugin-api/terminal-api`, 宿主构建 6.8.0 / 5304, MPL 2.0) 作为终端契约 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限与错误码), 插件身份常量改由它提供, 并在 `locks/host-api-aars.lock` 中锁定哈希
