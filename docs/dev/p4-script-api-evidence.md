# P4.1 script API `terminal`: service layer, augment and the first two tiers (2026-10-02)

Host side only (repository `D:/idea-projects/AutoJs6`, build 5310, local commit
`feat(terminal): add the terminal script API with its service layer, first and second tiers`).
The plugin repository carries no code change for this item; this file records the evidence.

## Implementation

- `runtime/api/terminal/TerminalService.kt`: per-script `Closeable` owner of the Binder calls
  (`TerminalPluginHost`), the `TERMINAL_OPEN` screen start (`TerminalPluginInspector.resolveEntry`
  + `TerminalLauncher.intent` + `FLAG_ACTIVITY_NEW_TASK`, D19; a start Android refuses is `INTERNAL`
  with the system's reason, D34), the five-state `state()` (manifest inspection, downgraded to
  `unavailable` when the plugin looks fine but does not answer) and the polling `awaitExit` behind
  `exec({ wait: true })` (`TIMEOUT` after `timeout` ms, `0` = no limit). Availability failures get
  the plugin center's localized hint in front (install / enable / app disabled / host too old /
  plugin too old). Sessions belong to the plugin and its user: `close()` on script exit refuses
  later calls and closes nothing; `closeOnExit` of the roadmap is the plugin-side `keepOpen: false`.
- `runtime/api/terminal/TerminalScriptArguments.kt` (pure Kotlin): `open(dir | options)`,
  `exec(command, options)`, `npmRun(script, dir, options)` (script name limited to
  `[A-Za-z0-9_.:@/+-]`, command `npm run <script>`), `sessionId`, `input(text | bytes)`,
  `resolvePath` (the `files.path` rule: absolute passes, relative resolves segment by segment
  against the script's cwd, refused without a cwd), `request` (cwd defaults to the script
  directory, validated by `TerminalJson.OpenRequest.validate`). Unknown option keys are refused
  by name with the list of known keys; `env` cannot override `HOME` / `PREFIX` / `PATH`.
- `runtime/api/augment/terminal/Terminal.kt`: `AugmentableKey("terminal")` (alias `$terminal`),
  first tier `open` / `openAsync`, `sessions` / `sessionsAsync`, `session(id)`, `close` /
  `closeAsync`, `closeAll` / `closeAllAsync`, `show` / `showAsync`, `state()`, `isAvailable` /
  `isAvailableAsync`; second tier `exec` / `execAsync`, `npm.run` / `npm.runAsync`, `env` /
  `envAsync`; `TerminalError` constructor. `TerminalCalls` / `TerminalPromises` /
  `TerminalJsErrors` are terminal copies of the epub plumbing (sync form blocks inside
  `runBlocking(scriptRuntime.coroutineContext)`, Async form settles through
  `ScriptAsyncDispatcher`, arguments become Gson trees via `JSON.stringify`, every failure is a
  `TerminalError(message, code, sessionId?)`).
- `runtime/api/augment/terminal/TerminalSessionNativeObject.kt`: the P4.1 session object with
  `id` / `cwd` / `title` / `createdAt` / `pid` / `state` / `exitCode` and `isAlive()` /
  `write(text | bytes)` / `show()` / `close()` (+ `Async`). P4.2 turns it into an EventEmitter with
  `waitFor`, `transcript` and the `output` / `exit` / `overflow` events.
- `runtime/ScriptRuntime.kt`: field, construction, `close`, `Terminal(this, terminal).augment(target)`.

## Evidence

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `:app:testAppDebugUnitTest --tests org.autojs.autojs.runtime.api.terminal.* --tests ...augment.terminal.*` | `TerminalScriptArgumentsTest` 8 / 8, `TerminalJsErrorsTest` 4 / 4 |
| Build | `:app:assembleAppDebug` (offline) | passed, `autojs6-v6.8.0-arm64-v8a.apk` build 5310 |
| DEVICE API 33, plugin present | Sony XQ-DQ72 `QV770340J7`, host debug 5310, plugin build 19, Node.js Runtime 1.5.6 | probe passed (below) |
| DEVICE API 33, plugin missing | Redmi 22120RN86C `bek749scrwv4wo8h`, host debug 5310, no plugin | probe passed (below) |

