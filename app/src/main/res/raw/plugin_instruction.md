3-Shell Terminal is an interactive terminal on your phone: it runs the system `/system/bin/sh` in a pty, keeps several sessions open at once, and keeps them running in the background after you leave the screen. With the Node.js Runtime plugin installed, commands such as node and npm are available too. The "Terminal" entry in AutoJs6 and the `terminal` object in scripts are both provided by this plugin.

### Getting started

1. Tap "Terminal" in the AutoJs6 home drawer, or choose "Open in terminal" from a folder menu in the file manager; you can also tap the 3-Shell Terminal icon on the home screen.
2. Type a command and press Enter to run it. The extra keys bar at the bottom of the screen provides Esc, Tab, Ctrl, arrow keys and common symbols.
3. Create, switch or close sessions from the "Sessions" menu in the toolbar. Sessions keep running after you leave the screen; tap the notification to open "Terminal manager".
4. Before entering shared storage folders such as `/sdcard`, tap "Grant" when the terminal prompts you, or turn on "All files access" in the settings.

### Main features

- **Multiple sessions**: create, switch and close sessions; "Terminal manager" lists them all. A foreground service keeps sessions running in the background, and the notification shows the current folder and the session count.
- **Terminal screen**: extra keys bar, text selection and copy, "Share transcript", and pinch to zoom the text; the settings offer 4 schemes under "Terminal colors" and 2 choices under "Terminal font".
- **Command editor**: when you paste a long or multi-line command, the full text is shown first; you can paste it into the current session, or run it in a new session and see the exit code.
- **Node.js toolchain**: with the Node.js Runtime plugin installed, node, npm, npx and corepack (yarn / pnpm) are available. In the settings you can pick the "npm registry" or turn on "Ignore npm lifecycle scripts".
- **Command completion notifications**: when a command that a script ran without keeping an interactive shell finishes, a notification is posted; tap it to see the end of the output. You can turn off "Command completion notifications" in the settings.
- **Separate settings**: language, night mode and theme color follow AutoJs6 by default and can also be set separately; the launcher icon comes in 4 styles. When AutoJs6 supports it, the settings page menu offers "Open general settings".

### Using it in scripts

Scripts use the global object `terminal` (alias `$terminal`) to create sessions, run commands, write input and listen to output:

```js
if (!terminal.isAvailable()) exit();
let result = terminal.exec('uname -m', {
    show: false,
    keepOpen: false,
    wait: true,
});
console.log(result.exitCode);
let session = terminal.open({ show: false });
session.on('output', (text) => console.log(text));
session.write('ls\n');
session.show();
```

- By default, `exec` keeps an interactive shell after the command finishes; to wait for the exit code, set both `keepOpen: false` and `wait: true`.
- Synchronous methods block the script thread and cannot be called on the UI thread; most methods also have an `Async` form that returns a Promise.
- Sessions keep running after the script exits; call `session.close()` when you no longer need them. A failed operation throws a `TerminalError` with a `code`.
- For all options and events, see [AutoJs6 docs > Terminal](https://docs.autojs6.com/#/terminal).

### Requirements

- Android 7.0 or later, and AutoJs6 6.8.0 or later.
- Commands such as node and npm need the [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) plugin 1.5.0 or later.
- Entering shared storage folders needs "All files access" (Android 11 and later) or the storage permission; the notification permission is used for session notifications and command completion notifications.
- Commands run with this plugin's own identity and permissions, not those of AutoJs6; for commands that need the permissions of AutoJs6, use `shell()` in a script instead.

### FAQ

**Cannot enter folders under `/sdcard`**

This plugin needs its own storage access. Tap "Grant" when the terminal prompts you, or turn on "All files access" in the settings; on Android 10 and earlier, granting the storage permission is enough. A `content://` address is not a file path and cannot be entered.

**The node command is not found**

Make sure the Node.js Runtime plugin is installed and "Node.js integration" is on in the settings, then open "Node.js launcher check" to see why. If the plugin is not signed with the official signature, that page shows the SHA-256 of its signature; once you confirm you trust it, new sessions can use it.

**Background sessions are ended by the system**

Once the system ends the plugin process, its sessions cannot be restored. Systems such as HyperOS and MIUI may restrict background activity; in the system settings, allow 3-Shell Terminal to post notifications and run in the background.

For details on the extra keys, the script interface and permissions, see the [project page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
