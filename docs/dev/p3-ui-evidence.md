# P3.1 terminal screen (2026-10-02)

The host's terminal UI now runs inside the plugin: `ui/TerminalActivity` with the key bar, text
selection, menus, the Node.js banner (D17) and the storage banner (D18), themed by the plugin's
own palette that follows the host's language / night mode / theme color. The entry Activity (P3.3)
and the session manager (P3.2) are not part of this step, so the screen is reachable only through
explicit intents (tests) until P3.3 lands.

## Implementation and migration differences

- `HostAppearance` reads `AutoJs6HostSettingsContract.METHOD_GET_SETTINGS` through an unstable
  provider client on a worker thread, decodes the Bundle strictly (protocol 1, language tag regex,
  opaque colors) and caches the snapshot; `Appearance.resolve` prefers the host values and falls
  back to the system locale / night mode with the shared `#FFDEAD` color. `HostAppearanceActivity`
  applies the night mode to the AppCompat delegate and wraps the base context's locale, re-reads
  the host snapshot on every resume and recreates itself when it changed (unless a progress dialog
  or a text selection is open).
- `TerminalPalette` builds the runtime colors from the neutral `colors.xml` / `values-night`
  values and the HCT accent rule of the standalone settings specification (chroma < 4 -> 0,
  otherwise >= 48 and <= 96; tones 40 / 100 light, 80 / 20 dark). The accent is pushed to 4.5:1
  against the window background, surface, surface variant, terminal background and both tonal
  fills, so the armed Ctrl / Alt keys and banner actions stay readable for any theme color.
- `UiKit` replaces the host's `MaterialDialog`, `ViewUtils` and `ClipboardUtils`: Material 3
  dialogs with 24 dp corners, choose-then-confirm selectors (560 dp max width), tinted controls,
  clipboard, external intents and a small worker executor. The emulator view, key bar and text
  selection were ported one to one; the key bar reads its colors from the palette instead of
  theme attributes.
- `TerminalActivity` keeps the host flow (restore by `sessionId`, `newSession`, the most recent
  live session, `cd` into a requested directory of a reused session, `command` typed into a new
  session) but starts sessions through `SessionAssembly` so directory fallback and Node.js
  resolution match the Binder. The extras use the `TerminalContract` names so the P3.3 entry
  Activity forwards host intents unchanged. Edge-to-edge layout: a spacer takes the status bar,
  the root pads for the navigation bar or the keyboard, whichever is taller.
- Node.js banner: `PluginMissing` -> "Install Node.js Runtime" opens the GitHub Releases page of
  the Node.js Runtime plugin (the host's plugin center Activity is not exported and takes private
  extras; a host-exported plugin-center intent is a follow-up for the host). `PluginTooOld` ->
  "Update" (same page), `IntegrationDisabled` -> "Enable" switches the integration back on for new
  sessions, every other state -> "Details" with the probe report and a copy action. npm menu items
  first run the same check and show the remedy as a dialog.
- Storage banner: a `STORAGE_PERMISSION_REQUIRED` fallback shows the requested directory with
  "Grant" (API < 30: runtime permission request through the Activity result API; API 30+: the
  "All files access" settings intents of `StorageAccess`), re-rendered on every resume so that a
  grant from the system settings turns the action into "Re-enter directory", which types the `cd`.
  `DIRECTORY_INACCESSIBLE` stays a toast.
- `POST_NOTIFICATIONS` (API 33+) is requested once by the screen when it starts its first session
  (`TerminalPreferences.notificationPermissionRequested`); Binder-created sessions never prompt.
- Menu differences against the host: "Terminal manager" arrives with P3.2, "Settings" and "About"
  with P5.1; the host-side plugin-state prompts stay in the host (D27).
- Strings: 41 host keys renamed to `terminal_*` per locale, `terminal_tips_content` reworded for
  the plugin's own uid, 20 plugin-only keys added (actions, banner texts, dialog titles); the
  Spanish `confirm_close_running` lost its inverted question mark to satisfy the ASCII punctuation
  guard.

