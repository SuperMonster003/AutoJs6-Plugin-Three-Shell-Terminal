3-Shell Terminal 是手机上的交互式终端: 在 pty 中运行系统的 `/system/bin/sh`, 可以同时打开多个会话, 离开界面后会话在后台继续运行. 安装 Node.js Runtime 插件后还能使用 node 与 npm 等命令. AutoJs6 的 "终端" 入口与脚本中的 `terminal` 对象都由本插件提供.

### 开始使用

1. 在 AutoJs6 主页抽屉中点按 "终端", 或在文件管理器的目录菜单中选择 "在终端中打开"; 也可以点按桌面上的 3-Shell Terminal 图标.
2. 输入命令后回车执行. 屏幕底部的快捷键栏提供 Esc, Tab, Ctrl, 方向键与常用符号.
3. 在工具栏的 "会话" 菜单中新建, 切换或关闭会话. 离开界面后会话继续运行, 点按通知可以打开 "终端管理器".
4. 进入 `/sdcard` 等共享存储目录前, 按终端中的提示点按 "授予", 或在设置中打开 "全部文件访问".

### 主要功能

- **多会话**: 新建, 切换与关闭会话, "终端管理器" 列出全部会话. 前台服务让会话在后台保持运行, 通知栏显示当前目录与会话数.
- **终端界面**: 快捷键栏, 文本选择与复制, "分享终端记录", 双指缩放字号; 设置中提供 4 种 "终端配色" 与 2 种 "终端字体".
- **命令编辑器**: 粘贴多行或较长的命令时先显示全文, 可以粘贴到当前会话, 或在新会话中执行并查看退出码.
- **Node.js 工具链**: 安装 Node.js Runtime 插件后可以使用 node, npm, npx 与 corepack (yarn / pnpm). 设置中可以选择 "npm 镜像源", 也可以 "忽略 npm 生命周期脚本".
- **命令完成通知**: 脚本以不保留交互式 shell 的方式执行的命令结束后发出通知, 点按可以查看输出末尾. 可以在设置中关闭 "命令完成通知".
- **独立设置**: 语言, 夜间模式与主题色默认跟随 AutoJs6, 也可以单独设置; 启动器图标有 4 种样式. AutoJs6 支持时, 设置页的菜单提供 "打开通用设置页".

### 在脚本中使用

脚本通过全局对象 `terminal` (别名 `$terminal`) 创建会话, 执行命令, 写入输入与监听输出:

```js
if (!terminal.isAvailable()) exit();
let result = terminal.exec('uname -m', {
    show: false,
    keepOpen: false,
    wait: true,
});
console.log(result.exitCode);
let session = terminal.open({ show: false });
session.on('output', (text) => console.log(text));
session.write('ls\n');
session.show();
```

- `exec` 默认在命令结束后保留交互式 shell; 需要等待退出码时, 同时设置 `keepOpen: false` 与 `wait: true`.
- 同步方法会阻塞脚本线程, 不能在界面线程调用; 多数方法还有返回 Promise 的 `Async` 形式.
- 脚本退出后会话继续运行, 不再需要时调用 `session.close()`. 操作失败时抛出带 `code` 的 `TerminalError`.
- 完整的选项与事件见 [AutoJs6 文档 > 终端](https://docs.autojs6.com/#/terminal).

### 运行要求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本.
- node 与 npm 等命令需要 [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) 插件 1.5.0 或更高版本.
- 进入共享存储目录需要 "全部文件访问" (Android 11 及以上) 或存储权限; 通知权限用于会话通知与命令完成通知.
- 命令以本插件自己的身份与权限运行, 不继承 AutoJs6 的权限; 需要 AutoJs6 权限的命令请在脚本中改用 `shell()`.

### 常见问题

**无法进入 `/sdcard` 下的目录**

本插件需要自己的存储授权. 按终端中的提示点按 "授予", 或在设置中打开 "全部文件访问"; Android 10 及以下授予存储权限即可. `content://` 地址不是文件路径, 无法进入.

**找不到 node 命令**

请确认已安装 Node.js Runtime 插件, 且设置中的 "Node.js 集成" 已打开, 然后打开 "Node.js 启动器检测" 查看原因. 插件签名不是官方签名时, 该页显示签名的 SHA-256, 确认信任后新会话即可使用.

**后台会话被系统结束**

插件进程被系统结束后, 会话无法恢复. HyperOS, MIUI 等系统可能限制后台运行, 请在系统设置中允许 3-Shell Terminal 发送通知并在后台运行.

快捷键, 脚本接口与权限的详细说明见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
