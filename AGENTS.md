# AutoJs6-Plugin-Three-Shell-Terminal AGENTS.md

本文件是本仓库的工程约定, 由 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么". 同目录的 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` (Three 系列命名), `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` (独立设置页) 与 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标) 在对应能力落地时同样适用.

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库包含原生库 (jackpal libtermexec 的两个 `.so` x 4 ABI, 来自宿主的 16 KB 重建产物) 与 ABI 拆分; 参考规范中原生库, ABI 拆分, 16 KB 页对齐与原生库重建配方的 CONDITIONAL 条款全部适用 (第 5.4 节, 第 9 节).
- 本仓库拥有: 运行在插件进程中的 pty shell 会话与前台服务 (路线图 P2), 共享存储访问权限 (P2.2), 对 Node.js Runtime 插件二进制的跨包执行与签名信任 (P2.3), 宿主调用的导出入口 Activity (P3.3), 独立设置页与发行历史 (P5). 这些条款以 CONDITIONAL 形式保留在第 6, 9, 12 节.
- 没有特权进程 (Shizuku / Root), 没有隐藏 API, 没有 Compose.
- 插件只在用户手动检查发行版本 (P5.2) 时访问固定的 GitHub Releases API; shell 子进程 (例如 `npm install`) 的网络访问由用户命令决定, 使用本插件的 `INTERNET` 权限.

## 2. 仓库身份

