# P6 performance and size

Recorded on 2026-10-07. Prompt timings exclude the first warmup session and Node archive
extraction. Samples are device measurements, not synthetic estimates.

| Environment | Median first prompt | Maximum of 10 measured prompts | Scope |
| --- | --- | --- | --- |
| Xiaomi 23046RP50C, API 35 | 98 ms | 102 ms | Development build 32, before the final shared-descriptor refinement |
| Xiaomi 23046RP50C, API 35 | 79 ms | 103 ms | Final shared-descriptor implementation, 2026-10-07 acceptance |
| API 24 x86 AVD | 70 ms | 106 ms | Shared-descriptor implementation, complete local suite |
| API 37.1 x86_64, 16 KiB AVD | 34 ms | 38 ms | Shared-descriptor implementation, complete local suite |

All ten final physical API 35 samples are below the 500 ms target:
74, 79, 51, 55, 79, 80, 90, 74, 98 and 103 ms. The later process-restoration change only selects
the session during Activity restoration; it does not change the measured session factory.

An isolated Node CLI extraction on Xiaomi measured 693 ms for 1699 entries / 10068758 bytes;
the immediate cached setup took 0 ms at millisecond resolution. An earlier run on the same
device measured 409 ms. The Sony G8441 baseline measured 1439 ms and a 2 ms cached setup.
These checks used temporary private cache roots and removed only their own extracted trees.
The final Xiaomi extraction measured 420 ms for the same 1699 entries / 10068758 bytes, with
a 1 ms cached setup (installed Node.js Runtime build 220).

## Signed candidate sizes

Candidate build 34, version 1.0.0, was verified with the official signer, exact package/version,
the ABI/library inventory and CRC32 filenames. These are candidate measurements; the final
Release checksums and size record must be regenerated after the last source commit.

| APK | Bytes |
| --- | --- |
| arm64-v8a | 1782482 |
| armeabi-v7a | 1779698 |
| x86 | 1780914 |
| x86_64 | 1782780 |
| universal | 1881583 |

The universal candidate is approximately 1.79 MiB, below the 6 MiB target. Release shrinking
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

## Published APK sizes

Final v1.0.0 build 35, source `7af6fc2`, official signer, verified uploaded SHA-256:

| APK | Bytes |
| --- | --- |
| arm64-v8a | 1783222 |
| armeabi-v7a | 1780438 |
| universal | 1882323 |
| x86 | 1781654 |
| x86_64 | 1783520 |

The universal APK is 1.80 MiB. All four ABI-specific APKs and the universal APK were installed
and exercised after their final source commit. Release shrinking and 16 KiB alignment passed.
