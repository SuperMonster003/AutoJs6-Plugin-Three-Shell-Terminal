# Bounded terminal I/O and shutdown patch

This is the plugin's Apache-2.0 patch to jackpal emulatorview 1.0.42, based on
upstream commit `35188f8a8b57989a4a4ec9485e11187b46be26d9`.
The original `TermSession.java` and the original host-staged AAR are retained in
`upstream/`. Their origin is recorded in `../../THIRD_PARTY_NOTICES.md`.
The upstream license and notice are in `../../native/jackpal-termexec/` and are
already bundled in the application. The source under `src/` preserves its copyright header.

The 2026-10-05 P6 patch changes only `TermSession` and its nested classes:

- Coalesce input notifications. Process at most one 4 KiB buffer per main-thread
  message, then requeue so UI input and timers can run between output chunks.
- Do not call `ByteQueue.read` on an empty queue: even a zero-byte request waits
  for input there, so an extra notification can block the main thread.
- Drain queued output before EOF, including when coalescing reorders notifications.
- Publish shutdown across workers, interrupt a reader waiting on a full queue,
  and close the writer looper even if shutdown preceded Handler initialization.
- Permit idempotent cleanup before emulator initialization.

The plugin enables EOF cleanup in `TerminalPtySession`, and closes failed or
unattached exited sessions as well. No JNI symbol or terminal public API changes.
All other original classes and AAR entries keep their original bytes.

Rebuild with JDK 21 and the Android SDK platform jar recorded by SHA-256 in
`provenance.json`:

```text
python vendor/emulatorview/build.py --android-jar <sdk>/platforms/android-37.0/android.jar
python vendor/emulatorview/build.py --android-jar <sdk>/platforms/android-37.0/android.jar --check
```

The build is offline and uses canonical, uncompressed ZIP entries with fixed
timestamps. Update `locks/vendored-aars.lock` and the third-party notice together
after an intentional rebuild. Ordinary Gradle builds consume the locked AAR.
Device regressions cover 100 MiB output, independently blocked subscribers,
100 create/close cycles, prompt latency, and actual kernel pty resizing.
