# P6 performance and size

Recorded on 2026-10-07. Prompt timings exclude the first warmup session and Node archive
extraction. Samples are device measurements, not synthetic estimates.

| Environment | Median first prompt | Maximum of 10 measured prompts | Scope |
| --- | --- | --- | --- |
| Xiaomi 23046RP50C, API 35 | 98 ms | 102 ms | Development build 32, before the final shared-descriptor refinement |
| API 24 x86 AVD | 70 ms | 106 ms | Shared-descriptor implementation, complete local suite |
| API 37.1 x86_64, 16 KiB AVD | 34 ms | 38 ms | Shared-descriptor implementation, complete local suite |

The physical API 35 baseline is below the 500 ms target. Its final source/release spot-check
remains pending while shared devices are in use by other tasks.

An isolated Node CLI extraction on Xiaomi measured 693 ms for 1699 entries / 10068758 bytes;
the immediate cached setup took 0 ms at millisecond resolution. An earlier run on the same
device measured 409 ms. The Sony G8441 baseline measured 1439 ms and a 2 ms cached setup.
These checks used temporary private cache roots and removed only their own extracted trees.

## Signed candidate sizes

Candidate build 32, version 1.0.0, was verified with the official signer, exact package/version,
the ABI/library inventory and CRC32 filenames. These are candidate measurements; the final
Release checksums and size record must be regenerated after the last source commit.

| APK | Bytes |
| --- | --- |
| arm64-v8a | 1813194 |
| armeabi-v7a | 1810410 |
| x86 | 1811626 |
| x86_64 | 1813492 |
| universal | 1912295 |

The universal candidate is approximately 1.82 MiB, below the 6 MiB target. Release shrinking
and R8 are enabled. JNI entry points in `jackpal.androidterm.**` remain kept. All eight native
library/ABI combinations pass the 16384-byte alignment gate.

## Regression thresholds

- Warm first prompt: all 10 physical API 35 samples below 500 ms, excluding initial archive
  extraction. Record emulator results without treating shared-runner timing as physical evidence.
- Universal APK: below 6 MiB; investigate an increase above 10 percent from the accepted release.
- 100 create/close cycles: no remaining terminal workers or pty descriptors, and no increase in
  the warmed total fd count. Android framework descriptors can disappear independently.
- 100 MiB output: the draining subscriber retains the complete file, stalled subscribers report
  their own loss, and main-thread heartbeat gaps stay below 2000 ms in the device stress test.