下列值在 Gradle, Manifest, Kotlin 常量 (`ThreeShellTerminalPlugin`), 资源, 文档, 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `ManifestContractTest` 与 `ThreeShellTerminalPluginRuntimeInfoTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-Three-Shell-Terminal` |
| `rootProject.name` | `autojs6-plugin-three-shell-terminal` |
| 应用标题 (不可翻译) | `3-Shell Terminal` (机器标识写 `three` / `Three`, 面向人的文本写 `3-Shell Terminal`, 宿主向导回退标题写 `Three Shell Terminal`) |
| `applicationId` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.shell.terminal` |
| 插件 ID / engine / variant | `three-shell-terminal` / `terminal` / `default` |
| Binder 服务类 | `ThreeShellTerminalPluginService` (主进程, 与终端界面和会话注册表同进程) |
| 服务发现 action / category | `org.autojs.plugin.TERMINAL` / `terminal` |
| INFO 服务 | `ThreeShellTerminalPluginInfoService`, action `org.autojs.plugin.INFO`, category `terminal` |
| 界面入口 action (P3.3) | `org.autojs.plugin.TERMINAL_OPEN` (`ThreeShellTerminalEntryActivity`, 导出, PLUGIN 权限) |
| 设置入口 action (P5.1) | `org.autojs.plugin.TERMINAL_SETTINGS` |
| 前台服务 / 通知渠道 | `ThreeShellTerminalSessionService` (`specialUse`) / `three.shell.terminal.sessions` |
| 宿主契约标识 | AIDL 包 `org.autojs.plugin.terminal.api`, 契约类 `TerminalContract` / `TerminalIds` / `TerminalActions` / `TerminalCapabilityKeys` / `TerminalErrorCodes` / `ITerminal*` 由宿主 `terminal-api` AAR (路线图 P1.1) 决定; P0 阶段 `ThreeShellTerminalPlugin` 以字面量声明同一组值, P1.1 落地后改为引用契约常量 |
| Node.js 契约 | 宿主 `nodejs-api` AAR 的 `NodeJsPluginCapabilityKeys.NODE_CLI_*` / `NodeJsPluginActions.RUNTIME` / `NodeJsRuntimeContract.NODE_CLI_SCHEMA_VERSION`; 目标包 `io.github.supermonster003.autojs6.plugin.nodejs` (1.5.0+) |
| 最低宿主 versionCode | `ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION` = `TerminalIds.REQUIRED_HOST_VERSION_CODE`, = 5304 (P1.4 回填确认, 2026-10-01: 首个已提交 `version.properties` 含 `terminal-api` 与宿主客户端的 6.8.0 构建; 宿主协议文档 `docs/dev/terminal-plugin-protocol-v1.md`) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 与 `autojs6-native-alignment` 1.8.3 (与兄弟仓库统一升级时再更新) |
| 原生库 / ABI | `libjackpal-androidterm5.so`, `libjackpal-termexec2.so`; `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` + universal |
| 发布文件名 | `autojs6-plugin-three-shell-terminal-v{VERSION_NAME}-{abi}-{CRC32}.apk` (5 个) |
| 图标源图 | `.python/icons/three-shell-ic-launcher-light.png` / `-dark.png` (1254 x 1254 RGBA, alpha 一致, 包围盒 746 x 653, 图案 `#272727` / `#D8D8D8`; light / dark 指使用它的模式); `ADAPTIVE_GLYPH = 0.45` |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.
- 涉及宿主 (`D:/idea-projects/AutoJs6`) 或兄弟仓库 (`D:/idea-projects/AutoJs6-Plugin-NodeJs-Runtime`) 的条目, 同样先检查其工作树; 其它会话可能在宿主有未提交改动, 只按明确文件列表暂存与提交.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主入口和公共契约.
- 不提交本地缓存, IDE 状态, 调试输出或无意生成的二进制文件; `releases/` 与构建产物不入库.
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整. 本地构建 SHOULD 传入 `-Pautojs.gradle.build.number.auto.increment.enabled=false -Pautojs.gradle.build.time.update.enabled=false` (PowerShell 下加引号) 避免无意变更.
- 修改第三方依赖或 vendored AAR 时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`, `libs/README.md`, 对应锁文件), 并在 changelog 的 `dependency` 分类记录.
- 从宿主迁入的源码保留 MPL-2.0 文件头, 只改包名与资源引用; 行为差异在提交说明与路线图证据中写明.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, ABI, 度量值), 不勾选没有证据的条目.

### 3.3 提交

- 维护者于 2026-10-01 指示: 本插件仓库与对应的宿主改动当前仅作本地提交, 暂时不推送到 GitHub 远端, 不登记官方索引, 不发 Release (路线图 D9). 后续会话继续遵守, 直至维护者明确恢复推送.
- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(node): ...`, `feat(binder): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 本地路径, 临时 APK 或无关改动.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-Three-Shell-Terminal/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml (JVM / APK / lint / 16 KB 校验 + API 24 x86 / API 35 x86_64 模拟器契约测试), markdown.yml
|-- .python/                    generate_markdown.py (+ .bat, check_markdown.bat), generate_launcher_icons.py, icons/
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/
|   |-- src/main/java/io/github/supermonster003/autojs6/plugin/three/shell/terminal/
|   |   |-- ThreeShellTerminalPlugin.kt                身份常量
|   |   |-- ThreeShellTerminalPluginInfoService.kt     IPluginInfoProvider
|   |   |-- ThreeShellTerminalPluginService.kt         org.autojs.plugin.TERMINAL (P2.4 起带宿主身份校验的 ITerminalPlugin.Stub)
|   |   |-- ThreeShellTerminalEntryActivity.kt         org.autojs.plugin.TERMINAL_OPEN 转发器 (P3.3: EntryRequest 解析 extras, EntryCaller / EntryCallerPolicy 校验可识别的调用方)
|   |   |-- WakeActivity.kt
|   |   |-- ui/                                        P3.1: HostAppearance (宿主外观快照 + Appearance 解析), HostAppearanceActivity (跟随语言 / 夜间 / 主题色的基类),
|   |   |                                              TerminalPalette (中性色 + HCT 强调色 + 4.5:1 保证), UiKit (Material 3 对话框 / 控件着色 / 剪贴板 / 外部 Intent / 工作线程),
|   |   |                                              TerminalActivity, TerminalEmulatorView, TerminalToolbarView, TerminalTextSelection, TerminalBanner + NodeBanner + StorageBanner,
|   |   |                                              NodeProbeReport, TerminalNpmDialogs, TerminalSettingsDialogs, SessionStarter (Activity 与管理器共用的两步启动);
|   |   |                                              P3.2: TerminalManagerDialog (四个可收起分组的会话管理器), TerminalManagerActivity (透明承载, 通知点击 / manager=true 入口), ElapsedTime;
|   |   |                                              P3.3: LauncherActivity (启动器图标目标, 与终端同 taskAffinity 的不可见转发器)
|   |   `-- (路线图 4.2 节: core/ service/ node/ storage/ binder/ ui/settings/ 随 P2 - P5 加入)
|   |-- src/main/java/jackpal/androidterm/           PtyBridge.java, emulatorview/{TerminalSelectionSnapshot,TerminalCursorPosition}.java (同包访问 AAR 包级 API, 包名不变)
|   |-- src/main/res/           values*/ x 11 (strings, colors + values-night, themes, ids), mipmap*/ (生成), raw*/plugin_instruction.md (生成), layout/ (activity_terminal, include_terminal_banner), menu/ (menu_terminal), xml/
|   |-- src/test/               JVM 契约, 资源守卫, 迁入的终端逻辑测试, 调色板 / 外观解析 (ui/TerminalPaletteTest)
|   |-- src/androidTest/        Binder 契约, 会话 / 存储 / Node / 终端界面 (ui/TerminalActivityInstrumentationTest) instrumentation
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   |-- build.gradle.kts / proguard-rules.pro
|-- build-logic/                org.autojs.build.{utils,versions,signs,properties,jvm-convention,local-arr-register-convention}
|-- docs/dev/                   各阶段证据 (p0-spike-evidence.md 等)
|-- gradle/                     wrapper, libs.versions.toml
|-- libs/                       common-plugin-api.aar, nodejs-api.aar, terminal-api.aar (host-api-aars.lock 锁定);
|                               jackpal/{term-1_0_70.aar, emulatorview-1_0_42.aar, libtermexec-1_0.aar} (vendored-aars.lock 锁定); README.md
|-- locks/                      host-api-aars.lock, vendored-aars.lock
|-- native/jackpal-termexec/    原生库重建配方副本 (build.py, CMakeLists.txt, provenance.json, upstream.lock.json, LICENSE, NOTICE, README.md)
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties, gradlew(.bat)
`-- sign.properties             本地签名配置, Git 忽略
```

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- 平台插件与 `autojs6-native-alignment` 只在根 `settings.gradle.kts` 应用一次, 位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不重复应用. 禁止 `mavenLocal()`.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 声明 `com.android.application` 并 `apply false`; 模块只应用插件, 不写版本. 不声明 `org.jetbrains.kotlin.android` (AGP 内置 Kotlin 已覆盖).
- `app` 模块从 `version.properties` 与 `org.autojs.build.versions` 读取 compileSdk / minSdk / targetSdk / versionCode / versionName.
- 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`. 不提交 `gradle/data` 消费端覆盖.

平台验收命令 (Temurin 环境模拟, 必须只输出一段版本决策):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界

- Gradle 构建 MUST 自包含, 禁止引用兄弟仓库或宿主的路径, JAR / AAR 或 `flatDir`.
- 宿主 AAR (`common-plugin-api`, `nodejs-api`, `terminal-api`) 只从 `libs/` 消费, 由 `locks/host-api-aars.lock` 锁定 SHA-256; jackpal 三份 AAR 从 `libs/jackpal/` 消费, 由 `locks/vendored-aars.lock` 锁定; `app/build.gradle.kts` 在配置期拒绝缺失文件, debug 产物 (宿主 API AAR), 占位哈希, 多余锁条目与摘要不符. 更新任一 AAR 时同一提交内更新对应锁文件, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md`.
- 新增依赖优先 Maven Central / Google Maven; 当前运行时依赖只有 AndroidX appcompat / core-ktx, Material, Gson.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, 由 `.gitignore` 忽略 (`git check-ignore` 已验证).
- `appendDigestToReleasedFiles` 依赖 `assembleRelease`, 签名缺失时失败, 校验产物集合为 `autojs6-plugin-three-shell-terminal-v{VERSION_NAME}-{abi}.apk` x 4 + `-universal.apk`, 并对每个 APK 校验其打包的 ABI 目录集合, 再追加 CRC32.
- 构建产物不入库 (`releases/` 被忽略).

