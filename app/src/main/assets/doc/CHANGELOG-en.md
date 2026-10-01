******

### Release History

******

# v1.0.0

###### 2026/10/02

* `Hint` P2 development preview: shell sessions, storage access, trusted Node.js integration and host session control are implemented. The terminal screen, script API and settings page will follow the stages in ROADMAP.md.
* `Feature` Plugin identity `three-shell-terminal` (engine `terminal`) with the INFO service, the Wake Activity and the `org.autojs.plugin.TERMINAL` service skeleton for host discovery
* `Feature` APKs split by ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal APK, with native libraries aligned to 16 KB pages
* `Feature` README, plugin center instruction and changelog in 10 languages
* `Feature` Session core ported from the host terminal: pty-backed shell sessions with a process-wide registry (title and exit code recorded for the Binder), the session environment and directory layout under the plugin's own files directory, Node.js launcher discovery with the npm / corepack installer, and the foreground service that keeps sessions running with a "Close sessions" notification (channel `three.shell.terminal.sessions`)
* `Feature` Storage access resolution (`StorageAccess`): the plugin's own permission state (legacy runtime permissions below API 30, "All files access" from API 30), shared-storage detection for `/sdcard`, `/storage/...` and the own `Android/{data,obb,media}` folders, start-directory fallback to `$HOME` with `STORAGE_PERMISSION_REQUIRED` or `DIRECTORY_INACCESSIBLE`, and the settings intents that open the all-files-access switch
* `Feature` Node.js integration with signer trust (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): the Node.js Runtime plugin is used only when it is signed by the official AutoJs6 plugin key or by this plugin's own key, the settings switch short-circuits before any lookup, every outcome maps onto the contract's `node-cli` states (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), and each session start refreshes the `usr/bin` command links, extracts the npm / corepack archive once per digest and exports the npm / corepack environment
* `Feature` AutoJs6 can create and control up to 16 terminal sessions, receive live output with up to 4 listeners per session, read recent output and query the shell environment. Closing AutoJs6 leaves the sessions running; invalid requests are rejected with a specific reason.
* `Feature` Package management supports npm init, dependency and package installation, listing and running package.json scripts, Yarn / pnpm commands and npm search. Registry choices include npmjs, npmmirror and custom HTTPS URLs, with an option to ignore install scripts. Clearing terminal data closes all sessions before resetting home / usr and rebuilding the layout, preserving settings and external projects. Menus and settings UI will follow in later stages.
* `Feature` Terminal screen (`TerminalActivity`): the host's terminal UI ported onto the plugin's own Material 3 theme, with the key bar (Esc / Tab / Ctrl / Alt / arrows / paging), pinch-to-zoom text size, long-press text selection with copy, the session / text / package-management / settings / help menus, a toolbar subtitle that shows the shell's directory and copies it on tap, and a Node.js banner that explains a missing, untrusted, outdated or disabled Node.js Runtime with install / update / enable / details actions; a storage banner appears when a shared-storage directory cannot be entered and offers "Grant" and "Re-enter directory"; the screen follows the host's language, night mode and theme color through the host settings provider and falls back to the system values with the shared `#FFDEAD` color
* `Fix` A system restriction on background activity no longer crashes the plugin when a session starts. The session continues without foreground service protection.
* `Fix` Long transcript reads keep their newest text within the cross-process reply size limit.
* `Dependency` Added jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) as the terminal emulation and pty native libraries, hash-locked in `locks/vendored-aars.lock`
* `Dependency` Added `common-plugin-api.aar` and `nodejs-api.aar` (AutoJs6 modules `plugin-api/common-plugin-api` and `plugin-api/nodejs-api`, host build 6.8.0 / 5303, MPL 2.0) as the shared plugin contract and the Node.js manifest contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added `terminal-api.aar` (AutoJs6 module `plugin-api/terminal-api`, host build 6.8.0 / 5304, MPL 2.0) as the terminal contract V1 (`ITerminalPlugin` / `ITerminalCallback`, identity, ceilings and error codes); the plugin identity constants now come from it, hash-locked in `locks/host-api-aars.lock`
