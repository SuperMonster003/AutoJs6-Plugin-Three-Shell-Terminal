# P6 compatibility matrix

Updated 2026-10-07. D32 requires the four physical devices below, API 24 x86 and an API 37
16 KiB AVD. Development checks are recorded separately from final signed release acceptance.
Candidate build 34 uses the official signer and release shrinking. Its complete installation
matrix passed on 2026-10-07; final Release assets are rechecked after the last source commit.

| Device | API / ABI | Completed development evidence | Signed candidate 34 ABI + universal acceptance |
| --- | --- | --- | --- |
| Xiaomi 23046RP50C | 35 / arm64-v8a | Pressure/cleanup, final shared-descriptor prompt timings, Node CLI, isolated extraction, rotation/keyboard/task removal | arm64-v8a and universal: 2/2 each |
| Sony G8441 | 28 / arm64-v8a and armeabi-v7a | Corrected npx case, lifecycle, natural exits and Node extraction passed | arm64-v8a, armeabi-v7a and universal: 2/2 each; 32-bit process confirmed by primaryCpuAbi=armeabi-v7a |
| Sony XQ-AT72 | 31 / arm64-v8a | Corrected npx case, lifecycle and Node 1.5.0 command checks passed | arm64-v8a and universal: 2/2 each |
| Redmi 22120RN86C | 33 / arm64-v8a | Pressure/cleanup, lifecycle after foreground-retry fix, host script suite 7/7; corrected npx case passed 1/1 on recovery source | arm64-v8a and universal: 2/2 each |
| Disposable API 24 AVD | 24 / x86 | Complete suite: 71 passed, 7 optional skips; real split screen, foreground-service path without channels, native loading; actual process termination/relaunch passed | x86 and universal: 2/2 each |
| Disposable API 37.1 AVD | 37 / x86_64, 16384-byte pages | Complete suite: 70 passed, 8 optional skips; native loading and pressure/cleanup; lifecycle/entry regression after recovery fix passed | x86_64 and universal: 2/2 each |

The two original 71-case physical runs and the Redmi run failed an incorrect test assumption:
`npx` may reuse a dependency already installed in the current project without populating its
own cache. The corrected case runs from a separate directory with an explicit package version.
The corrected case passed on both Sony devices, Redmi and in the complete API 35 AVD suite. It retains
the expected Android bin-shim limitation (exit 127); the test does not reinterpret it as success.

## Host acceptance harness

`TerminalReleaseMatrixDeviceTest` is in the AutoJs6 host and references only the public host
client/API. It can test signed, minified plugin APKs without depending on obfuscated plugin
implementation classes. It exercises:

- The drawer's `openNew` request, the directory menu's `open(path)` request and the project
  toolbar's `open(path)` request through `TerminalLauncher`, with a visible terminal, directory
  changes and the same session. This is route-level automation; the earlier P3 record covers
  actual widget clicks and Back navigation.
- A temporary plugin storage grant, `cd /sdcard`, echo, optional node/npm/corepack version
  commands and the actual bundled open-directory sample. Permission state is restored after
  owned sessions are cleaned up.

The harness passed 2/2 on Xiaomi API 35 against the development plugin. Node returned
`v24.21.0`, npm `11.19.0` and corepack `0.36.0`. The host check is built in an isolated checkout
to avoid another task's active edits/build output. Existing user sessions and external projects
are not intentionally cleared by this harness.

## OEM and platform notes

- Xiaomi/Redmi can deny foreground protection while the app is in the background. Opening
  the terminal now retries protection when its visible state permits it. Notifications and
  background activity still remain subject to Android/OEM settings.
- API 24 has no `stty` utility in the tested image; mksh's SIGWINCH-updated dimensions are
  used there. Newer images use `stty size` when available.
- Native ELF and APK alignment checks cover all four ABIs at 16384 bytes. The matrix includes
  native execution in a real armv7 process and on the 16384-byte-page AVD.
- ADB reconnections interrupted some local attempts. Interrupted runs and invalid test-selector
  invocations are not counted as product test passes.

The maintainer resumed device acceptance on 2026-10-07. All 13 candidate installations and
their 26 host acceptance tests passed. The final source also fixes restoration of a task after
process death; its separate lifecycle and process checks are in `p6-robustness-evidence.md`.
Final Release APK hashes and reinstallation results are recorded with the publication receipt.

## Published v1.0.0 acceptance

The final source is `7af6fc27984546f0b8032af03c5254a77558c769`, build 35. After the restoration
fix, all six rows above were repeated using the exact five APK files attached to the stable
Release: 13 signed installations and 26 host tests passed. Every installed artifact hash was
matched to the uploaded asset and index admission record. Sony G8441 again ran the armv7
process; the API 37.1 AVD again used 16384-byte pages. Sony XQ-DQ72 (QV770340J7) additionally
received the final universal APK without clearing its data.

The [stable Release](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases/tag/v1.0.0)
contains the five APKs and `SHA256SUMS`. Catalog day/night PNGs are byte-identical to the
plugin's transparent Plugin Center resources, with #FAFAFA / #212121 backgrounds.
