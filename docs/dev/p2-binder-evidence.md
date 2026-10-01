# P2.4 Binder evidence (2026-10-02)

The plugin implements the thirteen V1 methods from the locked `terminal-api.aar`.
The frozen contract takes precedence over the early roadmap sketch: open requests use
`cwd`, `command`, `env`, `title`, `keepOpen`; advertised optional features are `node-cli`,
`output-subscription`, `transcript`. Sessions and command execution are baseline V1 methods.
The UI and settings entry points remain P3 / P5 work.

## Implementation

- `HostCallerGuard` checks the PLUGIN permission, installed host uid and package, minimum
  build and equal non-empty signer sets before routing any request. The INFO service remains
  available to the host's discovery protocol as before.
- Requests use strict JSON and UTF-8 byte limits. Input queues are bounded as well as individual
  calls. Unknown session writes and capped transcript reads follow the frozen AIDL semantics.
- Every open returns a pending session while a worker prepares storage and Node assets. Main-thread
  creation transfers pending taps and input atomically. Cancelling a pending session prevents its
  delayed plan from starting a shell. The production router survives service rebindings.
- Raw output travels through descriptor pipes, with one fixed 1 MiB ring per subscription. Oldest
  bytes are dropped and reported, including the last coalesced report. Session exit drains output;
  a reader that remains blocked for five seconds is released. Explicit unsubscribe and host death
  abort immediately. Callbacks are keyed by Binder and linked to death; sessions survive host death.
- Output writer descriptors are released after remote parcel transfer. The host owns its received
  read descriptor. Debug endpoints are non-exported and absent from release builds; the permission
  denial client belongs only to the separately installed instrumentation APK.
- Commands run verbatim after `cd`, on their own script line, and then either enter an interactive
  shell or exit with the command status. Closing uses SIGHUP followed by a 500 ms grace period and
  SIGKILL if needed, without waiting on the main thread. Thread names contain no command contents.

## Validation

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `testDebugUnitTest` | 116 passed, including strict requests, limits, UTF-8 transcript tails, caller policy and ring buffer |
| ANDROID_BUILD | debug APKs, androidTest APK, lintDebug | Passed; lint 0 errors |
| ANDROID_BUILD | `verifyNativePageAlignment` | Debug and release native libraries aligned to 16384 bytes |
| BINDER / DEVICE | `emulator-5554`, Android 7.0 / API 24 / x86 | 26 passed: Binder 16, session core 4, plugin contract 6 |
| BINDER / DEVICE | `968e9f18`, Xiaomi 23046RP50C / API 35 / arm64-v8a | Same 26 passed; Node.js Runtime installed |
| DOCS | Markdown and launcher icon generators | 10 language documents and 15 icons verified |

The Binder cases cover open/write/pipe/read/close, malformed and oversized requests, the 17th
session, 5th subscription and 9th callback, pending input/cancellation, remote transactions and FD
ownership, real client process death, output overflow, a full pipe with a non-reading subscriber,
command exit 7 with its final output, production caller rejection and notification close-all.
The `.test` APK requests no PLUGIN permission and runs its denial service in its own uid; Android
refuses its attempt to bind the production service on both devices.

## Findings and limits

- API 24 does not wake an already blocked pipe write just because another thread closes its FD.
  A regression test reproduced the stranded writer. Polling for writable space and writing at most
  Linux PIPE_BUF (4096 bytes) from the single writer fixes cancellation without hidden APIs.
- The Pad originally had `RUN_ANY_IN_BACKGROUND=ignore` and no notification grant. ActivityManager
  logs showed `startForeground` rejected by the background restriction, followed by
  `ForegroundServiceDidNotStartInTimeException` on stop. The plugin now checks
  `ActivityManager.isBackgroundRestricted` before requesting foreground protection. The restricted
  Binder suite passed after this change. An IDE debugger also verified the session-start call chain
  on a Redmi API 33 device, where the restriction was false; that observation is not Pad evidence.
- For the Pad's notification test, background activity and notifications were temporarily allowed.
  Both were restored to their original state in `finally`. No app was uninstalled and no shared
  storage data was cleared. APKs were installed by explicit serial and matching ABI.
- The cross-process endpoint uses the production router with a plugin-uid test guard. The production
  guard itself is covered by denial tests and the JVM signer policy. An end-to-end positive host
  UI binding and drawer session count remain in the P3 host-entry follow-up, because the entry
  Activity does not exist yet.
- No new feature strings are needed by this logic-only stage. The 10 language preview status and
  changelog were regenerated. No remote push, index registration or Release was performed.
