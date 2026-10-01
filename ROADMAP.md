# AutoJs6 3-Shell Terminal 插件 Roadmap

本文是 `AutoJs6-Plugin-Three-Shell-Terminal` (显示名 `3-Shell Terminal`; 将 AutoJs6 宿主 6.8.0 开发线中的内置终端能力 (多会话 pty shell, 前台服务, 快捷键栏, 文本选择, 会话管理器, 经 Node.js Runtime 插件提供的 node / npm / corepack 命令与 npm 镜像源设置) 整体迁出为独立插件, 并为脚本提供全局对象 `terminal`) 的可执行状态表.
以 2026-10-01 的宿主本地代码快照 (`AutoJs6 master@bf102da416`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5302`, 6.8.0 尚未正式发布, 终端功能从未进入任何 tag), 平台版本插件 `1.8.3`, 上游 jackpal Android-Terminal-Emulator (`35188f8a8b57989a4a4ec9485e11187b46be26d9`, Apache-2.0) 与 Node.js Runtime 插件 1.5.6 (build 221, `NODE_CLI` 契约 schema 1) 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / ABI), 证据等级见附录 E.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(兄弟)` `D:/idea-projects/AutoJs6-Plugin-NodeJs-Runtime`, `(文档)` 文档 / d.ts / Ace / 离线文档四个关联仓库, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(测试)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 D 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策".
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 同时遵循 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` (Three 系列命名), `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` (独立设置页) 与 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标); 新仓库 AGENTS 的裁剪版即本仓库 `AGENTS.md`; 之后的工程约定以 `AGENTS.md` 为准, 本文件只记录 "改什么" 与证据.
7. 宿主终端代码是迁移对象而不是参考对象: 核心与界面源码以 "复制并改包名" 的方式迁入 (宿主与本插件同为 MPL-2.0), jackpal 三份 AAR 与原生库重建配方以副本形式入库 (Apache-2.0), 不引用兄弟仓库路径 (D13).

---

## 1. 固定决策

以下决策 D1-D11 已由维护者于 2026-10-01 分三轮确认, 后续阶段不再重新讨论 (D2 含一处核实后的修正, 见附录 D 的 Q1); D12-D33 为据此派生的技术决策, D34 为维护者于 2026-10-01 对附录 D 的拍板; 附录 D 全部问题已关闭, 后续视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 仓库 `AutoJs6-Plugin-Three-Shell-Terminal`, 显示名 `3-Shell Terminal` (Three 系列, `ThreeShell` 恰好 10 个字母); 脚本全局对象 `terminal` (别名 `$terminal`); 插件 ID `three-shell-terminal`, engine `terminal`, variant `default`; applicationId 与 Kotlin 包 `io.github.supermonster003.autojs6.plugin.three.shell.terminal` (点分写法, 与 3-Setup Installer / 3-Stove Agent 一致); 机器标识写 `three` / `Three`, 面向人的文本写 `3-Shell Terminal`, 宿主向导回退标题写 `Three Shell Terminal`. 图标源图由维护者提供两张黑白透明 PNG (`E:/tmp/three-shell-terminal-ic-launcher-{light,dark}.png`), 已入库为 `.python/icons/three-shell-ic-launcher-{light,dark}.png` |
| D2 | 宿主 jackpal AAR 保留范围 | 维护者选择 "仅移出 emulatorview"; 核实后 emulatorview 不可移出: term AAR 的 `GenericTermSession extends jackpal.androidterm.emulatorview.TermSession`, 且脚本 `shell` API 的两份实现 (`runtime/api/Shell.java`, `com/stardust/autojs/core/util/Shell.java`) 直接 `import jackpal.androidterm.emulatorview.TermSession`. 因此宿主保留三份 AAR (`:libs:jackpal-androidterm-1_0_70`, `:libs:jackpal-androidterm-emulatorview-1_0_42`, `:libs:jackpal-androidterm-libtermexec-1_0`) 与 `proguard-rules.pro` 的 `-dontwarn jackpal.androidterm.**`, 只删除终端 UI / core / 资源 / Manifest 组件 / 偏好 (第 3.1 节); 脚本 `shell` API 与 INRT 打包模板 (`ApkBuildLibraryCatalog.TERMINAL_EMULATOR`, `RemoteApkLightweightBuilder.DEFAULT_NATIVE_LIBRARIES`) 不动. 宿主 `isMinifyEnabled=false`, emulatorview 的 45 个类继续留在宿主 APK, 体积影响可忽略. 插件自带三份 AAR 副本 (D13) |
| D3 | 集成形态 | 新增宿主契约模块 `plugin-api/terminal-api` (action `org.autojs.plugin.TERMINAL`, category `terminal`), 宿主入口全部保留: 主页抽屉 "终端" 开关 (显示会话数, 有会话时关闭全部, 标题点击新建会话, 管理器入口), 文件管理器目录菜单 "在终端中打开", 项目工具栏终端按钮; 入口统一经 `TerminalLauncher` 五态退化 (D25); 会话数与状态经 Binder 回调 (D26); 界面启动用导出 Activity 而不是 PendingIntent (D19) |
| D4 | Node.js 接入 | 插件直读 Node.js Runtime 插件的 `NODE_CLI_*` manifest 契约 (复用宿主 `nodejs-api` AAR 常量), 自行校验签名信任 (D17), 不依赖宿主插件中心的启用 / 授权状态; 设置页提供 "Node.js 集成" 开关 (默认开); 插件脱离宿主也能工作 |
| D5 | 脚本 API 范围 | 三档全做: 第一档会话管理 (`open` / `sessions` / `session` / `close` / `closeAll` / `show` / `state` / `isAvailable`), 第二档可见执行 (`exec`, `npm.run`, `env`), 第三档会话驱动 (`session.write`, `output` / `exit` / `overflow` 事件, `waitFor`, `transcript`); 全部排在宿主入口迁移 (P1) 与插件主体 (P2 / P3) 之后的独立阶段 P4; 草案见附录 A |
| D6 | 存储权限 | 插件声明 `MANAGE_EXTERNAL_STORAGE` (API 30+) 与 `READ_EXTERNAL_STORAGE` (maxSdk 32) / `WRITE_EXTERNAL_STORAGE` (maxSdk 29); 目标目录位于共享存储且未授权时显示横幅引导到系统设置, 未授权则回退到 `HOME` 并提示; 细节见 D18 |
| D7 | 旧数据 | 不迁移宿主 `<filesDir>/terminal` (home / usr / 已解压 npm CLI / 全局包) 与终端偏好; 宿主升级后在后台一次性清理残留 (D28), changelog hint 说明 |
| D8 | 宿主设置页 | 完全移除宿主设置页 "终端" 分类 (npm 镜像源 / 忽略安装脚本 / 清除终端数据) 与开发者选项的 "Node 环境探测" 项, 对应能力全部融入插件设置页 (D29); 宿主设置页不再保留任何终端条目, 插件设置经插件中心 "设置" 与插件自身到达 |
| D9 | 版本与推送 | `VERSION_NAME` 从 1.0.0 起; 与 3-Setup Installer 一致, 本仓库与宿主改动均仅本地 Conventional Commits, 不推送 GitHub, 不登记官方索引, 不发 Release, 直至维护者明确恢复; 宿主 `PluginInstallWizardCatalog` 条目先落地 (索引可解析后自然生效) |
| D10 | 宿主 6.8.0 changelog | 改写为插件化条目: 删除描述宿主内置终端的 feature / fix / improvement 条目, 修改混合条目去掉终端部分, 新增 hint "终端改由 3-Shell Terminal 插件提供" 与 feature "集成 3-Shell Terminal 插件 (抽屉 / 文件管理器 / 项目工具栏 / 脚本 API `terminal`)", 与既有 hint "部分内置功能改由独立插件提供" 一致; 清单见 P1.4 |
| D11 | 输出流传输 | 第三档的会话输出经 `ParcelFileDescriptor` 管道 (插件 `createPipe`, 返回读端给宿主), 不用 Binder 回调推送字节; 细节见 D21 |
| D12 | 身份派生表 | 见第 4.4 节; 全部值在 Gradle, Manifest, 契约常量, 资源, 文档, 测试, 宿主注册与官方索引中 MUST 一致 |
| D13 | jackpal 入库方式 | 从宿主 `libs/` 复制 `term-debug.aar` (jackpal.androidterm 1.0.70, 只用 `Exec` 的 `setPtyWindowSizeInternal` / `setPtyUTF8ModeInternal` JNI 声明), `emulatorview-release.aar` (1.0.42: `TermSession`, `EmulatorView`, 渲染器) 与 `libtermexec-release.aar` (1.0: `TermExec.createSubprocess` / `waitFor` / `sendSignal` JNI + 4 ABI x 2 个 `.so`, 宿主已用 NDK 28.2.13676358 重建为 16 KB 对齐) 到本仓库 `libs/jackpal/`, 连同宿主 `libs/jackpal-androidterm-libtermexec-1_0/native-build/` (build.py, CMakeLists.txt, provenance.json, upstream.lock.json, LICENSE, NOTICE, README.md) 复制为 `native/jackpal-termexec/`; `locks/vendored-aars.lock` 锁三份 AAR 的 SHA-256 (与宿主 provenance 的 `aarSha256` 比对); 宿主 API AAR (`common-plugin-api`, `nodejs-api`, `terminal-api`) 仍用 `locks/host-api-aars.lock`; Gradle 不引用兄弟仓库路径 |
| D14 | 原生库与 ABI | 4 个 ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, 与 AAR 内容一致, x86 供 API 24 x86 模拟器 CI) + universal, `splits.abi` + `isUniversalApk = true`; `autojs6-native-alignment` 的 `verifyNativePageAlignment` 要求 `PT_LOAD` 对齐 `0x4000`; `getInfo().supportedAbis` 为当前 APK 实际打包的 ABI; 发布文件名 `autojs6-plugin-three-shell-terminal-v{VERSION_NAME}-{abi}-{CRC32}.apk` 共 5 个 |
| D15 | 进程与会话模型 | 会话常驻插件主进程 (`TerminalSessionManager` 进程级注册表, 原样迁入); 前台服务 `ThreeShellTerminalSessionService` (`foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, 通知渠道 `three.shell.terminal.sessions`, 通知显示当前目录与会话数, "关闭全部" 动作; API 24 / 25 用 `startService`, API 26+ 用 `startForegroundService`; API 33+ 首次创建会话时请求 `POST_NOTIFICATIONS`, 拒绝则静默降级, 会话仍运行); Binder 服务 `ThreeShellTerminalPluginService` 与界面同进程, 直接读写注册表 |
| D16 | shell 与目录布局 | 只运行 `/system/bin/sh`; 包装脚本 `cd -- "$1" 2>/dev/null \|\| cd "$HOME"; exec "$0"` 与命令构造 (`sh -c <wrapper> sh <cwd>`) 原样迁入; 目录布局 `<filesDir>/terminal/{home, usr/{bin, etc/profile, tmp, lib/autojs6-node-cli, lib/node_modules, .npm, .corepack}}` 与宿主一致, `TerminalPaths` / `TerminalEnvironment` 及其 JVM 测试原样迁入; 所有进程以插件 uid 运行, 持有插件 (而非宿主) 的 Android 权限 |
| D17 | Node CLI 接入细节 | 迁入 `NodeCliLocator` / `NodeCliInstaller` / `NodeCliArchive` / `NodeCliMetadata` / `NodeCliProbe` / `TerminalNodeSetup` / `TerminalNodeEnvironment`; Manifest `<queries>` 声明包名 `io.github.supermonster003.autojs6.plugin.nodejs` 与 intent `org.autojs.plugin.nodejs.RUNTIME`; 信任规则: Node.js 插件签名 SHA-256 属于官方签名集合 (与宿主 `PluginTrustManager.OFFICIAL_SHA_256` 同值 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`) 或与本插件自身签名一致 (开发构建), 否则 `PluginNotTrusted` (取代宿主的 `PluginNotAuthorized`); 阈值沿用宿主: `NODE_CLI_SCHEMA = 1`, Node.js Runtime 1.5.0+ (`PluginTooOld`); 可执行文件存在性, ELF magic, SHA-256 格式与一次性 `--version` 探测保持不变; 关闭开关或不可用时终端为纯 shell, 横幅给出类型化原因与动作 ("安装 Node.js Runtime" -> 宿主插件中心按包名过滤, 宿主缺失时打开该插件的 GitHub Release 页) |
| D18 | 存储权限细节 | Manifest: `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`), `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=29`), `MANAGE_EXTERNAL_STORAGE` (`tools:ignore="ScopedStorage"`), 注释说明插件 uid 不继承宿主授权 (与 Node.js Runtime 插件同样写法); 目录解析顺序: 可读可执行 -> `cd`; 位于共享存储且未授权 -> 回退 `HOME` + 横幅 (API 30+ `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` 带包名, API < 30 运行时权限请求), 授权后横幅提供 "重新进入目录"; 不存在 / 非目录 -> `HOME` + 提示; 宿主侧不做预检, 只把绝对路径传给插件 |
| D19 | 界面启动协议 | 导出 Activity `ThreeShellTerminalEntryActivity` (`Theme.NoDisplay` 转发器), action `org.autojs.plugin.TERMINAL_OPEN`, 受 `org.autojs.permission.PLUGIN` 签名权限保护, extras `directory` / `sessionId` / `newSession` / `command` (键名在 `TerminalContract`); 宿主显式 Intent 启动 (与官方插件设置契约 V1 的 `OPEN_SETTINGS` 同模式), 不用 PendingIntent (避免 Android 14+ 的 `PendingIntent.send` 后台启动授权语义); 脚本 `terminal.open()` / `show()` 由宿主以同一 Intent 启动, 脚本处于后台时受系统后台启动限制 (与 `app.startActivity` 一致, 文档注明) |
| D20 | Binder 面 | `ITerminalPlugin` (`getInfo`, `getCapabilities`, `listSessions`, `openSession`, `closeSession`, `closeAllSessions`, `writeInput`, `subscribeOutput`, `unsubscribeOutput`, `readTranscript`, `getEnvironment`, `registerCallback`, `unregisterCallback`) + `ITerminalCallback` (oneway: `onSessionsChanged`, `onSessionExited`, `onOutputOverflow`); 草案见附录 B, 顺序由宿主 `TerminalAidlOrderTest` 冻结 |
| D21 | 输出流细节 | `subscribeOutput(sessionId, optionsJson)` 返回 `Bundle` (`subscriptionId`, 只读 `ParcelFileDescriptor`); 插件为每个订阅者起写线程 + 1 MiB 环形缓冲, 缓冲满时丢弃最旧字节并 `onOutputOverflow(sessionId, subscriptionId, droppedBytes)`; 宿主 Binder 死亡或 `unsubscribeOutput` 时关闭写端; 每会话 <= 4 个订阅; 传输原始 pty 字节 (含 ANSI 序列), 宿主侧 `stripAnsi` 选项默认开, 按行切分事件, 行长上限 64 KiB (超长按块) |
| D22 | 契约上限 | 并发会话 <= 16 (宿主现状无上限, 新增), 命令 <= 64 KiB, 单次 `writeInput` <= 64 KiB, 环境覆盖 <= 64 项且每项 <= 4 KiB, 目录路径 <= 4096 字节, JSON 文档 <= 64 KiB, 回调注册者 <= 8, 订阅 <= 4 / 会话, `readTranscript` 单次 <= 1 MiB |
| D23 | 错误码 | 宿主侧 `PLUGIN_UNAVAILABLE` / `PLUGIN_INCOMPATIBLE`; 契约 `INVALID_ARGUMENT`, `SESSION_NOT_FOUND`, `SESSION_LIMIT`, `SESSION_CLOSED`, `DIRECTORY_INACCESSIBLE`, `STORAGE_PERMISSION_REQUIRED`, `PTY_FAILED`, `NODE_UNAVAILABLE`, `SUBSCRIPTION_LIMIT`, `TIMEOUT`, `CANCELLED`, `INTERNAL`; 宿主 `terminal-api` 与脚本 `TerminalError.code` 使用同一字符串 (附录 B.4) |
| D24 | 脚本 API 形态 | 同步 + Async 双形态 (`open` / `openAsync` 等, 参数与结果一致); `TerminalSession` 为 EventEmitter (`output`, `exit`, `overflow`), 另有 `write`, `show`, `close`, `waitFor`, `transcript`, `isAlive`; 错误类型 `TerminalError` (`code`, `message`, `sessionId`); `terminal.npm.run(script, dir?)` 为 `exec` 的便捷层; 草案见附录 A |
| D25 | 宿主退化路由 | 宿主新增 `core/plugin/terminal/TerminalLauncher` 单入口, 全部界面调用方改走它: 可用 -> `TERMINAL_OPEN` 显式 Intent; 未安装 -> `PluginCenterActivity.launch(sourceScope = OFFICIAL, initialQuery = 包名)`; 未启用 / 未授权 -> `PluginSettingsActivity.launch`; 不兼容 -> 对话框 (需要版本 x, 带插件中心按钮); 绑定失败 -> toast + 日志; 抽屉项在不可用时退化为普通项 (点击走同一路由, 副标题显示状态文案), 不显示会话数 |
| D26 | 宿主会话状态监听 | `core/plugin/terminal/TerminalPluginStateMonitor`: 抽屉可见时绑定并 `registerCallback`, 收到 `onSessionsChanged` 刷新开关与会话数; 监听包状态广播 (安装 / 卸载 / 更新 / 启用变化) 重新发现; 抽屉隐藏后由 `AidlPluginHost` 的空闲解绑 (30 s) 释放; 不常驻绑定, 与 `ScreenColorPickerPluginStateMonitor` 同范式 |
| D27 | 宿主删除与保留清单 | 删除第 3.1 节全部终端源码 / 测试 / 布局 / 菜单 / id / 偏好 / Manifest 组件; 保留并复用 `ic_terminal_black_48dp`, `text_terminal`, `description_terminal` (文案改写为插件语义), `text_open_in_terminal`, `text_terminal_new_session`, `text_terminal_running_sessions`; 连接管理器改用新字符串 `text_close_session`, `text_terminal_close_session` 随终端删除; `licenses.xml` 的 "Android Terminal Emulator" 条目保留 (term / libtermexec 仍打包); 其余 50 余条 `*_terminal_*` 字符串随代码迁入插件 (改为插件资源名) |
| D28 | 旧数据清理 | 宿主启动后在后台线程执行幂等清理: `<filesDir>/terminal` 存在则递归删除; `SharedPreferences` 中前缀 `key_$_terminal_` 的全部键与 `key_$_dialog_terminal_tips` 移除; 不设标记位 (`exists()` 与键扫描成本可忽略); changelog hint 注明 "终端数据不迁移, 全局 npm 包需在插件内重新安装" |
| D29 | 插件设置页 | 遵循独立设置页规范: 外观四项 (语言 / 夜间模式 / 主题色 / 启动器图标, 默认跟随 AutoJs6) 在前; 终端组: 字号 (6-40 sp, 默认 12), npm 镜像源 (npmjs / npmmirror / 自定义 URL, 仅 https), npm 忽略安装脚本 (默认关), Node.js 集成开关 (默认开) + 环境探测详情 (迁入 `TerminalNodeProbePreference` 逻辑), 全部文件访问状态行 (跳系统设置), 清除终端数据 (迁入 `TerminalClearDataPreference` 逻辑: 关闭全部会话后删除 home / usr); 关于 / 发行历史 / 更新检查; 设置入口 action `org.autojs.plugin.TERMINAL_SETTINGS` |
| D30 | 启动器形态 | 启动器 alias 直接打开终端 (有会话则恢复最近会话, 否则新建), 不设独立首页; 终端工具栏溢出菜单含 "设置" 与 "关于"; 终端 Activity 使用自有 `taskAffinity` (与宿主现状一致), 从宿主入口进入时 Back 返回宿主, 从启动器进入时 Back 回桌面 |
| D31 | 许可证 | 插件 MPL-2.0 (与宿主及 Three 系列一致); jackpal Android-Terminal-Emulator (Apache-2.0) 的三份 AAR 与原生库重建配方记入 `THIRD_PARTY_NOTICES.md` 与关于页; 宿主 AAR (`common-plugin-api`, `nodejs-api`, `terminal-api`, MPL-2.0) 记录 SHA-256; 从宿主迁入的 MPL-2.0 源码保留原文件头 |
| D32 | 兼容矩阵 | API 24 AVD x86, API 28 Sony G8441 (arm64), API 31 Sony XQ-AT72, API 33 Redmi 22120RN86C, API 35 Xiaomi 23046RP50C (HyperOS), API 37 AVD (16 KB 页); Node.js Runtime 已安装的设备上额外验证 node / npm / corepack; 每台设备记录 ABI 与实际安装的 APK 变体 |
| D33 | Explorer Action v2 | 1.0.0 不使用, 文件管理器 "在终端中打开" 仍由宿主硬编码并经 `TerminalLauncher` (D25); 契约已有 `ExplorerActionValues.TARGET_DIRECTORY`, 1.1.0 改为插件声明的目录动作以去掉宿主硬编码 (附录 D Q6) |
| D34 | 附录 D 拍板 (2026-10-01) | Q1 接受三份 AAR 全部保留, 放弃缩减收益; Q2 打包 x86; Q3 1.0.0 只信任官方 / 自身签名, 1.1.0 增加 "信任此签名" 确认流程 (P8); Q4 `command` 按推荐方式嵌入包装脚本; Q5 `terminal.show()` 行为与 `app.startActivity` 一致; Q6 1.1.0 迁移到 Explorer Action v2 目录动作; Q7 维护者接受短期无终端, P1.3 随 P1.2 一并执行, 不等待 P3 |

---

## 2. 范围与非目标

范围 (1.0.0):

- 终端会话: 多会话 pty shell (`/system/bin/sh`), 会话管理器, 后台运行 (前台服务 + 通知), 快捷键栏 (Esc / Tab / Ctrl / 方向 / 符号两行), 原生文本选择与复制 / 全选, 转录复制 / 分享, 字号设置, 粘贴 / 清屏 (D15, D16).
- Node.js 工具链: 经 Node.js Runtime 插件的 `NODE_CLI` 契约提供 node / npm / npx / corepack / yarn / pnpm, npm / corepack 归档校验与一次性解压, 每次会话刷新 `usr/bin` 符号链接, npm 镜像源 / 忽略安装脚本设置, 包管理菜单 (npm init / install / 安装包 / 运行脚本 / 其它包管理器 / 搜索), 环境探测 (D4, D17).
- 存储: 全部文件访问权限与引导, 共享存储目录的进入与回退 (D6, D18).
- 宿主集成: `terminal-api` 契约, 宿主客户端与五态退化, 抽屉 / 文件管理器 / 项目工具栏入口, 会话状态监听, 旧终端删除与数据清理, 插件中心注册 (D3, D25-D28).
- 脚本 API `terminal` 三档 (D5, D24), 文档 / d.ts / Ace / 离线文档.
- 独立应用形态: 设置页, 关于 / 发行历史 / 更新检查, 四 alias 启动器图标, 启动器直接进入终端 (D29, D30).

非目标 (1.0.0, 列入 P8 或不做):

- 其它 shell (bash / zsh / busybox), 包管理器 (apt / pkg), SSH 客户端, 多标签页 UI, 配色方案与字体选择: P8 评估, 1.0.0 不做.
- 会话跨进程死亡持久化 (pty 随进程消亡, 无法恢复): 不做, 文档注明.
- 以宿主 uid 运行 shell 或共享宿主权限: 不做 (D16); 需要宿主权限的命令改用脚本 `shell()` API.
- 替换宿主脚本 `shell()` API 或改动 INRT 原生库清单: 不做 (D2).
- Explorer Action v2 目录动作: P8 (D33).
- 从宿主迁移终端数据或偏好: 不做 (D7).

---

## 3. 现状诊断

### 3.1 宿主终端现状

核心 (`app/src/main/java/org/autojs/autojs/core/terminal/`):

| 文件 | 行数 | 角色 | 迁出后 |
| --- | --- | --- | --- |
| `TerminalPtySession.kt` | 256 | `TermSession(false)` 子类: `/dev/ptmx` 打开, `PtyBridge.createSubprocess`, 窗口尺寸, UTF-8 模式, 退出等待, 信号 | 迁入插件 `core/` |
| `TerminalSessionManager.kt` | 96 | 进程级会话注册表 (`Session(id, pty, initialDirectory, paths, createdAt)`), `create` / `close` / `closeAll` / 监听器 | 迁入 (Binder 服务的数据源) |
| `TerminalSessionService.kt` | 173 | 前台服务 `specialUse`, 渠道 `autojs6.terminal.sessions`, `ACTION_CLOSE_ALL`, API 26 渠道创建, API 34 类型 | 迁入并改名 `ThreeShellTerminalSessionService` (D15) |
| `TerminalSessionLauncher.kt` | 30 | `WRAPPER_SCRIPT` 与 `buildCommand(cwd, shell)` | 迁入 (D16) |
| `TerminalEnvironment.kt` | 97 | `Spec` -> 环境变量表 (`HOME`, `PREFIX`, `TMPDIR`, `PATH`, `TERM`, `LANG` 等) | 迁入 |
| `TerminalPaths.kt` | 97 | `<filesDir>/terminal` 布局与 `ensureLayout` | 迁入 (D16) |
| `TerminalPreferences.kt` | 43 | 字号 (6-40, 默认 12), npm 镜像源 (npmjs / npmmirror / 自定义), 忽略安装脚本, `nodeEnvironmentOptions()` | 迁入并改为插件 `SharedPreferences` (D29) |
| `TerminalKeySequences.kt` | 95 | 快捷键栏的 ANSI 序列 | 迁入 |
| `TerminalNodeSetup.kt` / `TerminalNodeEnvironment.kt` | 63 / 68 | 会话启动时的 Node 环境装配, `AUTOJS6_NODE_CLI_ROOT`, `npm_config_*`, `COREPACK_*`, `NPMMIRROR_REGISTRY` | 迁入 (D17) |
| `NodeCliLocator.kt` / `NodeCliInstaller.kt` / `NodeCliArchive.kt` / `NodeCliMetadata.kt` / `NodeCliProbe.kt` | 153 / 205 / 125 / 135 / 121 | 发现 Node.js 插件, 解析 `NODE_CLI_*` meta-data, 校验并解压归档, 创建符号链接, 一次性 `--version` 探测; `Resolution.Available` / `Unavailable.{PluginMissing, PluginNotAuthorized, PluginTooOld, ExecutableMissing, ExecDenied, SetupFailed}` | 迁入; `PluginNotAuthorized` 改为签名信任判定 `PluginNotTrusted` (D17) |
| `NpmProjectScripts.kt` / `ShellQuoting.kt` | 44 / 26 | `package.json` scripts 读取, POSIX 引号 | 迁入 |

界面 (`app/src/main/java/org/autojs/autojs/ui/terminal/`):

| 文件 | 行数 | 角色 | 迁出后 |
| --- | --- | --- | --- |
| `TerminalActivity.kt` | 641 | 全屏终端: extras `new_session` / `session_id` / `directory`, `launch` / `launchNew` / `launchSession(id, preserveManager)`, 菜单 (会话 / 文本 / 包管理 / 设置 / 帮助 / 显示键盘), Node 横幅, 探测详情, 字号对话框, 提示 | 迁入并改名 `TerminalActivity` (插件包内), 新增设置 / 关于菜单项 (D30) |
| `TerminalEmulatorView.kt` | 91 | `EmulatorView` 子类, `attachSession`, 字号, 选择回调 | 迁入 |
| `TerminalToolbarView.kt` | 136 | 两行快捷键栏 | 迁入 |
| `TerminalTextSelection.kt` | 105 | 原生选择手柄, 复制 / 全选 | 迁入 |
| `TerminalManagerDialog.kt` | 138 | 会话管理器 (分组展开 / 收起, 新建 / 打开 / 关闭 / 关闭全部, 复制目录) | 迁入; 宿主抽屉的管理器入口改为打开插件管理器 (`TERMINAL_OPEN` + `manager=true`) |
| `TerminalNpmActions.kt` / `TerminalSettingsDialogs.kt` | 109 / 50 | 包管理菜单动作, 镜像源 / 字号对话框 | 迁入 |

设置与桥接:

| 文件 | 行数 | 角色 | 迁出后 |
| --- | --- | --- | --- |
| `ui/settings/TerminalNpmRegistryPreference.kt` / `TerminalClearDataPreference.kt` / `TerminalNodeProbePreference.kt` | 80 / 47 / 110 | 宿主设置页三项与开发者选项一项 | 删除; 逻辑迁入插件设置页 (D8, D29) |
| `jackpal/androidterm/PtyBridge.java` | 63 | 同包访问 `TermExec.createSubprocess` / `waitFor` / `sendSignal` (libtermexec) 与 `Exec.setPtyWindowSizeInternal` / `setPtyUTF8ModeInternal` (term) | 迁入 (保持 `jackpal.androidterm` 包名以访问包级 API) |
| `jackpal/androidterm/emulatorview/TerminalSelectionSnapshot.java` | 45 | 同包读取转录快照 | 迁入 (同上) |

资源与清单:

- 布局 `res/layout/activity_terminal.xml` (69), `include_terminal_node_banner.xml` (46); 菜单 `res/menu/menu_terminal.xml` (49); `res/values/ids_terminal.xml` (4); 图标 `res/drawable/ic_terminal_black_48dp.xml` (保留, 抽屉项仍用).
- 字符串 `res/values/strings.xml` 62 条 x 10 语言 (`text_terminal*`, `description_terminal`, `entry_terminal_npm_registry_*`, `hint_terminal_npm_registry_custom_url`, `summary_terminal_*`, `text_open_in_terminal`); `strings_donottranslate.xml` 偏好键 `key_terminal_clear_data`, `key_terminal_manager_control_level` / `_sessions_level` / `_settings_collapsed` / `_status_level`, `key_terminal_node_probe`, `key_terminal_npm_ignore_scripts`, `key_terminal_npm_registry` (+ `_custom`, `_custom_url`, `_npmjs`, `_npmmirror`), `key_terminal_text_size`, `key_dialog_terminal_tips`, `default_key_terminal_npm_registry`; `arrays.xml` `keys_terminal_npm_registry` / `values_terminal_npm_registry`.
- `res/xml/fragment_preferences.xml:186-211` 终端分类 (三项), `res/xml/fragment_developer_options.xml:97-101` Node 探测项.
- `res/menu/menu_dir_options.xml` `action_open_in_terminal`; `res/layout/explorer_project_toolbar.xml` `project_terminal`; `res/layout/connection_manager_client_item.xml` 复用 `text_terminal_close_session`.
- Manifest: `TerminalActivity` (L543-, `exported=false`, 自有 `taskAffinity`, `AppTheme.FullScreen`, `adjustResize|stateVisible`), `TerminalSessionService` (L607-, `specialUse` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).
- Gradle: `app/build.gradle.kts:586-588` 三份 AAR 依赖 (保留, D2), `settings.gradle.kts:81-83` (保留), `proguard-rules.pro:45` (保留).
- `res/raw/licenses.xml:81-84` "Android Terminal Emulator" (保留).

测试: JVM `app/src/test/.../core/terminal/` 10 个文件 857 行 (`NodeCliInstallerTest`, `NodeCliMetadataTest`, `NodeCliProbeTest`, `NpmProjectScriptsTest`, `ShellQuotingTest`, `TerminalEnvironmentTest`, `TerminalKeySequencesTest`, `TerminalNodeEnvironmentTest`, `TerminalPathsTest`, `TerminalSessionLauncherTest`), instrumentation `TerminalSessionsInstrumentationTest.kt` 211 行 (切换会话保留 shell 状态与未提交输入, 选择快照跨新输出, 启动期间关闭不留活 shell, 管理器跨 Activity 存活) -> 全部迁入插件, 断言改为插件包名与资源.

合计 4678 行 (源码 3442 + 测试 1068 + 布局 / 菜单 168).

### 3.2 宿主入口与调用方

| 位置 | 现状 | 迁出后 |
| --- | --- | --- |
| `ui/main/drawer/DrawerFragment.kt:726-757, 936` | `mTerminalItem` (`DrawerMenuToggleableItem`): `isActive = TerminalSessionManager.hasSessions`, `toggle` 关闭全部或 `TerminalActivity.launchNew`, 副标题 `text_terminal_running_sessions`, 标题点击新建会话, 管理器入口 `TerminalManagerDialog.show`, `TerminalSessionManager.addListener` | 改为 `TerminalLauncher` + `TerminalPluginStateMonitor` (D25, D26); 管理器入口打开插件管理器 |
| `ui/explorer/ExplorerPageViewHolder.kt:162, 238-240` | 非目录 / 归档页移除菜单项; `action_open_in_terminal -> TerminalActivity.launch(host.context, selectedItem.path)` | `TerminalLauncher.open(context, path)` |
| `ui/explorer/ExplorerProjectToolbar.java:26, 85-92, 187-194` | `openInTerminal()` -> `TerminalActivity.launch(getContext(), directory.getPath())` | `TerminalLauncher.open` |
| `ui/devplugin/ConnectionManagerDialog.kt:273`, `res/layout/connection_manager_client_item.xml` | 复用 `text_terminal_close_session` | 改用 `text_close_session` (D27) |
| `res/xml/fragment_preferences.xml:186-211`, `fragment_developer_options.xml:97-101` | 终端分类与探测项 | 删除 (D8) |
| `engine/NodeBridgeProtocol.kt:1587, 1673` | `ui.terminal_close` 是 Node Bridge 的 UI 事件名, 与终端无关 | 不动 |

### 3.3 宿主 jackpal 依赖事实 (D2 的依据)

- `libs/jackpal-androidterm-1_0_70/term-debug.aar`: `jackpal.androidterm.{Exec, GenericTermSession, ShellTermSession, TermService, Term, ...}`, `GenericTermSession extends emulatorview.TermSession`; `Exec` 的 JNI 在 `libjackpal-androidterm5.so`.
- `libs/jackpal-androidterm-emulatorview-1_0_42/emulatorview-release.aar`: 45 个类 (`TermSession`, `EmulatorView`, `TerminalEmulator`, `TranscriptScreen`, 渲染器, `TermKeyListener`), 纯 Java.
- `libs/jackpal-androidterm-libtermexec-1_0/libtermexec-release.aar`: `jackpal.androidterm.libtermexec.v1.TermExec` + `jni/{arm64-v8a, armeabi-v7a, x86, x86_64}/libjackpal-{androidterm5, termexec2}.so`; `native-build/provenance.json` 记录 commit `35188f8a...`, NDK `28.2.13676358`, platform 24, 每个 `.so` 的 SHA-256 与 `minLoadAlign 16384`.
- 非终端消费者: `runtime/api/Shell.java` 与 `com/stardust/autojs/core/util/Shell.java` (`ShellTermSession`, `TermSettings`, `TermSession`), `apkbuilder/ApkBuildLibraryCatalog.kt:40-47` (`TERMINAL_EMULATOR` 两个 `.so`), `plugins/apk-builder-template/.../RemoteApkLightweightBuilder.kt:651-654` (`DEFAULT_NATIVE_LIBRARIES`).
- 结论: 三份 AAR 都被脚本 `shell()` 链路使用, 宿主不能删除任何一份; 插件需要三份 (D13).

### 3.4 Node.js Runtime 插件契约事实 (2026-10-01 核实)

- `docs/nodejs/TERMINAL.md` (last reviewed 2026-09-14): schema 1 的 `NODE_CLI_SCHEMA` / `NODE_CLI_EXECUTABLE` (`libnodexe.so`) / `NODE_CLI_COMMANDS` (`node,npm,npx,corepack,yarn,yarnpkg,pnpm,pnpx`) / `NODE_CLI_ARCHIVE` (asset 路径) / `NODE_CLI_ARCHIVE_SHA256` / `NODE_CLI_ARCHIVE_ROOT` (`lib/node_modules`) / `NODE_CLI_ARCHIVE_ENTRY_COUNT` / `NODE_CLI_ARCHIVE_BYTES` / `NODE_CLI_NPM_VERSION` / `NODE_CLI_COREPACK_VERSION` 作为 `NodeJsRuntimePluginService` 的 `<meta-data>`; 常量在宿主 `plugin-api/nodejs-api` (`NodeJsPluginCapabilityKeys.NODE_CLI_*`, 前缀 `org.autojs.plugin.nodejs.`, `NodeJsPluginActions.RUNTIME`, `NodeJsRuntimeContract.NODE_CLI_SCHEMA_VERSION = "1"`).
- 启动器按 `basename(argv[0])` 分发, `process.execPath` 为启动器自身; 归档为确定性 zip (`.bin`), `tools/nodejs/cli/node-cli.lock.json` 记录来源与摘要.
- 限制: W^X (插件写出的文件不可执行, `npm_config_bin_links=false`), 原生 addon 不可加载, corepack 默认 `COREPACK_DEFAULT_TO_LATEST=0`, 1.5.1+ small-icu 与 stderr 直通.
- 文档中 "宿主 uid" 的描述在迁出后变为 "3-Shell Terminal 插件 uid", 需同步 (P7.1 的 `(兄弟)` 条目).
- 该插件 Manifest 已声明 `MANAGE_EXTERNAL_STORAGE` 与 `READ/WRITE_EXTERNAL_STORAGE` (带 maxSdk), 注释说明插件不继承宿主授权: 本插件沿用同一写法 (D18).

### 3.5 兄弟仓库可复用事实

- 3-Setup Installer (`D:/idea-projects/AutoJs6-Plugin-Three-Setup-Installer`, 36 次本地提交, 未推送): 最新的 Three 系列骨架 (平台插件 1.8.3, `build-logic` 6 个约定插件, `locks/host-api-aars.lock` 配置期校验, `resValue` 五键, `appendDigestToReleasedFiles`, CI API 24 x86 + API 35 x86_64 契约测试, `ROADMAP.md` / `AGENTS.md` 结构); 本仓库的 `AGENTS.md` 与路线图格式以它为模板.
- 3-Stove Agent: 四 alias 启动器图标与 `generate_launcher_icons.py`, 代码构建的设置 / 关于 / 发行历史 / 宿主外观跟随 (`ui/{AboutActivity, AppearancePreferences, HostAppearanceActivity, LauncherActivity, LauncherIcons, DocumentText, LegacyInputTintContext}.kt`).
- Node.js Runtime: ABI 拆分 + universal 的 `appendDigestToReleasedFiles` (5 个产物, `supportedAbis` 由实际打包 ABI 推导), `verifyNativePageAlignment`, 存储权限写法.
- 宿主范式: `core/plugin/screencolorpicker/{ScreenColorPickerPluginHost, ScreenColorPickerPluginStateMonitor}.kt` (Binder 控制 + 包状态监听驱动抽屉项), `ui/settings/McpServerSettingsLauncher.kt` (五态退化到插件中心 / 插件设置页 / 导出 Activity), `core/plugin/epub/EpubPluginHost.kt` + `runtime/api/epub/EpubService.kt` + `runtime/api/augment/epub/Epub.kt` (`AidlPluginHost` 泛型客户端, 每脚本 `Closeable` 服务层, augment 全局对象与 Async 双形态, 在 `ScriptRuntime.kt:498-499, 825-826, 1056-1057` 装配), `plugin-api/{epub-api, installer-api, screen-color-picker-api}` 契约模块布局 (AIDL + `*Actions` / `*Ids` / `*Contract` / `*CapabilityKeys` / `*ErrorCodes` + `*AidlOrderTest` / `*ContractTest`), `docs/dev/{official-plugin-settings-contract-v1, installer-plugin-protocol-v1, readium-epub-reader-plugin-integration}.md` 文档体例.
- 宿主插件中心注册点: `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` (L1044-1067), `InstalledPluginRepository` 发现分派, Manifest `<queries>` (L37-100), `PluginInstallWizardCatalog` (`entry(official("three.setup.installer"), "Three Setup Installer", TOOLS)` 为样板), `PluginInstalledIconResolver.transparentIconPackages` (L16-21, Three 系列透明图标 allowlist), `PluginSettingsFragment` (插件设置入口).
- 官方索引: `official-repositories.json` 45 个仓库按字母序; `release-manifests/<applicationId>/<versionCode>.json` (schemaVersion 1, `signerSha256`, 多 ABI `artifacts` 数组, Node.js Runtime 220.json 为多产物样板).

### 3.6 缺口

- 没有 `terminal-api` 契约, 宿主客户端, 退化路由与状态监听; 抽屉 / 文件管理器 / 项目工具栏直接依赖 `TerminalActivity` 与 `TerminalSessionManager`.
- 没有脚本 API `terminal`, 没有文档 / d.ts 页面.
- Node CLI 的信任判定依赖宿主插件中心的授权状态 (`PluginNotAuthorized`), 插件进程无法读取, 需改为签名信任 (D17).
- 终端进程以宿主 uid 运行并继承宿主存储授权; 迁出后需要插件自己的存储权限与引导 (D18).
- 宿主 6.8.0 changelog (10 语言) 与 README 已含 8 条宿主内置终端的描述 (D10).

---

## 4. 目标架构

### 4.1 数据流

```text
宿主入口 (抽屉 / 文件管理器目录菜单 / 项目工具栏)
  -> 宿主 TerminalLauncher (五态: 可用 / 未安装 / 未启用 / 不兼容 / 失败)
       |- 可用:   显式 Intent org.autojs.plugin.TERMINAL_OPEN (directory / sessionId / newSession / manager)
       |          -> 插件 ThreeShellTerminalEntryActivity -> TerminalActivity (同 task, 自有 taskAffinity)
       `- 不可用: 插件中心 (按包名过滤) / 插件设置页 / 不兼容对话框 / toast

宿主抽屉开关与会话数
  -> 宿主 TerminalPluginStateMonitor (AidlPluginHost<ITerminalPlugin>, registerCallback, 包状态广播)
  <- 插件 ThreeShellTerminalPluginService.onSessionsChanged / onSessionExited
  -> 关闭全部: ITerminalPlugin.closeAllSessions

脚本 terminal.open(dir) / exec(cmd) / session.write / session.on('output')
  -> 宿主 TerminalService (每脚本, Closeable, 参数规范化, 订阅读线程)
  -> 宿主 TerminalPluginHost (信任 / 版本 / 能力协商)
  -> 插件 ThreeShellTerminalPluginService (ITerminalPlugin.Stub, HostCallerGuard, 上限校验)
  -> 插件 TerminalSessionManager (进程级注册表) -> TerminalPtySession (/dev/ptmx + TermExec) -> /system/bin/sh
       |- 前台服务 ThreeShellTerminalSessionService (通知: 目录 / 会话数 / 关闭全部)
       |- Node CLI: NodeCliLocator (queries + 签名信任) -> NodeCliInstaller (归档解压 + usr/bin 符号链接) -> 环境变量
       `- 输出: subscribeOutput -> ParcelFileDescriptor 管道 (1 MiB 环形缓冲, 溢出回调)
  -> 宿主 TerminalSession (EventEmitter: output / exit / overflow), 同步等待 / Promise

插件启动器 alias -> LauncherActivity -> TerminalActivity (恢复最近会话或新建)
插件中心 "设置" -> org.autojs.plugin.TERMINAL_SETTINGS -> SettingsActivity
```

### 4.2 插件包结构 (`app/src/main/java/io/github/supermonster003/autojs6/plugin/three/shell/terminal/`)

```text
ThreeShellTerminalPlugin.kt             身份常量 (引用 terminal-api 的 TerminalIds / TerminalActions)
ThreeShellTerminalPluginInfoService.kt  IPluginInfoProvider
ThreeShellTerminalPluginService.kt      ITerminalPlugin.Stub (Binder 路由, HostCallerGuard, 上限)
ThreeShellTerminalEntryActivity.kt      org.autojs.plugin.TERMINAL_OPEN 转发器 (D19)
WakeActivity.kt
core/     TerminalPtySession, TerminalSessionManager, TerminalSessionLauncher, TerminalEnvironment, TerminalPaths,
          TerminalPreferences, TerminalKeySequences, ShellQuoting, NpmProjectScripts (自宿主迁入)
service/  ThreeShellTerminalSessionService (前台服务, 通知), SessionNotifications
node/     NodeCliLocator, NodeCliInstaller, NodeCliArchive, NodeCliMetadata, NodeCliProbe, NodeCliTrust (签名规则 D17),
          TerminalNodeSetup, TerminalNodeEnvironment
storage/  StorageAccess (权限状态, 目录解析与回退 D18)
binder/   HostCallerGuard, TerminalBinder (Bundle / JSON 编解码, 上限), OutputSubscription (管道 + 环形缓冲 D21),
          CallbackRegistry
ui/       TerminalActivity, TerminalEmulatorView, TerminalToolbarView, TerminalTextSelection, TerminalManagerDialog,
          TerminalNpmActions, TerminalSettingsDialogs, NodeBanner, StorageBanner, LauncherActivity
ui/settings/ SettingsActivity, NodeProbeActivity, AboutActivity, ReleaseHistoryActivity, AppearancePreferences,
          HostAppearanceActivity, LauncherIcons
jackpal/androidterm/PtyBridge.java, jackpal/androidterm/emulatorview/TerminalSelectionSnapshot.java (包名不变)
```

### 4.3 宿主包结构

```text
plugin-api/terminal-api/                AIDL: ITerminalPlugin, ITerminalCallback
                                        常量: TerminalActions, TerminalIds, TerminalContract, TerminalCapabilityKeys, TerminalErrorCodes
core/plugin/terminal/                   TerminalPluginHost, TerminalLauncher (D25), TerminalPluginStateMonitor (D26),
                                        TerminalJson, TerminalErrorMapper, ThreeShellTerminalOfficialPlugin, TerminalLegacyCleanup (D28)
runtime/api/terminal/                   TerminalService, TerminalScriptArguments, TerminalOutputReader (管道读线程)
runtime/api/augment/terminal/           Terminal.kt (AugmentableKey("terminal")), TerminalSessionNativeObject, TerminalJsErrors, TerminalPromises
```

### 4.4 身份派生表 (D12)

| 占位符 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Shell-Terminal` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-shell-terminal` |
| `{APP_NAME}` | `3-Shell Terminal` |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.shell.terminal` |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-shell-terminal` / `terminal` / `default` |
| `{PLUGIN_SERVICE}` | `ThreeShellTerminalPluginService` (主进程) |
| INFO 服务 | `ThreeShellTerminalPluginInfoService`, action `org.autojs.plugin.INFO`, category `terminal` |
| `{CAPABILITY_API}` | `terminal-api` (宿主 `plugin-api/terminal-api`, AIDL 包 `org.autojs.plugin.terminal.api`) |
| `{SERVICE_ACTION}` / `{SERVICE_CATEGORY}` | `org.autojs.plugin.TERMINAL` / `terminal` |
| 界面入口 action | `org.autojs.plugin.TERMINAL_OPEN` (D19) |
| 设置入口 action | `org.autojs.plugin.TERMINAL_SETTINGS` (官方插件设置契约 V1) |
| 前台服务 / 通知渠道 | `ThreeShellTerminalSessionService` / `three.shell.terminal.sessions` |
| 类前缀 / 主题 | `ThreeShellTerminal*` / `Theme.ThreeShellTerminal` |
| `{REQUIRES_HOST_VERSION}` | P1.4 回填 (交付 `terminal-api` 的宿主构建, >= 5303) |
| `{PLATFORM_VERSIONS_PLUGIN_VERSION}` | `1.8.3` |
| 原生库 / ABI | `libjackpal-androidterm5.so` + `libjackpal-termexec2.so`; `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` + universal (D14) |
| 发布文件名 | `autojs6-plugin-three-shell-terminal-v{VERSION_NAME}-{abi}-{CRC32}.apk` (5 个) |
| README `icon_alt` | `autojs6-plugin-three-shell-terminal-ic-launcher` |
| CI artifact | `three-shell-terminal-debug-apks` |
| 图标源图 | `.python/icons/three-shell-ic-launcher-light.png` / `-dark.png` (1254 x 1254 RGBA, alpha 包围盒 `(254, 300, 1000, 953)` 即 746 x 653, 高 / 宽 0.875, 图案 `#272727` / `#D8D8D8`, 两图 alpha 一致); `ADAPTIVE_GLYPH = 0.45` (几何上限 `66 / (108 * sqrt(1 + 0.875^2)) = 0.4599`, 取 0.45 使半对角线 32.3 dp < 33 dp), `OPTICAL_X` / `OPTICAL_Y` 初值 0 并在 P5.4 真机复核 |
| 宿主向导条目 | `entry(official("three.shell.terminal"), "Three Shell Terminal", TOOLS)` |
| 宿主透明图标 allowlist | `PluginInstalledIconResolver.transparentIconPackages` 加入本包名 |
| 官方索引 | `official-repositories.json` 插入 `AutoJs6-Plugin-Three-Shell-Terminal` (D9 解锁后) |

---

## 5. 阶段总览

| 阶段 | 目标 | 主要落点 | 前置 |
| --- | --- | --- | --- |
| P0 | 仓库骨架 (三份 AAR 入库与锁, 4 ABI + universal, 16 KB 校验, 图标, 文案源, CI) + pty spike (插件 uid 下的 shell / 共享存储 / Node 启动器) | 插件 | 无 |
| P1 | `terminal-api` 契约, 宿主客户端 / 路由 / 状态监听, 入口改造与插件中心注册, 删除宿主终端与数据清理, changelog 改写, 协议文档 | 宿主 | P0 骨架 (可并行) |
| P2 | 插件核心: 会话 / pty / 环境 / 前台服务, 存储权限, Binder 路由与上限, Node CLI 接入与信任, 包管理 | 插件 | P0 spike; P1.1 契约 |
| P3 | 插件界面: 终端 Activity, 快捷键栏 / 选择 / 菜单, 会话管理器, 横幅, 入口 Activity 与启动器 | 插件 | P2 |
| P4 | 脚本 API `terminal` 三档 (同步 + Async + 会话 EventEmitter, 输出管道, TerminalError, 示例) | 宿主 | P1, P2 |
| P5 | 独立应用形态: 设置页, 关于 / 发行历史 / 更新检查, 启动器图标 | 插件 | P2, P3 |
| P6 | 健壮性, 兼容矩阵, 性能, 体积 | 全部 | P3, P4, P5 |
| P7 | 文档, d.ts, Ace, 离线文档, README, changelog, 1.0.0 本地 gate (推送 / 索引 / Release 按 D9 门控) | 文档 + 发布 | P6 |
| P8 | 1.1.0 候选: Explorer Action v2 目录动作, 配色 / 字体, 命令完成通知, 多标签页评估 | 插件 (+ 宿主小) | P7 |

建议会话切分: P0 一次 (骨架 + spike); P1 两次 (契约 + 客户端 + 入口改造 + 注册为一次; 删除旧终端 + 数据清理 + changelog + 文档为一次, 维护者已接受短期无终端 (D34 Q7), 两次可连续执行); P2 两到三次 (会话 / 服务 / 存储; Binder 与上限; Node CLI 与包管理); P3 一到两次; P4 两次 (第一 / 二档 + 错误; 第三档会话对象 + 输出管道 + 示例); P5 一到两次; P6 一到两次; P7 一次; P8 按需.

当前进度 (2026-10-01): 调研与决策完成, 本 Roadmap, `AGENTS.md` 与图标源图入库; 未写 Gradle / 源码 (P0.1), 未改宿主.

---

## P0: 仓库骨架与 pty spike

目标: 让 `AutoJs6-Plugin-Three-Shell-Terminal` 成为可构建, 可安装, 能被宿主插件中心发现的最小 APK (含原生库与 4 ABI 拆分), 并在阶段末用真机证明三件事: 插件 uid 下 `/system/bin/sh` 在 pty 中运行并可交互; 授予全部文件访问后可 `cd` 进 `/sdcard` 下的脚本目录; 插件 uid 可直接执行 Node.js Runtime 插件的 `libnodexe.so --version`.

### P0.1 仓库骨架

- [x] (插件) 按第 4.4 节身份派生表确定全部标识并全仓库一致; 确认平台版本插件 `1.8.3` 可从公共仓库解析 (否则暂用已验证版本并记录); `{REQUIRES_HOST_VERSION}` 暂取 5302 (骨架所对宿主构建), P1.4 回填. (SOURCE 2026-10-01: 平台插件 `io.github.supermonster003.autojs6-platform-versions` 与 `autojs6-native-alignment` 1.8.3 均从 Gradle Plugin Portal 解析; 骨架所对宿主为 9545a7f4aa / 5303, `REQUIRED_HOST_VERSION = 5303L` 与两处 `requiresHostVersion` / `common.json` 一致, `ThreeShellTerminalPluginRuntimeInfoTest` 校验)
- [x] (插件) 以 3-Setup Installer (最新 Three 骨架, `build-logic`, AAR 锁, CI) 与 Node.js Runtime (ABI 拆分 + universal, `verifyNativePageAlignment`, 五产物 `appendDigestToReleasedFiles`) 为模板生成: `settings.gradle.kts` (平台插件与 `autojs6-native-alignment` 位于 `includeBuild("build-logic")` 之前, 无 `mavenLocal()`, `include(":app")`), 根 `build.gradle.kts`, `build-logic/`, `app/build.gradle.kts` (`resValue` 五键, `buildFeatures { aidl = true; resValues = true }`, `hostApiIds` 含 `common-plugin-api` 与 `nodejs-api`, P1.1 后追加 `terminal-api`; `splits.abi` 四 ABI + universal; `nativeAlignment` 期望两库 16 KB; `appendDigestToReleasedFiles` 校验 5 个产物), `version.properties` (`VERSION_NAME=1.0.0`, SDK 37 / 37 / 24, `OVERRIDDEN_*=NONE`), `gradle.properties`, wrapper; 依赖: appcompat, material, core-ktx, gson; 无 Compose. (SOURCE 2026-10-01: `settings.gradle.kts` / `build-logic/` (13 个约定插件源) / `app/build.gradle.kts` 落地, `nativeAbis` 四 ABI + universal, `lockedAars()` 通用锁校验, `appendDigestToReleasedFiles` 校验 5 个签名产物 (ApkVerifier + 原生库精确集合 + ELF 魔数) 写入 `releases/`; `bundleLegalAssets` 同时打包 jackpal LICENSE / NOTICE; core-ktx 未显式依赖 (随 appcompat 传递, lint `UseKtx` 已禁用); `:app:assembleDebug` / `:app:assembleRelease` / `:app:appendDigestToReleasedFiles` 通过, release 产物 ~1.0 MiB x4 + universal 1.07 MiB)
- [x] (插件) jackpal 入库 (D13): 复制宿主三份 AAR 到 `libs/jackpal/{term-1_0_70.aar, emulatorview-1_0_42.aar, libtermexec-1_0.aar}`, 复制 `native-build/` 为 `native/jackpal-termexec/`, 新建 `locks/vendored-aars.lock` (SHA-256 与宿主 `provenance.json` 的 `aarSha256` 一致), `app/build.gradle.kts` 配置期校验缺失 / 摘要不符即失败; `libs/README.md` 说明来源 (宿主路径, 上游 commit, NDK 版本) 与重建命令. (SOURCE 2026-10-01: `libs/jackpal/` 三份 AAR 与 `native/jackpal-termexec/` (build.py 路径改为仓库相对) 入库; `locks/vendored-aars.lock` 三条 SHA-256, libtermexec `46c6aea8...` 与 `provenance.json` `aarSha256` 一致; term 仅有 `term-debug.aar` 发行, 锁校验对该组关闭 debug 名称检查; `VendoredAarLockTest` 5 项校验锁 / 文件 / provenance / jni 条目 / 第三方声明互相一致; vendored AAR 声明 minSdk 4 / targetSdk 22, Manifest 以 `tools:overrideLibrary` 覆盖)
- [x] (插件) 从宿主 / 3-Setup Installer 复制 `.gitignore` (保留 `AGENTS.md` 跟踪, 忽略 `*.apk` / `*.jks` / `sign.properties` / `local.properties` / `releases/`), `sign.properties`, `app/sm003.jks` 到相同相对路径, `git check-ignore` 确认后两者不入库. (SOURCE 2026-10-01: `git check-ignore -v` -> `.gitignore:44 /sign.properties`, `.gitignore:5 *.jks app/sm003.jks`; `releases/` 与 `app/build/` 同样被忽略)
- [x] (插件) Manifest 骨架: `org.autojs.permission.PLUGIN`; 存储三项 (D18) 与注释; `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`; `POST_NOTIFICATIONS`; `INTERNET` (更新检查; npm 本身走 shell 子进程, 同 uid 同样需要此权限, 在 `AGENTS.md` 权限表注明); `<queries>`: `org.autojs.autojs6`, Node.js 插件包名 + `org.autojs.plugin.nodejs.RUNTIME` intent; `WAKE_ACTIVITY` + `WakeActivity`; `org.autojs.plugin.info.AUTHOR`; `NATIVE_PAGE_ALIGNMENT=16384`; INFO 服务 (`org.autojs.plugin.INFO`, category `terminal`, `requiresHostVersion`); `ThreeShellTerminalPluginService` (`org.autojs.plugin.TERMINAL`, category `terminal`, PLUGIN 权限, P2.4 前返回携带 `ITerminalPlugin` descriptor 的占位 Binder); `allowBackup=false`; `dataExtractionRules`; `localeConfig`; `supportsRtl`. 入口 Activity, 前台服务与启动器 alias 在 P2 / P3 / P5 追加. (SOURCE 2026-10-01: `app/src/main/AndroidManifest.xml` 如述, 另加 `<uses-sdk tools:overrideLibrary>` 覆盖 jackpal 三库; `ManifestContractTest` 6 项通过; `ThreeShellTerminalPluginService` 返回 `Binder().attachInterface(null, "org.autojs.plugin.terminal.api.ITerminalPlugin")`)
- [x] (插件) 资源骨架: 10 语言 `strings.xml` (`plugin_description` 各语言, 句尾无点号, 不含 "AutoJs6", 例如 `多会话终端, 在 pty 中运行 shell, 支持后台运行, 快捷按键及 Node.js 命令` / `A multi-session terminal running the shell in a pty, with background sessions, a key bar and Node.js commands`), `strings_donottranslate.xml` (`app_name=3-Shell Terminal`), 按 `name` 升序, ASCII 标点; 图标: `.python/generate_launcher_icons.py` 从 `.python/icons/three-shell-ic-launcher-light.png` 提取 alpha, `ADAPTIVE_GLYPH = 0.45` (第 4.4 节), 传统 / 圆盘图案宽 66%, `OPTICAL_X` / `OPTICAL_Y` 初值 0; 生成 `mipmap/ic_launcher.png` + `mipmap-night/` + `ic_launcher_system{,_light,_auto}` + 前景 / 单色层 + v26 XML, 两次运行字节一致, `--check` 通过. (SOURCE 2026-10-01: 10 语言 `plugin_description` + `strings_donottranslate.xml`; `generate_launcher_icons.py` `ADAPTIVE_GLYPH = 0.45`, 生成 15 个资源, `--check` 输出 `Verified 15 icon resources`; `StringResourceParityTest` 4 项与 `ApplicationTextPunctuationTest` 通过 (es 去掉西语倒问号, ar 用 ASCII 问号))
- [x] (插件) `.readme/` + `.changelog/` (10 个 `lang_*.json` + 模板, 列表键 `features` / `usage_steps` / `node_points` / `compatibility_points` / `faq_items` / `security_points`) + `.python/generate_markdown.py` (写入与 `--check`) + `.bat` 入口; 根 `README.md` 标明简体中文并与 `README-zh-Hans.md` 同源; `LICENSE` (MPL-2.0); `THIRD_PARTY_NOTICES.md` (common-plugin-api, nodejs-api, jackpal 三份 AAR 与原生库配方 Apache-2.0 含上游 commit 与 NDK 版本, 迁入的宿主源码 MPL-2.0). (SOURCE 2026-10-01: `common.json` 新增 `docs_terminal_url` / `nodejs_plugin_url` / `upstream_url`; 10 语言 README 源 (`h3_node` / `p_node_intro` / `node_points` / `text_link_terminal_docs` / `text_link_nodejs_plugin` / `text_link_upstream`) 与 v1.0.0 changelog 源 (released_date 2026/10/01; hint / feature x3 / dependency x2); 模板快速开始改为附录 A 的 `terminal.open` / `exec({ wait })` / `on('output')` / `waitFor` 示例; `generate_markdown.py` `README_LIST_KEYS` 与 `placeholder_node_points` 改名; `py -3 .python/generate_markdown.py` 与 `--check` 输出 `MARKDOWN_OK languages=10 artifacts=36`; `LICENSE` MPL-2.0; `THIRD_PARTY_NOTICES.md` 四节 (宿主 AAR 含 SHA-256 / jackpal 三 AAR + 原生配方 / 迁入宿主源码 MPL-2.0 / 运行时依赖))
- [x] (插件) 复制 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 为本仓库 `AGENTS.md` 并裁剪 (有原生库与 ABI 拆分, 有前台服务, 有存储权限, 有独立设置页与发行历史, 有跨包执行 Node.js 插件二进制, 权限理由表, jackpal 许可证边界); 身份表引用第 4.4 节; 引用三份同目录规范. (SOURCE 2026-10-01: `AGENTS.md` 16 节, 第 2 节身份表, 第 6 节权限理由表, 第 9 节 pty / 原生库 / Node 信任边界, 第 14 节验证顺序; 本次会话写入)
- [x] (插件) `git init`; 本次会话以一笔 `docs` 提交入库路线图, `AGENTS.md`, 图标源图, `.gitignore` 与 `version.properties` (`VERSION_BUILD=1`); P0.1 其余条目按 "构建骨架 / 契约与 Binder / 资源与文档 / 测试与 CI" 拆分提交, 每次提交前把 `VERSION_BUILD` 写为 "当前提交数 + 1", 最后校验 `VERSION_BUILD == git rev-list --count HEAD` 且工作树干净. (SOURCE 2026-10-01: `git init -b master`, 首笔提交 `cd5136a`, `VERSION_BUILD=1`)
- [x] (测试) `ThreeShellTerminalPluginRuntimeInfoTest` (PluginInfo 纯数据映射与身份常量对齐 `common.json` / `build.gradle.kts` / `settings.gradle.kts`, `supportedAbis` 推导), `ManifestContractTest` (权限, queries, meta-data, 组件导出与权限, 服务发现契约), `StringResourceParityTest`, `ApplicationTextPunctuationTest`, `VendoredAarLockTest` (锁条目与 `libs/jackpal/` 一致); instrumentation `ThreeShellTerminalPluginContractTest` (Wake Activity, INFO `getInfo()` 往返含 `supportedAbis`, TERMINAL 服务 descriptor, 原生库可加载: `System.loadLibrary("jackpal-termexec2")` 与 `jackpal-androidterm5`). (SOURCE 2026-10-01: JVM 25/25 通过 (`RuntimeInfoTest` 4, `NativeLibraryInventoryTest` 5, `ManifestContractTest` 6, `StringResourceParityTest` 4, `ApplicationTextPunctuationTest` 1, `VendoredAarLockTest` 5); DEVICE: `ThreeShellTerminalPluginContractTest` 6/6 在 API 24 x86 AVD `emulator-5554`, API 37 x86_64 16 KB AVD `emulator-5558` (`getconf PAGE_SIZE` = 16384), Xiaomi Pad API 35 arm64 `968e9f18` 三台通过; 原生库在 `useLegacyPackaging = false` 下不解压到 `nativeLibraryDir`, 测试改为 `System.loadLibrary` + APK 内 `lib/<abi>/` 条目断言; AndroidX startup 合并一个非导出 provider, 断言改为 "无导出 provider"; 宿主侧: 在 `emulator-5558` 安装 release x86_64 产物与宿主 debug 5304, 以 `RunIntentActivity` 运行 `build/probe-discovery.js`: INFO / TERMINAL 各发现 1 个服务, `getInfo()` 返回 id / engine / variant / 1.0.0 (2) / `supportedAbis=[x86_64]` / instruction 2533 字符, `InstalledPluginRepository.discoverInstalled` 返回 `engine=terminal engineId=three-shell-terminal requiresHostVersion=5303 nativePageAlignment=16384 bindError=null`)
- [x] (插件) CI: `build.yml` (JVM 测试, `verifyNativePageAlignment`, debug / androidTest / release APK, lint, API 24 x86 + API 35 x86_64 模拟器契约测试, artifact `three-shell-terminal-debug-apks`), `markdown.yml` (Windows `check_markdown.bat`); 仓库未推送, 工作流只做本地语法校验. (SOURCE 2026-10-01: `.github/workflows/build.yml` (artifact `three-shell-terminal-debug-apks`, AVD `Three_Shell_Terminal_Conformance_CI_<api>`, API 24 x86 + API 35 x86_64) 与 `markdown.yml` 入库; 本地等价命令 `:app:assembleDebugUnitTest :app:verifyNativePageAlignment :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug :app:lintRelease` 通过 (lint 13 条 warning: 依赖新版本提示, `UnusedResources` 为 P5 前预置的启动器图标 / `plugin_*` resValue, `ChromeOsAbiSupport` 误报, 无 error); R8 需 `-dontwarn kotlinx.parcelize.Parcelize` (与 3-Setup Installer 相同); `verifyDebugNativePageAlignment` / `verifyReleaseNativePageAlignment` 通过)

### P0.2 pty / 存储 / Node spike

- [x] (插件) debug-only `SpikeActivity` 或 instrumentation: 以插件 uid 用迁入前的最小代码 (`PtyBridge` + `TermExec` + `TermSession`) 打开 `/dev/ptmx`, 启动 `/system/bin/sh -c 'echo spike-$$; pwd; exit 7'`, 读到输出并得到退出码 7; API 24 AVD x86 与 API 35 真机各一次. (DEVICE 2026-10-01: instrumentation `spike/P0SpikeTest.ptyRunsTheSystemShellAndReportsItsExitCode` + 迁入的 `jackpal/androidterm/PtyBridge.java` (宿主 c6e24541ef); `/dev/ptmx` + `PtyBridge.createSubprocess` + `waitFor` 得到 exit 7 与 `spike-<pid>` 回显, 子进程 `id` 显示插件 uid 与 `u:r:untrusted_app` 域; 五台通过: API 24 x86 AVD 12 ms, Sony API 28 arm64 29 ms, Redmi API 33 52 ms, Xiaomi Pad API 35 64 ms, API 37 x86_64 16 KB AVD 34 ms; 子进程 cwd 默认 `/`)
- [x] (插件) 共享存储: 未授权时 `cd /sdcard/脚本` 失败并被识别为 `STORAGE_PERMISSION_REQUIRED`; 跳转 `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` 授权后 `ls /sdcard/脚本` 成功; API < 30 用运行时权限验证同一流程 (API 24 / 28). (DEVICE 2026-10-01: `sharedStorageAccessFollowsThePluginsOwnGrant`; 未授权: API 24 / 28 `ls` 与写入均 `Permission denied`, API 33 / 35 / 37 顶层 `ls` 可列目录名但写入 `Operation not permitted`; 授权后 (API 24 / 28 `pm grant READ/WRITE_EXTERNAL_STORAGE`, API 35 `appops set ... MANAGE_EXTERNAL_STORAGE allow` 即系统设置 "所有文件访问" 的等价态) `ls` + 写入成功; 宿主在每台设备均已授权但对插件 uid 无效; 结论: `StorageAccess` 按插件自身权限状态 (`isExternalStorageManager()` / 运行时权限) 判定而非解析 shell 错误文本)
- [x] (插件) Node 启动器: 在已安装 Node.js Runtime 1.5.6 的设备上, 插件进程直接 `exec` 其 `nativeLibraryDir/libnodexe.so --version` 得到 Node 版本 (SELinux 允许 `untrusted_app` 执行其它应用 `apk_data_file` 的事实在插件 uid 下复核); 读取 `NODE_CLI_*` meta-data 并比对签名 (官方签名集合 / 自身签名). (DEVICE 2026-10-01: `nodeLauncherOfTheRuntimePluginExecutesFromThisProcess`; Sony API 28 (Node.js Runtime 1.5.6 build 217, 32 位 `lib/arm`) 54 ms, Redmi API 33 (build 220) 153 ms, Xiaomi Pad API 35 (build 220) 205 ms 均 `exit=0 v24.21.0`, 进程域 `untrusted_app`; `NODE_CLI_*` schema 1 / `libnodexe.so` / 8 个命令 / 归档 `nodejs/cli/node-cli-24.21.0.bin` 读取成功; 签名 `31a681fc...` 命中官方集合 (与 `PluginTrustManager.OFFICIAL_SHA_256` 相同) 且与本插件 debug 签名一致; 未观察到 `ExecDenied`; 结论: 启动器 ABI 取自 Node.js 插件的 `nativeLibraryDir` 而非本进程 ABI; API 24 / 37 AVD 无该插件, 用例按 assumption 跳过)
- [x] (文档) spike 结果写入 `docs/dev/p0-spike-evidence.md` (设备, API, ABI, SELinux 上下文 `ps -Z`, 耗时); D15-D18 按实测修订 (例如某 OEM 拒绝执行其它应用的二进制时, 记录为 `ExecDenied` 的真实触发条件). (SOURCE 2026-10-01: `docs/dev/p0-spike-evidence.md` 含五台设备矩阵, 三节 logcat 摘录与结论; D15-D18 无需修订, 另记 cwd 默认 `/`, 存储按状态判定, 启动器 ABI 来源, `Process.waitFor(timeout)` 为 API 26+ 四条实现注记)

验收条件: `:app:assembleDebug` / `:app:testDebugUnitTest` / `:app:assembleDebugAndroidTest` / `:app:lintDebug` / `:app:verifyNativePageAlignment` 通过; 宿主插件中心能发现并启用本插件 (INFO 往返, 透明图标 allowlist 在 P1.2 加入前显示系统圆盘); 三个 spike 在至少两台设备通过; 证据写入本节与 `docs/dev/p0-spike-evidence.md`.

验收 (2026-10-01): 五个 Gradle 任务通过 (JVM 25/25, lint 0 error); 宿主 debug 5304 在 16 KB AVD 上经 `InstalledPluginRepository.discoverInstalled` 发现本插件 (`engine=terminal`, `supportedAbis=[x86_64]`, `nativePageAlignment=16384`, `bindError=null`), 插件中心界面内的启用操作与图标 allowlist 留待 P1.2 一并复核; 三个 spike 在 5 台设备 (API 24 / 28 / 33 / 35 / 37-16 KB) 通过, Node 启动器在 3 台已装 Node.js Runtime 的真机通过; 证据见本节各条与 `docs/dev/p0-spike-evidence.md`. P0 关闭.

---

## P1: 宿主契约, 入口改造与旧终端迁出

目标: 宿主拥有 `plugin-api/terminal-api`, 三处界面入口与抽屉状态经 `TerminalLauncher` / `TerminalPluginStateMonitor`, 插件中心识别 engine `terminal`, 旧终端代码 / 资源 / 偏好 / 组件删除, 旧数据后台清理, changelog 改写; 宿主构建, 单元测试与 lint 通过.

### P1.1 契约模块 `plugin-api/terminal-api`

- [x] (宿主) 新建 Android library (namespace `org.autojs.plugin.terminal.api`, `aidl = true`, `api(project(":plugin-api:common-plugin-api"))`), 注册到 `settings.gradle.kts` 的 `pluginApi` 列表; AIDL `ITerminalPlugin` 13 方法与 `ITerminalCallback` 3 个 oneway 方法 (附录 B.1); 不阻塞 Binder 线程的方法全部返回即时结果, 长输出走管道. (SOURCE 2026-10-01: 宿主提交 `b8f4d6c939` 新建 `plugin-api/terminal-api` (`aidl = true`, `api(project(":plugin-api:common-plugin-api"))`, `consumer-rules.pro` keep 规则, `aarMetadata.minCompileSdk = 36`), `settings.gradle.kts` `pluginApi` 追加 `terminal-api`, 宿主 `app` 依赖 `:plugin-api:terminal-api`; `ITerminalPlugin` 13 方法 / `ITerminalCallback` 3 个 oneway 方法与附录 B.1 顺序一致, 长输出经 `subscribeOutput` 返回的 `fd` 管道)
- [x] (宿主) 常量: `TerminalActions` (`SERVICE_ACTION`, `SERVICE_CATEGORY`, `PLUGIN_PERMISSION`, `OPEN_TERMINAL`, `OPEN_SETTINGS`), `TerminalIds` (`PLUGIN_ID`, `ENGINE`, `VARIANT_DEFAULT`, `DEFAULT_PACKAGE_NAME`, `REQUIRED_HOST_VERSION_CODE`), `TerminalContract` (契约版本 1, Intent extra 键, Bundle / JSON 键, 会话状态字符串, 上限 D22), `TerminalCapabilityKeys` (`CONTRACT_VERSION`, `FEATURES`, `MAX_SESSIONS`, `MAX_SUBSCRIPTIONS`, `NODE_CLI`), `TerminalErrorCodes` (附录 B.4). (SOURCE 2026-10-01: 五个常量文件落地于宿主 `b8f4d6c939`; `TerminalIds.REQUIRED_HOST_VERSION_CODE = 5304L` 为暂定值, P1.4 回填确认; `TerminalContract` 另含 `MIN/MAX_CONTRACT_VERSION` 与 `supportsContractVersion`, `FIELD_*` JSON 字段名, `NODE_CLI_STATES` 八态, `STORAGE_ACCESS_STATES`; `TerminalErrorCodes.HOST_ONLY` 标记四个仅宿主产生的码)
- [x] (测试) `TerminalAidlOrderTest` (transaction 顺序冻结), `TerminalContractTest` (常量, 上限, 错误码唯一, 与 `PluginCapabilityKeys` 无冲突); `:plugin-api:terminal-api:testDebugUnitTest` 通过. (SOURCE 2026-10-01: 两个测试类 9 项, `:plugin-api:terminal-api:testDebugUnitTest` 9/9 通过)
- [x] (宿主) 发布 AAR (`:plugin-api:terminal-api:assembleRelease`), 记录 SHA-256; 插件 `libs/terminal-api.aar` + `locks/host-api-aars.lock` 条目; 插件身份常量改由契约提供; 同时把 `nodejs-api` 当前 AAR 入插件 `libs/` 并锁定 (若 P0.1 已做则只核对摘要). (SOURCE 2026-10-01: release AAR SHA-256 `98b43a9deb236d642f0d3b18696a4da9a9c4b399374d6e398d80d3abf0c86bc6` 入插件 `libs/terminal-api.aar` 并写入 `locks/host-api-aars.lock` (`hostApiIds` 三项, R8 keep `org.autojs.plugin.terminal.api.**`); `nodejs-api.aar` 已在 P0.1 入库, 摘要 `4334b94a6f86e8ef8ff2912b6ace817dbf9bca1e887be7d615918f990f57ab71` 核对一致; `ThreeShellTerminalPlugin` 的身份 / 动作 / 描述符 / 契约版本 / 最低宿主版本常量全部改由 `TerminalIds` / `TerminalActions` / `TerminalContract` / `ITerminalPlugin.DESCRIPTOR` 提供, Manifest 与 `common.json` 的 `requiresHostVersion` 同步为 5304; `THIRD_PARTY_NOTICES.md` / `libs/README.md` / 10 语言 changelog 追加 terminal-api 条目, 36 个 markdown 产物再生成; 验证: JVM 25/25, `assembleDebug` + lint 0 error, 契约 instrumentation 在 API 37 x86_64 16 KB AVD 6/6, 宿主 debug 5304 经 INFO 服务绑定读到 `requiresHostVersion=5304` / `engine=terminal` / `supportedAbis=[x86_64]`)

### P1.2 宿主客户端, 路由, 状态监听与注册

- [ ] (宿主) `core/plugin/terminal/TerminalPluginHost.kt` (`AidlPluginHost<ITerminalPlugin>`: action / category / `ITerminalPlugin.Stub::asInterface`, 最低宿主版本, 信任与服务校验同 `EpubPluginHost.validateService`), `ThreeShellTerminalOfficialPlugin.kt` (`DISPLAY_NAME`, `PACKAGE_NAME`, 仓库 URL), `TerminalJson.kt`, `TerminalErrorMapper.kt`.
- [ ] (宿主) `TerminalLauncher.kt` (D25): `open(context, directory?)`, `openNew(context)`, `openSession(context, id)`, `openManager(context)`, `closeAll(context)`; 可用性判定 (`TerminalPluginHost.discover` + `PluginEnableStore` / `PluginTrustManager` 状态) 映射到五态, 各态动作如 D25; 文案新增 `text_terminal_plugin_required` / `text_terminal_plugin_disabled` / `text_terminal_plugin_incompatible` / `text_terminal_plugin_unavailable` (10 语言).
- [ ] (宿主) `TerminalPluginStateMonitor.kt` (D26): 抽屉可见期间 `registerCallback`, `onSessionsChanged(count, sessionsJson)` 推主线程刷新; 包状态广播 (`PACKAGE_ADDED` / `REMOVED` / `REPLACED` / `CHANGED`, data scheme `package`, API 33+ `RECEIVER_NOT_EXPORTED`) 触发重新发现; `AidlPluginHost` 空闲解绑后回调自动失效, 再次可见时重新注册.
- [ ] (宿主) 入口改造: `DrawerFragment.kt:726-757, 936` 的 `mTerminalItem` 改为 `TerminalLauncher` + 监听器 (可用: 开关 + 会话数 + 关闭全部 + 新建 + 管理器; 不可用: 普通项 + 状态副标题); `ExplorerPageViewHolder.kt:238-240` 与 `ExplorerProjectToolbar.java:187-194` 改为 `TerminalLauncher.open(context, path)`; `ConnectionManagerDialog.kt:273` / `connection_manager_client_item.xml` 改用 `text_close_session`.
- [ ] (宿主) 插件中心注册: `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` 增加 `TerminalIds.ENGINE to TerminalPluginHost.ACTION_TERMINAL`, `InstalledPluginRepository` 发现分派, Manifest `<queries>` 增加 `org.autojs.plugin.TERMINAL` + category `terminal` 与 `org.autojs.plugin.TERMINAL_OPEN` / `TERMINAL_SETTINGS` activity intent, `PluginInstallWizardCatalog` 增加 `entry(official("three.shell.terminal"), "Three Shell Terminal", TOOLS)`, `PluginInstalledIconResolver.transparentIconPackages` 加入本包名, `PluginSettingsFragment` 的 "打开插件设置" 走 `org.autojs.plugin.TERMINAL_SETTINGS`.
- [ ] (测试) JVM: 五态决策表 (安装 / 启用 / 授权 / 版本 / 绑定 四个输入 -> 动作), `TERMINAL_OPEN` Intent 构造 (extras 键与值), JSON 编解码, 错误映射; `:app:testAppDebugUnitTest --tests org.autojs.autojs.core.plugin.terminal.*` 通过.

### P1.3 删除宿主终端与数据清理

- [ ] (宿主) 删除第 3.1 节源码: `core/terminal/` 17 个文件, `ui/terminal/` 7 个文件, `ui/settings/Terminal{NpmRegistry,ClearData,NodeProbe}Preference.kt`, `jackpal/androidterm/PtyBridge.java`, `jackpal/androidterm/emulatorview/TerminalSelectionSnapshot.java`; 删除测试 `app/src/test/.../core/terminal/` 10 个文件与 `androidTest/.../core/terminal/TerminalSessionsInstrumentationTest.kt` (先确认已复制到插件, P2.1 / P3.1).
- [ ] (宿主) 删除资源: `layout/activity_terminal.xml`, `layout/include_terminal_node_banner.xml`, `menu/menu_terminal.xml`, `values/ids_terminal.xml`; `strings.xml` 10 语言删除除 D27 保留项外的全部 `*_terminal_*` 条目 (约 55 条 x 10), `description_terminal` 改写 ("由 3-Shell Terminal 插件提供..." 语义), 新增 `text_close_session`; `strings_donottranslate.xml` 删除 `key_terminal_*` / `key_dialog_terminal_tips` / `default_key_terminal_npm_registry`; `arrays.xml` 删除两个数组; `fragment_preferences.xml:186-211` 与 `fragment_developer_options.xml:97-101` 删除 (D8); `lint` 无未使用资源 / 缺失翻译告警.
- [ ] (宿主) Manifest 删除 `TerminalActivity` 与 `TerminalSessionService` 两个组件 (含 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`); 若 `FOREGROUND_SERVICE_SPECIAL_USE` 权限无其它使用者则一并删除 (先 grep 确认), 否则保留.
- [ ] (宿主) `TerminalLegacyCleanup.kt` (D28): `App` 启动后在 IO 线程执行 `<filesDir>/terminal` 递归删除 + 偏好键扫描删除; 幂等, 异常只记日志; 10 语言 changelog hint.
- [ ] (测试) `TerminalLegacyCleanupTest` (JVM, 临时目录与内存偏好: 存在则删, 不存在无副作用, 非前缀键保留); 宿主 `:app:assembleAppDebug` / `:app:assembleInrtDebug` / `:app:testAppDebugUnitTest` / `:app:lintAppDebug` 通过; 真机: 升级安装后 `run-as` 不可用时用 `adb shell ls /data/data/org.autojs.autojs6/files/terminal` 需 root, 改为在 debug 构建的开发者选项 "数据目录" 查看或由 `TerminalLegacyCleanupTest` 的 instrumentation 变体验证.

### P1.4 协议文档, changelog 与版本回填

- [ ] (宿主) `docs/dev/terminal-plugin-protocol-v1.md`: 范围与边界表 (宿主 / 插件各自拥有), 身份, Binder 面, Intent 协议, JSON 键, 上限, 错误码, 版本协商, 五态退化, 会话状态监听, 旧数据清理, 测试; 体例同 `installer-plugin-protocol-v1.md`.
- [ ] (宿主) changelog 改写 (D10, 10 语言逐条核对, 以 `终端` / `Terminal` 关键词检索并排除 "terminal result" / "terminal completion" 等非终端功能用法): 删除 zh-Hans feature "终端功能, 支持多会话管理..." 与 "终端支持 node/npm/npx/corepack/yarn/pnpm 命令及 npm 镜像源设置...", fix "终端会话在启动期间关闭时可能遗留后台进程的问题", 以及 en 多出的 improvement "Terminal uses direct shell input..." / "Creating or resuming a session from the terminal manager..." / "Terminal: sessions keep running in the background..."; 修改混合条目去掉终端部分: fix "Android 7/8.0 设备上部分插件文件快照, Node.js 包安装, TypeScript 项目文件操作及终端环境探测的兼容性问题", fix "打包应用或宿主缺少运行时组件时, 终端原生库可能缺失或使用错误 ABI..." (改为 "shell 原生库"), improvement "无障碍服务, 连接及终端管理器统一分组布局..."; 新增 hint "终端改由 3-Shell Terminal 插件提供, 使用前需在插件中心安装并启用; 宿主内置终端的数据不迁移" 与 feature "集成 3-Shell Terminal 插件: 主页抽屉 / 文件管理器目录菜单 / 项目工具栏入口, 以及脚本全局对象 terminal" (脚本 API 条目在 P4.3 落地时再加); 运行 `.python/generate_markdown.py` 同步 README.
- [ ] (宿主) `version.properties` `VERSION_BUILD` 按提交数递增; 回填 `TerminalIds.REQUIRED_HOST_VERSION_CODE` 与插件 `requiresHostVersion` / `ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION` 为交付 `terminal-api` 的宿主构建号; 插件 `THIRD_PARTY_NOTICES.md` / `libs/README.md` / 10 语言 changelog 同步.

验收条件: 宿主三入口在 "插件可用 / 未安装 / 已禁用" 三态下行为符合 D25 (真机各一次); 抽屉会话数随插件会话变化 (P2.4 后复验); 宿主 APK 不再含终端 Activity / 服务 / 字符串; 升级安装后 `files/terminal` 与 `key_$_terminal_*` 消失; 宿主 changelog 10 语言无内置终端描述.

---

## P2: 插件核心

目标: 插件进程内可创建 / 关闭多会话, 前台服务与通知正确, 存储权限与目录回退可用, Binder 面完整并带上限与宿主身份校验, Node CLI 接入与签名信任生效, 包管理动作可用.

### P2.1 会话, pty, 环境与前台服务

- [ ] (插件) 迁入 `core/` 九个文件 (第 3.1 节) 与 `jackpal/androidterm/` 两个桥接类, 改包名, 保留 MPL-2.0 文件头; `TerminalPreferences` 改为插件 `SharedPreferences` (键名 `terminal_text_size` / `npm_registry` / `npm_registry_custom_url` / `npm_ignore_scripts` / `node_integration_enabled`); `TerminalSessionManager` 增加会话标题 (最近命令或目录名) 与 `exitCode` 记录供 Binder 使用.
- [ ] (插件) `ThreeShellTerminalSessionService` (D15): 迁入并改渠道 / action / 通知文案, API 24 / 25 `startService` 分支, API 33+ 通知权限请求点 (首次创建会话的 Activity 中请求, Binder 发起的会话不弹权限, 通知静默缺失), `specialUse` 子类型属性文案改为插件语义.
- [ ] (测试) 迁入 10 个 JVM 测试并通过 (`TerminalPathsTest`, `TerminalEnvironmentTest`, `TerminalSessionLauncherTest`, `TerminalKeySequencesTest`, `ShellQuotingTest`, `NpmProjectScriptsTest`, `TerminalNodeEnvironmentTest`, `NodeCli*Test`); instrumentation: 创建两个会话, 各写入 `echo $$`, 输出不串, `closeAll` 后 `ps` 无残留 `sh` (API 24 AVD 与 API 35).

### P2.2 存储权限与目录解析

- [ ] (插件) `storage/StorageAccess.kt` (D18): `state()` (`GRANTED` / `DENIED_LEGACY` / `DENIED_ALL_FILES` / `NOT_APPLICABLE`), `resolveDirectory(path)` -> `Resolved(directory, fallbackReason?)`; 共享存储判定用 `Environment.getExternalStorageDirectory()` 与 `/sdcard` / `/storage/emulated/<user>` 规范化; 请求授权的 Intent 构造 (API 30+ 带包名 URI, 失败回退到无包名的 `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`).
- [ ] (测试) JVM: 路径规范化与共享存储判定表 (`/sdcard/脚本`, `/storage/emulated/0/x`, `/data/data/<pkg>/files`, 相对路径, 空串); instrumentation: 未授权 -> 回退 `HOME` 且原因为 `STORAGE_PERMISSION_REQUIRED`; 授权后进入目录 (API 24 运行时权限, API 35 全部文件访问).

### P2.3 Node CLI 接入与信任

- [ ] (插件) 迁入 `node/` 七个文件; `NodeCliTrust` 实现 D17 签名规则 (`PackageManager.GET_SIGNING_CERTIFICATES`, API 28+ `signingInfo.apkContentsSigners`, API < 28 `signatures`; 官方 SHA-256 集合常量 + 自身签名); `NodeCliLocator` 的 `PluginNotAuthorized` 改为 `PluginNotTrusted`, 增加 `IntegrationDisabled` (设置开关关闭); 缓存键沿用 `包名@versionCode:lastUpdateTime`.
- [ ] (插件) 会话启动装配: `TerminalNodeSetup` 在每次会话创建时刷新 `usr/bin` 符号链接 (Node.js 插件更新后 `nativeLibraryDir` 变化), 归档只解压一次 (按 SHA-256 记录), 失败 -> `SetupFailed` 并保持纯 shell; 环境变量表与宿主一致 (`AUTOJS6_NODE_CLI_ROOT`, `npm_config_cache` / `prefix` / `bin_links=false` / `update_notifier=false`, 非默认时 `npm_config_registry` + `COREPACK_NPM_REGISTRY`, `COREPACK_HOME`, `COREPACK_DEFAULT_TO_LATEST=0`, `npm_config_ignore_scripts` 按设置).
- [ ] (测试) JVM: 信任判定表 (官方 / 自身 / 其它 / 多签名), `IntegrationDisabled` 短路; DEVICE: 安装 Node.js Runtime 1.5.6 的设备上 `node --version` / `npm --version` / `corepack --version` / `npx --yes cowsay hi` (需网络) 在终端输出正确; 卸载 Node.js 插件后新会话为纯 shell 且横幅原因 `PluginMissing`.

### P2.4 Binder 路由与上限

- [ ] (插件) `ThreeShellTerminalPluginService` 实现 `ITerminalPlugin.Stub` (附录 B.1): `HostCallerGuard` (调用方必须持有 `org.autojs.permission.PLUGIN` 且签名可信, 与 3-Setup Installer 同实现); `getCapabilities` 声明 `CONTRACT_VERSION=1`, `FEATURES` (`sessions`, `exec`, `output-stream`, `transcript`, `node-cli`), `MAX_SESSIONS=16`, `MAX_SUBSCRIPTIONS=4`, `NODE_CLI` 状态; `openSession(requestJson)` (directory / command / env / newWindow) -> `sessionJson`; `writeInput(sessionId, bytes)`; `readTranscript(sessionId, maxBytes)`; `getEnvironment()` (home / prefix / node 状态 / registry); 全部输入按 D22 校验, 超限 `INVALID_ARGUMENT`, 不崩溃.
- [ ] (插件) `OutputSubscription` (D21): `ParcelFileDescriptor.createPipe()`, 写线程 + 1 MiB 环形缓冲, 溢出计数与 `onOutputOverflow`, 宿主死亡 (`linkToDeath`) / 取消订阅 / 会话退出时关闭写端 (退出前先刷完缓冲); `CallbackRegistry` (<= 8, death 自动移除) 分发 `onSessionsChanged` / `onSessionExited`.
- [ ] (测试) instrumentation (`TerminalBinderContractTest`): happy path (open -> write `echo ok` -> 管道读到 `ok` -> close), 敌意输入 (超长命令, 非法 JSON, 不存在的 sessionId, 第 17 个会话 `SESSION_LIMIT`, 第 5 个订阅 `SUBSCRIPTION_LIMIT`), 调用方无权限被拒 (用未持有 PLUGIN 权限的测试包), 宿主模拟死亡后订阅写端关闭; API 24 与 API 35.

### P2.5 包管理与设置项逻辑

- [ ] (插件) 迁入 `TerminalNpmActions` 的非 UI 部分 (npm init / install / 安装包 / 运行脚本 / 其它包管理器说明 / 搜索 URL), `NpmProjectScripts` 读取 `package.json`; 镜像源 (`https` 限制, `sanitizeRegistry`), 忽略安装脚本, 清除数据 (关闭全部会话 -> 删除 `home` / `usr` -> 重建布局) 作为纯逻辑类供 P3 / P5 的 UI 调用.
- [ ] (测试) JVM: 镜像源规范化与拒绝 `http://`, 清除数据的目录集合; DEVICE: 在含 `package.json` 的目录中 `npm run` 列表正确, `npm install` 在 npmmirror 下完成 (需网络).

验收条件: 两台设备 (API 24 AVD + API 35 真机) 上 Binder 契约测试全绿; Node CLI 三命令可用且卸载后退化正确; 前台服务通知在离开界面后持续显示并可 "关闭全部"; 全部 JVM 测试通过.

---

## P3: 插件界面

目标: 迁入全部终端界面并接入入口协议; 从宿主与启动器两种路径进入, 体验与宿主内置版一致或更好.

### P3.1 终端 Activity, 快捷键栏, 选择与菜单

- [ ] (插件) 迁入 `TerminalActivity` (641 行) 与 `TerminalEmulatorView` / `TerminalToolbarView` / `TerminalTextSelection` / `TerminalSettingsDialogs` / `TerminalNpmActions` (UI 部分), 布局 `activity_terminal.xml` 与菜单 `menu_terminal.xml`; 主题 `Theme.ThreeShellTerminal` (全屏, `adjustResize|stateVisible`, 跟随宿主外观: 夜间模式与主题色经 `AutoJs6HostSettingsContract`); 菜单新增 "设置" (P5.1) 与 "关于"; 字符串 10 语言迁入并改名为插件资源 (`terminal_*`).
- [ ] (插件) Node 横幅 (`include_terminal_node_banner.xml`) 迁入, 原因文案增加 `PluginNotTrusted` / `IntegrationDisabled`, 动作: 安装 Node.js Runtime (宿主插件中心 / GitHub Release), 打开插件设置, 查看探测详情; 存储横幅 (D18) 新增: 文案 + "授予" + "重新进入目录".
- [ ] (测试) 迁入 `TerminalSessionsInstrumentationTest` 四个用例并通过 (API 24 / 35); 新增: 存储横幅在未授权打开 `/sdcard/脚本` 时出现, 授权后消失 (API 35 用 `appops set <pkg> MANAGE_EXTERNAL_STORAGE allow` 驱动).

### P3.2 会话管理器

- [ ] (插件) 迁入 `TerminalManagerDialog` (分组展开 / 收起偏好键迁为插件偏好), 支持从入口协议 `manager=true` 直接打开 (无前台 Activity 时以对话框主题 Activity 承载); 新建 / 打开 / 关闭 / 关闭全部 / 复制目录; 与 Binder `onSessionsChanged` 共享同一注册表监听.
- [ ] (测试) instrumentation: 管理器跨新建 / 恢复会话 Activity 存活并恢复原终端 (迁入用例); 从入口协议打开管理器时无终端 Activity 的 Back 行为.

### P3.3 入口 Activity 与启动器

- [ ] (插件) `ThreeShellTerminalEntryActivity` (D19): 校验调用方 (`callingPackage` 持有 PLUGIN 权限且签名可信, 否则 `finish`), 解析 extras (`directory` / `sessionId` / `newSession` / `command` / `manager`), 转发到 `TerminalActivity` 或管理器, `Theme.NoDisplay`, `excludeFromRecents`; Manifest 导出 + PLUGIN 权限 + intent-filter `org.autojs.plugin.TERMINAL_OPEN`.
- [ ] (插件) `LauncherActivity` (D30): 有会话恢复最近会话, 否则新建 (目录 `HOME`); 启动器 alias 在 P5.4 挂到它; 任务栏行为: 自有 `taskAffinity`, 从宿主进入 Back 回宿主, 从启动器进入 Back 回桌面 (`TerminalActivity` 按 `isTaskRoot` 决定).
- [ ] (测试) instrumentation: 无 PLUGIN 权限的调用被拒 (测试包不持有权限), 宿主签名调用成功进入指定目录; 宿主侧真机: 抽屉 / 目录菜单 / 项目工具栏三入口各进入一次并回到宿主.

验收条件: 两台设备上宿主三入口 + 启动器 + 管理器全部可用; `TerminalSessionsInstrumentationTest` 四用例 + 新增用例通过; 文本选择 / 复制 / 分享 / 字号 / 键盘显示与宿主内置版行为一致 (手动清单写入 `docs/dev/p3-ui-evidence.md`).

---

## P4: 脚本 API `terminal`

目标: 宿主 Rhino 全局对象 `terminal` 三档可用, 同步 + Async 双形态, 会话 EventEmitter 与输出管道, 错误类型与示例齐备.

### P4.1 服务层, augment 与第一 / 二档

- [ ] (宿主) `runtime/api/terminal/TerminalService.kt` (每脚本, `Closeable`: 脚本结束时取消订阅与回调, 不关闭用户会话, 除非 `exec` 的 `closeOnExit`), `TerminalScriptArguments.kt` (纯 Kotlin 参数形态检查: 路径遵循 `files.path` 工作目录规则, 拒绝 `content://`; 选项键 `cwd` / `env` / `show` / `keepOpen` / `title` / `stripAnsi`), `ScriptRuntime.kt` 装配 (字段, `close`, augment).
- [ ] (宿主) `runtime/api/augment/terminal/Terminal.kt` (`AugmentableKey("terminal")`, 别名 `$terminal`): `open` / `openAsync`, `sessions` / `sessionsAsync`, `session(id)`, `close(id)` / `closeAsync`, `closeAll` / `closeAllAsync`, `show(id)` / `showAsync`, `exec` / `execAsync`, `npm.run` / `npm.runAsync`, `env` / `envAsync`, `state()`, `isAvailable()` / `isAvailableAsync()`, `TerminalError` 构造器; 五态映射 `PLUGIN_UNAVAILABLE` / `PLUGIN_INCOMPATIBLE`; `show` / `open({ show: true })` 以 `TERMINAL_OPEN` Intent 启动界面 (D19).
- [ ] (测试) JVM: `TerminalScriptArgumentsTest` (选项形态, 路径规则, 上限), `TerminalJsonTest`; DEVICE: 示例脚本 `terminal.open(files.cwd())` 打开终端并进入脚本目录, `terminal.sessions()` 返回该会话, `terminal.closeAll()` 清空, 插件缺失时抛 `TerminalError` code `PLUGIN_UNAVAILABLE`.

### P4.2 第三档: 会话对象与输出管道

- [ ] (宿主) `TerminalSessionNativeObject` (EventEmitter): `id` / `cwd` / `title` / `createdAt` / `isAlive()`, `write(text | bytes)`, `show()`, `close()`, `waitFor(pattern, timeout?)` (正则或字符串, 返回匹配行, 超时 `TIMEOUT`), `transcript(maxBytes?)`, 事件 `output(line | chunk)`, `exit(code)`, `overflow(droppedBytes)`; `TerminalOutputReader` 工作线程读管道 (`stripAnsi` 默认开, 按行切分, 行上限 64 KiB), 事件投递到脚本循环 (`Loopers`), 脚本结束时取消订阅; 首次 `on('output')` 时才 `subscribeOutput`, 最后一个监听器移除后 `unsubscribeOutput`.
- [ ] (宿主) 同步等待语义: `waitFor` 与 `exec({ wait: true })` 在脚本线程阻塞并保持事件循环投递 (同 `epub` / `installer` 的同步形态实现), 默认超时 10 分钟, 可传 `0` 表示不限.
- [ ] (测试) JVM: `TerminalOutputReaderTest` (ANSI 剥离, 行切分, 超长行, 溢出事件顺序); DEVICE: `exec('for i in 1 2 3; do echo line$i; sleep 1; done; exit 3', { show: false })` 收到 3 行 `output` 与 `exit(3)`; `waitFor(/line2/)` 返回 `line2`; 高吞吐 `yes | head -c 50m` 触发 `overflow` 且终端界面不冻结.

### P4.3 示例, 守卫与 changelog

- [ ] (宿主) 示例脚本 `assets-app/sample/终端/` 三个: `在终端中打开脚本目录.js` (open + show), `运行 npm 脚本并等待完成.js` (npm.run + waitFor / exit), `会话驱动与输出监听.js` (exec + on('output') + write); 全部在插件缺失时给出友好提示 (`isAvailable` 守卫).
- [ ] (宿主) 10 语言 changelog feature "脚本全局对象 terminal: 会话管理, 可见执行与输出监听 (需要 3-Shell Terminal 插件)"; `docs/dev/terminal-plugin-protocol-v1.md` 增加脚本 API 章节.
- [ ] (测试) 三个示例在 API 35 真机运行通过 (含插件缺失态的提示); `:app:testAppDebugUnitTest` 全量通过.

验收条件: 附录 A 的全部方法在真机可用; 第三档吞吐测试不冻结界面; 示例三个通过; 文档章节落地.

---

## P5: 独立应用形态

目标: 插件作为独立应用可配置, 可查看发行历史与更新, 启动器图标符合 Three 系列规范.

### P5.1 设置页

- [ ] (插件) `SettingsActivity` (D29): 外观四项 (语言 / 夜间模式 / 主题色 / 启动器图标, 默认跟随 AutoJs6, 经 `AutoJs6HostSettingsContract` 读取宿主当前值, 宿主缺失时回退本地值), 终端组 (字号, npm 镜像源三选一 + 自定义 URL 输入, 忽略安装脚本, Node.js 集成开关, 环境探测详情行 -> `NodeProbeActivity` (迁入 `TerminalNodeProbePreference` 的探测与报告), 全部文件访问状态行, 清除终端数据 (确认对话框, 关闭会话后执行)), 关于 / 发行历史 / 更新检查; Material 3, 先选后确定, 24 dp 圆角对话框, 72 dp 行高; 代码构建视图 (复用 3-Stove Agent 的 `AppearancePreferences` / `HostAppearanceActivity` / `DocumentText` / `LegacyInputTintContext`).
- [ ] (插件) Manifest: `SettingsActivity` 导出 + PLUGIN 权限 + intent-filter `org.autojs.plugin.TERMINAL_SETTINGS`; 终端菜单 "设置" 直接启动 (同进程无需权限校验).
- [ ] (测试) JVM: 镜像源 / 字号 / 开关的序列化与边界; instrumentation: 设置页各行点击打开对应对话框, 更改字号后终端字号变化, 关闭 Node.js 集成后新会话横幅原因 `IntegrationDisabled`; 宿主插件中心 "设置" 能打开本页 (API 35).

### P5.2 关于, 发行历史与更新检查

- [ ] (插件) `AboutActivity` (版本 / build / 提交 / 许可证 / 第三方声明含 jackpal Apache-2.0 与宿主 AAR), `ReleaseHistoryActivity` (按 locale 读取 `doc/CHANGELOG-{tag}.md`, 回退英语), 更新检查 (固定 GitHub Releases API, 12 小时间隔, 超时 / 取消 / 失败提示 / 忽略版本, Neutral 打开内置发行历史, Positive 打开发布页); 与 3-Stove Agent 实现同源.
- [ ] (测试) JVM: 版本比较, 忽略版本, 频率限制; instrumentation: 关于页与发行历史页可打开并显示当前版本条目.

### P5.3 启动器与图标

- [ ] (插件) 四个 alias (`AdaptiveLightIconAlias`, `AdaptiveDarkIconAlias`, `AdaptiveAutoIconAlias` 默认启用, `TransparentIconAlias`) 指向 `LauncherActivity`, `MAIN` / `LAUNCHER` 只在 alias 上; `LauncherIcons` 切换逻辑 (`PackageManager.setComponentEnabledSetting`, 同一时刻恰好一个启用); Three 的 `ic_launcher` 保持透明 BitmapDrawable, 不创建同名自适应 XML.
- [ ] (测试) instrumentation: 四 alias 恰好一个启用, 切换后 `queryIntentActivities(MAIN/LAUNCHER)` 命中唯一组件; 真机截图 (API 35 HyperOS 与 API 24 AVD) 复核光学居中, 必要时调整 `OPTICAL_X` / `OPTICAL_Y` 并重新生成 (`--check` 通过), 结果回填第 4.4 节.

验收条件: 设置页全部项可用且与宿主外观跟随一致; 插件中心 "设置" 入口可达; 四 alias 切换正确; 发行历史与更新检查可用 (更新检查在未推送前只验证失败 / 超时路径).

---

## P6: 健壮性, 兼容矩阵, 性能与体积

### P6.1 健壮性

- [ ] (插件) 插件进程被系统杀死 (有会话时前台服务保活; 无会话时正常退出) 与用户从最近任务划掉 (会话保留, 通知仍在); 宿主进程死亡时订阅写端与回调注册自动清理 (`linkToDeath`), 不影响会话; Node.js 插件在会话运行中更新 / 卸载: 已运行的 node 进程不受影响, 新会话刷新符号链接或退化为纯 shell.
- [ ] (插件) 大输出 (`cat` 100 MiB 文件, `yes`) 下界面保持可响应, 环形缓冲溢出只影响订阅者; 100 个会话快速创建 / 关闭无句柄泄漏 (`ls /proc/<pid>/fd | wc -l` 前后一致); 旋转 / 分屏 / 键盘弹出下 pty 尺寸同步.
- [ ] (测试) instrumentation 覆盖上述可自动化部分; 手动项写入 `docs/dev/p6-robustness-evidence.md`.

### P6.2 兼容矩阵

- [ ] (测试) D32 六台设备 / AVD 各执行: 安装对应 ABI 变体 + universal 各一次, 新建会话, `echo`, 共享存储授权与 `cd /sdcard`, Node.js 三命令 (已安装设备), 宿主三入口, 脚本示例一个, 前台服务通知; API 24 的 `startService` 分支与通知渠道缺失路径; API 37 AVD 的 16 KB 页 (`verifyNativePageAlignment` + 实机加载); HyperOS 的后台启动与通知限制记录.
- [ ] (文档) 矩阵表写入 `docs/dev/p6-compat-matrix.md` (设备 / API / ABI / APK 变体 / 结果 / 备注).

### P6.3 性能与体积

- [ ] (插件) 会话创建到首个提示符 < 500 ms (API 35 真机, 排除 Node 归档首次解压); 首次 Node 归档解压耗时与大小记录; universal APK 与各 ABI APK 大小记录 (预期 universal < 6 MiB: Material + 两库 x 4 ABI 共约 60 KB 原生); R8 开启 (`isMinifyEnabled = true`) 且 `jackpal.androidterm.**` 规则保留 JNI 类.
- [ ] (测试) 数值写入本节证据; 回归阈值写入 `AGENTS.md` 第 14 节.

验收条件: 矩阵表无未解释的失败; 健壮性手动清单全部通过; 体积与耗时数值落档.

---

## P7: 文档, 发布与 1.0.0 本地 gate

目标: 四个关联仓库同步, 插件 README / changelog 定稿, 本地 gate 通过; 推送 / 索引 / Release 按 D9 门控.

### P7.1 文档与声明

- [ ] (文档) `D:/webstorm-projects/AutoJs6-Documentation`: `api/terminal.md` (模块页, 结构参照 `api/epub.md` / `api/installer.md`: 插件依赖说明, `PLUGIN_UNAVAILABLE`, 同步 / Async / 会话三形态, `terminal` 与 `$terminal`, 与 `shell()` 的区别与各自适用场景互相提示), `api/terminalSessionType.md`, `api/terminalOpenOptionsType.md`, `api/terminalErrorType.md` (或合并进模块页, 按既有粒度); `api/shell.md` 增加指向 `terminal` 的提示; `api/sidebar.md` / `api/toc.md` 登记; 从 `generator/` 运行 `python auto-generate.py terminal shell` 只生成涉及页面, 提交文档仓库; 离线文档插件同步按既有策略另行决定.
- [ ] (文档) `D:/webstorm-projects/AutoJs6-TypeScript-Declarations`: `declarations/autojs6/aj6-int-terminal.d.ts` (`@Source` 指向宿主 `runtime/api/augment/terminal/*.kt` 与 `runtime/api/terminal/*.kt`, `Internal.Terminal` 命名空间, 重载与事件类型), `index.d.ts` 引用; `aj6dts.bat -Publish -DeclarationsOutput ... -ApiRoots ...`; Ace 插件同步 `aj6-int-terminal.d.ts` (LF), 两仓库版本号 +1 且版本名称按语义升级 (新增模块 -> y+1), Ace 执行 `:app:generateAutoJs6LspDeclarations`; 分别提交.
- [ ] (兄弟) `AutoJs6-Plugin-NodeJs-Runtime/docs/nodejs/TERMINAL.md` 与 `Roadmap.md` M21 注记: 消费者由 "AutoJs6 宿主 (宿主 uid)" 改为 "3-Shell Terminal 插件 (插件 uid)", 宿主侧布局段落改为插件侧; 不改契约; 本地提交.
- [ ] (宿主) `docs/dev/terminal-plugin-protocol-v1.md` 补齐脚本 API 与会话流章节; 宿主 changelog 核对 (P1.4 / P4.3 条目合并整理, 日期为当日).

### P7.2 插件 README 与 changelog

- [ ] (插件) `.readme/lang_*.json` 10 语言: 简介, 功能 (多会话 / 后台运行 / 快捷键栏 / 文本选择 / Node.js 命令 / npm 镜像源 / 包管理菜单), 安装 (插件中心向导或 Release), Node.js 说明 (需要 Node.js Runtime 1.5.0+, W^X 限制, corepack 默认版本), 存储权限说明 (为什么需要全部文件访问, 不授权时的行为), 脚本示例 (`terminal.open`, `exec`, `npm.run`, `session.on('output')`), 兼容性 (Android 7.0+, 16 KB 页, ABI 变体选择), 常见问题 (进程以插件 uid 运行 / `EACCES` / 会话不可跨进程恢复 / HyperOS 后台限制), 发行历史, 许可证与第三方声明; 生成器 `--check` 通过.
- [ ] (插件) `.changelog` 10 语言 `v1.0.0` 定稿 (`feature` / `improvement` / `dependency`, 依赖用 `附加` 记录 common-plugin-api / nodejs-api / terminal-api / jackpal Android-Terminal-Emulator).

### P7.3 本地发布 gate

- [ ] (发布) 平台验收构建 (Temurin 参数) + `:app:testDebugUnitTest` + `:app:assembleDebugAndroidTest` + `:app:lintDebug` + `:app:verifyNativePageAlignment` + `:app:appendDigestToReleasedFiles` (5 个签名 APK, CRC32 文件名); `git diff --check`; `VERSION_BUILD == git rev-list --count HEAD`; 工作树干净; 宿主与兄弟仓库同样工作树干净.
- [ ] (发布, D9 门控) GitHub 仓库 `SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal` (功能性描述, 例如 `Terminal plugin for AutoJs6 with multi-session pty shells and Node.js commands`), 推送, tag `v1.0.0`, Release 附 5 个 APK 与 SHA-256; 维护者明确恢复推送后执行.
- [ ] (索引, D9 门控) `official-repositories.json` 插入仓库名 (字母序, 计数同步), `release-manifests/io.github.supermonster003.autojs6.plugin.three.shell.terminal/<versionCode>.json` (多产物 `artifacts`), 本地运行生成器验证后提交推送 `main`.
- [ ] (宿主, D9 门控) 宿主提交 (`feat(terminal): ...` 系列) 推送按维护者指示.

验收条件: 四个关联仓库已本地提交; 本地 gate 全部通过; 门控条目在解锁前保持未勾选并注明原因.

---

## P8: 1.1.0 候选

- [ ] (插件 + 宿主) Explorer Action v2 目录动作 (D33 / Q6): 插件声明 `TARGET_DIRECTORY` 动作 "在终端中打开", 宿主删除 `menu_dir_options.xml` 的硬编码项与 `ExplorerPageViewHolder` 的分支; 保留项目工具栏与抽屉的 `TerminalLauncher` 路径.
- [ ] (插件) 配色方案 (至少 深 / 浅 / 跟随主题色 三套) 与等宽字体选择 (系统等宽 / 内置一款 OFL 字体); 字号与配色同步到设置页.
- [ ] (插件) 命令完成通知: `exec` 的非交互会话退出时发通知 (可在设置关闭); 通知点击打开对应会话.
- [ ] (插件) Node.js 插件签名的 "信任此签名" 确认流程 (D34 Q3): 探测详情页对非官方签名显示 SHA-256 与确认按钮, 确认后持久化到插件偏好, 设置页可撤销.
- [ ] (插件) 多标签页 UI 评估 (当前为多 Activity 实例 + 管理器); 评估结论写入附录 D.
- [ ] (插件) 共享存储外的 `content://` 目录 (SAF) 评估: 终端只能操作真实路径, 结论预计为不支持, 文档注明.

---

## 附录 A: 脚本 API 草案

### A.1 全局对象

```ts
declare const terminal: Terminal;
declare const $terminal: Terminal;

interface Terminal {
    // 第一档: 会话管理
    open(dir?: string): TerminalSession;
    open(options?: TerminalOpenOptions): TerminalSession;
    openAsync(dirOrOptions?: string | TerminalOpenOptions): Promise<TerminalSession>;
    sessions(): TerminalSession[];
    sessionsAsync(): Promise<TerminalSession[]>;
    session(id: string): TerminalSession | null;
    close(id: string): boolean;
    closeAsync(id: string): Promise<boolean>;
    closeAll(): number;
    closeAllAsync(): Promise<number>;
    show(id?: string): void;             // 无 id 时打开管理器或最近会话
    showAsync(id?: string): Promise<void>;
    state(): 'available' | 'not_installed' | 'disabled' | 'incompatible' | 'unavailable';
    isAvailable(): boolean;
    isAvailableAsync(): Promise<boolean>;

    // 第二档: 可见执行
    exec(command: string, options?: TerminalExecOptions): TerminalSession;
    execAsync(command: string, options?: TerminalExecOptions): Promise<TerminalSession>;
    npm: {
        run(script: string, dir?: string, options?: TerminalExecOptions): TerminalSession;
        runAsync(script: string, dir?: string, options?: TerminalExecOptions): Promise<TerminalSession>;
    };
    env(): TerminalEnvironmentInfo;
    envAsync(): Promise<TerminalEnvironmentInfo>;

    TerminalError: typeof TerminalError;
}
```

### A.2 选项

```ts
interface TerminalOpenOptions {
    cwd?: string;                 // 默认脚本工作目录 (files.cwd()), 遵循 files.path 规则, 拒绝 content://
    env?: Record<string, string>; // <= 64 项, 每项 <= 4 KiB, 不可覆盖 HOME / PREFIX / PATH
    show?: boolean;               // 默认 true: 打开终端界面; false 仅创建后台会话
    title?: string;               // 会话标题 (管理器与通知显示), 默认目录名
}

interface TerminalExecOptions extends TerminalOpenOptions {
    keepOpen?: boolean;           // 默认 true: 命令结束后进入交互 shell; false: 命令结束即关闭会话
    wait?: boolean;               // 默认 false; true 时同步等待 exit 并把 exitCode 挂到返回会话
    timeout?: number;             // wait 的超时 (ms), 默认 600000, 0 不限
    stripAnsi?: boolean;          // output 事件是否剥离 ANSI 序列, 默认 true
}
```

### A.3 会话 (EventEmitter)

```ts
interface TerminalSession {
    readonly id: string;
    readonly cwd: string;
    readonly title: string;
    readonly createdAt: number;
    readonly exitCode: number | null;  // exec({ wait: true }) 或 exit 事件后可用
    isAlive(): boolean;
    write(text: string): void;         // 发送文本 (含 \n 才会执行), <= 64 KiB
    write(bytes: number[]): void;      // 原始字节 (例如 [3] = Ctrl+C)
    show(): void;
    close(): boolean;
    waitFor(pattern: string | RegExp, timeout?: number): string;   // 返回匹配行, 超时 TIMEOUT
    transcript(maxBytes?: number): string;                         // 当前屏幕与回滚缓冲文本, <= 1 MiB
    on(event: 'output', listener: (line: string) => void): this;
    on(event: 'exit', listener: (code: number) => void): this;
    on(event: 'overflow', listener: (droppedBytes: number) => void): this;
    once(...): this; off(...): this; removeAllListeners(event?: string): this;
}
```

### A.4 环境信息与错误

```ts
interface TerminalEnvironmentInfo {
    home: string; prefix: string; shell: string;
    node: { available: boolean; version?: string; packageName?: string; pluginVersion?: string; reason?: string };
    npmRegistry: string | null;
    storageAccess: 'granted' | 'denied' | 'not_applicable';
    pluginVersion: string; contractVersion: number;
}

class TerminalError extends Error {
    readonly code: TerminalErrorCode;   // 附录 B.4
    readonly sessionId?: string;
}
```

### A.5 示例

```js
// 在终端中打开当前脚本目录
terminal.open(files.cwd());

// 可见地安装依赖, 等待结束
let s = terminal.exec('npm install', { cwd: '/sdcard/脚本/my-project', wait: true, timeout: 0 });
toastLog(`npm install 退出码 ${s.exitCode}`);

// 会话驱动: 自动回答提示
let t = terminal.exec('npm init', { cwd: dir, show: true });
t.on('output', line => { if (/package name/i.test(line)) t.write('\n'); });
t.waitFor(/Is this OK\?/i, 60e3); t.write('yes\n');
t.on('exit', code => console.log('done', code));
```

---

## 附录 B: 契约草案 (`plugin-api/terminal-api`)

### B.1 Binder 面 (V1, 顺序冻结)

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `ITerminalPlugin` | `PluginInfo getInfo()` | 与 INFO 服务一致 |
| | `Bundle getCapabilities()` | `CONTRACT_VERSION`, `FEATURES` (String[]), `MAX_SESSIONS`, `MAX_SUBSCRIPTIONS`, `NODE_CLI` (状态字串) |
| | `String listSessions()` | `sessionsJson` (数组, 每项 B.2 会话字段) |
| | `String openSession(String requestJson)` | `cwd`, `command?`, `env?`, `title?`, `keepOpen?`; 返回会话 JSON 或错误 JSON |
| | `boolean closeSession(String sessionId)` | |
| | `int closeAllSessions()` | 返回关闭数 |
| | `void writeInput(String sessionId, in byte[] data)` | <= 64 KiB |
| | `Bundle subscribeOutput(String sessionId, String optionsJson)` | 返回 `subscriptionId` + 只读 `ParcelFileDescriptor` (`fd`) |
| | `void unsubscribeOutput(String subscriptionId)` | |
| | `Bundle readTranscript(String sessionId, int maxBytes)` | `text`, `truncated` |
| | `String getEnvironment()` | A.4 的 JSON |
| | `void registerCallback(ITerminalCallback cb)` | <= 8, death 自动移除 |
| | `void unregisterCallback(ITerminalCallback cb)` | |
| `ITerminalCallback` | `void onSessionsChanged(int count, String sessionsJson)` | oneway |
| | `void onSessionExited(String sessionId, int exitCode)` | oneway |
| | `void onOutputOverflow(String sessionId, String subscriptionId, long droppedBytes)` | oneway |

### B.2 Intent 与 JSON 键

- Intent `org.autojs.plugin.TERMINAL_OPEN` extras (`TerminalContract.EXTRA_*`): `directory` (String), `sessionId` (String), `newSession` (boolean), `command` (String), `manager` (boolean).
- 会话 JSON: `id`, `cwd`, `title`, `createdAt`, `alive`, `exitCode?`, `pid`.
- 错误 JSON: `code`, `message`, `sessionId?`.
- 订阅选项 JSON: `fromStart` (是否先推送当前转录, 默认 false).

### B.3 线程与生命周期

- Binder 方法全部即时返回 (< 50 ms 目标), 不在 Binder 线程做 I/O 等待; 归档解压在会话创建的工作线程, `openSession` 在解压完成前返回 `pending` 状态的会话, 完成后 `onSessionsChanged`.
- 宿主 Binder 死亡: 关闭其订阅写端, 移除回调; 会话本身不受影响 (用户可见资产).
- 插件进程被杀: 会话全部丢失, 宿主下次发现得到空列表; 前台服务存在时系统通常不杀.

### B.4 错误码

| 码 | 含义 |
| --- | --- |
| `PLUGIN_UNAVAILABLE` | 宿主侧: 插件缺失 / 禁用 / 未授权 / 绑定失败 (消息区分) |
| `PLUGIN_INCOMPATIBLE` | 宿主侧: 契约版本或最低宿主版本不满足 |
| `INVALID_ARGUMENT` | 形态或上限不满足 |
| `SESSION_NOT_FOUND` | 会话不存在 |
| `SESSION_LIMIT` | 超过 16 个会话 |
| `SESSION_CLOSED` | 写入或订阅已退出的会话 |
| `DIRECTORY_INACCESSIBLE` | 目录不存在 / 非目录 / 无权限 (非共享存储原因) |
| `STORAGE_PERMISSION_REQUIRED` | 共享存储目录且未授权全部文件访问 |
| `PTY_FAILED` | `/dev/ptmx` 或子进程创建失败 |
| `NODE_UNAVAILABLE` | `npm.run` 等需要 Node 的调用在纯 shell 态 |
| `SUBSCRIPTION_LIMIT` | 单会话第 5 个订阅 |
| `TIMEOUT` | `waitFor` / `wait` 超时 |
| `CANCELLED` | 脚本结束或显式取消 |
| `INTERNAL` | 其它 |

### B.5 上限常量 (D22)

`MAX_SESSIONS = 16`, `MAX_COMMAND_BYTES = 65536`, `MAX_INPUT_BYTES = 65536`, `MAX_ENV_ENTRIES = 64`, `MAX_ENV_ENTRY_BYTES = 4096`, `MAX_PATH_BYTES = 4096`, `MAX_JSON_BYTES = 65536`, `MAX_CALLBACKS = 8`, `MAX_SUBSCRIPTIONS_PER_SESSION = 4`, `MAX_TRANSCRIPT_BYTES = 1048576`, `OUTPUT_BUFFER_BYTES = 1048576`.

### B.6 版本协商

- `CONTRACT_VERSION` 从 1 起; 宿主读取能力 Bundle 后按版本启用方法; 新方法追加到 AIDL 末尾, `TerminalAidlOrderTest` 冻结顺序.
- `REQUIRED_HOST_VERSION_CODE` 为交付 `terminal-api` 的宿主构建号; 插件 `requiresHostVersion` meta-data 与之一致.

---

## 附录 C: 宿主改动清单 (按文件)

| 文件 | 改动 | 阶段 |
| --- | --- | --- |
| `settings.gradle.kts` | `pluginApi` 增加 `terminal-api`; 三个 jackpal `libs` 条目保留 | P1.1 |
| `plugin-api/terminal-api/**` | 新模块 (2 个 AIDL, 5 个常量文件, 2 个测试) | P1.1 |
| `app/build.gradle.kts` | 依赖 `:plugin-api:terminal-api`; L586-588 三份 AAR 保留 | P1.1 |
| `app/src/main/AndroidManifest.xml` | 删除 `TerminalActivity` (L543-) 与 `TerminalSessionService` (L607-); `<queries>` 增加 `org.autojs.plugin.TERMINAL` 服务与 `TERMINAL_OPEN` / `TERMINAL_SETTINGS` activity intent | P1.2 / P1.3 |
| `core/terminal/**` (17), `ui/terminal/**` (7), `ui/settings/Terminal*Preference.kt` (3), `jackpal/androidterm/PtyBridge.java`, `jackpal/androidterm/emulatorview/TerminalSelectionSnapshot.java` | 删除 (已迁入插件) | P1.3 |
| `app/src/test/.../core/terminal/**` (10), `androidTest/.../core/terminal/TerminalSessionsInstrumentationTest.kt` | 删除 (已迁入插件) | P1.3 |
| `core/plugin/terminal/**` | 新增 `TerminalPluginHost`, `TerminalLauncher`, `TerminalPluginStateMonitor`, `TerminalJson`, `TerminalErrorMapper`, `ThreeShellTerminalOfficialPlugin`, `TerminalLegacyCleanup` | P1.2 / P1.3 |
| `App.kt` | 启动后调度 `TerminalLegacyCleanup` | P1.3 |
| `ui/main/drawer/DrawerFragment.kt:726-757, 936` | 抽屉项改走 `TerminalLauncher` + 状态监听 | P1.2 |
| `ui/explorer/ExplorerPageViewHolder.kt:238-240`, `ui/explorer/ExplorerProjectToolbar.java:26, 187-194` | 走 `TerminalLauncher.open` | P1.2 |
| `ui/devplugin/ConnectionManagerDialog.kt:273`, `res/layout/connection_manager_client_item.xml` | 改用 `text_close_session` | P1.2 |
| `core/plugin/center/{PluginCenterViewModel,InstalledPluginRepository,PluginInstalledIconResolver,PluginSettingsFragment}.kt`, `core/plugin/center/wizard/PluginInstallWizardCatalog.kt` | engine `terminal` 的发现, 透明图标, 设置入口, 向导条目 | P1.2 |
| `res/layout/{activity_terminal,include_terminal_node_banner}.xml`, `res/menu/menu_terminal.xml`, `res/values/ids_terminal.xml` | 删除 | P1.3 |
| `res/values*/strings.xml` (10 目录) | 删除约 55 条终端字符串; 改写 `description_terminal`; 新增 `text_close_session` 与四条五态文案 | P1.2 / P1.3 |
| `res/values/strings_donottranslate.xml`, `res/values/arrays.xml` | 删除终端偏好键与数组 | P1.3 |
| `res/xml/fragment_preferences.xml:186-211`, `res/xml/fragment_developer_options.xml:97-101` | 删除 | P1.3 |
| `res/drawable/ic_terminal_black_48dp.xml`, `res/menu/menu_dir_options.xml`, `res/layout/explorer_project_toolbar.xml`, `res/raw/licenses.xml:81-84` | 保留 | - |
| `runtime/api/Shell.java`, `com/stardust/autojs/core/util/Shell.java`, `apkbuilder/ApkBuildLibraryCatalog.kt`, `plugins/apk-builder-template/.../RemoteApkLightweightBuilder.kt`, `app/proguard-rules.pro:45` | 不动 (D2) | - |
| `runtime/ScriptRuntime.kt` | `terminal: TerminalService` 字段, `close`, `Terminal(this, terminal).augment(target)` | P4.1 |
| `runtime/api/terminal/**`, `runtime/api/augment/terminal/**` | 新增 | P4.1 / P4.2 |
| `assets-app/sample/终端/*.js` | 三个示例 | P4.3 |
| `docs/dev/terminal-plugin-protocol-v1.md` | 新增 | P1.4 / P4.3 / P7.1 |
| `docs/dev/evidence/terminal-*-20260917.md` | 保留为历史证据, 文首加注 "已迁出到 3-Shell Terminal 插件" | P1.4 |
| `.changelog/lang_*.json` (10), `README.md` / `.readme/*` (生成) | D10 改写 + 新条目 | P1.4 / P4.3 / P7.1 |
| `version.properties` | `VERSION_BUILD` 递增 | 每次提交 |

---

## 附录 D: 待决事项

### Q1 (已按核实结果处理): 宿主 emulatorview AAR

- 现状: 维护者选 "仅移出 emulatorview"; 核实 `ShellTermSession -> GenericTermSession -> emulatorview.TermSession` 与 `Shell.java` 的直接 import 后, 该选项与 "三份全部保留" 等价.
- 处理: D2 记为三份全部保留, 只删终端代码 / 资源 / 组件. 若维护者仍希望缩减, 可选项是把 emulatorview 中 `EmulatorView` 及渲染器类从宿主副本剔除 (重打包 AAR), 收益约几十 KB, 不推荐.
- 拍板 (2026-10-01): 接受, 放弃缩减收益 (D34).

### Q2 (P0 前): x86 ABI 是否打包

- 现状: AAR 含 x86 原生库 (2017 年上游构建, 非宿主重建); CI 用 API 24 x86 模拟器.
- 推荐: 打包 x86 (D14), 使 CI 契约测试能加载原生库; 若 `verifyNativePageAlignment` 对 x86 旧库报错, 用 `native/jackpal-termexec/build.py` 重建 x86 并更新锁.
- 拍板 (2026-10-01): 按推荐实施 (D34).

### Q3 (P2 前): Node.js 插件信任的手动覆盖

- 现状: D17 只接受官方签名或自身签名.
- 推荐: 1.0.0 不提供手动信任开关; 探测详情页显示被拒签名的 SHA-256 便于排查. 若维护者需要第三方构建的 Node.js 插件, 1.1.0 增加 "信任此签名" 确认流程.
- 拍板 (2026-10-01): 按推荐实施; "信任此签名" 确认流程列入 P8 (D34).

### Q4 (P2 前): `openSession` 的 `command` 执行方式

- 现状: 宿主只有交互 shell + 包装脚本.
- 推荐: `command` 非空时命令为 `sh -c 'cd -- "$1" 2>/dev/null || cd "$HOME"; <command>; <keepOpen ? exec "$0" : exit $?>' sh <cwd>`, 命令按原文嵌入 (由脚本作者负责引号), 长度受 D22 限制.
- 拍板 (2026-10-01): 按推荐实施 (D34).

### Q5 (P4 前): `terminal.show()` 在后台脚本中的行为

- 现状: Android 10+ 禁止后台启动 Activity, 宿主通常持有悬浮窗权限 (系统允许例外).
- 推荐: 不做特殊处理, 行为与 `app.startActivity` 一致, 失败时抛 `TerminalError` code `INTERNAL` 并附系统原因; 文档注明.
- 拍板 (2026-10-01): 按推荐实施 (D34).

### Q6 (P8 前): Explorer Action v2 目录动作

- 现状: D33; 契约已有 `TARGET_DIRECTORY`.
- 推荐: 1.1.0 迁移, 迁移后宿主只剩抽屉与项目工具栏两处硬编码入口.
- 拍板 (2026-10-01): 按推荐实施 (D34).

### Q7 (P1 前): 删除宿主终端的时机

- 现状: 维护者本机依赖宿主终端做日常操作的可能性.
- 推荐: P1.3 在 P3 完成后执行 (阶段总览已注明); 若维护者接受短期无终端, 可随 P1.2 一并执行以减少两套代码并存.
- 拍板 (2026-10-01): 接受短期无终端, P1.3 随 P1.2 执行 (D34).

---

## 附录 E: 证据等级, 设备池与退路

### E.1 证据等级

| 标签 | 可以证明 | 不能证明 |
| --- | --- | --- |
| `SOURCE` | 源码存在, 结构符合设计 | 编译或行为正确 |
| `JVM` | Android-free 逻辑的单元测试 (JUnit4) | Binder / pty / 真机行为 |
| `ANDROID_BUILD` | `assembleDebug` / `testDebugUnitTest` / `lintDebug` / `assembleRelease` / `verifyNativePageAlignment` 通过 | 真机行为 |
| `BINDER` | 指定设备上的 instrumentation: 发现, 绑定, 往返, 敌意输入, 管道 | 其它设备 / OEM 行为 |
| `DEVICE` | 指定设备与 API / ABI 上的真实会话, 存储授权, Node 命令, 宿主入口 | 未列出设备 / API / ABI |
| `DOCS` | README (10 语言), changelog, 协议文档, 文档 / d.ts / Ace / 离线文档已同步且版本号已更新 | - |
| `RELEASE` | 签名 APK x 5, CRC32 文件名, (解锁后) GitHub Release 与官方索引 | 未明确覆盖的设备 |

条目勾选时在其后追加证据, 格式示例: `[x] ... (JVM: StorageAccessTest 7 用例; DEVICE: Xiaomi 23046RP50C / API 35 / arm64-v8a, AVD API 24 / x86, 2026-10-xx; commit abc1234)`.

当前可用设备池 (以当日 `adb devices -l` 为准, 端口与 API 的映射每次重新读取): Xiaomi 23046RP50C (API 35, HyperOS), Sony G8441 (API 28), Sony XQ-AT72 (API 31), Redmi 22120RN86C (API 33), AVD API 24 (x86) / API 37 (16 KB 页). Node.js Runtime 已安装的设备以 `pm list packages io.github.supermonster003.autojs6.plugin.nodejs` 当日确认.

### E.2 退路: 原生库重建

触发条件: `verifyNativePageAlignment` 对任一 ABI 失败, 或某设备加载 `.so` 失败. 形态: 用 `native/jackpal-termexec/build.py` (宿主配方副本, NDK 28.2.13676358, platform 24) 重建全部 ABI, 更新 `libs/jackpal/libtermexec-1_0.aar`, `locks/vendored-aars.lock` 与 `provenance.json`, 在 changelog `dependency` 记录.

### E.3 退路: 入口协议

触发条件: 某 OEM 在宿主前台时仍拒绝显式启动插件 Activity (预期不会发生, 显式 Intent + 前台调用方是标准路径). 形态: `ITerminalPlugin` 末尾追加 `PendingIntent getOpenPendingIntent(String requestJson)`, 宿主以 `ActivityOptions.setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)` 发送; 契约版本 +1.

---

## 附录 F: 参考与许可证边界

- jackpal Android-Terminal-Emulator (`https://github.com/jackpal/Android-Terminal-Emulator`, Apache-2.0, commit `35188f8a8b57989a4a4ec9485e11187b46be26d9`): term / emulatorview / libtermexec 三份 AAR 与原生库来源; 宿主 `libs/jackpal-androidterm-libtermexec-1_0/native-build/` 为 16 KB 重建配方.
- AutoJs6 宿主 (MPL-2.0): 迁入的 `core/terminal`, `ui/terminal`, 桥接类与测试; `docs/dev/evidence/terminal-{managers,manager-refinements,console-node-media}-20260917.md` 为内置终端时期的设计与验收记录.
- AutoJs6-Plugin-NodeJs-Runtime (`docs/nodejs/TERMINAL.md`, `Roadmap.md` M21 / M21.2): `NODE_CLI` 契约与启动器事实.
- 宿主先例: `docs/dev/official-plugin-settings-contract-v1.md`, `docs/dev/installer-plugin-protocol-v1.md`, `docs/dev/readium-epub-reader-plugin-integration.md`; 兄弟仓库 `AutoJs6-Plugin-Three-Setup-Installer/ROADMAP.md`, `AutoJs6-Plugin-Three-Stove-Agent/ROADMAP.md`.
- 许可证边界 (D31): 本仓库 MPL-2.0; Apache-2.0 组件保留 LICENSE / NOTICE 并在 `THIRD_PARTY_NOTICES.md` 与关于页列出; 不引入 GPL 代码.

---

## 会话记录

### 2026-10-01

- 完成: 宿主终端现状盘点 (核心 17 / 界面 7 / 设置 3 / 桥接 2 个文件共 3442 行, 测试 1068 行, 布局 / 菜单 168 行, 62 条字符串 x 10 语言, 偏好键 15 个, Manifest 两组件, 三处界面入口与两处设置入口); jackpal 依赖核实 (三份 AAR 均被脚本 `shell()` 链路使用, 宿主不能删除任何一份); Node.js Runtime `NODE_CLI` 契约核实 (schema 1, 文档 `TERMINAL.md`); 兄弟仓库范式盘点 (3-Setup Installer 骨架与路线图格式, 3-Stove Agent 图标与设置套件, Node.js Runtime ABI 拆分, 宿主 Screen Color Picker / MCP Server / EPUB 三种集成范式); 维护者三轮拍板 D1-D11 (其中 D2 按核实结果修正为三份全部保留); 派生 D12-D33; 图标源图入库并改名为 `three-shell-ic-launcher-{light,dark}.png` (1254 x 1254, alpha 一致, `#272727` / `#D8D8D8`, 包围盒 746 x 653); 本 Roadmap 与 `AGENTS.md`; `git init` 与首笔提交 `cd5136a` (build 1).
- 未做: 仓库 Gradle / Manifest / 资源 / CI 骨架 (P0.1 其余条目), spike (P0.2), 任何宿主改动, 任何兄弟仓库改动.
- 维护者拍板 (同日稍后): 附录 D Q1-Q7 全部按推荐实施, Q7 接受短期无终端; 回填为 D34.
- 下次会话建议起点: P0.1 全部条目 (Gradle 骨架, jackpal 三份 AAR 入库与锁, 4 ABI 拆分, Manifest, 资源与图标生成, 文案源, 测试, CI) + P0.2 spike (pty / 共享存储 / Node 启动器); spike 通过后同一会话可开始 P1.1 契约模块.
- P0.1 (同日第二段): Gradle 骨架 (`build-logic`, 平台插件 1.8.3, 四 ABI + universal, 双 AAR 锁, 五产物摘要任务), jackpal 三份 AAR 与原生配方入库, Manifest / 身份常量 / INFO 服务 / 占位 TERMINAL Binder / `NativeLibraryInventory` (`supportedAbis` 由打包的 pty 库推导), 10 语言字符串与 15 个图标资源, 10 语言 README / changelog 源与模板改写 (`node_points`), `THIRD_PARTY_NOTICES.md` / `libs/README.md`, 6 个 JVM 测试类 (25 项) 与 instrumentation 契约测试 (6 项), CI 两工作流; 验证: JVM 25/25, 契约测试在 API 24 x86 / API 37 x86_64 16 KB / Xiaomi Pad API 35 arm64 三台 6/6, `assembleRelease` + `verifyNativePageAlignment` + lint (0 error) + `appendDigestToReleasedFiles` 5 产物, 宿主 debug 5304 在 16 KB AVD 上经 `InstalledPluginRepository.discoverInstalled` 发现 `engine=terminal` / `supportedAbis=[x86_64]` / `nativePageAlignment=16384` (插件中心可识别; 启用与图标 allowlist 的界面确认留待 P1.2). 过程发现: 宿主 `useLegacyPackaging = false` 时原生库不解压, 测试须以 `System.loadLibrary` 验证; AndroidX startup 带入一个非导出 provider; R8 需 `-dontwarn kotlinx.parcelize.Parcelize`; 宿主 `RunIntentActivity` 读 `/sdcard` 脚本前需 `appops set org.autojs.autojs6 MANAGE_EXTERNAL_STORAGE allow`.
- P0.2 (同日第三段): `PtyBridge.java` 迁入 + instrumentation `spike/P0SpikeTest` 三用例; 五台设备 (API 24 x86 AVD / Sony API 28 / Redmi API 33 / Xiaomi Pad API 35 / API 37 x86_64 16 KB AVD) pty 全部 exit 7, 存储按插件自身授权状态切换 (API < 30 `Permission denied`, API 30+ `Operation not permitted`), Node 启动器在三台真机 `v24.21.0` 且签名命中官方集合; `docs/dev/p0-spike-evidence.md` 写入; D15-D18 无需修订; 测试安装已从三台真机卸载, AVD 上保留. P0 关闭, 下次会话起点 P1.1 (宿主 `plugin-api/terminal-api` 契约模块).
- P1.1 (同日第四段): 宿主提交 `b8f4d6c939` 新建 `plugin-api/terminal-api` (2 AIDL + 5 常量文件 + 2 测试类 9 项, 仅自有文件入库, 另一会话的未提交改动未触碰); release AAR (`98b43a9deb236d642f0d3b18696a4da9a9c4b399374d6e398d80d3abf0c86bc6`) 入插件 `libs/` 并锁定, 插件身份常量改由契约提供, `requiresHostVersion` 5303 -> 5304; 验证: 插件 JVM 25/25, debug 构建 + lint 0 error, 契约 instrumentation 在 16 KB AVD 6/6, 宿主 5304 发现与绑定正常. P1.1 关闭, 下次会话起点 P1.2 (宿主 `core/plugin/terminal/*` 客户端, 入口改接, 插件中心注册).
