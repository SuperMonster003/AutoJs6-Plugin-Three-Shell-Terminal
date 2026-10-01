******

### リリース履歴

******

# v1.0.0

###### 2026/10/01

* `ヒント` P0 開発プレビュー: リポジトリの骨組み, AutoJs6 プラグインセンターに認識されるプラグイン ID, および pty / ストレージ / Node.js ランチャーのスパイク. Binder 契約, セッションコア, ターミナル画面, スクリプト API, 設定ページは ROADMAP.md のフェーズに従って進みます.
* `機能` プラグイン ID `three-shell-terminal` (engine `terminal`), INFO サービス, Wake Activity, ホスト検出用の `org.autojs.plugin.TERMINAL` サービスの骨組み
* `機能` ABI 別 APK (arm64-v8a, armeabi-v7a, x86_64, x86) と universal APK, 16 KB ページに整列したネイティブライブラリ
* `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
* `依存関係` 端末エミュレーションと pty ネイティブライブラリとして jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) を追加し, `locks/vendored-aars.lock` でハッシュを固定
* `依存関係` 共有プラグイン契約と Node.js マニフェスト契約として `common-plugin-api.aar` と `nodejs-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api` と `plugin-api/nodejs-api`, ホストビルド 6.8.0 / 5303, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