### 5.4 ABI 拆分, 16 KB 页与原生库配方 (CONDITIONAL, P0.1 起)

- `splits.abi` 启用, `include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")`, `isUniversalApk = true`; `getInfo().supportedAbis` 由当前 APK 实际打包的 ABI 推导 (universal 为四个).
- `nativeAlignment` 期望两个库在每个 ABI 下 `PT_LOAD` 对齐 `0x4000`; `:app:verifyNativePageAlignment` 在 CI 与发布前运行.
- 原生库只来自 `libs/jackpal/libtermexec-1_0.aar`; 不在本仓库编译 C 代码. 需要重建时使用 `native/jackpal-termexec/build.py` (NDK 28.2.13676358, platform 24), 更新 AAR, 锁与 `provenance.json`, 在 changelog `dependency` 记录.
- Manifest `NATIVE_PAGE_ALIGNMENT=16384`.

## 6. Manifest 与激活协议

- `org.autojs.permission.PLUGIN`, `WAKE_ACTIVITY` meta-data, `WakeActivity` (exported, `Theme.NoDisplay`, PLUGIN 权限, WAKE action + DEFAULT category, 立即结束), `org.autojs.plugin.info.AUTHOR`, `NATIVE_PAGE_ALIGNMENT=16384`.
- INFO 服务与 TERMINAL 服务均 exported, 受 PLUGIN 权限保护, 携带 `requiresHostVersion` meta-data.
- `ThreeShellTerminalEntryActivity` (P3.3 / D19): exported, PLUGIN 权限, `Theme.NoDisplay`, `excludeFromRecents`, intent-filter `org.autojs.plugin.TERMINAL_OPEN` + DEFAULT; 宿主要求该 action 恰好解析到一个受 PLUGIN 权限保护的导出 Activity, 否则视插件为 "不兼容". 它不设 `taskAffinity` (留在调用方任务), 终端界面以 `FLAG_ACTIVITY_NEW_TASK` 进入自有任务, 管理器留在调用方任务之上. `LauncherActivity` 不导出, `Theme.NoDisplay`, 与 `TerminalActivity` 同 `taskAffinity`, 不设 `excludeFromRecents` (任务根排除会隐藏整个终端任务); P5.3 的 MAIN / LAUNCHER 只在 alias 上.
- `TerminalActivity` 的离开统一走 `finishScreen()`: `isTaskRoot` 时 `finishAndRemoveTask()` (宿主入口 / 启动器进入; 最近任务不会重放带 `command` 的启动请求), 否则 `finish()` (管理器之上叠放的终端).
- 权限清单与理由 (每次新增权限时更新本表, README 安全节与 `ManifestContractTest`):

