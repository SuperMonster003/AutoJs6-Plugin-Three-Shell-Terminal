<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 とそのスクリプトのためのマルチセッション端末. pty でシステムシェルを実行し, バックグラウンド実行, キーバー, Node.js コマンドに対応</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語

******

現在の README.md は以下の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### はじめに

******

3-Shell Terminal は AutoJs6 の内蔵ターミナルを引き継ぎます: ホームドロワーの "ターミナル" スイッチ, ファイルマネージャーのディレクトリメニューとプロジェクトツールバーの "ターミナルで開く", そしてセッションを開き, 操作し, 監視するためのスクリプト側グローバルオブジェクト `terminal`. 各セッションは pty 内で動くシステムシェル (`/system/bin/sh`) で, 画面を離れてもバックグラウンドで動き続けます.

AutoJs6 は Binder サービスでプラグインを検出し, 明示的な Intent でターミナル画面を開き, Binder を通じてセッション数の取得, 全セッションの終了, スクリプトセッションの操作を行います. セッションの出力はパイプでスクリプトに戻ります. Node.js Runtime プラグインがインストールされていると, ターミナルはそのマニフェスト契約を直接読み取り, 署名とランチャーを検証したうえで node / npm / npx / corepack / yarn / pnpm を提供します.

******

### 現在の状態

******

1.0.0 は独立した端末, バックグラウンドセッション, 対話型スクリプト API と設定画面を提供します. 完全なスクリプト API には AutoJs6 6.8.0 ビルド 5315 以降が必要です. 基本プラグインプロトコルの要件はビルド 5304 です. Android 7.0 以降に対応します.

******

### 機能

******

プラグインは以下の機能を提供します:

- 複数セッション: 作成, 切り替え, 終了とセッションマネージャー. 画面を離れた後もフォアグラウンドサービスがセッションを維持し, 通知に現在のディレクトリとセッション数, "セッションを終了" 操作を表示します.
- ターミナル画面: 2 段のキーバー (Esc / Tab / Ctrl / 矢印 / よく使う記号), ネイティブのテキスト選択とコピー / すべて選択, 記録のコピーと共有, 文字サイズ, 貼り付けと消去.
- Node.js ツールチェーン: Node.js Runtime プラグイン (1.5.0+) をインストールすると node / npm / npx / corepack / yarn / pnpm が使え, npm レジストリと "インストールスクリプトを無視" の設定, パッケージメニュー (npm init / install / run script など) も利用できます.
- AutoJs6 の入口: ホームドロワーのスイッチ (セッション数, すべて終了), ファイルマネージャーのディレクトリメニューとプロジェクトツールバーの "ターミナルで開く".
- スクリプト API `terminal` (別名 `$terminal`): セッション管理, 可視実行 (`exec`, `npm.run`), `output` / `exit` イベントと `write`, `waitFor` を持つセッションオブジェクト. 失敗はすべて安定した `code` を持つ `TerminalError` です.
- 単体アプリ: ランチャーアイコンから直接ターミナルを開き, 設定ページ (AutoJs6 に追従する外観, 文字サイズ, npm レジストリ, Node.js 連携, すべてのファイルへのアクセス, ターミナルデータの消去), 情報, リリース履歴を備えます.

******

### 使い方

******

1. AutoJs6 build 5304 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) から端末の ABI に合うプラグイン APK (または universal APK) をインストールします.
2. AutoJs6 のプラグインセンターを開き, `3-Shell Terminal` が認識されていることを確認して有効にします.
3. AutoJs6 のホームドロワーで "ターミナル" をオンにするか, ファイルマネージャーでディレクトリの "ターミナルで開く" を選ぶか, スクリプトから `terminal.open(...)` を呼び出します. `/sdcard` などの共有ストレージ内のディレクトリに入るには, プラグインの案内に従って "すべてのファイルへのアクセス" を許可してください.

******

### Node.js コマンド

******

ターミナルが node / npm を得る仕組みと制限:

- Node.js Runtime プラグイン 1.5.0 以降が必要です. プラグインはそのマニフェスト契約を読み, 署名, ランチャー, npm / corepack アーカイブを検証したうえで, セッション開始のたびにコマンドを `PATH` にリンクします. プラグインがない場合や検証に失敗した場合も, これらのコマンドを除いてターミナルは使えます.
- Android はアプリが書き出したファイルの実行を禁止します. npm は既定で bin リンクを無効にするため, `node_modules/.bin/*` や `npx <パッケージ>` で入口を直接実行できません. `node node_modules/<パッケージ>/<入口>.js` を使用してください. npm パッケージ内のネイティブ実行ファイルは `EACCES` で失敗し, ネイティブアドオン (`.node`) は読み込めません.
- corepack は既定で同梱の pnpm 11.x と Yarn 1.x を使い (`COREPACK_DEFAULT_TO_LATEST=0`), 明示的に指定したバージョンは要求時にダウンロードします. npm レジストリは設定で npmmirror またはカスタム https URL に切り替えられます.

