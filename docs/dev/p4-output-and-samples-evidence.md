# P4.2 session events and output pipes (2026-10-02)

This continues the P4.1 evidence in `p4-script-api-evidence.md`. The existing uncommitted
terminal changes in both repositories were retained and completed. The host was verified in
an isolated checkout of `bb7aa5c48f` plus the terminal changes, because concurrent installer
and layout-inspector work was changing the original host tree during this session. The
verification APK reports host build 5312; the terminal implementation matches the files
returned to the original host workspace. Plugin verification uses build 22 / 1.0.0.

## Implementation

- `TerminalSessionNativeObject` is an EventEmitter with output, exit and overflow events,
  `waitFor` / `waitForAsync`, `transcript` / `transcriptAsync`, and `off`. Listener registration
  holds the script loop alive. A first output/overflow listener opens a subscription; removing
  the last one, including consuming a once listener, releases it. The first subscription of
  a script-created session requests transcript replay, while recovered sessions start live.
- `TerminalOutputReader` incrementally decodes UTF-8, strips ANSI by default, splits lines at
  64 KiB of UTF-8 without breaking code points, and emits idle prompt fragments after the
  pump has seen no bytes for 100 ms. The pump uses bounded polling, including on API 24.
- `TerminalPromises` pumps the originating thread's terminal dispatchers while a synchronous
  wait runs. An output handler can answer a prompt with `write` while `waitFor` waits. Async
  results and events return to the same script thread; the UI thread requires Async methods.
- `TerminalService` owns one callback binding lease per script, registered before creating
  sessions so fast exits retain their codes. Subscriptions are published before their reader
  starts. Script exit closes readers, unsubscribes and releases the callback lease without
  closing user sessions. Release jobs survive script cancellation; acquiring a Binder fd or
  lease is protected against cancellation midway through ownership transfer.
- Plugin replay and transcript reads remove trailing screen padding while retaining prompt
  spaces and the line boundary before live output. `TerminalCursorPosition` is a package
  bridge into the existing emulator AAR; no AAR or public contract changed.
- Plugin session lists now snapshot pending and running sessions under the same startup lock.
  Previously the transfer could occur between these reads, briefly making a valid id disappear
  and causing visible `exec` to fail in `show` with `SESSION_NOT_FOUND`. The new instrumentation
  regression observes the transition concurrently across 24 session starts.

## JVM and build evidence

All Gradle invocations disabled automatic build-number and build-time updates. Host verification
also used `-Pksp.incremental=false` after a concurrent build invalidated generated KSP classes in
the original workspace; the isolated checkout built without changing those unrelated sources.

| Check | Result |
| --- | --- |
| Host `:app:testAppDebugUnitTest` | 3334 total, 3328 passed, 6 existing skips, 0 failures |
| Host `TerminalOutputReaderTest` | 9 / 9: ANSI/raw modes, split UTF-8, line splitting, UTF-8 byte cap, prompt fragments, overflow order and EOF |
| Host `TerminalBlockingWaitTest` | 5 / 5: originating-thread delivery, terminal ordering, cancellation, completion and listener failures |
| Host `:app:assembleAppDebug`, `:app:assembleAppDebugAndroidTest` | passed; first isolated full build took 8 minutes |
| Plugin `:app:testDebugUnitTest` | 146 / 146 |
| Plugin debug APK, androidTest APK, lint debug | passed; lint 0 errors / 34 existing warnings |
| Plugin `:app:verifyNativePageAlignment` | passed for debug and release; 16384-byte alignment |
| Plugin Markdown and launcher icon checks | passed: 10 languages / 36 Markdown artifacts, 15 icon resources |

The initial host full test run found one existing stale assertion: the plugin wizard expected
46 entries after the terminal plugin had raised the catalog to 47. The count and the terminal
category/title guard were updated; no other test failure was suppressed.

## Devices and stream evidence

SDK and ABI were re-read before deployment. Matching ABI APKs were installed with replacement;
no application was uninstalled and no shared-storage grant or user directory was changed.

| Device | Configuration | Result |
| --- | --- | --- |
| Xiaomi Pad 23046RP50C, `968e9f18` | API 35, arm64-v8a; Node.js Runtime 1.5.6 | Host `TerminalScriptSmokeDeviceTest` 6 / 6 |
| Same device | API 35, plugin instrumentation | `TerminalBinderContractTest` 18 / 18, including the concurrent startup/list regression |
| Android SDK x86 AVD, `emulator-5554` | API 24, x86 | Prompt input, quick exit/Async waits and script-stop cleanup 3 / 3 |

The three-line command delivered `line1`, `line2`, `line3`, then `exit(3)` on the originating
script thread. `waitFor(/line2/)` returned `line2`; a simultaneous blocking wait continued to
dispatch output and exit events. A following command retained exit code 7, and repeated quick
commands retained 0 through 7; Async execution retained 11. The exit-only listener observed 9.

The prompt case printed `name? ` without a newline. Its output handler sent an answer through
`write`, and a concurrent `waitFor` returned `hello_terminal`. Transcript reading included the
answer; malformed limits produced `INVALID_ARGUMENT`, and a short unmatched wait produced
`TIMEOUT`. Once/off removal released the script's subscriptions, repeated subscriptions did
not exhaust the plugin limit, and stopping a script left its session alive with zero owned
subscriptions after the script thread finished.

The visible throughput case ran `yes | head -c 50m; exit 3` after a startup delay and deliberately
stopped consuming script events for four seconds. It delivered 442359 output events, reported
74580732 dropped bytes, and ended with code 3. Pty newline expansion means this byte count is
not comparable to the 50 MiB command input as a simple percentage. The host performed 66
`readTranscript` probes that cross the plugin's UI thread during the run; the slowest took
289 ms, below the test's 5000 ms stall bound. The test asserts no output/overflow after exit.

Two test-harness issues were corrected without changing production lifecycle semantics:
Android refused the original `/` test cwd as unreadable, so fixtures now use plugin HOME;
the execution listener fires before engine destruction, so cleanup assertions wait for the
script thread to finish. IDEA's debugger router could not target the host verification tree
outside the currently loaded plugin project; lifecycle ordering was checked in source and
then verified by the device cleanup assertions.

Output events remain a bounded, lossy observation stream. Replay cannot recover output from a
session that was removed before subscription. `waitFor` searches subsequent output, including
prompt fragments; it does not search old transcripts. A timeout leaves the shell running.
Use `keepOpen: false` to wait for a command's exit instead of its trailing interactive shell.