| 权限 | 理由 |
|---|---|
| `READ_EXTERNAL_STORAGE` (maxSdk 32) / `WRITE_EXTERNAL_STORAGE` (maxSdk 29) / `MANAGE_EXTERNAL_STORAGE` | 终端以插件 uid 运行, 不继承宿主授权; 进入 `/sdcard` 下的脚本与工程目录需要全部文件访问 (路线图 D6 / D18); 未授权时回退到 `HOME` 并引导 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 会话在界面离开后继续运行的前台服务 (D15); `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 说明用途 |
| `POST_NOTIFICATIONS` | 前台服务通知 (目录 / 会话数 / 关闭全部), 缺失时静默降级, 会话仍运行 |
| `INTERNET` | shell 子进程 (npm / corepack 等) 的网络访问由用户命令决定; 用户手动检查更新时访问本插件固定的 GitHub Releases API (12 小时间隔与缓存结果) |

- `<queries>`: 宿主包 `org.autojs.autojs6`; Node.js Runtime 插件包名与 `org.autojs.plugin.nodejs.RUNTIME` intent (读取 `NODE_CLI_*` meta-data 与签名). 新增跨包访问时先加 queries.
- 入口 Activity (P3.3), 前台服务 (P2.1), 设置 Activity (P5.1) 与启动器 alias (P5.3) 为 CONDITIONAL: 入口与设置 Activity 导出但受 PLUGIN 权限保护且校验调用方签名; 启动器 MAIN / LAUNCHER 只放在四个 alias 上.

## 7. PluginInfo 与能力协商

- `name` 来自不可翻译的 `app_name`, `description` 来自当前 locale 的 `plugin_description`, `instruction` 来自 `@raw/plugin_instruction`, `versionName` / `versionCode` 来自已安装包, `versionDate` 来自 `plugin_version_date` resValue, `id` / `engine` / `variant` 来自 `ThreeShellTerminalPlugin`.
- `supportedAbis` 由当前 APK 实际打包的 ABI 推导 (`BuildConfig` 或 `applicationInfo.nativeLibraryDir` 探测), 不硬编码.
- `capabilities` 自 P1.1 起含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION` 与 `TerminalCapabilityKeys.CONTRACT_VERSION`; `FEATURES`, `MAX_SESSIONS`, `MAX_SUBSCRIPTIONS`, `NODE_CLI` 随 P2.4 的真实 Binder 路由一起声明 (路线图附录 B), 不提前声明尚未实现的能力.
- 新增可选方法时先协商能力, 不通过捕获异常猜测协议版本.

