# P5 standalone application: settings, About and launcher icons (2026-10-02)

Roadmap P5 turns the plugin into a complete standalone application: P5.3 added the four launcher
icon aliases (plugin build 24, commit `39db2e9`), P5.2 the About screen with the bundled document
viewer and the manual update check (build 25, commit `6973e64`), and P5.1 the standalone settings
page reached from the terminal menu and from the host's plugin center (build 26). This document
collects the implementation notes and the device evidence for the three steps.

## P5.3 launcher icons (build 24)

- Four `activity-alias` entries (`AdaptiveLightIconAlias`, `AdaptiveDarkIconAlias`,
  `AdaptiveAutoIconAlias` enabled by default, `TransparentIconAlias`) carry `MAIN` / `LAUNCHER`
  and point at `ui/LauncherActivity`; `ui/settings/LauncherIcons` switches them through
  `PackageManager.setComponentEnabledSetting` so that exactly one alias is enabled at any time,
  `LauncherIconStatePolicy` normalizes any inconsistent state (none or several enabled) back to
  the stored choice, and `LauncherIconUpdateReceiver` repeats that normalization after
  `MY_PACKAGE_REPLACED`. `HostAppearanceActivity.onCreate` runs the normalization off the main
  thread as well.
- Evidence: `LauncherIconStatePolicyTest` (JVM) and `ui/settings/LauncherIconInstrumentationTest`
  (exactly one alias enabled, `queryIntentActivities(MAIN / LAUNCHER)` resolves a single component
  after every switch) on the API 24 x86 AVD and the Xiaomi Pad (API 35); `ManifestContractTest`
  freezes the alias list and the exported set. The optical-centering screenshot review of the
  generated icons is left for the P6.2 compatibility matrix; `OPTICAL_X` / `OPTICAL_Y` are
  unchanged and `generate_launcher_icons.py --check` passes (15 icon resources).

## P5.2 About, release history and update check (build 25)

- `ui/settings/AboutActivity` shows the version name, build number and build date, the
  developer, the plugin license (MPL 2.0) and a third-party section; `ReleaseHistoryActivity`
  renders the bundled documents (`BundledDocument`: release history by locale with English
  fallback, LICENSE, THIRD_PARTY_NOTICES, the jackpal LICENSE and NOTICE) through `DocumentText`
  without a WebView.
- `update/AppUpdateRepository` asks the GitHub Releases API for
  `SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal` over HTTPS only (10 s connect / read
  timeout, response size cap, strict `ReleaseInfoCodec` decoding that rejects drafts, prereleases
  and foreign URLs); `AppUpdateCoordinator` drives the dialog (release page / built-in history /
  ignore version), cancels on request, reuses the answer for 12 hours (`UpdateSchedulePolicy`) and
  remembers ignored tags (`AppUpdateSettings`). While nothing is published the endpoint answers
  404, which the code reports as "no release yet"; only the failure, timeout and injected-source
  paths are verified before the first push (roadmap acceptance note).
- Evidence: JVM `update/AppVersionPolicyTest`, `update/UpdateSchedulePolicyTest`,
  `update/ReleaseInfoCodecTest`, `ui/settings/ReleaseHistoryTest`; instrumentation
  `ui/settings/AboutInstrumentationTest` (3 cases: the version block names the installed version
  and version code, every bundled legal document opens offline with its marker text, an injected
  `v99.0.0` release drives the dialog, ignore / un-ignore round-trips within the reuse window and a
  failed fetch leaves no dialog) on the API 24 AVD and the Pad.

## P5.1 settings page (build 26)

### Implementation

- `ui/settings/SettingsActivity` builds the page in code: appearance (language, night mode, theme
  color with a color dot, launcher icon), terminal (text size, npm registry, ignore-scripts
  switch, Node.js integration switch, environment probe, all-files access, clear data) and
  information (update check, version history, About). Every choice dialog follows
  choose-then-confirm; `TerminalSettingsDialogs.showTextSize` / `showNpmRegistry` gained an
  `onPrompt` callback so the page can track the open dialog as its unconfirmed draft (the recreate
  guard of `HostAppearanceActivity`). The clear-data row uses the danger color, confirms with the
  terminal root path and shows a progress dialog while `TerminalSettingsActions.clearData` runs.
- `ui/AppearancePreferences` stores the plugin's own language / night mode / theme color choices
  in the `terminal` preferences file (`appearance_language`, `appearance_dark_mode`,
  `appearance_color`) and layers them over the host snapshot in `Appearance.resolve`: an explicit
  choice wins, "follow AutoJs6" takes the host value and falls back to the system, "follow system"
  ignores the host. Because every screen extends `HostAppearanceActivity`, the terminal, the
  manager, About, the document viewer and the probe follow the same choices on their next resume.
- `ui/settings/ThemeColorChooser` offers "Follow AutoJs6" (showing the host color), the 16
  presets, a HEX / `rgb(r, g, b)` field with live preview and an error state; only OK confirms.
  `ThemeColorValue` is the pure parsing / formatting / contrast policy; `LegacyInputTintContext`
  tints the cursor and handles of that one field on API 24-28.
