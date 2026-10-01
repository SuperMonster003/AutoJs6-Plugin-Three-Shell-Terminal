# P2.5 package management and settings logic (2026-10-02)

The P3 menus and P5 settings page can now call shared logic without owning shell quoting,
registry validation or filesystem deletion. No new Activity or script API is introduced here.

## Implementation and migration differences

- `core/TerminalNpmActions` extracts the host menu's non-UI operations: npm init, install,
  whitespace-separated package specs, project script choices and execution, Yarn / pnpm version
  commands and encoded npm search URLs. `NpmProjectScripts` already migrated in P2.1; it keeps file
  order, distinguishes a missing or invalid file from an empty script list and ignores non-string
  script entries. The dialog bodies and their translated help remain in the migration snapshot for P3.
- `TerminalNodeEnvironment.sanitizeRegistry` now parses the whole URI. It accepts HTTPS with a host,
  optional port and path, normalizes scheme/host case and a trailing slash, and rejects HTTP, malformed
  hosts, control characters, credentials, query strings and fragments. `TerminalPreferences.setRegistry`
  validates before atomically saving the choice and custom URL. Rejected input leaves settings intact.
- `TerminalSettingsActions` exposes registry selection/summary, ignore-scripts, Node integration and
  asynchronous clear-data. The registry and ignore-scripts affect new sessions, matching the original
  host behavior. Node integration changes invalidate the resolution cache.
- Clear-data first blocks new session plans, invalidates old plans and closes live/pending sessions.
  A worker also waits for shells already removed from the list but still in their close grace period, and serializes deletion with Node extraction/link updates. It removes
  exactly `home` and `usr`, recreates the layout/profile and invalidates Node resolution. The terminal
  settings and projects elsewhere are preserved. Completion, including errors, returns on the main
  thread; failure releases the creation gate so later sessions remain possible.
- `TerminalDataCleaner` deletes links themselves, including dangling links, without traversing
  directory symlinks into external projects. It refuses a symlinked terminal root and reports failed
  recreation. The old `TerminalPaths.clearAll` also uses the non-following deletion helper.
- A generation travels with every Binder pending request and `SessionAssembly.Plan`. A delayed plan
  cannot reinstall assets or launch a shell after clear-data finishes. Notification close-all and
  Binder close-all share the registry's pending cancellation hook.
- Final Binder review added a UTF-16 character budget to transcript replies as well as the UTF-8
  byte ceiling: a 600,000-character ASCII transcript would exceed the Binder buffer after Parcel
  encoding. Replies keep the newest text on a Unicode boundary and set `truncated` when either limit
  cuts older text. This does not change the AAR vocabulary or advertised V1 ceilings.

## Evidence

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `testDebugUnitTest` | 125 passed, 0 failed |
| ANDROID_BUILD | debug and androidTest APKs, release APKs / R8 | Passed |
| ANDROID_BUILD | lintDebug / lintRelease | 0 errors (11 / 14 warnings) |
| ANDROID_BUILD | `verifyNativePageAlignment` | Debug and release aligned to 16384 bytes |
| BINDER / DEVICE | API 24 / x86, `emulator-5554` | 32 passed, 1 skipped across the focused regressions |
| BINDER / DEVICE | Xiaomi 23046RP50C / API 35 / arm64-v8a, `968e9f18` | Same 33 distinct cases passed |
| DEVICE / NETWORK | Pad, fresh npm cache, npmmirror | `is-number@7.0.0` installed; `npm run verify` exit 0; postinstall marker absent with ignore-scripts enabled |
| DOCS | 10 language JSON sources, generated Markdown and launcher icons | Checks passed |

The 33 distinct cases are Binder 17, package/settings 5, session core 4, plugin contract 6, plus
one Node case per device. The initial core regression ran 31 cases; the long-transcript case ran
separately. After the close-grace regression was added, all 26 Binder/package/session cases were
rerun together on both devices. The plugin contract and Node command checks passed in the earlier run. The only API 24 skip is the online npm case because
that AVD has no Node.js Runtime. Its separate Node case proves a plain shell without the runtime;
the Pad's Node case verifies node v24.21.0 / npm 11.19.0 / corepack 0.36.0 inside a real session.

The five package/settings device cases cover:

1. Atomic custom-registry rejection, normalized summary and registry / ignore-scripts environment.
2. One live shell and one blocked pending plan closed before clearing an isolated fixture. Its
   external-project symlink target, dangling-link handling and stored preferences are checked.
3. Failed deletion/recreation delivered on the main thread, unchanged invalid root and a reopened
   session-creation gate.
4. A shell which ignores SIGHUP and was already removed from the visible session list must be
   reaped before clear-data completes.
5. Real npm install from npmmirror using a fresh per-test cache, scripts in package.json order,
   explicit script execution and a suppressed postinstall hook.

## Scope and device state

Clear-data tests operate under the plugin cache directory, never the user's actual terminal home
or shared-storage projects. Registry and Node preferences touched by the new tests are restored.
For notification verification, the Pad's background app-op and notification grant were temporarily
allowed and restored in `finally` to `RUN_ANY_IN_BACKGROUND=ignore` and notification permission denied.
The restricted-state Binder regression also passed (its notification case correctly skipped).

Instrumentation was run with explicit serials and matching x86 / arm64 APKs, rather than running
`connectedDebugAndroidTest` against all attached user devices. Release digest collection, remote
push, official index registration and publication were not requested and remain gated by D9 / P7.
UI navigation and positive host-entry integration are P3 work; the independent settings UI is P5.

The README's 10 language Android limitation notes now explain that disabled npm bin links also
prevent `npx <package>` from directly launching the package entry. This follows P2.3 device evidence;
the reference Node.js Runtime document still contains its older `npx` suggestion. No host or sibling
repository files were changed in this session.

The host working tree acquired unrelated concurrent edits during this session. They were neither
modified nor staged by this task; the commits and clean-worktree checks apply to this plugin repository.
