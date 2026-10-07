# P6 compatibility matrix

Updated 2026-10-07. D32 requires the four physical devices below, API 24 x86 and an API 37
16 KiB AVD. Development checks are recorded separately from final signed release acceptance.
Source/build numbers are development evidence, not proof that the future Release assets were installed.

| Device | API / ABI | Completed development evidence | Final signed ABI + universal acceptance |
| --- | --- | --- | --- |
| Xiaomi 23046RP50C | 35 / arm64-v8a | Pressure/cleanup, prompt timings, Node CLI, isolated extraction, rotation/keyboard/task removal; host route/storage/sample checks 2/2 | Pending |
| Sony G8441 | 28 / arm64-v8a, supports armv7 | Original 71-case run had only the old npx-cache test failure; corrected npx case passed; lifecycle, natural exits and Node extraction passed | Pending, include armv7 native execution |
| Sony XQ-AT72 | 31 / arm64-v8a | Original 71-case run had only the old npx-cache test failure; corrected npx case passed; lifecycle and Node 1.5.0 command checks passed | Pending |
| Redmi 22120RN86C | 33 / arm64-v8a | Pressure/cleanup, lifecycle after foreground-retry fix, host script suite 7/7 | Pending, including corrected npx case |
| Disposable API 24 AVD | 24 / x86 | Latest complete suite: 71 passed, 7 optional skips; real split screen, foreground-service path without channels, native loading | Pending |
| Disposable API 37.1 AVD | 37 / x86_64, 16384-byte pages | Latest complete suite: 70 passed, 8 optional skips; native loading and pressure/cleanup | Pending |

The two original 71-case physical runs and the Redmi run failed an incorrect test assumption:
`npx` may reuse a dependency already installed in the current project without populating its
own cache. The corrected case runs from a separate directory with an explicit package version.
The corrected case passed on both Sony devices and in the complete API 35 AVD suite. It retains
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
- Native ELF and APK alignment checks cover all four ABIs at 16384 bytes. The final release
  still needs the installation rows above, including a real armv7 process, not only static checks.
- ADB reconnections interrupted some local attempts. Interrupted runs and invalid test-selector
  invocations are not counted as product test passes.

Shared-device acceptance was deferred at the maintainer's request on 2026-10-07. The Release
and official index publication remain gated on completing the pending rows.
