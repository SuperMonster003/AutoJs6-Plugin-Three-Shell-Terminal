<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>为 AutoJs6 及其脚本提供多会话终端, 在 pty 中运行系统 shell, 支持后台运行, 快捷按键及 Node.js 命令</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Shell Terminal 接管 AutoJs6 的内置终端: 主页抽屉的 "终端" 开关, 文件管理器目录菜单与项目工具栏的 "在终端中打开", 以及脚本侧用于打开, 驱动与监听终端会话的全局对象 `terminal`. 每个会话都是一个在 pty 中运行的系统 shell (`/system/bin/sh`), 离开界面后继续在后台运行.

AutoJs6 通过 Binder 服务发现插件, 以显式 Intent 打开终端界面, 并经 Binder 获取会话数, 关闭全部会话或驱动脚本会话; 会话输出经管道传回脚本. 安装了 Node.js Runtime 插件时, 终端直接读取其清单契约, 校验签名与启动器后提供 node / npm / npx / corepack / yarn / pnpm 命令.

******

### 当前状态

******

1.2.0 在独立终端, 后台会话, 交互式脚本 API 和设置页面之上, 增加终端配色与字体, 命令完成通知和第三方 Node.js 签名信任. 完整脚本 API 需要 AutoJs6 6.8.0 构建 5315 或更高版本; 基础插件协议要求构建 5304; 文件管理器目录菜单的插件入口要求构建 5320. 支持 Android 7.0 及以上版本.

******

### 功能

******

插件提供以下能力:

- 多会话: 新建, 切换, 关闭与会话管理器; 离开界面后会话由前台服务保持运行, 通知栏显示当前目录与会话数并提供 "关闭全部".
- 终端界面: 两行快捷键栏 (Esc / Tab / Ctrl / 方向键 / 常用符号), 原生文本选择与复制 / 全选, 转录复制与分享, 字号设置, 粘贴与清屏; 4 种终端配色 (跟随外观 / 深色 / 浅色 / 按主题色着色) 与终端字体 (系统等宽 / 内置无连字的 JetBrains Mono).
- 命令编辑器: 多行或超过 80 个字符的菜单 / 输入法粘贴先显示完整内容. 可直接粘贴, 或在当前目录的新会话执行并显示原文和退出码 (命令上限 16 KiB). 新会话不继承临时变量或交互式 su 登录; 需要 Root 时在命令中使用 su. 提交原文仅在该会话内存中保留, 可从文字操作菜单查看与复制.
- Node.js 工具链: 安装 Node.js Runtime 插件 (1.5.0+) 后可用 node / npm / npx / corepack / yarn / pnpm, npm 镜像源与 "忽略安装脚本" 设置, 以及 npm init / install / 运行脚本等包管理菜单.
- AutoJs6 入口: 主页抽屉开关 (显示会话数, 可关闭全部), 文件管理器目录菜单与项目工具栏的 "在终端中打开". 自 AutoJs6 构建 5320 起, 目录菜单项由插件经 Explorer Action 协议提供, AutoJs6 只传递所选目录的路径. AutoJs6 6.8.1 起, 该项位于子菜单 `3-Shell Terminal` 中.
- 脚本 API `terminal` (别名 `$terminal`): 会话管理, 可见执行 (`exec`, `npm.run`), 以及带 `output` / `exit` 事件, `write` 与 `waitFor` 的会话对象; 每个失败都是带稳定 `code` 的 `TerminalError`.
- 命令完成通知: 不带交互式 shell 的命令 (例如脚本以 `keepOpen: false` 调用 `exec`) 自行结束时发出通知, 点击查看输出末尾, 可复制, 分享或在原目录打开终端; 通知只写会话编号与目录, 不含命令与输出, 可在设置中关闭.
- 独立应用: 启动器图标直接进入终端, 设置页 (外观跟随 AutoJs6, 字号, 终端配色与字体, npm 镜像源, Node.js 集成, 受信任的 Node.js 签名, 命令完成通知, 全部文件访问, 清除终端数据), 关于与发行历史.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5304 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 安装与设备 ABI 对应的插件 APK (或 universal).
2. 打开 AutoJs6 插件中心, 确认 `3-Shell Terminal` 已被识别并启用它.
3. 在 AutoJs6 主页抽屉打开 "终端", 在文件管理器中对目录选择 "在终端中打开", 或在脚本中调用 `terminal.open(...)`. 需要进入共享存储 (如 `/sdcard`) 下的目录时, 按插件提示授予 "所有文件访问权限".