## 8. Binder 与公共 API 设计

- 常量, Intent extra 键, JSON key, capability key, 错误码, 上限集中在宿主 `terminal-api` 契约 (路线图附录 B); 插件不散落字符串字面量.
- 全部输入按路线图 D22 上限校验: 会话 <= 16, 命令 / 单次输入 <= 64 KiB, 环境 <= 64 项 x 4 KiB, 路径 <= 4096 字节, JSON <= 64 KiB, 回调 <= 8, 订阅 <= 4 / 会话, 转录读取 <= 1 MiB. 超限返回 `INVALID_ARGUMENT`, 不崩溃.
- Binder 方法即时返回, 不在 Binder 线程等待 I/O; 会话输出只经 `ParcelFileDescriptor` 管道传输 (D21), 不经回调推送字节; 回调只传状态变化与溢出计数.
- 已发布 AIDL 只在末尾追加方法, 由宿主 `TerminalAidlOrderTest` 冻结顺序.
- 调用方校验 (`HostCallerGuard`): 持有 PLUGIN 签名权限且签名可信; 入口 / 设置 Activity 同样校验 `callingPackage`.
- 宿主进程死亡 (`linkToDeath`) 时关闭其订阅写端并移除回调; 会话是用户可见资产, 不随宿主死亡关闭.

## 9. pty, 原生库与 Node 信任边界 (CONDITIONAL, P2 起)

- 只运行 `/system/bin/sh`; 包装脚本与命令构造 (`TerminalSessionLauncher`) 不接受来自调用方的 shell 名; 调用方提供的 `command` 按原文嵌入并受长度上限约束 (路线图附录 D Q4).
- pty 经 `/dev/ptmx` + `TermExec.createSubprocess` 创建; 每个会话一个子进程, 关闭时 `SIGHUP` 后等待, 超时 `SIGKILL`; 启动期间关闭不得留下活 shell (迁入的 instrumentation 用例守卫).
- 全部子进程以插件 uid 与插件权限运行; 不尝试获取宿主权限, 不共享宿主 uid.
- Node.js Runtime 插件的二进制只在签名可信时执行 (官方签名 SHA-256 集合或与本插件自身签名一致, D17); 读取 `NODE_CLI_*` meta-data 并校验 schema, ELF magic, 归档 SHA-256 与一次性 `--version`; 任何失败只降级为纯 shell 并给出类型化原因, 不弹错误对话框打断会话.
- npm / corepack 归档解压受 `NODE_CLI_ARCHIVE_ENTRY_COUNT` / `NODE_CLI_ARCHIVE_BYTES` 上限约束, 拒绝路径穿越与符号链接条目; 解压目录只写一次, 以 SHA-256 记录.
- 原生库 (Apache-2.0) 的 `LICENSE` / `NOTICE` 随 `native/jackpal-termexec/` 保留; 不修改 JNI 符号名 (`jackpal.androidterm.libtermexec.v1.TermExec`, `jackpal.androidterm.Exec`); R8 规则保留这两个类.
- 日志不记录会话输入输出正文, 只记录会话 id, 目录, 耗时与错误码.

