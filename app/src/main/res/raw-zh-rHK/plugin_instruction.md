3-Shell Terminal 接管 AutoJs6 的內建終端機: 主頁抽屜的 "終端機" 開關, 檔案管理員目錄選單與專案工具列的 "在終端機中開啟", 以及指令碼側用於開啟, 驅動與監聽終端機工作階段的全域物件 `terminal`. 每個工作階段都是一個在 pty 中執行的系統 shell (`/system/bin/sh`), 離開介面後繼續在背景執行.

P2 開發預覽: 已實現 shell 工作階段, 儲存存取, 帶簽章信任的 Node.js 整合與宿主工作階段控制. 終端介面, 指令碼 API 與設定頁將繼續按 ROADMAP.md 的階段推進. AutoJs6 6.8.0 (build 5304+).

### 使用方法

1. 在安裝了 AutoJs6 建置 5304 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 安裝與裝置 ABI 對應的外掛 APK (或 universal).
2. 開啟 AutoJs6 外掛中心, 確認 `3-Shell Terminal` 已被識別並啟用它.
3. 在 AutoJs6 主頁抽屜開啟 "終端機", 在檔案管理員中對目錄選擇 "在終端機中開啟", 或在指令碼中呼叫 `terminal.open(...)`. 需要進入共用儲存空間 (如 `/sdcard`) 下的目錄時, 按外掛提示授予 "所有檔案存取權限".

### Node.js 命令

- 需要 Node.js Runtime 外掛 1.5.0 或更高版本; 外掛讀取其清單契約, 驗證簽署, 啟動器與 npm / corepack 封存後, 在每次新建工作階段時把命令連結進 `PATH`. 未安裝或驗證失敗時終端機仍可用, 只是不含這些命令.
- Android 禁止執行應用程式寫出的檔案: `node_modules/.bin/*` 與 npm 套件自帶的原生可執行檔會以 `EACCES` 失敗, 請改用 `node <進入點檔案>` 或 `npx`; 原生擴充 (`.node`) 不可載入.
- corepack 預設使用內建的 pnpm 11.x 與 Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`), 顯式指定版本時按指定版本下載; npm 鏡像來源可在設定中切換為 npmmirror 或自訂 https 位址.

安裝指南與目前進度請參閱 [項目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
