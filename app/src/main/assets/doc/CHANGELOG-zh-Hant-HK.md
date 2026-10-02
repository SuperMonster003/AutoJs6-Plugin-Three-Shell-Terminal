******

### 發行歷史

******

# v1.0.0

###### 2026/10/02

* `提示` P5 本機開發預覽: 已實作終端介面, 多工作階段管理, 指令碼 API (輸出監聽, 互動輸入與結束代碼等待), 獨立設定頁, 關於 / 版本歷史 / 檢查更新, 以及啟動器圖示. 指令碼 API 需使用包含 P4 實作的 AutoJs6 組建; 外掛中心的設定入口需支援 TERMINAL_SETTINGS 的組建.
* `新增` 外掛識別碼 `three-shell-terminal` (engine `terminal`), 含 INFO 服務, Wake Activity 以及供主程式發現的 `org.autojs.plugin.TERMINAL` 服務骨架
* `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 與 universal APK, 原生程式庫按 16 KB 分頁對齊
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` 自主程式終端機遷入工作階段核心: 基於 pty 的 shell 工作階段與處理程序級註冊表 (記錄標題與結束代碼供 Binder 使用), 外掛自有檔案目錄下的工作階段環境與目錄佈局, Node.js 啟動器發現與 npm / corepack 安裝器, 以及保持工作階段執行並提供 "關閉工作階段" 通知的前景服務 (頻道 `three.shell.terminal.sessions`)
* `新增` 儲存空間存取解析 (`StorageAccess`): 以外掛自身的權限狀態判定 (API 30 以下為舊式執行階段權限, API 30 起為 "所有檔案存取權限"), 識別 `/sdcard`, `/storage/...` 等共用儲存空間與自有 `Android/{data,obb,media}` 目錄, 起始目錄回退 `$HOME` 並給出 `STORAGE_PERMISSION_REQUIRED` 或 `DIRECTORY_INACCESSIBLE`, 以及開啟所有檔案存取開關的設定 Intent
* `新增` 帶簽署信任的 Node.js 整合 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): 僅當 Node.js Runtime 外掛由 AutoJs6 官方外掛金鑰或本外掛自身金鑰簽署時才使用, 設定開關在任何查詢之前短路, 每種結果對應到契約的 `node-cli` 狀態 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), 每次工作階段啟動時重新整理 `usr/bin` 命令連結, 按摘要只解壓一次 npm / corepack 封存並匯出 npm / corepack 環境變數
* `新增` AutoJs6 可建立和控制最多 16 個終端工作階段, 每個工作階段支援最多 4 個即時輸出監聽, 並可讀取最近輸出和查詢 shell 環境. 關閉 AutoJs6 後工作階段繼續執行; 無效要求會傳回具體原因.
* `新增` 套件管理支援 npm init, 安裝相依套件或指定套件, 讀取並執行 package.json 指令碼, 查看 Yarn / pnpm 命令與搜尋 npm. 支援 npmjs, npmmirror 與自訂 HTTPS 鏡像來源, 以及忽略安裝指令碼. 清除終端資料會先關閉所有工作階段, 再清空 home / usr 並重建目錄, 保留設定和外部專案. 選單與設定頁將於後續階段接入.
* `新增` 終端介面 (`TerminalActivity`): 宿主終端介面遷入插件自有的 Material 3 主題, 含快捷鍵欄 (Esc / Tab / Ctrl / Alt / 方向 / 翻頁), 雙指縮放字號, 長按選取並複製文字, 工作階段 / 文字 / 套件管理 / 設定 / 說明選單, 顯示 shell 目前目錄並可點按複製的工具列副標題, 以及說明 Node.js 執行環境缺失 / 不受信任 / 版本過舊 / 已停用並提供安裝 / 更新 / 啟用 / 詳情動作的 Node.js 橫幅; 無法進入共用儲存目錄時出現儲存橫幅並提供 "授予" 與 "重新進入目錄"; 介面透過宿主設定提供者跟隨 AutoJs6 的語言, 夜間模式與主題色, 無宿主時回退至系統值與共用的 `#FFDEAD` 顏色
* `新增` 工作階段管理員: 含狀態 / 控制 / 工作階段 / 設定四個可收合分組的對話框, 列出全部執行中的工作階段及其目錄, PID 與執行時間, 可開啟或關閉單一工作階段, 新建工作階段, 關閉全部, 檢視詳情並複製, 並提供字號, npm 鏡像源與 ignore-scripts 設定; 它與宿主 `onSessionsChanged` 使用同一工作階段登錄表, 可從終端選單, 工作階段通知 (點按) 進入, 宿主的 `manager=true` 入口則由透明的 `TerminalManagerActivity` 承載, 關閉後不會留下終端介面
* `新增` 宿主入口與啟動器: 匯出的 `TERMINAL_OPEN` 入口 Activity 受 `org.autojs.permission.PLUGIN` 簽名權限保護, 對可識別的呼叫方校驗權限持有與簽名一致, 按契約上限校驗 `directory` / `sessionId` / `newSession` / `command` / `manager` extras 並轉發到自有工作中的終端介面或覆蓋在呼叫方之上的工作階段管理員; `LauncherActivity` (圖示 alias 的目標) 恢復最近工作階段或在主目錄新建工作階段; 從宿主進入的終端按返回鍵回到宿主, 從啟動器進入的回到桌面, 終端工作隨之離開最近工作, 啟動請求不會被重放
* `新增` 啓動器圖標選擇: 私有啓動器轉發器之上的四個 `MAIN / LAUNCHER` activity alias (自適應亮色, 自適應暗色, 自適應自動, 透明背景), 預設啓用自動圖標; 選擇由 PackageManager 以唯一啓用的 alias 持久化, 每次界面啓動與套件更新後自動修復, 固定或動態快捷方式隨之遷移到所選 alias
* `新增` 關於, 版本歷史與檢查更新: 關於頁顯示已安裝版本, 建置號與建置日期, 開發者, 外掛授權條款 (MPL 2.0), 並在第三方元件分組中離線開啟內置的 jackpal Android-Terminal-Emulator 授權條款與聲明 (Apache-2.0), AutoJs6 外掛 API 聲明 (MPL 2.0) 與依賴庫聲明; 文件頁不使用 WebView, 按目前語言渲染內置更新日誌 (回退英文); 手動 "檢查更新" 僅經 HTTPS 存取 GitHub Releases API, 可取消, 有逾時, 結果複用 12 小時, 可忽略指定版本, 並提供開啟發行頁或內置版本歷史. 終端溢出選單新增 "關於"
* `新增` 獨立設定頁, 分外觀, 終端與資訊三組: 外觀含語言, 夜間模式, 主題色與啟動器圖示 (預設均跟隨 AutoJs6, 主題色提供 16 個預設與 HEX / rgb() 輸入); 終端含字號, npm 鏡像源, 忽略安裝指令碼, Node.js 整合開關, 環境偵測頁, 所有檔案存取與清除資料; 資訊含檢查更新, 版本歷史與關於. 選擇對話框先選後確定, 更改即時生效. 終端溢出選單的 "設定" 直接進入該頁 (原快捷設定子選單移除), AutoJs6 外掛中心的 "設定" 經 TERMINAL_SETTINGS 進入, 外掛能力宣告新增 settings
* `修復` 系統限制背景活動時, 啟動工作階段不再導致外掛程式崩潰; 工作階段會在沒有前景服務保護的情況下繼續執行.
* `修復` 讀取較長轉錄時保留最新文字, 並控制回覆大小, 避免跨程序訊息超限.
* `修復` 指令碼讀取或重播終端輸出時移除畫面填充的尾部空行, 保留提示字元空格與後續輸出的行邊界
* `修復` 工作階段從準備中進入執行中時, 宿主查詢偶爾找不到該工作階段, 導致可見執行無法開啟終端的問題
* `依賴` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作為終端機模擬與 pty 原生程式庫, 並在 `locks/vendored-aars.lock` 中鎖定雜湊
* `依賴` 附加 `common-plugin-api.aar` 與 `nodejs-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api` 與 `plugin-api/nodejs-api`, 主程式建置 6.8.0 / 5303, MPL 2.0) 作為共用外掛契約與 Node.js 清單契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `依賴` 附加 `terminal-api.aar` (AutoJs6 模組 `plugin-api/terminal-api`, 主程式建置 6.8.0 / 5304, MPL 2.0) 作為終端機契約 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限與錯誤碼), 外掛身份常數改由它提供, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