## 10. 字符串资源

- 11 个目录: `values/` (默认英语) 与 `values-en/` 逐字相同, 另有 ar, es, fr, ja, ko, ru, zh, zh-rHK, zh-rTW.
- `strings.xml` 按 `name` 升序; 不可翻译项 (`app_name`) 放 `strings_donottranslate.xml`; plurals 放 `plurals.xml`.
- 全部 locale 使用 ASCII 标点 (含日语, 韩语, 阿拉伯语的逗号与句号), 省略号写 `...` 并加 `tools:ignore="TypographyEllipsis"` (lint 已全局禁用该检查). `ApplicationTextPunctuationTest` 扫描 `app/src/main`, `.readme`, `.changelog`, `docs`, `README.md`, `ROADMAP.md`, `AGENTS.md`, `THIRD_PARTY_NOTICES.md` 的 xml / md / json.
- `plugin_description` 句尾无点号, 不含 "AutoJs6" 字样 (`StringResourceParityTest`).
- 从宿主迁入的终端字符串改为 `terminal_*` 前缀, 10 语言译文沿用宿主既有译文, 仅改写与宿主语义绑定的文案 (例如 "宿主" -> "AutoJs6").
- 图标由 `.python/generate_launcher_icons.py` 从 `.python/icons/three-shell-ic-launcher-light.png` 确定性生成, 不手工编辑 `mipmap*/` 输出; 修改源图或比例后重新生成, 运行 `--check`, 并更新第 2 节与 changelog.

## 11. README, 插件说明与 changelog

- `.readme/lang_*.json` (10 语言, 键集合一致, 列表键 `features` / `usage_steps` / `node_points` / `compatibility_points` / `faq_items` / `security_points`) 与 `.changelog/lang_*.json` 是唯一文案源; 生成物 (`README.md`, `.readme/README-*.md`, `app/src/main/assets/doc/CHANGELOG*.md`, `app/src/main/res/raw*/plugin_instruction.md`) 不手工编辑.
- 修改 JSON 或模板后运行 `py .python/generate_markdown.py` 再 `--check`; CI `markdown.yml` 在 Windows 上执行 `.python/check_markdown.bat`.
- 根 `README.md` 为简体中文, 与 `.readme/README-zh-Hans.md` 同源.
- changelog 分类只用 `hint` / `feature` / `fix` / `improvement` / `dependency`; 简体中文依赖条目用 `附加` / `升级` / `降级` / `替换` / `移除`; 当前版本 key 为 `v{VERSION_NAME}` (忽略后缀), `released_date` 为当日 `YYYY/MM/DD`; 涉及 feature / fix / improvement / dependency 的提交 MUST 更新 10 语言 JSON.
- 文案面向使用者, 不写内部类拆分, Binder 传输细节或测试数量; 行为变化, 权限, 默认值与兼容性必须如实记录; Node.js 相关限制 (W^X, 原生 addon, corepack 默认版本) 按 Node.js Runtime 插件文档的事实写.

## 12. 独立界面与设置 (CONDITIONAL, P3 / P5 起)

- 终端界面, 入口 Activity, 设置页, 关于与发行历史遵循 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`: 外观四项顺序 (语言 / 夜间模式 / 主题色 / 启动器图标) 默认跟随 AutoJs6 (经 `AutoJs6HostSettingsContract`, 宿主缺失时用本地值), 先选后确定, 中性表面 + 主题色控件, 24 dp 圆角对话框, 72 dp 行高.
- 终端 Activity 全屏, `adjustResize|stateVisible`, 自有 `taskAffinity`; 从宿主入口进入时 Back 返回宿主, 从启动器进入时 Back 回桌面.
- 发行历史页按 locale 读取 `doc/CHANGELOG-{tag}.md`, 回退英语; 更新对话框 Neutral 打开内置发行历史, Positive 打开发布页; 更新检查有超时, 取消, 失败提示, 忽略版本与 12 小时频率限制.
- 启动器图标四 alias 见 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md`; Three 的 `ic_launcher` 保持透明 BitmapDrawable, 不创建同名自适应 XML; 启动器 alias 指向 `LauncherActivity` (恢复最近会话或新建, 无独立首页).

