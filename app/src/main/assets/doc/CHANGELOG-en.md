******

### Release History

******

# v1.0.0

###### 2026/10/07

* `Hint` Version 1.0.0 provides the standalone terminal, background sessions, interactive script APIs and settings. Complete script APIs require AutoJs6 6.8.0 build 5315 or later; build 5304 is the base plugin-protocol requirement. Android 7.0 or later is supported
* `Feature` Multiple sessions: create, switch, close and a session manager; a foreground service keeps sessions running after the screen is left, and its notification shows the current directory and the session count with a "Close sessions" action
* `Feature` Terminal screen: a two-row key bar (Esc / Tab / Ctrl / arrows / common symbols), native text selection with Copy / Select all, transcript copy and share, text size, paste and clear
* `Feature` Node.js toolchain: with the Node.js Runtime plugin (1.5.0+) installed, node / npm / npx / corepack / yarn / pnpm become available, together with the npm registry and "ignore install scripts" settings and the package menu (npm init / install / run script and more)
* `Feature` AutoJs6 entries: the home drawer switch (session count, close all), "Open in terminal" in the file manager directory menu and the project toolbar
* `Feature` Script API `terminal` (alias `$terminal`): session management, visible execution (`exec`, `npm.run`) and a session object with `output` / `exit` events, `write` and `waitFor`; every failure is a `TerminalError` with a stable `code`
* `Feature` Standalone app: the launcher icon opens the terminal directly; a settings page (appearance following AutoJs6, text size, npm registry, Node.js integration, all files access, clear terminal data), About and the release history
* `Feature` APKs split by ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal APK, with native libraries aligned to 16 KB pages
* `Feature` README, plugin center instruction and changelog in 10 languages
* `Fix` A system restriction on background activity no longer crashes the plugin when a session starts. The session continues without foreground service protection.
* `Fix` Long transcript reads keep their newest text within the cross-process reply size limit.
* `Fix` Script transcript reads and output replay omit trailing screen padding while preserving prompt spaces and the boundary before live output
* `Fix` A starting session could briefly disappear from host queries and prevent visible execution from opening its terminal
* `Fix` Continuous output no longer blocks the terminal message queue; closing sessions during startup and natural process exits release their ptys and I/O workers
* `Fix` Opening an existing terminal retries foreground-service protection when background restrictions prevented it from starting
* `Improvement` Consistent visual sizing for launcher and Plugin Center icons, with transparent backgrounds and neutral black, white or grayscale artwork
* `Improvement` Plugin Center icons use the sizes, positions, light and dark artwork, and circular backgrounds adjusted in Icon Studio, retaining reproducible sources and parameters
* `Improvement` Android App info icons share Icon Studio artwork and light/dark backgrounds while preserving transparent Plugin Center artwork and existing launcher choices
* `Dependency` Added jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) as the terminal emulation and pty native libraries, hash-locked in `locks/vendored-aars.lock`
* `Dependency` Added `common-plugin-api.aar` and `nodejs-api.aar` (AutoJs6 modules `plugin-api/common-plugin-api` and `plugin-api/nodejs-api`, host build 6.8.0 / 5303, MPL 2.0) as the shared plugin contract and the Node.js manifest contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added `terminal-api.aar` (AutoJs6 module `plugin-api/terminal-api`, host build 6.8.0 / 5304, MPL 2.0) as the terminal contract V1 (`ITerminalPlugin` / `ITerminalCallback`, identity, ceilings and error codes); the plugin identity constants now come from it, hash-locked in `locks/host-api-aars.lock`
