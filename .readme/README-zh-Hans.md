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

P4 本地开发预览: 已实现终端界面, 多会话管理与脚本 API, 包括输出监听, 交互输入和退出码等待. 独立设置页按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 的 P5 推进. 脚本 API 需使用包含 P4 实现的 AutoJs6 构建.

******

### 功能

******

插件提供以下能力:

- 多会话: 新建, 切换, 关闭与会话管理器; 离开界面后会话由前台服务保持运行, 通知栏显示当前目录与会话数并提供 "关闭全部".
- 终端界面: 两行快捷键栏 (Esc / Tab / Ctrl / 方向键 / 常用符号), 原生文本选择与复制 / 全选, 转录复制与分享, 字号设置, 粘贴与清屏.
- Node.js 工具链: 安装 Node.js Runtime 插件 (1.5.0+) 后可用 node / npm / npx / corepack / yarn / pnpm, npm 镜像源与 "忽略安装脚本" 设置, 以及 npm init / install / 运行脚本等包管理菜单.
- AutoJs6 入口: 主页抽屉开关 (显示会话数, 可关闭全部), 文件管理器目录菜单与项目工具栏的 "在终端中打开".
- 脚本 API `terminal` (别名 `$terminal`): 会话管理, 可见执行 (`exec`, `npm.run`), 以及带 `output` / `exit` 事件, `write` 与 `waitFor` 的会话对象; 每个失败都是带稳定 `code` 的 `TerminalError`.
- 独立应用: 启动器图标直接进入终端, 设置页 (外观跟随 AutoJs6, 字号, npm 镜像源, Node.js 集成, 全部文件访问, 清除终端数据), 关于与发行历史.

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
- **为什么没有 node 命令?** 请在 AutoJs6 插件中心安装 Node.js Runtime 插件 (1.5.0+); 插件设置中的 "环境探测" 会显示具体原因 (未安装, 版本过旧, 签名不受信任或启动器不可执行).
- **离开终端后命令还在运行吗?** 是. 会话由前台服务保持, 通知栏显示会话数; 关闭通知中的 "关闭全部", 抽屉开关或会话本身才会结束 shell.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 服务与界面入口受 `org.autojs.permission.PLUGIN` 签名权限保护并校验调用方签名, 只有 AutoJs6 能够访问; 启动器入口只打开终端, 不接收外部命令.
- 存储权限 (Android 11+ 为 "所有文件访问权限") 只用于进入你选择的目录; 终端不会扫描或上传文件.
- `INTERNET` 权限由你在 shell 中运行的命令 (如 `npm install`) 使用, 以及手动检查更新时访问本插件固定的 GitHub Releases 接口; 插件自身不在后台联网.
- Node.js Runtime 插件的启动器只在其签名为官方签名 (或与本插件一致) 时执行; 插件不记录会话输入输出, 并将私有存储排除在备份之外.

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

