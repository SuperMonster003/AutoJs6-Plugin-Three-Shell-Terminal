<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 及其指令碼提供多工作階段終端機, 在 pty 中執行系統 shell, 支援背景執行, 快捷按鍵及 Node.js 命令</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Shell Terminal 接管 AutoJs6 的內建終端機: 主頁抽屜的 "終端機" 開關, 檔案管理員目錄選單與專案工具列的 "在終端機中開啟", 以及指令碼側用於開啟, 驅動與監聽終端機工作階段的全域物件 `terminal`. 每個工作階段都是一個在 pty 中執行的系統 shell (`/system/bin/sh`), 離開介面後繼續在背景執行.

AutoJs6 透過 Binder 服務發現外掛, 以顯式 Intent 開啟終端機介面, 並經 Binder 取得工作階段數, 關閉全部工作階段或驅動指令碼工作階段; 工作階段輸出經管道傳回指令碼. 安裝了 Node.js Runtime 外掛時, 終端機直接讀取其清單契約, 驗證簽署與啟動器後提供 node / npm / npx / corepack / yarn / pnpm 命令.

******

### 目前狀態

******

P2 開發預覽: 已實現 shell 工作階段, 儲存存取, 帶簽章信任的 Node.js 整合與宿主工作階段控制. 終端介面, 指令碼 API 與設定頁將繼續按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 的階段推進. AutoJs6 6.8.0 (build 5304+).

******

### 功能

******

外掛程式提供以下能力:

- 多工作階段: 新建, 切換, 關閉與工作階段管理員; 離開介面後工作階段由前景服務保持執行, 通知欄顯示目前目錄與工作階段數並提供 "關閉全部".
- 終端機介面: 兩列快捷按鍵列 (Esc / Tab / Ctrl / 方向鍵 / 常用符號), 原生文字選取與複製 / 全選, 轉錄複製與分享, 字型大小設定, 貼上與清除畫面.
- Node.js 工具鏈: 安裝 Node.js Runtime 外掛 (1.5.0+) 後可用 node / npm / npx / corepack / yarn / pnpm, npm 鏡像來源與 "忽略安裝指令碼" 設定, 以及 npm init / install / 執行指令碼等套件管理選單.
- AutoJs6 入口: 主頁抽屜開關 (顯示工作階段數, 可關閉全部), 檔案管理員目錄選單與專案工具列的 "在終端機中開啟".
- 指令碼 API `terminal` (別名 `$terminal`): 工作階段管理, 可見執行 (`exec`, `npm.run`), 以及帶 `output` / `exit` 事件, `write` 與 `waitFor` 的工作階段物件; 每個失敗都是帶穩定 `code` 的 `TerminalError`.
- 獨立應用程式: 啟動器圖示直接進入終端機, 設定頁 (外觀跟隨 AutoJs6, 字型大小, npm 鏡像來源, Node.js 整合, 所有檔案存取, 清除終端機資料), 關於與發行歷史.

******

### 使用方法

******

1. 在安裝了 AutoJs6 建置 5304 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 安裝與裝置 ABI 對應的外掛 APK (或 universal).
2. 開啟 AutoJs6 外掛中心, 確認 `3-Shell Terminal` 已被識別並啟用它.
3. 在 AutoJs6 主頁抽屜開啟 "終端機", 在檔案管理員中對目錄選擇 "在終端機中開啟", 或在指令碼中呼叫 `terminal.open(...)`. 需要進入共用儲存空間 (如 `/sdcard`) 下的目錄時, 按外掛提示授予 "所有檔案存取權限".

******

### Node.js 命令

******

終端機如何取得 node / npm 以及相關限制:

- 需要 Node.js Runtime 外掛 1.5.0 或更高版本; 外掛讀取其清單契約, 驗證簽署, 啟動器與 npm / corepack 封存後, 在每次新建工作階段時把命令連結進 `PATH`. 未安裝或驗證失敗時終端機仍可用, 只是不含這些命令.
- Android 禁止執行應用程式寫出的檔案: npm 預設關閉 bin 連結, 因而 `node_modules/.bin/*` 與 `npx <套件名稱>` 不能直接執行套件入口. 請使用 `node node_modules/<套件名稱>/<入口>.js`. npm 套件附帶的原生可執行檔案會以 `EACCES` 失敗, 原生擴充 (`.node`) 無法載入.
- corepack 預設使用內建的 pnpm 11.x 與 Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`), 顯式指定版本時按指定版本下載; npm 鏡像來源可在設定中切換為 npmmirror 或自訂 https 位址.

******

### 快速開始

******

一個開啟指令碼目錄, 可見地安裝相依套件並等待結束, 以及驅動互動式命令的指令碼 (自路線圖 P4 起可用):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let init = terminal.exec('npm init', { cwd: files.cwd(), show: true });
init.on('output', line => { if (/package name/i.test(line)) init.write('\n'); });
init.waitFor(/Is this OK\?/i, 60e3);
init.write('yes\n');
init.on('exit', code => console.log('npm init exited with ' + code));
terminal.npm.run('build', files.cwd());
```