## 13. 测试要求

### 13.1 JVM

- `ManifestContractTest` (权限, queries, meta-data, 组件导出与权限, 服务发现契约, 入口 / 设置 action), `ThreeShellTerminalPluginRuntimeInfoTest` (PluginInfo 纯数据映射, `supportedAbis` 推导, 身份常量对齐 `common.json` / `build.gradle.kts` / `settings.gradle.kts`), `StringResourceParityTest`, `ApplicationTextPunctuationTest`, `VendoredAarLockTest`; P3.3 起: `ThreeShellTerminalEntryRequestTest` (extras 校验: 空白视为缺省, UTF-8 字节上限, 互斥组合; 调用方策略: 无名调用方信任 Manifest 权限, 具名调用方需持权限且签名一致).
- P2 起: 迁入的 10 个终端逻辑测试 (`TerminalPaths`, `TerminalEnvironment`, `TerminalSessionLauncher`, `TerminalKeySequences`, `ShellQuoting`, `NpmProjectScripts`, `TerminalNodeEnvironment`, `NodeCli*`), 存储路径判定, 信任判定, 上限与错误映射; P3 起: `ui/TerminalPaletteTest` (两种夜间模式 x 10 个种子色的强调色对每个承载表面 >= 4.5:1, 中性文字角色对比度, HCT 规则, 无宿主回退与宿主快照优先, 语言标签校验; 中性色直接解析 `colors.xml` / `values-night/colors.xml`), `ui/ElapsedTimeTest`; P5 起: 设置序列化, 版本比较.

### 13.2 Android instrumentation

- `ThreeShellTerminalPluginContractTest`: Wake Activity 契约, INFO 服务 `getInfo()` 往返 (含 `supportedAbis` 与能力 Bundle), TERMINAL 服务 descriptor, 原生库可加载, P0 无启动器入口.
- P2 起: `TerminalBinderContractTest` (happy path, 敌意输入, 上限, 无权限调用方, 宿主死亡后管道关闭), 会话 / 存储 / Node 用例; P3 起: `ui/TerminalActivityInstrumentationTest` (迁入的会话切换用例, 副标题 / 剪贴板, 离开界面保活并恢复, 存储横幅未授权出现 / 授权后 "重新进入目录"), `ui/TerminalManagerInstrumentationTest` (迁入的管理器跨新建 / 恢复会话 Activity 存活并恢复原终端, 独立管理器 Activity 无终端显示且 Back 不带出终端, 注册表外创建 / 关闭的会话即时进出列表), P3.3 入口用例; P5 起: 设置页与 alias.
- P3.3 起: `ThreeShellTerminalEntryInstrumentationTest` (adb shell uid 2000 启动入口被 Android 以 PLUGIN 权限拒绝: `am` 的 SecurityException 只在 stderr, UiAutomation 拿不到, 用例改读 `logcat -s ActivityManager:W ActivityTaskManager:W` 的 "Permission Denial"; 插件自身 uid (与宿主同签名) 经入口进入指定目录的新会话且终端为任务根, Back 连同任务移除而会话存活; `manager=true` 只弹管理器; 互斥 extras 被忽略; `LauncherActivity` 无会话时在主目录新建, 否则恢复最近会话).
- Activity 用例在 API 33+ 先 `uiAutomation.grantRuntimePermission(POST_NOTIFICATIONS)`: 终端界面首次建会话会请求该权限, 系统对话框会让 `startActivitySync` 等到 45 s 超时 (Xiaomi Pad API 35 实测). 比较 shell 目录用 canonical 路径或 (dev, inode), 因为 `/data/user/0` 在部分设备是 `/data/data` 的符号链接而 procfs 给出真实路径.
- 设备池与证据等级见 `ROADMAP.md` 附录 E; 多台设备时用明确 serial, 每次会话重新读取 SDK / ABI 并安装对应 ABI 变体; 不卸载用户的已安装应用, 不清空启动器数据, 不删除用户的 `/sdcard` 内容 (测试目录放在 `/sdcard/Android/data/<pkg>/` 或临时目录).
- 存储授权在 API 30+ 用 `appops set <pkg> MANAGE_EXTERNAL_STORAGE allow` 驱动并在用例结束时恢复 `default`.

