# P3.3 entry Activity and launcher (2026-10-02)

The host reaches the terminal UI only through `ThreeShellTerminalEntryActivity`, the exported
`org.autojs.plugin.TERMINAL_OPEN` forwarder behind the `org.autojs.permission.PLUGIN` signature
permission (roadmap D19). `ui/LauncherActivity` is the invisible target the icon aliases of P5.3
will point at (D30). Both finish inside `onCreate`; the terminal screen owns a task of its own.

## Implementation

- Caller check: Android enforces the manifest permission before the Activity exists. In addition
  `EntryCaller` names the caller from `callingPackage` (start for result), `launchedFromPackage`
  (API 34+) or the system-derived referrer `android-app://<package>`, and `EntryCallerPolicy`
  refuses a named caller that does not hold the permission or whose signer digests differ from the
  plugin's (`binder/PackageSigners`, shared with `HostCallerGuard`). The host's plain `startActivity`
  below API 34 names nobody; such a start is accepted because the manifest permission is then the
  whole check, which is what D19's "signature permission" boundary means in practice.
- Extras: `EntryRequest.of` applies the host's own validation (`TerminalOpenRequest.validate`):
  blank strings are absent, `directory` is capped at `MAX_PATH_BYTES` and `command` at
  `MAX_COMMAND_BYTES` (UTF-8 bytes), `manager` cannot be combined with a session request,
  `sessionId` cannot be combined with `newSession` or `command`. Booleans are accepted as `Boolean`
  or as the string `true`. A request that breaks the contract is logged (no payload) and dropped.
- Routing: `manager=true` starts `TerminalManagerActivity` without a new task, so the dialog sits
  on the caller's screen; everything else starts `TerminalActivity` with `FLAG_ACTIVITY_NEW_TASK`
  into the terminal's own affinity task.
- Back (D30): every exit of `TerminalActivity` goes through `finishScreen()`: `finishAndRemoveTask()`
  when the screen is its task root (host entry, launcher icon), so the caller's task or the home
  screen comes back and recents cannot replay a start request that carried `command`; plain
  `finish()` when the screen is stacked above the manager.
- `LauncherActivity` shares the terminal's task affinity (the task the home screen creates is the
  terminal task) and must not be `excludeFromRecents` (that would hide the whole terminal task).

## Evidence

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `testDebugUnitTest` | 145 passed, 0 failed (+6 `ThreeShellTerminalEntryRequestTest`, +2 manifest cases for the entry and the launcher) |
| Build | `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` | passed; lint 0 errors, 34 warnings (unchanged set) |
| Docs | `generate_markdown.py` / `--check` | 10 languages, 36 artifacts |
| DEVICE API 24 | x86 AVD `emulator-5554`, `ThreeShellTerminalEntryInstrumentationTest` | 5 / 5 |
| DEVICE API 33 | Sony XQ-DQ72 `QV770340J7`, arm64-v8a, host debug 5307 installed, same test class | 5 / 5 |
| DEVICE API 35 | Xiaomi 23046RP50C `968e9f18`, arm64-v8a, HyperOS 2, same test class | 5 / 5 |
| DEVICE API 33, host side | Sony XQ-DQ72, host debug 5307 (zh-CN UI), driven with `uiautomator dump` + `input tap` | see below |

The five instrumentation cases: the adb shell (uid 2000, no PLUGIN permission) is refused by Android
("Permission Denial ... requires org.autojs.permission.PLUGIN", no screen, no session); the plugin's
own uid (signed like the host, holding the permission) reaches `usr/tmp` in a new session whose screen
is a task root and whose Back keeps the session; `manager=true` shows only the manager; `manager`
combined with `newSession` is ignored; the launcher starts a session at home when none exists,
restores the most recent one otherwise, and restores a session created in between on the next launch.

Host side on the Sony (the plugin APK under test carried versionCode 19; the build number becomes 20
at commit time):

1. Drawer "终端" row: switch enabled, no error subtitle (available). Title tap opens the host's
   description dialog (管理器 / 关闭 / 新建会话); 新建会话 opened `TerminalActivity` in its own task
   (`#437`, affinity `...ui.TerminalActivity`, subtitle `~`) above the host task `#411`. Back returned
   to the host `MainActivity`, the terminal task disappeared, and the drawer subtitle read
   "运行中 [ 会话: 1 ]" (the drawer count deferred from P1).
2. 管理器 in the same dialog opened `TerminalManagerActivity` inside the host task (task size 2)
   showing status running / 1 session, the session row with PID and uptime, and the settings group
   (screenshot taken). Back returned to the host.
3. The drawer switch closed all sessions through the Binder: the subtitle vanished and
   `ThreeShellTerminalSessionService` stopped.
4. Explorer directory menu (更多 -> 在终端中打开) on `Scripts/layouts`: terminal subtitle
   `/storage/emulated/0/Scripts/layouts`. Two Back presses returned to the host: the first dismissed the
   keyboard that `stateVisible` had opened.
5. Project toolbar terminal button in a throwaway project `Scripts/ThreeShellProbe` (created and
   deleted by the session): subtitle `/storage/emulated/0/Scripts/ThreeShellProbe`; `open` without
   `newSession` reused the existing session and changed its directory, as the host did before.
6. For the shared-storage directories the plugin's `MANAGE_EXTERNAL_STORAGE` appop was set to `allow`
   from adb beforehand and reset to `default` afterwards; the host explorer was navigated back to the
   scripts root.

Devices with host 5303 (API 24 AVD, Xiaomi Pad) cannot use the plugin (required host 5304); no host
was installed on them by this session.

## Findings

- `am start` writes its `SecurityException` to stderr, and `UiAutomation.executeShellCommand` returns
  stdout only, so the refusal case reads the activity manager's "Permission Denial" line from
  `logcat -s ActivityManager:W ActivityTaskManager:W` (tag differs by API level).
- `Activity.getCallingPackage()` is null for the host's `startActivity`; below API 34 only the referrer
  carries a name. The in-Activity check therefore guards named callers and relies on the manifest
  permission for unnamed ones.
- A terminal opened with `windowSoftInputMode=stateVisible` consumes the first Back to hide the
  keyboard; the second Back leaves.
