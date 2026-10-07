<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>A multi-session terminal for AutoJs6 and its scripts, running the system shell in a pty with background sessions, a key bar and Node.js commands</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Shell Terminal takes over the built-in terminal of AutoJs6: the "Terminal" switch of the home drawer, "Open in terminal" in the file manager directory menu and the project toolbar, and the script-side global object `terminal` for opening, driving and observing terminal sessions. Every session is a system shell (`/system/bin/sh`) running in a pty that keeps running in the background after the screen is left.

AutoJs6 discovers the plugin through its Binder service, opens the terminal screen with an explicit Intent, and uses the Binder to read the session count, close all sessions or drive script sessions; session output flows back to scripts through a pipe. When the Node.js Runtime plugin is installed, the terminal reads its manifest contract directly, verifies the signature and the launcher, and provides node / npm / npx / corepack / yarn / pnpm.

******

### Status

******

Version 1.0.0 provides the standalone terminal, background sessions, interactive script APIs and settings. Complete script APIs require AutoJs6 6.8.0 build 5315 or later; build 5304 is the base plugin-protocol requirement. Android 7.0 or later is supported.

******

### Features

******

The plugin provides the following capabilities:

- Multiple sessions: create, switch, close and a session manager; a foreground service keeps sessions running after the screen is left, and its notification shows the current directory and the session count with a "Close sessions" action.
- Terminal screen: a two-row key bar (Esc / Tab / Ctrl / arrows / common symbols), native text selection with Copy / Select all, transcript copy and share, text size, paste and clear.
- Node.js toolchain: with the Node.js Runtime plugin (1.5.0+) installed, node / npm / npx / corepack / yarn / pnpm become available, together with the npm registry and "ignore install scripts" settings and the package menu (npm init / install / run script and more).
- AutoJs6 entries: the home drawer switch (session count, close all), "Open in terminal" in the file manager directory menu and the project toolbar.
- Script API `terminal` (alias `$terminal`): session management, visible execution (`exec`, `npm.run`) and a session object with `output` / `exit` events, `write` and `waitFor`; every failure is a `TerminalError` with a stable `code`.
- Standalone app: the launcher icon opens the terminal directly; a settings page (appearance following AutoJs6, text size, npm registry, Node.js integration, all files access, clear terminal data), About and the release history.

******

### Usage

******