******

### クイックスタート

******

スクリプトのディレクトリを開き, 依存関係を見える形でインストールして結果を待ち, 対話的なコマンドを操作するスクリプト (ロードマップ P4 以降で利用可能):

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

### 互換性

******

プラグインの能力を左右するプラットフォームの事実:

- Android 7.0 (API 24) 以降. arm64-v8a, armeabi-v7a, x86_64, x86 の APK と universal APK を提供し, ネイティブライブラリは 16 KB ページに整列しています. ホストビルドとプラグインは [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) の端末マトリクスで一緒に検証されます.
- ターミナルのプロセスはプラグイン自身の uid と権限で動作し, AutoJs6 の権限を引き継ぎません. AutoJs6 の権限が必要なコマンドにはスクリプトの `shell()` API を使ってください.
- セッションはプラグインのプロセスが生きている間だけ存在します. システムがプロセスを終了すると復元できませんが, フォアグラウンドサービスと通知がその可能性を下げます.

******

### よくある質問

******

- **`cd /sdcard/Scripts` が失敗するのはなぜ?** プラグインには独自のストレージ許可が必要です. プラグイン設定を開くか, ターミナルのバナーに従って "すべてのファイルへのアクセス" (Android 11+) または旧システムではストレージ権限を許可してください.
- **node コマンドがないのはなぜ?** AutoJs6 プラグインセンターから Node.js Runtime プラグイン (1.5.0+) をインストールしてください. プラグイン設定の "環境プローブ" が正確な理由 (未インストール, 古すぎる, 信頼されない署名, ランチャーが実行不可) を表示します.
- **システムはバックグラウンドセッションを停止できますか?** はい. プラグインのプロセス終了後はセッションを復元できません. HyperOS や MIUI などでは Android 設定で通知とバックグラウンド動作を許可してください. 端末を開くと許可された範囲で前景保護を再試行します. 通常は画面を離れてもセッションが続きますが, 前景サービスもシステム制限を回避できません.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder サービスと画面の入口は署名権限 `org.autojs.permission.PLUGIN` で保護され, 呼び出し元の署名を検証するため AutoJs6 だけがアクセスできます. ランチャーの入口はターミナルを開くだけで外部コマンドを受け付けません.
- ストレージ権限 (Android 11+ では "すべてのファイルへのアクセス") は, あなたが選んだディレクトリに入るためだけに使われます. ターミナルがファイルを走査したりアップロードしたりすることはありません.
- `INTERNET` 権限は, シェルで実行するコマンド (`npm install` など) と, このプラグイン固定の GitHub Releases API への手動更新確認で使われます. プラグイン自身がバックグラウンドで通信することはありません.
- Node.js Runtime プラグインのランチャーは, その署名が公式 (またはこのプラグインと同一) の場合にのみ実行されます. プラグインはセッションの入出力を記録せず, 私用ストレージをバックアップから除外します.

プラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストおよびプラグインの開発者向けです. ホストはこれらの識別子を使ってプラグインを検出し, 互換性を交渉します:

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

