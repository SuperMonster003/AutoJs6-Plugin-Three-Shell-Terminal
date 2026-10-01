3-Shell Terminal takes over the built-in terminal of AutoJs6: the "Terminal" switch of the home drawer, "Open in terminal" in the file manager directory menu and the project toolbar, and the script-side global object `terminal` for opening, driving and observing terminal sessions. Every session is a system shell (`/system/bin/sh`) running in a pty that keeps running in the background after the screen is left.

Version 1.0.0 is the P0 development preview: the repository skeleton, the plugin identity recognized by the AutoJs6 plugin center, and the pty / storage / Node.js launcher spike. The Binder contract, the session core, the terminal screen, the script API and the settings page follow the phases of [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). Requires AutoJs6 6.8.0 (build 5304) or later.

### Usage

1. Install the plugin APK matching the device ABI (or the universal APK) from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) on a device with AutoJs6 build 5304 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Shell Terminal` is recognized, and enable it.
3. Turn on "Terminal" in the AutoJs6 home drawer, choose "Open in terminal" on a directory in the file manager, or call `terminal.open(...)` from a script. Grant "All files access" when the plugin asks for it to enter directories under shared storage such as `/sdcard`.

### Node.js commands

- Requires the Node.js Runtime plugin 1.5.0 or later; the plugin reads its manifest contract, verifies the signature, the launcher and the npm / corepack archive, and links the commands into `PATH` whenever a session starts. Without the plugin, or when verification fails, the terminal stays usable without these commands.
- Android refuses to execute files written by apps: `node_modules/.bin/*` and native executables shipped by npm packages fail with `EACCES`; run `node <entry file>` or `npx` instead. Native addons (`.node`) cannot be loaded.
- corepack defaults to its bundled pnpm 11.x and Yarn 1.x (`COREPACK_DEFAULT_TO_LATEST=0`) and downloads an explicitly named version on request; the npm registry can be switched to npmmirror or a custom https URL in the settings.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) for the installation guide and the current progress.
