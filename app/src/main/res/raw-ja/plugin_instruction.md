3-Shell Terminal は AutoJs6 の内蔵ターミナルを引き継ぎます: ホームドロワーの "ターミナル" スイッチ, ファイルマネージャーのディレクトリメニューとプロジェクトツールバーの "ターミナルで開く", そしてセッションを開き, 操作し, 監視するためのスクリプト側グローバルオブジェクト `terminal`. 各セッションは pty 内で動くシステムシェル (`/system/bin/sh`) で, 画面を離れてもバックグラウンドで動き続けます.

P2 開発プレビュー: shell セッション, ストレージアクセス, 署名を検証する Node.js 統合, ホストからのセッション操作を実装済みです. 端末画面, スクリプト API, 設定画面は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) の段階に従って実装します. AutoJs6 6.8.0 (build 5304+).

### 使い方

1. AutoJs6 build 5304 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) から端末の ABI に合うプラグイン APK (または universal APK) をインストールします.
2. AutoJs6 のプラグインセンターを開き, `3-Shell Terminal` が認識されていることを確認して有効にします.
3. AutoJs6 のホームドロワーで "ターミナル" をオンにするか, ファイルマネージャーでディレクトリの "ターミナルで開く" を選ぶか, スクリプトから `terminal.open(...)` を呼び出します. `/sdcard` などの共有ストレージ内のディレクトリに入るには, プラグインの案内に従って "すべてのファイルへのアクセス" を許可してください.

### Node.js コマンド

- Node.js Runtime プラグイン 1.5.0 以降が必要です. プラグインはそのマニフェスト契約を読み, 署名, ランチャー, npm / corepack アーカイブを検証したうえで, セッション開始のたびにコマンドを `PATH` にリンクします. プラグインがない場合や検証に失敗した場合も, これらのコマンドを除いてターミナルは使えます.
- Android はアプリが書き出したファイルの実行を禁止します. npm は既定で bin リンクを無効にするため, `node_modules/.bin/*` や `npx <パッケージ>` で入口を直接実行できません. `node node_modules/<パッケージ>/<入口>.js` を使用してください. npm パッケージ内のネイティブ実行ファイルは `EACCES` で失敗し, ネイティブアドオン (`.node`) は読み込めません.
- corepack は既定で同梱の pnpm 11.x と Yarn 1.x を使い (`COREPACK_DEFAULT_TO_LATEST=0`), 明示的に指定したバージョンは要求時にダウンロードします. npm レジストリは設定で npmmirror またはカスタム https URL に切り替えられます.

インストール手順と現在の進捗は [プロジェクト README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) を参照してください.