`ThreeShellTerminalPluginService` は `org.autojs.plugin.TERMINAL` (category `terminal`) に応答し, ロードマップ P2 以降でホストの terminal-api 契約 `org.autojs.plugin.terminal.api.ITerminalPlugin` を実装します. `ThreeShellTerminalPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo で応答します. `WakeActivity` はホストがプラグインを起動するためのもので, ターミナル画面は `org.autojs.plugin.TERMINAL_OPEN` で開かれます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/10/07_

- `ヒント` 1.0.0 は独立した端末, バックグラウンドセッション, 対話型スクリプト API と設定画面を提供します. 完全なスクリプト API には AutoJs6 6.8.0 ビルド 5315 以降が必要です. 基本プラグインプロトコルの要件はビルド 5304 です. Android 7.0 以降に対応します
- `機能` 複数セッション: 作成, 切り替え, 終了とセッションマネージャー. 画面を離れた後もフォアグラウンドサービスがセッションを維持し, 通知に現在のディレクトリとセッション数, "セッションを終了" 操作を表示します
- `機能` ターミナル画面: 2 段のキーバー (Esc / Tab / Ctrl / 矢印 / よく使う記号), ネイティブのテキスト選択とコピー / すべて選択, 記録のコピーと共有, 文字サイズ, 貼り付けと消去
- `機能` Node.js ツールチェーン: Node.js Runtime プラグイン (1.5.0+) をインストールすると node / npm / npx / corepack / yarn / pnpm が使え, npm レジストリと "インストールスクリプトを無視" の設定, パッケージメニュー (npm init / install / run script など) も利用できます
- `機能` AutoJs6 の入口: ホームドロワーのスイッチ (セッション数, すべて終了), ファイルマネージャーのディレクトリメニューとプロジェクトツールバーの "ターミナルで開く"
- `機能` スクリプト API `terminal` (別名 `$terminal`): セッション管理, 可視実行 (`exec`, `npm.run`), `output` / `exit` イベントと `write`, `waitFor` を持つセッションオブジェクト. 失敗はすべて安定した `code` を持つ `TerminalError` です
- `機能` 単体アプリ: ランチャーアイコンから直接ターミナルを開き, 設定ページ (AutoJs6 に追従する外観, 文字サイズ, npm レジストリ, Node.js 連携, すべてのファイルへのアクセス, ターミナルデータの消去), 情報, リリース履歴を備えます
- `機能` ABI 別 APK (arm64-v8a, armeabi-v7a, x86_64, x86) と universal APK, 16 KB ページに整列したネイティブライブラリ
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
- `修正` システムがバックグラウンド動作を制限していても, セッション開始時にプラグインがクラッシュしなくなりました. セッションはフォアグラウンドサービスの保護なしで継続します.
- `修正` 長い出力履歴は最新のテキストを保持し, プロセス間応答のサイズ上限を超えないようにします.
- `修正` スクリプトによる出力の読み取りと再生で画面末尾の空行を除去し, プロンプトの空白と後続出力との行境界を保持
- `修正` 起動中のセッションがホストの照会から一時的に消え, 表示付き実行で端末を開けない問題
- `修正` 大量の連続出力が端末のメッセージキューをブロックしなくなり, 起動中のセッション終了と子プロセスの自然終了で pty と入出力スレッドを解放
- `修正` バックグラウンド制限で前景サービスを起動できなかった場合, 既存の端末を開くと保護の開始を再試行
- `修正` プラグインのプロセス終了後にタスクを開くと新しいシェルを作成し, 以前のコマンドを再実行しないように修正
- `改善` ランチャーとプラグインセンターのアイコンの見た目の大きさを統一し, 透明な背景と白黒または無彩色のグレースケールを使用
- `改善` プラグインセンターのアイコンに Icon Studio で調整したサイズ, 位置, 明暗の図稿と円形背景を適用し, 再生成可能な原稿とパラメーターを保持
- `改善` Android のアプリ情報アイコンに Icon Studio と共通の図案と明暗の背景色を使用し, プラグインセンターの透明な図案と既存のランチャー設定を維持
- `依存関係` 端末エミュレーションと pty ネイティブライブラリとして jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) を追加し, `locks/vendored-aars.lock` でハッシュを固定
- `依存関係` 共有プラグイン契約と Node.js マニフェスト契約として `common-plugin-api.aar` と `nodejs-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api` と `plugin-api/nodejs-api`, ホストビルド 6.8.0 / 5303, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` ターミナル契約 V1 (`ITerminalPlugin` / `ITerminalCallback`, ID, 上限, エラーコード) として `terminal-api.aar` (AutoJs6 モジュール `plugin-api/terminal-api`, ホストビルド 6.8.0 / 5304, MPL 2.0) を追加. プラグインの ID 定数はこれから取得し, `locks/host-api-aars.lock` でハッシュを固定

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

このセクションはソースからプラグインをビルドしたい開発者向けです. 通常のユーザーは Releases ページのビルド済み APK をインストールするだけで済みます.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM ユニットテストを実行し, インストルメンテーションテスト APK をビルドする:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする:

```powershell
.\gradlew.bat :app:assembleRelease
```

リリース成果物を収集し, ファイル名にバージョンと CRC32 ダイジェストを追加する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントのソースと生成物が同期していることを検証する (CI でも実施):

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降と Android SDK 37 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

******

### ローカライズとドキュメント生成

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

`.readme/` と `.changelog/` の言語 JSON ファイルが README, プラグインセンターの説明, 変更履歴の唯一のソースです. 常にこれらの JSON ソースを編集して `py .python/generate_markdown.py` を再実行してください. 生成された README, `plugin_instruction.md`, 変更履歴は手で編集しません. `py .python/generate_markdown.py --check` を実行するとすべての生成物を検証できます.

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE) の下で提供されます. サードパーティのコンポーネントとそのライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md) に記載しています.

******

### リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- ターミナルモジュールのドキュメント: https://docs.autojs6.com/#/terminal
- Node.js Runtime プラグイン: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (端末エミュレーションと pty ネイティブライブラリ, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