1. Install the plugin APK matching the device ABI (or the universal APK) from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) on a device with AutoJs6 build 5304 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Shell Terminal` is recognized, and enable it.
3. Turn on "Terminal" in the AutoJs6 home drawer, choose "Open in terminal" on a directory in the file manager, or call `terminal.open(...)` from a script. Grant "All files access" when the plugin asks for it to enter directories under shared storage such as `/sdcard`.

******

### Node.js commands

******

How the terminal obtains node / npm and what the limits are:

- Requires the Node.js Runtime plugin 1.5.0 or later; the plugin reads its manifest contract, verifies the signature, the launcher and the npm / corepack archive, and links the commands into `PATH` whenever a session starts. Without the plugin, or when verification fails, the terminal stays usable without these commands.
- Android prevents execution of app-written files. npm disables bin links by default, so `node_modules/.bin/*` and `npx <package>` cannot directly run package entry points. Use `node node_modules/<package>/<entry>.js`. Native executables bundled in npm packages fail with `EACCES`, and native addons (`.node`) cannot be loaded.
- corepack defaults to its bundled pnpm 11.x and Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`) and downloads an explicitly named version on request; the npm registry can be switched to npmmirror or a custom https URL in the settings.

******

### Quick Start

******

A script that opens the script directory, installs dependencies visibly and waits for the result, and drives an interactive command (available from roadmap P4 on):

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

### Compatibility

******

Platform facts that shape what the plugin can do:

- Android 7.0 (API 24) and later; APKs for arm64-v8a, armeabi-v7a, x86_64 and x86 plus a universal APK, with native libraries aligned to 16 KB pages; the host build and the plugin are verified together on the device matrix listed in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
- Terminal processes run under the plugin's own uid and permissions and do not inherit the permissions of AutoJs6; use the script `shell()` API for commands that need AutoJs6 permissions.
- Sessions live as long as the plugin process; after the system ends the process they cannot be restored, which the foreground service and its notification make unlikely.

******

### FAQ

******

- **Why does `cd /sdcard/Scripts` fail?** The plugin needs its own storage grant. Open the plugin settings or follow the terminal banner to grant "All files access" (Android 11+), or the storage permission on older systems.
- **Why is there no node command?** Install the Node.js Runtime plugin (1.5.0+) from the AutoJs6 plugin center; the "environment probe" in the plugin settings shows the exact reason (missing, too old, untrusted signature or launcher not executable).
- **Can the system stop background sessions?** Yes. Sessions cannot be restored after the plugin process is terminated. On HyperOS, MIUI and other restricted systems, allow notifications and background activity in Android settings; opening the terminal retries foreground protection when permitted. Leaving the screen normally keeps sessions running, but a foreground service cannot override system restrictions.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder service and the screen entry are protected by the `org.autojs.permission.PLUGIN` signature permission and verify the caller signature, so only AutoJs6 can reach them; the launcher entry only opens the terminal and accepts no external command.
- The storage permission ("All files access" on Android 11+) is only used to enter the directories you choose; the terminal never scans or uploads files.
- The `INTERNET` permission is used by the commands you run in the shell (for example `npm install`) and by the manual release check against this plugin's fixed GitHub Releases API; the plugin itself never goes online in the background.
- The launcher of the Node.js Runtime plugin is executed only when its signature is the official one (or matches this plugin); the plugin does not log session input or output and excludes its private storage from backups.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

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

`ThreeShellTerminalPluginService` answers `org.autojs.plugin.TERMINAL` (category `terminal`) and implements the host terminal-api contract `org.autojs.plugin.terminal.api.ITerminalPlugin` from roadmap P2 on. `ThreeShellTerminalPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin; the terminal screen is opened through `org.autojs.plugin.TERMINAL_OPEN`.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/10/07_

- `Hint` Version 1.0.0 provides the standalone terminal, background sessions, interactive script APIs and settings. Complete script APIs require AutoJs6 6.8.0 build 5315 or later; build 5304 is the base plugin-protocol requirement. Android 7.0 or later is supported
- `Feature` Standalone 3-Shell Terminal plugin with host discovery, terminal control and protected UI/settings entry points
- `Feature` APKs split by ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal APK, with native libraries aligned to 16 KB pages
- `Feature` README, plugin center instruction and changelog in 10 languages
- `Feature` Session core ported from the host terminal: pty-backed shell sessions with a process-wide registry (title and exit code recorded for the Binder), the session environment and directory layout under the plugin's own files directory, Node.js launcher discovery with the npm / corepack installer, and the foreground service that keeps sessions running with a "Close sessions" notification (channel `three.shell.terminal.sessions`)
- `Feature` Storage access resolution (`StorageAccess`): the plugin's own permission state (legacy runtime permissions below API 30, "All files access" from API 30), shared-storage detection for `/sdcard`, `/storage/...` and the own `Android/{data,obb,media}` folders, start-directory fallback to `$HOME` with `STORAGE_PERMISSION_REQUIRED` or `DIRECTORY_INACCESSIBLE`, and the settings intents that open the all-files-access switch
- `Feature` Node.js integration with signer trust (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): the Node.js Runtime plugin is used only when it is signed by the official AutoJs6 plugin key or by this plugin's own key, the settings switch short-circuits before any lookup, every outcome maps onto the contract's `node-cli` states (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), and each session start refreshes the `usr/bin` command links, extracts the npm / corepack archive once per digest and exports the npm / corepack environment
- `Feature` AutoJs6 can create and control up to 16 terminal sessions, receive live output with up to 4 listeners per session, read recent output and query the shell environment. Closing AutoJs6 leaves the sessions running; invalid requests are rejected with a specific reason.
- `Feature` npm init, dependency/package installation, package.json script discovery/execution and npm search; npmjs, npmmirror, custom HTTPS registries and ignore-scripts settings. Clearing terminal data closes sessions and rebuilds the private home/usr layout while preserving settings and external projects
- `Feature` Terminal screen (`TerminalActivity`): the host's terminal UI ported onto the plugin's own Material 3 theme, with the key bar (Esc / Tab / Ctrl / Alt / arrows / paging), pinch-to-zoom text size, long-press text selection with copy, the session / text / package-management / settings / help menus, a toolbar subtitle that shows the shell's directory and copies it on tap, and a Node.js banner that explains a missing, untrusted, outdated or disabled Node.js Runtime with install / update / enable / details actions; a storage banner appears when a shared-storage directory cannot be entered and offers "Grant" and "Re-enter directory"; the screen follows the host's language, night mode and theme color through the host settings provider and falls back to the system values with the shared `#FFDEAD` color
- `Feature` Session manager: a dialog with collapsible status / controls / sessions / settings sections that lists every running session with its directory, PID and uptime, opens or closes a single session, starts a new one, closes all, shows session details with a copy action, and offers the text size, npm registry and ignore-scripts settings; it follows the same session registry the host's `onSessionsChanged` uses, is reachable from the terminal menu, from the session notification (tap) and, for the host's `manager=true` entry, through a transparent `TerminalManagerActivity` that leaves no terminal behind when closed
- `Feature` Host entry and launcher: the exported `TERMINAL_OPEN` entry Activity behind the `org.autojs.permission.PLUGIN` signature permission checks the caller it can name (permission held, signed like the plugin), validates the `directory` / `sessionId` / `newSession` / `command` / `manager` extras against the contract ceilings and forwards them to the terminal screen in its own task or to the session manager over the caller; `LauncherActivity` (target of the icon aliases) restores the most recent session or starts one at home; Back from a terminal reached through the host returns to the host, from the launcher to the home screen, and the terminal task leaves recents so a start request is never replayed
- `Feature` Launcher icon choice: four `MAIN / LAUNCHER` activity aliases over the private launcher forwarder (adaptive light, adaptive dark, adaptive automatic, transparent background), with the automatic icon enabled by default; the choice is persisted by PackageManager as the single enabled alias, repaired on every screen start and after a package update, and pinned or dynamic shortcuts move to the chosen alias
- `Feature` About, version history and update check: an About screen with the installed version, build number and date, the developer, the plugin license (MPL 2.0) and a third-party section that opens the bundled jackpal Android-Terminal-Emulator license and notice (Apache-2.0), the AutoJs6 plugin API notices (MPL 2.0) and the library notices offline; a document screen that renders the bundled changelog in the current language (English fallback) without a WebView; and a manual "Check for updates" that asks the GitHub Releases API over HTTPS only, can be cancelled, times out, reuses its answer for 12 hours, ignores a chosen version and offers the release page or the built-in history. The terminal overflow menu gains "About"
- `Feature` Standalone settings page in three groups: appearance with language, night mode, theme color and launcher icon (each following AutoJs6 by default; the theme color offers 16 presets and HEX / rgb() input), terminal with text size, npm registry, ignore-scripts, the Node.js integration switch, an environment probe screen, all-files access and clear data, and information with update check, version history and About. Choice dialogs confirm before applying and changes take effect at once. The terminal overflow menu's "Settings" opens the page directly (the quick settings submenu is gone), the AutoJs6 plugin center reaches it through TERMINAL_SETTINGS, and the plugin now declares the settings capability
- `Fix` A system restriction on background activity no longer crashes the plugin when a session starts. The session continues without foreground service protection.
- `Fix` Long transcript reads keep their newest text within the cross-process reply size limit.
- `Fix` Script transcript reads and output replay omit trailing screen padding while preserving prompt spaces and the boundary before live output
- `Fix` A starting session could briefly disappear from host queries and prevent visible execution from opening its terminal
- `Fix` Continuous output no longer blocks the terminal message queue; closing sessions during startup and natural process exits release their ptys and I/O workers
- `Fix` Opening an existing terminal retries foreground-service protection when background restrictions prevented it from starting
- `Improvement` Consistent visual sizing for launcher and Plugin Center icons, with transparent backgrounds and neutral black, white or grayscale artwork
- `Improvement` Plugin Center icons use the sizes, positions, light and dark artwork, and circular backgrounds adjusted in Icon Studio, retaining reproducible sources and parameters
- `Improvement` Android App info icons share Icon Studio artwork and light/dark backgrounds while preserving transparent Plugin Center artwork and existing launcher choices
- `Dependency` Added jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) as the terminal emulation and pty native libraries, hash-locked in `locks/vendored-aars.lock`
- `Dependency` Added `common-plugin-api.aar` and `nodejs-api.aar` (AutoJs6 modules `plugin-api/common-plugin-api` and `plugin-api/nodejs-api`, host build 6.8.0 / 5303, MPL 2.0) as the shared plugin contract and the Node.js manifest contract, hash-locked in `locks/host-api-aars.lock`
- `Dependency` Added `terminal-api.aar` (AutoJs6 module `plugin-api/terminal-api`, host build 6.8.0 / 5304, MPL 2.0) as the terminal contract V1 (`ITerminalPlugin` / `ITerminalCallback`, identity, ceilings and error codes); the plugin identity constants now come from it, hash-locked in `locks/host-api-aars.lock`

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

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

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- Terminal module documentation: https://docs.autojs6.com/#/terminal
- Node.js Runtime plugin: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (terminal emulation and pty native libraries, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