******

### Node.js 命令

******

终端如何获得 node / npm 以及相关限制:

- 需要 Node.js Runtime 插件 1.5.0 或更高版本; 插件读取其清单契约, 校验签名, 启动器与 npm / corepack 归档后, 在每次新建会话时把命令链接进 `PATH`. 未安装或校验失败时终端仍可用, 只是不含这些命令.
- Android 禁止执行应用写出的文件: npm 默认关闭 bin 链接, 因而 `node_modules/.bin/*` 与 `npx <包名>` 不能直接运行包入口. 请使用 `node node_modules/<包名>/<入口>.js`. npm 包自带的原生可执行文件会以 `EACCES` 失败, 原生扩展 (`.node`) 不可加载.
- corepack 默认使用内置的 pnpm 11.x 与 Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`), 显式指定版本时按指定版本下载; npm 镜像源可在设置中切换为 npmmirror 或自定义 https 地址.

******

### 快速开始

******

一个打开脚本目录, 可见地安装依赖并等待结束, 以及驱动交互式命令的脚本 (自路线图 P4 起可用):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', keepOpen: false, wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let driven = terminal.exec('sleep 1; printf "name? "; read name; echo "received:$name"; sleep 1', {
    cwd: files.cwd(), show: true, keepOpen: false,
});
driven.on('output', line => { if (/name\?/.test(line)) driven.write('AutoJs6\n'); });
driven.on('exit', code => console.log('Session exited with ' + code));
console.log(driven.waitFor(/received:AutoJs6/, 15e3));
```

******

### 兼容性

******

决定插件能力边界的平台事实:

- Android 7.0 (API 24) 及以上; 提供 arm64-v8a, armeabi-v7a, x86_64, x86 四个 ABI 的 APK 与 universal APK, 原生库按 16 KB 页对齐; 宿主构建与插件在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 列出的设备矩阵上一起验证.
- 终端进程以插件自身的 uid 与权限运行, 不继承 AutoJs6 的权限; 需要 AutoJs6 权限的命令请改用脚本的 `shell()` API.
- 会话随插件进程存在; 进程被系统结束后会话无法恢复, 前台服务与通知用于降低这种情况的发生.

******

### 常见问题

******

- **为什么 `cd /sdcard/脚本` 失败?** 插件需要自己的存储授权. 打开插件设置或按终端横幅的提示授予 "所有文件访问权限" (Android 11+), 旧系统则授予存储权限.
- **能否打开 `content://` 目录 (例如通过系统文件选择器授权的目录)?** 不能. shell 只能进入真实的文件系统路径, 而 `content://` 地址只能经内容提供者访问. 脚本 API 会直接拒绝此类地址; 从其它入口打开时终端回到主目录并给出提示. 共享存储中的目录请使用 `/sdcard/...` 这样的路径并授予全部文件访问.
- **为什么没有 node 命令?** 请在 AutoJs6 插件中心安装 Node.js Runtime 插件 (1.5.0+); 插件设置中的 "环境探测" 会显示具体原因 (未安装, 版本过旧, 签名不受信任或启动器不可执行). 签名不受信任时, 该页会列出 SHA-256, 确认信任后新会话即可使用它的命令.
- **系统会停止后台会话吗?** 会. 插件进程被终止后不能恢复原会话. HyperOS, MIUI 等系统可能限制通知或后台运行, 请在 Android 设置中允许相应权限; 打开终端会在允许时重试前台保护. 正常离开界面会保留会话, 但前台服务不能绕过系统限制.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 服务与界面入口受 `org.autojs.permission.PLUGIN` 签名权限保护并校验调用方签名, 只有 AutoJs6 能够访问; 启动器入口只打开终端, 不接收外部命令.
- 存储权限 (Android 11+ 为 "所有文件访问权限") 只用于进入你选择的目录; 终端不会扫描或上传文件.
- `INTERNET` 权限由你在 shell 中运行的命令 (如 `npm install`) 使用, 以及手动检查更新时访问本插件固定的 GitHub Releases 接口; 插件自身不在后台联网.
- Node.js Runtime 插件的启动器只在其签名为官方签名, 与本插件一致, 或已由你在环境探测页查看 SHA-256 后确认信任时执行 (可在设置中撤销); 插件不记录会话输入输出, 并将私有存储排除在备份之外.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

