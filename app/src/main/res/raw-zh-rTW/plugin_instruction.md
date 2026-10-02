3-Shell Terminal 接管 AutoJs6 的內建終端機: 主頁抽屜的 "終端機" 開關, 檔案管理員目錄選單與專案工具列的 "在終端機中開啟", 以及指令碼側用於開啟, 驅動與監聽終端機工作階段的全域物件 `terminal`. 每個工作階段都是一個在 pty 中執行的系統 shell (`/system/bin/sh`), 離開介面後繼續在背景執行.

P4 本機開發預覽: 已實作終端介面, 多工作階段管理與指令碼 API, 包括輸出監聽, 互動輸入與結束代碼等待. 獨立設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 的 P5 推進. 指令碼 API 需使用包含 P4 實作的 AutoJs6 組建.

### 使用方式

1. 在安裝了 AutoJs6 建置 5304 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 安裝與裝置 ABI 對應的外掛 APK (或 universal).
2. 開啟 AutoJs6 外掛中心, 確認 `3-Shell Terminal` 已被識別並啟用它.
3. 在 AutoJs6 主頁抽屜開啟 "終端機", 在檔案管理員中對目錄選擇 "在終端機中開啟", 或在指令碼中呼叫 `terminal.open(...)`. 需要進入共用儲存空間 (如 `/sdcard`) 下的目錄時, 按外掛提示授予 "所有檔案存取權限".

### Node.js 命令

- 需要 Node.js Runtime 外掛 1.5.0 或更高版本; 外掛讀取其清單契約, 驗證簽署, 啟動器與 npm / corepack 封存後, 在每次新建工作階段時把命令連結進 `PATH`. 未安裝或驗證失敗時終端機仍可用, 只是不含這些命令.
- Android 禁止執行應用程式寫出的檔案: npm 預設關閉 bin 連結, 因而 `node_modules/.bin/*` 與 `npx <套件名稱>` 無法直接執行套件入口. 請使用 `node node_modules/<套件名稱>/<入口>.js`. npm 套件附帶的原生可執行檔會以 `EACCES` 失敗, 原生擴充 (`.node`) 無法載入.
- corepack 預設使用內建的 pnpm 11.x 與 Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`), 顯式指定版本時按指定版本下載; npm 鏡像來源可在設定中切換為 npmmirror 或自訂 https 位址.

安裝指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