Probe scripts were pushed to `/sdcard/Scripts/ThreeShellP41/` and started through
`RunIntentActivity` (`am start -a android.intent.action.VIEW -d file:///... -t application/x-javascript`),
so `files.cwd()` was the probe directory; the directory was deleted afterwards.

Sony (plugin present), `result.txt` of the probe:

```
cwd=/sdcard/Scripts/ThreeShellP41
state=available
isAvailable=true
sessionsBefore=0
opened id=3 cwd=/sdcard/Scripts/ThreeShellP41 state=pending pid=-1 title=ThreeShellP41 createdAt=number exitCode=null
opened alive=true state=running toString=TerminalSession { id: 3, cwd: /storage/emulated/0/Scripts/ThreeShellP41, state: running }
sessionsAfter=1 contains=true
session(id)=true session(nope)=null
env home=/data/user/0/io.github.supermonster003.autojs6.plugin.three.shell.terminal/files/terminal/home shell=/system/bin/sh node={"available":true,"version":"v24.21.0","packageName":"io.github.supermonster003.autojs6.plugin.nodejs","pluginVersion":"1.5.6","reason":null} storage=granted contract=1 plugin=1.0.0
exec exitCode=null state=exited alive=false cwd=/
npm.run exitCode=null state=exited
contentUri instance=true code=INVALID_ARGUMENT
show missing code=SESSION_NOT_FOUND sessionId=no-such-session
bogus option code=INVALID_ARGUMENT msg=unknown option 'options.bogus'; known options are cwd, env, keepOpen, show, stripAnsi, timeout, title, wait
closed=true closedAgain=false alive=false
async opened 6 title=P41 async
closeAll=1 remaining=0
```

`terminal.open(files.cwd())` started `org.autojs.plugin.TERMINAL_OPEN` from the host uid
(`ActivityTaskManager: START u0 {act=org.autojs.plugin.TERMINAL_OPEN ... flg=0x10000000
cmp=.../.ThreeShellTerminalEntryActivity}`) and the plugin's `TerminalActivity` became the top
resumed activity in its own task (#450). The first run, before the plugin held
`MANAGE_EXTERNAL_STORAGE`, ended with `TerminalError [STORAGE_PERMISSION_REQUIRED]: shared storage
access has not been granted to the plugin` at `terminal.open`, which is the contract's refusal for a
shared-storage directory; the appop was then set to `allow` from adb for the run and reset to
`default` afterwards.

Redmi (plugin missing), `result.txt` of the probe:

```
state=not_installed
isAvailable=false
sessions instance=true code=PLUGIN_UNAVAILABLE msg=Install 3-Shell Terminal from Plugin Center to use the terminal. The built-in terminal has moved into this plugin. (terminal plugin is unavailable: Missing required plugin for "terminal plugin". Please install the plugin and try again.)
open code=PLUGIN_UNAVAILABLE
show code=PLUGIN_UNAVAILABLE
openAsync instance=true code=PLUGIN_UNAVAILABLE
```

## Findings

- A `keepOpen: false` session is removed by the plugin as soon as its command ended (the pty
  finish callback removes it from the manager), so polling `listSessions` cannot observe the exit
  code: `exec({ wait: true })` returns `state = exited` with `exitCode = null` in P4.1. The exit
  code travels in the plugin's `onSessionExited(id, code)` callback, which P4.2 consumes.
- The document of a session whose command already ended reports `cwd = /`; P4.2 should check the
  directory fallback of the plugin's `TerminalDocuments` for an exited process.
- The host's working tree carried the maintainer's unrelated layout-inspector work (including
  changelog edits); only the terminal files and `version.properties` were staged. The host
  changelog entry for `terminal` stays with P4.3 as planned.
- The first commit's `TerminalScriptArguments.kt` carried a literal NUL character inside a char
  literal, which git classifies as binary; the follow-up host commit `refactor(terminal): spell the
  NUL guard of the terminal script arguments as a unicode escape` (build 5311) writes it as a
  Unicode escape. Behaviour is unchanged; the devices keep build 5310.