******

### 相容性

******

決定外掛程式能力邊界的平台事實:

- Android 7.0 (API 24) 及以上; 提供 arm64-v8a, armeabi-v7a, x86_64, x86 四個 ABI 的 APK 與 universal APK, 原生程式庫按 16 KB 分頁對齊; 主程式建置與外掛在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) 列出的裝置矩陣上一起驗證.
- 終端機程序以外掛自身的 uid 與權限執行, 不繼承 AutoJs6 的權限; 需要 AutoJs6 權限的命令請改用指令碼的 `shell()` API.
- 工作階段隨外掛程序存在; 程序被系統結束後工作階段無法恢復, 前景服務與通知用於降低這種情況的發生.

******

### 常見問題

******

- **為什麼 `cd /sdcard/腳本` 失敗?** 外掛需要自己的儲存授權. 開啟外掛設定或按終端機橫幅的提示授予 "所有檔案存取權限" (Android 11+), 舊系統則授予儲存權限.
- **為什麼沒有 node 命令?** 請在 AutoJs6 外掛中心安裝 Node.js Runtime 外掛 (1.5.0+); 外掛設定中的 "環境探測" 會顯示具體原因 (未安裝, 版本過舊, 簽署不受信任或啟動器不可執行).
- **離開終端機後命令還在執行嗎?** 是. 工作階段由前景服務保持, 通知欄顯示工作階段數; 關閉通知中的 "關閉全部", 抽屜開關或工作階段本身才會結束 shell.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 服務與介面入口受 `org.autojs.permission.PLUGIN` 簽署權限保護並驗證呼叫方簽署, 只有 AutoJs6 能夠存取; 啟動器入口只開啟終端機, 不接收外部命令.
- 儲存權限 (Android 11+ 為 "所有檔案存取權限") 只用於進入你選擇的目錄; 終端機不會掃描或上傳檔案.
- `INTERNET` 權限由你在 shell 中執行的命令 (如 `npm install`) 使用, 以及手動檢查更新時存取本外掛固定的 GitHub Releases 介面; 外掛自身不在背景連線.
- Node.js Runtime 外掛的啟動器只在其簽署為官方簽署 (或與本外掛一致) 時執行; 外掛不記錄工作階段輸入輸出, 並將私有儲存排除在備份之外.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼發現外掛並協商相容性:

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