### 13.3 CI

- `build.yml`: JVM 测试编译, `verifyNativePageAlignment`, `testDebugUnitTest`, androidTest 与 release APK, lint debug / release; API 24 x86 与 API 35 x86_64 模拟器运行契约测试 (安装 universal APK; Node.js 相关用例在无该插件时自动跳过).
- `markdown.yml`: Windows 上 `check_markdown.bat`.
- 仓库未推送期间工作流只做本地语法与路径校验.

## 14. 验证顺序

```powershell
py .python/generate_markdown.py --check
py .python/generate_launcher_icons.py --check
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:testDebugUnitTest
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:verifyNativePageAlignment
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:connectedDebugAndroidTest
```

- 纯文档改动只需前两条; 涉及源码的改动至少跑 JVM 测试与 debug 装配; 涉及 Manifest, Binder, 前台服务, pty 或原生库的改动必须在至少一台真机或 AVD 上跑 instrumentation.
- Release 前额外执行 `:app:appendDigestToReleasedFiles`, 检查 `releases/` 恰好 5 个已签名 APK 且 CRC32 与内容一致.
- 性能回归阈值 (P6.3 落档后填入): 会话创建到首个提示符, universal APK 大小.
- 任何未执行的验证都在最终说明中明确列出原因.

## 15. 许可证, 安全与隐私

- `LICENSE` 为 MPL-2.0 完整文本, README 徽章与 `THIRD_PARTY_NOTICES.md` 一致; jackpal Android-Terminal-Emulator (Apache-2.0) 与宿主 AAR (MPL-2.0) 在声明文件与关于页列出.
- `allowBackup=false` 且 `dataExtractionRules` 排除全部数据; 导出组件最小化并受签名权限保护.
- 不记录会话输入输出正文; 转录只在用户主动复制 / 分享或脚本显式 `transcript()` / 订阅时离开进程.
- 第三方组件的版本, 来源, 许可证与 SHA-256 (宿主 AAR, jackpal AAR) 记录在 `THIRD_PARTY_NOTICES.md`.

## 16. 参考项目路由

- 构建骨架, Three 身份, AAR 锁, 路线图 / AGENTS 格式, CI 矩阵: `D:/idea-projects/AutoJs6-Plugin-Three-Setup-Installer`
- 四 alias 图标, 代码构建的设置 / 关于 / 发行历史 / 宿主外观跟随套件: `D:/idea-projects/AutoJs6-Plugin-Three-Stove-Agent`
- ABI 拆分 + universal 的发布任务, `verifyNativePageAlignment`, 存储权限写法, `NODE_CLI` 契约与 `docs/nodejs/TERMINAL.md`: `D:/idea-projects/AutoJs6-Plugin-NodeJs-Runtime`
- 被迁出的终端实现, jackpal AAR 与原生库配方, 宿主入口与插件发现范式 (`core/plugin/screencolorpicker`, `ui/settings/McpServerSettingsLauncher.kt`, `core/plugin/epub`), 契约模块布局 (`plugin-api/`): `D:/idea-projects/AutoJs6`
- 上游 (只读, Apache-2.0): `https://github.com/jackpal/Android-Terminal-Emulator`

参考时以这些仓库的当前代码为准; 复制骨架后必须替换身份字段, URL, 文案, 常量, 版本与测试数据.
