# P6 robustness evidence

Recorded on 2026-10-07. The implementation is in `e2a52ae` (development build 32).
This record distinguishes completed checks from the remaining release acceptance work.

## Implemented fixes

- The emulatorview patch coalesces main-thread input messages, avoids empty blocking queue
  reads, drains queued bytes before EOF, and handles reader/writer shutdown during startup.
  Original sources, the original AAR, the modified source and the offline rebuild recipe are
  retained in `vendor/emulatorview/`; the consumed AAR is hash-locked.
- `TerminalPtySession` cleans up natural exits, failed starts and unattached sessions.
  `PtyIo` owns the original master descriptor for both streams. Bounded polling permits
  cancellation without closing a descriptor still used by another thread. It uses public
  APIs available on API 24 and does not introduce an inherited duplicate master descriptor.
- Resuming a terminal retries foreground protection if background restrictions denied its
  initial start. This was reproduced on Xiaomi API 35 and verified on Xiaomi and Redmi.
- Test cleanup joins the output reader before checking its termination and always closes
  the owned shell and removes the owned file, retaining the original failure if cleanup fails.

## Completed automation

`TerminalRobustnessInstrumentationTest` covers cancellation while the producer remains open,
natural/unattached exits, 100 create/close cycles, a real 100 MiB file, independent fast and
stalled output subscribers, interrupting `yes`, pty resizing and first-prompt measurements.

| Environment | Full suite result | Total fds before/after 100 cycles | Remaining terminal workers | Largest main-thread heartbeat gap during 100 MiB output |
| --- | --- | --- | --- | --- |
| API 24 x86 AVD | 71 passed, 7 explicitly skipped, 0 failed | 48 / 48 | 0 | 62 ms |
| API 37.1 x86_64 AVD, 16384-byte pages | 70 passed, 8 explicitly skipped, 0 failed | 117 / 117 | 0 | 91 ms |

Both runners report `OK (78 tests)`. Skips are the optional installed-Node cases and the
explicitly coordinated lifecycle case; the actual docked-stack check runs only on API 24.
The fast subscriber retained the complete 100 MiB output; only the stalled subscriber lost
bytes. Ctrl+C stopped `yes`, and the shell accepted the next command.

`TerminalLifecycleInstrumentationTest` verifies recreation, landscape/portrait changes,
keyboard visibility, the same surviving shell pid and matching pty dimensions. API 24 also
entered Android's real docked stack, returned to fullscreen and kept the same shell. The
window-size test waits for configuration/layout changes and uses unique output probes;
a persistent view/pty mismatch remains a failure.

On Xiaomi 23046RP50C (API 35), a background-created session originally had no foreground
notification even after its screen opened. Retrying from `onResume` restored protection.
Xiaomi and Redmi 22120RN86C (API 33) then passed both lifecycle cases. The recorded Xiaomi
task-removal result was `backgroundRestricted=false`, one visible notification, a live
session and a responding background shell. OEM restrictions are not bypassed.

`TerminalBinderContractTest` exercises real Binder transactions and a client process that
dies: its callback and output subscription are removed while the user session remains.
The host stream suite passed 7/7 on the API 37.1 AVD and Redmi API 33. It checks event order,
exit codes, async waits, bounded output pressure and script cleanup that preserves sessions.

The coordinated `NodeRuntimeLifecycleInstrumentationTest` passed on a disposable API 35
x86_64 AVD on 2026-10-05. Replacing, uninstalling and restoring the Node.js Runtime APK left
the already running node process producing output. A new plan refreshed command links after
replacement/restoration and provided a usable plain shell after uninstall. The coordinator
only acted on its own disposable AVD; no physical device's runtime was uninstalled.

## Remaining release acceptance

- Controlled termination of the actual plugin process, proof that its old children are gone,
  preservation of private data, and a clean new launch. The prepared API 24 coordinator needs
  the legacy `ps` format; its first attempt stopped before writing a marker or killing a process.
- Final signed ABI/universal installation and host acceptance rows in
  `p6-compat-matrix.md`, including the armv7 package on a compatible physical device.
- Final API 35 physical performance spot-check after the shared-descriptor implementation.

The maintainer asked on 2026-10-07 to prioritize work that does not use the shared devices
while other tasks still need them. These remaining items are intentionally not marked passed.