`ThreeShellTerminalPluginService` 响应 `org.autojs.plugin.TERMINAL` (category `terminal`), 自路线图 P2 起实现宿主 terminal-api 契约 `org.autojs.plugin.terminal.api.ITerminalPlugin`. `ThreeShellTerminalPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件; 终端界面经 `org.autojs.plugin.TERMINAL_OPEN` 打开.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/10/02_

- `提示` P4 本地开发预览: 已实现终端界面, 多会话管理与脚本 API, 包括输出监听, 交互输入和退出码等待. 独立设置页按 ROADMAP.md 的 P5 推进. 脚本 API 需使用包含 P4 实现的 AutoJs6 构建.
- `新增` 插件标识 `three-shell-terminal` (engine `terminal`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.TERMINAL` 服务骨架
- `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 与 universal APK, 原生库按 16 KB 页对齐
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `新增` 自宿主终端迁入会话核心: 基于 pty 的 shell 会话与进程级注册表 (记录标题与退出码供 Binder 使用), 插件自有文件目录下的会话环境与目录布局, Node.js 启动器发现与 npm / corepack 安装器, 以及保持会话运行并提供 "关闭会话" 通知的前台服务 (渠道 `three.shell.terminal.sessions`)
- `新增` 存储访问解析 (`StorageAccess`): 以插件自身的权限状态判定 (API 30 以下为旧式运行时权限, API 30 起为 "所有文件访问权限"), 识别 `/sdcard`, `/storage/...` 等共享存储与自有 `Android/{data,obb,media}` 目录, 起始目录回退 `$HOME` 并给出 `STORAGE_PERMISSION_REQUIRED` 或 `DIRECTORY_INACCESSIBLE`, 以及打开所有文件访问开关的设置 Intent
- `新增` 带签名信任的 Node.js 集成 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): 仅当 Node.js Runtime 插件由 AutoJs6 官方插件密钥或本插件自身密钥签名时才使用, 设置开关在任何查找之前短路, 每种结果映射到契约的 `node-cli` 状态 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), 每次会话启动刷新 `usr/bin` 命令链接, 按摘要只解压一次 npm / corepack 归档并导出 npm / corepack 环境变量
- `新增` AutoJs6 可创建和控制最多 16 个终端会话, 每个会话支持最多 4 个实时输出监听, 并可读取最近输出和查询 shell 环境. 关闭 AutoJs6 后会话继续运行; 无效请求会返回具体原因.
- `新增` 包管理支持 npm init, 安装依赖或指定包, 读取并运行 package.json 脚本, 查看 Yarn / pnpm 命令与搜索 npm. 支持 npmjs, npmmirror 与自定义 HTTPS 镜像源, 以及忽略安装脚本. 清除终端数据会先关闭所有会话, 再清空 home / usr 并重建目录, 保留设置和外部工程. 菜单与设置页将在后续阶段接入.
- `新增` 终端界面 (`TerminalActivity`): 宿主终端界面迁入插件自有的 Material 3 主题, 含快捷键栏 (Esc / Tab / Ctrl / Alt / 方向 / 翻页), 双指缩放字号, 长按选择并复制文本, 会话 / 文本 / 包管理 / 设置 / 帮助菜单, 显示 shell 当前目录并可点按复制的工具栏副标题, 以及说明 Node.js 运行时缺失 / 不受信任 / 版本过旧 / 已停用并提供安装 / 更新 / 启用 / 详情动作的 Node.js 横幅; 无法进入共享存储目录时出现存储横幅并提供 "授予" 与 "重新进入目录"; 界面通过宿主设置提供者跟随 AutoJs6 的语言, 夜间模式与主题色, 无宿主时回退到系统值与共用的 `#FFDEAD` 颜色
- `新增` 会话管理器: 含状态 / 控制 / 会话 / 设置四个可收起分组的对话框, 列出全部运行中的会话及其目录, PID 与运行时长, 可打开或关闭单个会话, 新建会话, 关闭全部, 查看会话详情并复制, 并提供字号, npm 镜像源与 ignore-scripts 设置; 它与宿主 `onSessionsChanged` 使用同一会话注册表, 可从终端菜单, 会话通知 (点击) 进入, 宿主的 `manager=true` 入口则经透明的 `TerminalManagerActivity` 承载, 关闭后不会留下终端界面
- `新增` 宿主入口与启动器: 导出的 `TERMINAL_OPEN` 入口 Activity 受 `org.autojs.permission.PLUGIN` 签名权限保护, 对可识别的调用方校验权限持有与签名一致, 按契约上限校验 `directory` / `sessionId` / `newSession` / `command` / `manager` extras 并转发到自有任务中的终端界面或覆盖在调用方之上的会话管理器; `LauncherActivity` (图标 alias 的目标) 恢复最近会话或在主目录新建会话; 从宿主进入的终端按返回键回到宿主, 从启动器进入的回到桌面, 终端任务随之离开最近任务, 启动请求不会被重放
- `新增` 启动器图标选择: 私有启动器转发器之上的四个 `MAIN / LAUNCHER` activity alias (自适应亮色, 自适应暗色, 自适应自动, 透明背景), 默认启用自动图标; 选择由 PackageManager 以唯一启用的 alias 持久化, 每次界面启动与包更新后自动修复, 固定或动态快捷方式随之迁移到所选 alias
- `修复` 系统限制后台活动时, 启动会话不再导致插件崩溃; 会话会在没有前台服务保护的情况下继续运行.
- `修复` 读取较长转录时保留最新文本, 并控制回复大小, 避免跨进程消息超限.
- `修复` 脚本读取或回放终端输出时去除屏幕填充的尾部空行, 保留提示符空格和后续输出的行边界
- `修复` 会话从准备中进入运行中时, 宿主查询偶尔找不到该会话, 导致可见执行无法打开终端的问题
- `依赖` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作为终端仿真与 pty 原生库, 并在 `locks/vendored-aars.lock` 中锁定哈希
- `依赖` 附加 `common-plugin-api.aar` 与 `nodejs-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api` 与 `plugin-api/nodejs-api`, 宿主构建 6.8.0 / 5303, MPL 2.0) 作为共享插件契约与 Node.js 清单契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 附加 `terminal-api.aar` (AutoJs6 模块 `plugin-api/terminal-api`, 宿主构建 6.8.0 / 5304, MPL 2.0) 作为终端契约 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限与错误码), 插件身份常量改由它提供, 并在 `locks/host-api-aars.lock` 中锁定哈希

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
