******

### Release History

******

# v1.0.0

###### 2026/10/01

* `Hint` P0 development preview: the repository skeleton, the plugin identity recognized by the AutoJs6 plugin center, and the pty / storage / Node.js launcher spike. The Binder contract, the session core, the terminal screen, the script API and the settings page follow the phases of ROADMAP.md.
* `Feature` Plugin identity `three-shell-terminal` (engine `terminal`) with the INFO service, the Wake Activity and the `org.autojs.plugin.TERMINAL` service skeleton for host discovery
* `Feature` APKs split by ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal APK, with native libraries aligned to 16 KB pages
* `Feature` README, plugin center instruction and changelog in 10 languages
* `Dependency` Added jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) as the terminal emulation and pty native libraries, hash-locked in `locks/vendored-aars.lock`
* `Dependency` Added `common-plugin-api.aar` and `nodejs-api.aar` (AutoJs6 modules `plugin-api/common-plugin-api` and `plugin-api/nodejs-api`, host build 6.8.0 / 5303, MPL 2.0) as the shared plugin contract and the Node.js manifest contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added `terminal-api.aar` (AutoJs6 module `plugin-api/terminal-api`, host build 6.8.0 / 5304, MPL 2.0) as the terminal contract V1 (`ITerminalPlugin` / `ITerminalCallback`, identity, ceilings and error codes); the plugin identity constants now come from it, hash-locked in `locks/host-api-aars.lock`