## Evidence

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `testDebugUnitTest` | 134 passed, 0 failed (+8 `ui/TerminalPaletteTest`, manifest test split into wake / terminal Activities, new preference key) |
| Build | `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `verifyNativePageAlignment` | passed; lint 0 errors, 25 warnings (pre-existing icon / dependency / dash warnings plus two `Overdraw` hints on the layouts and the unused `terminal_node_probe_running`, kept for P5.1) |
| Docs | `generate_markdown.py` / `--check`, `generate_launcher_icons.py --check` | 10 languages, 36 artifacts, 15 icons |
| DEVICE API 24 | x86 AVD `emulator-5554`, `ui/TerminalActivityInstrumentationTest` | 4 / 4: session switch keeps the shell variable and the unsubmitted draft; subtitle `~` then the real `tmp` path, clipboard follows the real directory (procfs reports `/data/data/...` while the configured path is `/data/user/0/...`, same inode); leaving the screen keeps the session and reopening without extras restores it; storage banner shown for `/storage/emulated/0/Download` under `DENIED_LEGACY`, `pm grant` + resume turned the action into "Re-enter directory", the shell entered the directory and the banner left |
| DEVICE API 24 | same AVD, `core/TerminalSessionsInstrumentationTest` | 4 / 4 (unchanged P2.1 cases) |
| DEVICE API 35 | Xiaomi 23046RP50C `968e9f18`, arm64-v8a, HyperOS 2, `ui/TerminalActivityInstrumentationTest` | 4 / 4 after the stale permission dialog was dismissed: same four outcomes; storage banner shown under `DENIED_ALL_FILES`, `appops set ... MANAGE_EXTERNAL_STORAGE allow` + resume turned the action into "Re-enter directory", the shell entered `/storage/emulated/0/Download` and the banner left; the appop was reset to `default` from adb afterwards |
| DEVICE API 35 | same device, `core/TerminalSessionsInstrumentationTest` | 4 / 4 (unchanged P2.1 cases) |

## Findings

- On the Xiaomi Pad the first run left the system `GrantPermissionsActivity` (package
  `com.lbe.security.miui`) on top of the plugin's task after the screen asked for
  `POST_NOTIFICATIONS`; `startActivitySync` then waited 45 s for a resume that never came, and
  the dialog survived `am force-stop` of the plugin (every later launch landed underneath it). The
  Activity tests therefore grant the permission up front with `UiAutomation.grantRuntimePermission`,
  and a stale dialog has to be dismissed (Back) before re-running.
- Withdrawing the storage grant from inside the instrumented process kills it: `pm revoke` of
  the runtime permissions (known) and also `appops set <pkg> MANAGE_EXTERNAL_STORAGE default` on
  HyperOS 2 / API 35 ("Process crashed" right after the banner case had passed). The test only
  grants, logs the adb command that resets the state, and the ungranted start is prepared from adb
  (`pm revoke ...` below API 30, `appops set ... default` from API 30) before a run.
- `TerminalPtySession.currentDirectory()` canonicalizes `/proc/<pid>/cwd`, which yields
  `/data/data/<pkg>/...` on devices where `/data/user/0` is a symlink, while `TerminalPaths`
  is configured with `/data/user/0/<pkg>/...` and Java's `canonicalPath` leaves that prefix alone
  there. The subtitle and the clipboard show the procfs spelling; tests compare by (device, inode).
- An instrumented process on HyperOS (API 35) is refused the foreground service start while no
  Activity is resumed ("Background activity is restricted"); sessions still run, the notification
  is simply absent. A screen opened by the user is foreground, so the real path is unaffected.

## Manual parity checklist (P3.3 acceptance, added 2026-10-02, automated 2026-10-07)

Items the roadmap asks to compare against the host's former built-in terminal. The 2026-10-07
review automated each row in `ui/TerminalParityInstrumentationTest` and found two defects that
the source port had carried since P3.1. The host's built-in terminal no longer exists, so the
comparison is against the ported behavior and the platform's own text selection.

| Item | Automated evidence | Result |
| --- | --- | --- |
| Text selection | long press on the emulator view opens the frozen transcript, Android starts its selection mode, Back closes only the selection (`longPressSelection...`) | Defect fixed: the synthetic touch that starts the selection reported a bare pointer source, so API 37 selected a word without handles or the Copy / Select all toolbar. A probe build without the fix still showed them on API 28 and 33. The events now carry the touchscreen source; a real `input swipe` long press on API 37 shows both |
| Copy | select-all + copy returns logical lines, a wrapped line comes back whole; menu "Copy transcript" after 10000 full rows | Defect fixed: API 24 threw `TransactionTooLargeException` (1539892-byte parcel) from `ClipboardManager.setPrimaryClip` on the main thread, which kills the process and every session. Copies now keep the newest 100000 characters, report truncation and survive a refusal |
| Share | menu "Share transcript" after the same output, chooser captured by an `ActivityMonitor` (inspected on API 26+, counted below) | same 100000-character tail and notice as Copy |
| Text size | two-finger pinch on the view changes the size and persists it; the dialog path is covered by `ui/settings/SettingsActivityInstrumentationTest` (12 -> 13 sp reaches the terminal view) | passes |
| Keyboard | `ui/TerminalLifecycleInstrumentationTest` shows / hides the IME and checks view and pty geometry (P6.1) | passes |

| Device | Parity class | Complete `ui` package |
| --- | --- | --- |
| API 24 x86 AVD | 3 / 3 | not repeated |
| API 37 x86_64 AVD, 16 KiB pages | 3 / 3 inside the package run after an AVD restart; an earlier run timed out launching the Activity while the AVD was swapping (load average 25) | 23 passed, 1 explicit skip (API 24 split screen) |
| Sony G8441, API 28 | 3 / 3 | not repeated |
| Redmi 22120RN86C, API 33 | 3 / 3 | 23 passed, 1 explicit skip |

Findings while automating: injected touches are taken by the system clipboard overlay of API 33+
after any copy, so the test dispatches gestures to the view; below API 26 an `ActivityMonitor` with an
`ACTION_CHOOSER` filter also matches the action-less terminal intent, so it is added only for the share
step; the API 24 mksh `printf` mangles wide numeric padding (`%0133d`), so the long line is a literal.