- `ui/settings/NodeProbeActivity` shows the `NodeProbeReport` resolved off the main thread with
  the integration switch honored, plus copy and "check again" (forced refresh).
- The terminal overflow menu loses its quick settings submenu (text size / registry /
  ignore-scripts) and gains a direct "Settings" item next to "About" (D30); the session manager
  keeps its settings group. `TerminalActivity.onResume` re-resolves the Node.js availability when
  the integration switch no longer matches the banner state.
- Manifest: `.ui.settings.SettingsActivity` is exported behind `org.autojs.permission.PLUGIN`
  with the `org.autojs.plugin.TERMINAL_SETTINGS` / `DEFAULT` filter (the host's
  `OfficialPluginSettingsLauncher` requires exactly one enabled, exported match);
  `.ui.settings.NodeProbeActivity` is private. `SettingsActivity.onCreate` applies the same
  caller rule as the terminal entry (`EntryCaller`). `ThreeShellTerminalPlugin.FEATURES` now
  declares `settings`.
- Strings: 41 translatable keys in all 11 directories plus 10 untranslated language labels
  (`app_language_*`); the Spanish confirmation drops the inverted question mark for the ASCII
  punctuation guard. `migration/host-terminal/` is deleted: its last 18 files (three preference
  widgets, preference keys, arrays, the settings XML block, the 11 string snapshots and the README)
  are either ported or host-side only (D27).

### Evidence

JVM (`:app:testDebugUnitTest`): 176 tests, 0 failures, including `ui/AppearancePreferencesTest`,
`ui/settings/ThemeColorValueTest`, the frozen appearance keys in `TerminalPreferencesTest` and the
`TERMINAL_SETTINGS` case in `ManifestContractTest`. `lintDebug` reports 0 errors;
`verifyNativePageAlignment`, `generate_markdown.py --check` and `generate_launcher_icons.py --check`
pass.

`ui/settings/SettingsActivityInstrumentationTest` (6 cases) and `binder/TerminalBinderContractTest`
(18 cases, `FEATURES` now lists `settings`):

| Device | API | Result | Logged evidence (`ThreeShellTerminalSettings`) |
| --- | --- | --- | --- |
| emulator-5554 (x86, en-US) | 24 | 6/6 + 18/18 | 14 rows present; language, theme color and clear-data dialogs cancelled without saving; text size 12 -> 13 sp and the terminal view reports 13 sp; Node.js integration true -> false gives state `disabled`, back gives `plugin-missing` (no Node.js Runtime installed); dark true -> false and color `#2196F3` applied through recreate; launcher icon AUTO -> TRANSPARENT and restored |
| Xiaomi Pad 968e9f18 (arm64, zh-CN, host 5312) | 35 | 6/6 + 18/18 | same sequence with zh-CN summaries ("跟随 AutoJs6 · 中文 (简体中文)", "自适应图标 (自动)"); Node.js state back to `available`; dark false -> true |

Caller rule: `Intent(TerminalActions.OPEN_SETTINGS).setPackage(<plugin>)` from the plugin's own
uid opens the page (toolbar title "设置" / "Settings"); the adb shell (uid 2000) is refused on
both devices:

```text
java.lang.SecurityException: Permission Denial: starting Intent { act=org.autojs.plugin.TERMINAL_SETTINGS ... cmp=io.github.supermonster003.autojs6.plugin.three.shell.terminal/.ui.settings.SettingsActivity } from null (pid=..., uid=2000) requires org.autojs.permission.PLUGIN
```

Host resolution on the Pad (host debug 5312, which contains `TerminalPluginSettingsLauncher`):

```text
$ adb -s 968e9f18 shell cmd package query-activities -a org.autojs.plugin.TERMINAL_SETTINGS
  Activity #0:
      name=io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.SettingsActivity
      enabled=true exported=true
      permission=org.autojs.permission.PLUGIN
```

The host resolves exactly one match, which is what `TerminalPluginInspector.resolveSettings`
requires. On the Pad the host main screen's plugin tab (reached by swiping the pager; the tab label
ignores UiAutomator taps) lists 3-Shell Terminal first, and its "设置" button opened the plugin's
`SettingsActivity` (`mCurrentFocus` and `topResumedActivity` named it) showing the appearance rows
"语言 / 夜间模式 / 主题色" with their "跟随 AutoJs6" summaries; Back returned to the host.

### Findings

- A `MaterialSwitch` row makes the switch itself non-clickable and lets the row own the tap, so
  instrumentation must `performClick()` the row view; clicking the switch only toggles its visual
  state.
- `am start` prints its `SecurityException` to stderr, which `UiAutomation.executeShellCommand`
  does not return (same as P3.3); the case therefore asserts that no `SettingsActivity` reaches
  RESUMED and the refusal text was captured from the PC side.
- `StringResourceParityTest` assumed `strings_donottranslate.xml` held only `app_name`; since
  P5.2 / P5.1 it also carries the About component names and the language labels, so the test now
  checks that every entry is untranslated and sorted and that `app_name` keeps its value.