```text
application id: io.github.supermonster003.autojs6.plugin.three.shell.terminal
plugin id: three-shell-terminal
engine: terminal
variant: default
service action: org.autojs.plugin.TERMINAL
service category: terminal
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.terminal.api.ITerminalPlugin
minimum host build: 5304 (6.8.0)
```

`ThreeShellTerminalPluginService` 响应 `org.autojs.plugin.TERMINAL` (category `terminal`), 自路线图 P2 起实现宿主 terminal-api 契约 `org.autojs.plugin.terminal.api.ITerminalPlugin`. `ThreeShellTerminalPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件; 终端界面经 `org.autojs.plugin.TERMINAL_OPEN` 打开. `ThreeShellTerminalExplorerActionService` 响应 `org.autojs.plugin.EXPLORER_ACTION` (Explorer Action 协议 v23), 声明单个目录的 "在终端中打开" 动作, 由 `ThreeShellTerminalExplorerActivity` 执行.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.2.1

_2026/11/20_

- `修复` 设备繁忙时新建终端会话后很快关闭, 个别会话关闭后仍占用两个文件描述符, 要等垃圾回收才释放的问题
- `依赖` 升级 jackpal emulatorview 补丁 1.0.42-p8.1 -> 1.0.42-p8.2: 会话在写线程启动前就已关闭时, 写线程立即释放消息队列的 eventfd 与 epoll 描述符; 只改动 `TermSession`, 其余类逐字节不变, `locks/vendored-aars.lock` 已更新

#### v1.2.0

_2026/11/20_

- `新增` 命令编辑器: 多行或长文本粘贴时显示可换行编辑的完整内容, 可直接粘贴到当前终端或在当前目录的新会话执行; 执行时显示命令原文和退出码, 支持查看与复制提交的命令
- `修复` 键盘输入 emoji 等四字节 Unicode 字符时末字节丢失, 以及随后输入中文等字符时编码损坏的问题
- `修复` 大段粘贴阻塞界面与回显的问题; 输入按顺序在后台发送, 直接粘贴不再应用粘滞 Ctrl / Alt, 队列满时明确提示而不截断输入
- `修复` 启动终端会话时, 命令参数, 目录名和环境变量中的中文等非 ASCII 字符被截断的问题
- `优化` 版本历史页改名为发行历史页, 按版本展开或收起: 收起时显示日期与各类别的条目数, 展开后每个类别只标注一次, 可一次展开或收起全部, 打开时展开最新版本; 子标题为插件名称
- `优化` 插件中心中的使用说明重新编写, 分为开始使用, 主要功能, 在脚本中使用, 运行要求与常见问题五部分
- `优化` 设置页右上角菜单新增 "打开通用设置页", 用于打开 AutoJs6 中本插件的通用设置页 (需要 AutoJs6 6.8.1 及以上版本, 更早的版本不显示该菜单项)
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 与 androidx.activity:activity-compose 版本 1.12.2 (发行历史页)

#### v1.1.1

_2026/11/07_

- `优化` AutoJs6 文件管理器把本插件的菜单选项归入子菜单 "3-Shell Terminal", 名称与插件中心一致 (需要 AutoJs6 6.8.1 及以上版本)

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

本节面向希望从源码构建插件的开发者; 普通用户直接安装 Releases 页面的预构建 APK 即可.

构建 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并构建 instrumentation 测试 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集发布产物并在文件名后追加版本与 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档源与生成产物是否同步 (CI 同样执行此检查):

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

******

### 本地化与文档生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` 与 `.changelog/` 下的语言 JSON 文件是 README, 插件中心说明与更新日志的唯一文案源. 请始终修改这些 JSON 源文件并重新运行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 与更新日志产物不得手工编辑. 运行 `py .python/generate_markdown.py --check` 可校验全部生成产物.

******

### 许可证

******

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- 终端模块文档: https://docs.autojs6.com/#/terminal
- Node.js Runtime 插件: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (终端仿真与 pty 原生库, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