`ThreeShellTerminalPluginService` 回應 `org.autojs.plugin.TERMINAL` (category `terminal`), 自路線圖 P2 起實作主程式 terminal-api 契約 `org.autojs.plugin.terminal.api.ITerminalPlugin`. `ThreeShellTerminalPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供主程式啟用外掛; 終端機介面經 `org.autojs.plugin.TERMINAL_OPEN` 開啟.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/10/02_

- `提示` P2 開發預覽: 已實現 shell 工作階段, 儲存存取, 帶簽章信任的 Node.js 整合與宿主工作階段控制. 終端介面, 指令碼 API 與設定頁將繼續按 ROADMAP.md 的階段推進.
- `新增` 外掛識別碼 `three-shell-terminal` (engine `terminal`), 含 INFO 服務, Wake Activity 以及供主程式發現的 `org.autojs.plugin.TERMINAL` 服務骨架
- `新增` 按 ABI 拆分的 APK (arm64-v8a, armeabi-v7a, x86_64, x86) 與 universal APK, 原生程式庫按 16 KB 分頁對齊
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌
- `新增` 自主程式終端機遷入工作階段核心: 基於 pty 的 shell 工作階段與處理程序級註冊表 (記錄標題與結束代碼供 Binder 使用), 外掛自有檔案目錄下的工作階段環境與目錄佈局, Node.js 啟動器發現與 npm / corepack 安裝器, 以及保持工作階段執行並提供 "關閉工作階段" 通知的前景服務 (頻道 `three.shell.terminal.sessions`)
- `新增` 儲存空間存取解析 (`StorageAccess`): 以外掛自身的權限狀態判定 (API 30 以下為舊式執行階段權限, API 30 起為 "所有檔案存取權限"), 識別 `/sdcard`, `/storage/...` 等共用儲存空間與自有 `Android/{data,obb,media}` 目錄, 起始目錄回退 `$HOME` 並給出 `STORAGE_PERMISSION_REQUIRED` 或 `DIRECTORY_INACCESSIBLE`, 以及開啟所有檔案存取開關的設定 Intent
- `新增` 帶簽署信任的 Node.js 整合 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): 僅當 Node.js Runtime 外掛由 AutoJs6 官方外掛金鑰或本外掛自身金鑰簽署時才使用, 設定開關在任何查詢之前短路, 每種結果對應到契約的 `node-cli` 狀態 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), 每次工作階段啟動時重新整理 `usr/bin` 命令連結, 按摘要只解壓一次 npm / corepack 封存並匯出 npm / corepack 環境變數
- `新增` AutoJs6 可建立和控制最多 16 個終端工作階段, 每個工作階段支援最多 4 個即時輸出監聽, 並可讀取最近輸出和查詢 shell 環境. 關閉 AutoJs6 後工作階段繼續執行; 無效要求會傳回具體原因.
- `新增` 套件管理支援 npm init, 安裝相依套件或指定套件, 讀取並執行 package.json 指令碼, 查看 Yarn / pnpm 命令與搜尋 npm. 支援 npmjs, npmmirror 與自訂 HTTPS 鏡像來源, 以及忽略安裝指令碼. 清除終端資料會先關閉所有工作階段, 再清空 home / usr 並重建目錄, 保留設定和外部專案. 選單與設定頁將於後續階段接入.
- `新增` 終端介面 (`TerminalActivity`): 宿主終端介面遷入插件自有的 Material 3 主題, 含快捷鍵欄 (Esc / Tab / Ctrl / Alt / 方向 / 翻頁), 雙指縮放字號, 長按選取並複製文字, 工作階段 / 文字 / 套件管理 / 設定 / 說明選單, 顯示 shell 目前目錄並可點按複製的工具列副標題, 以及說明 Node.js 執行環境缺失 / 不受信任 / 版本過舊 / 已停用並提供安裝 / 更新 / 啟用 / 詳情動作的 Node.js 橫幅; 無法進入共用儲存目錄時出現儲存橫幅並提供 "授予" 與 "重新進入目錄"; 介面透過宿主設定提供者跟隨 AutoJs6 的語言, 夜間模式與主題色, 無宿主時回退至系統值與共用的 `#FFDEAD` 顏色
- `新增` 工作階段管理員: 含狀態 / 控制 / 工作階段 / 設定四個可收合分組的對話框, 列出全部執行中的工作階段及其目錄, PID 與執行時間, 可開啟或關閉單一工作階段, 新建工作階段, 關閉全部, 檢視詳情並複製, 並提供字號, npm 鏡像源與 ignore-scripts 設定; 它與宿主 `onSessionsChanged` 使用同一工作階段登錄表, 可從終端選單, 工作階段通知 (點按) 進入, 宿主的 `manager=true` 入口則由透明的 `TerminalManagerActivity` 承載, 關閉後不會留下終端介面
- `修復` 系統限制背景活動時, 啟動工作階段不再導致外掛程式崩潰; 工作階段會在沒有前景服務保護的情況下繼續執行.
- `修復` 讀取較長轉錄時保留最新文字, 並控制回覆大小, 避免跨程序訊息超限.
- `依賴` 附加 jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) 作為終端機模擬與 pty 原生程式庫, 並在 `locks/vendored-aars.lock` 中鎖定雜湊
- `依賴` 附加 `common-plugin-api.aar` 與 `nodejs-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api` 與 `plugin-api/nodejs-api`, 主程式建置 6.8.0 / 5303, MPL 2.0) 作為共用外掛契約與 Node.js 清單契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `依賴` 附加 `terminal-api.aar` (AutoJs6 模組 `plugin-api/terminal-api`, 主程式建置 6.8.0 / 5304, MPL 2.0) 作為終端機契約 V1 (`ITerminalPlugin` / `ITerminalCallback`, 身份, 上限與錯誤碼), 外掛身份常數改由它提供, 並在 `locks/host-api-aars.lock` 中鎖定雜湊

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

******

### 建置與驗證

******

本節面向希望從原始碼建置外掛的開發者; 一般使用者直接安裝 Releases 頁面的預建 APK 即可.

建置 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並建置 instrumentation 測試 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集發佈產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與生成產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 本地化與文件生成

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部生成產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- 終端機模組文件: https://docs.autojs6.com/#/terminal
- Node.js Runtime 外掛: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (終端機模擬與 pty 原生程式庫, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
