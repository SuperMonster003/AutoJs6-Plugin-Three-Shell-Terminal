3-Shell Terminal 是手機上的互動式終端: 在 pty 中運行系統的 `/system/bin/sh`, 可以同時打開多個工作階段, 離開介面後工作階段在背景繼續運行. 安裝 Node.js Runtime 插件後還能使用 node 與 npm 等指令. AutoJs6 的 "終端" 入口與腳本中的 `terminal` 物件都由本插件提供.

### 開始使用

1. 在 AutoJs6 主頁抽屜中點按 "終端", 或在檔案管理器的目錄選單中選擇 "在終端中開啟"; 也可以點按桌面上的 3-Shell Terminal 圖示.
2. 輸入指令後按 Enter 執行. 屏幕底部的快捷鍵列提供 Esc, Tab, Ctrl, 方向鍵與常用符號.
3. 在工具列的 "工作階段" 選單中建立, 切換或關閉工作階段. 離開介面後工作階段繼續運行, 點按通知可以打開 "終端機管理員".
4. 進入 `/sdcard` 等共享儲存空間目錄前, 按終端中的提示點按 "授予", 或在設定中開啟 "所有檔案存取".

### 主要功能

- **多工作階段**: 建立, 切換與關閉工作階段, "終端機管理員" 列出全部工作階段. 前台服務讓工作階段在背景保持運行, 通知欄顯示目前目錄與工作階段數量.
- **終端介面**: 快捷鍵列, 文字選取與複製, "分享終端記錄", 雙指縮放字號; 設定中提供 4 種 "終端配色" 與 2 種 "終端字型".
- **指令編輯器**: 貼上多行或較長的指令時先顯示全文, 可以貼上到目前的工作階段, 或在新工作階段中執行並查看結束碼.
- **Node.js 工具鏈**: 安裝 Node.js Runtime 插件後可以使用 node, npm, npx 與 corepack (yarn / pnpm). 設定中可以選擇 "npm 鏡像源", 也可以 "忽略 npm 生命週期指令碼".
- **指令完成通知**: 腳本以不保留互動式 shell 的方式執行的指令結束後發出通知, 點按可以查看輸出末尾. 可以在設定中關閉 "指令完成通知".
- **獨立設定**: 語言, 夜間模式與主題色預設跟隨 AutoJs6, 也可以單獨設定; 啟動器圖示有 4 種樣式. AutoJs6 支援時, 設定頁的選單提供 "開啟通用設定頁".

### 在腳本中使用

腳本透過全域物件 `terminal` (別名 `$terminal`) 建立工作階段, 執行指令, 寫入輸入與監聽輸出:

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

- `exec` 預設在指令結束後保留互動式 shell; 需要等待結束碼時, 同時設定 `keepOpen: false` 與 `wait: true`.
- 同步方法會阻塞腳本線程, 不能在介面線程呼叫; 多數方法還有返回 Promise 的 `Async` 形式.
- 腳本結束後工作階段繼續運行, 不再需要時呼叫 `session.close()`. 操作失敗時拋出帶 `code` 的 `TerminalError`.
- 完整的選項與事件見 [AutoJs6 文檔 > 終端](https://docs.autojs6.com/#/terminal).

### 運行要求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本.
- node 與 npm 等指令需要 [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) 插件 1.5.0 或更高版本.
- 進入共享儲存空間目錄需要 "所有檔案存取" (Android 11 及以上) 或儲存權限; 通知權限用於工作階段通知與指令完成通知.
- 指令以本插件自己的身份與權限運行, 不繼承 AutoJs6 的權限; 需要 AutoJs6 權限的指令請在腳本中改用 `shell()`.

### 常見問題

**無法進入 `/sdcard` 下的目錄**

本插件需要自己的儲存授權. 按終端中的提示點按 "授予", 或在設定中開啟 "所有檔案存取"; Android 10 及以下授予儲存權限即可. `content://` 地址不是檔案路徑, 無法進入.

**找不到 node 指令**

請確認已安裝 Node.js Runtime 插件, 且設定中的 "Node.js 整合" 已開啟, 然後打開 "Node.js 啟動器檢測" 查看原因. 插件簽名不是官方簽名時, 該頁顯示簽名的 SHA-256, 確認信任後新工作階段即可使用.

**背景工作階段被系統結束**

插件進程被系統結束後, 工作階段無法恢復. HyperOS, MIUI 等系統可能限制背景運行, 請在系統設定中允許 3-Shell Terminal 發送通知並在背景運行.

快捷鍵, 腳本介面與權限的詳細說明見 [項目主頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
