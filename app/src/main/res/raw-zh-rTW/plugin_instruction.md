3-Shell Terminal 是手機上的互動式終端機: 在 pty 中執行系統的 `/system/bin/sh`, 可以同時開啟多個工作階段, 離開畫面後工作階段在背景繼續執行. 安裝 Node.js Runtime 外掛後還能使用 node 與 npm 等指令. AutoJs6 的 "終端機" 入口與指令碼中的 `terminal` 物件都由本外掛提供.

### 開始使用

1. 在 AutoJs6 主頁抽屜中點按 "終端機", 或在檔案管理員的目錄選單中選擇 "在終端機中開啟"; 也可以點按主畫面上的 3-Shell Terminal 圖示.
2. 輸入指令後按 Enter 執行. 螢幕底部的快速鍵列提供 Esc, Tab, Ctrl, 方向鍵與常用符號.
3. 在工具列的 "工作階段" 選單中建立, 切換或關閉工作階段. 離開畫面後工作階段繼續執行, 點按通知可以開啟 "終端機管理員".
4. 進入 `/sdcard` 等共用儲存空間目錄前, 按終端機中的提示點按 "授予", 或在設定中開啟 "所有檔案存取".

### 主要功能

- **多工作階段**: 建立, 切換與關閉工作階段, "終端機管理員" 列出全部工作階段. 前景服務讓工作階段在背景持續執行, 通知列顯示目前目錄與工作階段數量.
- **終端機介面**: 快速鍵列, 文字選取與複製, "分享終端機記錄", 雙指縮放字級; 設定中提供 4 種 "終端機配色" 與 2 種 "終端機字型".
- **指令編輯器**: 貼上多行或較長的指令時先顯示全文, 可以貼到目前的工作階段, 或在新工作階段中執行並查看結束代碼.
- **Node.js 工具鏈**: 安裝 Node.js Runtime 外掛後可以使用 node, npm, npx 與 corepack (yarn / pnpm). 設定中可以選擇 "npm 鏡像來源", 也可以 "忽略 npm 生命週期指令碼".
- **指令完成通知**: 指令碼以不保留互動式 shell 的方式執行的指令結束後發出通知, 點按可以查看輸出結尾. 可以在設定中關閉 "指令完成通知".
- **獨立設定**: 語言, 夜間模式與主題色預設跟隨 AutoJs6, 也可以單獨設定; 啟動器圖示有 4 種樣式. AutoJs6 支援時, 設定頁的選單提供 "開啟通用設定頁".

### 在指令碼中使用

指令碼透過全域物件 `terminal` (別名 `$terminal`) 建立工作階段, 執行指令, 寫入輸入與監聽輸出:

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

- `exec` 預設在指令結束後保留互動式 shell; 需要等待結束代碼時, 同時設定 `keepOpen: false` 與 `wait: true`.
- 同步方法會阻塞指令碼執行緒, 不能在 UI 執行緒呼叫; 多數方法還有傳回 Promise 的 `Async` 形式.
- 指令碼結束後工作階段繼續執行, 不再需要時呼叫 `session.close()`. 操作失敗時擲回帶 `code` 的 `TerminalError`.
- 完整的選項與事件見 [AutoJs6 文件 > 終端機](https://docs.autojs6.com/#/terminal).

### 執行需求

- Android 7.0 或更新版本, AutoJs6 6.8.0 或更新版本.
- node 與 npm 等指令需要 [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) 外掛 1.5.0 或更新版本.
- 進入共用儲存空間目錄需要 "所有檔案存取" (Android 11 以上) 或儲存權限; 通知權限用於工作階段通知與指令完成通知.
- 指令以本外掛自己的身分與權限執行, 不繼承 AutoJs6 的權限; 需要 AutoJs6 權限的指令請在指令碼中改用 `shell()`.

### 常見問題

**無法進入 `/sdcard` 下的目錄**

本外掛需要自己的儲存空間授權. 按終端機中的提示點按 "授予", 或在設定中開啟 "所有檔案存取"; Android 10 以下授予儲存權限即可. `content://` 位址不是檔案路徑, 無法進入.

**找不到 node 指令**

請確認已安裝 Node.js Runtime 外掛, 且設定中的 "Node.js 整合" 已開啟, 然後開啟 "Node.js 啟動器檢測" 查看原因. 外掛簽章不是官方簽章時, 該頁顯示簽章的 SHA-256, 確認信任後新工作階段即可使用.

**背景工作階段被系統結束**

外掛處理程序被系統結束後, 工作階段無法復原. HyperOS, MIUI 等系統可能限制背景執行, 請在系統設定中允許 3-Shell Terminal 傳送通知並在背景執行.

快速鍵, 指令碼介面與權限的詳細說明見 [專案首頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
