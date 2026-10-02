******

### リリース履歴

******

# v1.0.0

###### 2026/10/02

* `ヒント` P4 ローカル開発プレビュー: 端末画面, 複数セッションとスクリプト API を実装済み. 出力イベント, 対話入力と終了コードの待機に対応. 独立設定画面は ROADMAP.md の P5 で実装予定. API には P4 実装を含む AutoJs6 ビルドが必要.
* `機能` プラグイン ID `three-shell-terminal` (engine `terminal`), INFO サービス, Wake Activity, ホスト検出用の `org.autojs.plugin.TERMINAL` サービスの骨組み
* `機能` ABI 別 APK (arm64-v8a, armeabi-v7a, x86_64, x86) と universal APK, 16 KB ページに整列したネイティブライブラリ
* `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
* `機能` ホストのターミナルからセッションコアを移植: pty ベースのシェルセッションとプロセス全体のレジストリ (Binder 向けにタイトルと終了コードを記録), プラグイン自身のファイルディレクトリ配下のセッション環境とディレクトリ構成, Node.js ランチャーの検出と npm / corepack インストーラー, セッションを維持し "セッションを終了" 通知を提供するフォアグラウンドサービス (チャンネル `three.shell.terminal.sessions`)
* `機能` ストレージアクセスの解決 (`StorageAccess`): プラグイン自身の権限状態で判定 (API 30 未満は従来の実行時権限, API 30 以降は "すべてのファイルへのアクセス"), `/sdcard` や `/storage/...` などの共有ストレージと自身の `Android/{data,obb,media}` フォルダーの識別, 開始ディレクトリの `$HOME` へのフォールバックと `STORAGE_PERMISSION_REQUIRED` / `DIRECTORY_INACCESSIBLE` の理由, すべてのファイルへのアクセス設定を開く Intent
* `機能` 署名者の信頼を伴う Node.js 連携 (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): Node.js Runtime プラグインは AutoJs6 公式プラグイン鍵またはこのプラグイン自身の鍵で署名されている場合のみ使用, 設定スイッチはいかなる探索よりも先に短絡, すべての結果を契約の `node-cli` 状態 (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`) に対応付け, セッション開始ごとに `usr/bin` のコマンドリンクを更新し, npm / corepack アーカイブをダイジェストごとに一度だけ展開して npm / corepack 環境変数を出力
* `機能` AutoJs6 から最大 16 個の端末セッションを作成して操作し, セッションごとに最大 4 個のリスナーで出力を受信できます. 最近の出力と shell 環境も取得できます. AutoJs6 を閉じてもセッションは継続し, 無効な要求には具体的な理由が返されます.
* `機能` パッケージ管理で npm init, 依存関係や指定パッケージのインストール, package.json スクリプトの一覧と実行, Yarn / pnpm コマンド, npm 検索をサポートします. npmjs, npmmirror, カスタム HTTPS レジストリとインストールスクリプトの無視を設定できます. データ消去は全セッションを閉じてから home / usr を初期化し, 設定と外部プロジェクトを保持します. メニューと設定画面は後続の段階で追加します.
* `機能` ターミナル画面 (`TerminalActivity`): ホストのターミナル UI をプラグイン独自の Material 3 テーマに移植. キーバー (Esc / Tab / Ctrl / Alt / 矢印 / ページ送り), ピンチによる文字サイズ変更, 長押しによるテキスト選択とコピー, セッション / テキスト / パッケージ管理 / 設定 / ヘルプのメニュー, シェルの現在ディレクトリを表示しタップでコピーするツールバーの副題, Node.js Runtime の未インストール / 信頼されていない / 古い / 無効を説明しインストール / 更新 / 有効化 / 詳細の操作を提供する Node.js バナーを含む. 共有ストレージのディレクトリに入れない場合はストレージバナーが表示され "許可" と "ディレクトリに再度入る" を提供. 画面はホスト設定プロバイダーを通じて AutoJs6 の言語, ナイトモード, テーマカラーに従い, ホストがない場合はシステム値と共通の `#FFDEAD` 色にフォールバック
* `機能` セッションマネージャー: 折りたたみ可能なステータス / 操作 / セッション / 設定の各セクションを持つダイアログで, 実行中の全セッションをディレクトリ, PID, 稼働時間とともに一覧表示し, 個別のセッションを開く / 閉じる, 新規作成, 全て閉じる, 詳細の表示とコピー, 文字サイズ, npm レジストリ, ignore-scripts の設定を提供します. ホストの `onSessionsChanged` と同じセッションレジストリに従い, ターミナルメニュー, セッション通知 (タップ) から開けるほか, ホストの `manager=true` エントリは透明な `TerminalManagerActivity` が受け持ち, 閉じてもターミナル画面は残りません
* `機能` ホストエントリとランチャー: `org.autojs.permission.PLUGIN` 署名パーミッションで保護されたエクスポート済み `TERMINAL_OPEN` エントリ Activity は, 特定できる呼び出し元についてパーミッション保持とプラグインと同じ署名を確認し, `directory` / `sessionId` / `newSession` / `command` / `manager` の extras を契約上限に照らして検証したうえで, 独自タスクのターミナル画面または呼び出し元の上に重なるセッションマネージャーへ転送します. `LauncherActivity` (アイコン alias の転送先) は直近のセッションを復元するかホームで新規セッションを開始します. ホストから開いたターミナルで戻るとホストへ, ランチャーから開いた場合はホーム画面へ戻り, ターミナルのタスクは最近のタスクから消えるため起動要求が再実行されることはありません
* `機能` ランチャーアイコンの選択: 非公開のランチャー転送 Activity の上に 4 つの `MAIN / LAUNCHER` activity alias (アダプティブ ライト, アダプティブ ダーク, アダプティブ 自動, 透明背景) を用意し, 既定では自動アイコンを有効化; 選択は PackageManager により唯一有効な alias として保持され, 画面起動のたびとパッケージ更新後に修復され, 固定または動的ショートカットは選択した alias に移動
* `修正` システムがバックグラウンド動作を制限していても, セッション開始時にプラグインがクラッシュしなくなりました. セッションはフォアグラウンドサービスの保護なしで継続します.
* `修正` 長い出力履歴は最新のテキストを保持し, プロセス間応答のサイズ上限を超えないようにします.
* `修正` スクリプトによる出力の読み取りと再生で画面末尾の空行を除去し, プロンプトの空白と後続出力との行境界を保持
* `修正` 起動中のセッションがホストの照会から一時的に消え, 表示付き実行で端末を開けない問題
* `依存関係` 端末エミュレーションと pty ネイティブライブラリとして jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) を追加し, `locks/vendored-aars.lock` でハッシュを固定
* `依存関係` 共有プラグイン契約と Node.js マニフェスト契約として `common-plugin-api.aar` と `nodejs-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api` と `plugin-api/nodejs-api`, ホストビルド 6.8.0 / 5303, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
* `依存関係` ターミナル契約 V1 (`ITerminalPlugin` / `ITerminalCallback`, ID, 上限, エラーコード) として `terminal-api.aar` (AutoJs6 モジュール `plugin-api/terminal-api`, ホストビルド 6.8.0 / 5304, MPL 2.0) を追加. プラグインの ID 定数はこれから取得し, `locks/host-api-aars.lock` でハッシュを固定
