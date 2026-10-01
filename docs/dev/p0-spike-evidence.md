# P0 spike evidence

Roadmap P0.2 proves, under the plugin's own uid and before any host code is migrated, the three facts the
terminal depends on. The spike is `app/src/androidTest/.../spike/P0SpikeTest.kt`; it logs under the tag
`ThreeShellSpike` and the lines below are copied from `adb logcat -d -s ThreeShellSpike` on 2026-10-01.
`PtyBridge.java` (same-package bridge to the package-private libtermexec / term JNI, copied from host commit
`c6e24541ef`) is the only production source the spike needed.

Run it with the debug APK installed (`connectedDebugAndroidTest` uninstalls the app afterwards, so storage
grants are applied to a separate `adb install -r [-g]` of the debug APK and the test APK, then
`am instrument -w -e class ...spike.P0SpikeTest <appId>.test/androidx.test.runner.AndroidJUnitRunner`).

## Device matrix

| Device | API | ABI | Page size | Node.js Runtime | pty | storage | Node launcher |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AVD `emulator-5554` | 24 | x86 | 4 KB | not installed | pass (16 ms) | denied -> granted (runtime permission) | skipped (assumption) |
| Sony G8441 `BH900ASK9E` | 28 | arm64-v8a (plugin runs 32-bit `lib/arm`) | 4 KB | 1.5.6 (217, 32-bit) | pass (29 ms) | denied -> granted (runtime permission) | pass (54 ms) |
| Redmi `bek749scrwv4wo8h` | 33 | arm64-v8a | 4 KB | 1.5.6 (220) | pass (52 ms) | denied (`Operation not permitted`) | pass (153 ms) |
| Xiaomi Pad `968e9f18` | 35 | arm64-v8a | 4 KB | 1.5.6 (220) | pass (64 ms) | denied -> granted (all files access) | pass (205 ms) |
| AVD `emulator-5558` | 37 | x86_64 | 16 KB (`getconf PAGE_SIZE` = 16384) | not installed | pass (34 ms) | denied (`Operation not permitted`) | skipped (assumption) |

## 1. pty: `/dev/ptmx` + libtermexec fork / exec + exit code

`ParcelFileDescriptor.open("/dev/ptmx")`, `PtyBridge.createSubprocess(ptmx, "/system/bin/sh", ["/system/bin/sh", "-c",
"echo spike-$$; pwd; id; exit 7"], envp)`, `setWindowSize(24, 80)`, `setUtf8Mode(true)`, read the master until EOF,
`PtyBridge.waitFor(pid)`.

```text
API 24  pty: pid=19119 exit=7 elapsed=12ms uid=10305 api=24 abi=x86
        pty output: spike-19119 | / | uid=10305(u0_a305) ... context=u:r:untrusted_app:s0:c512,c768
API 28  pty: pid=20273 exit=7 elapsed=29ms uid=10862 api=28 abi=arm64-v8a
        pty output: spike-20273 | / | uid=10862(u0_a862) ... context=u:r:untrusted_app:s0:c94,c259,c512,c768
API 33  pty: pid=22325 exit=7 elapsed=52ms uid=10807 api=33 abi=arm64-v8a
        pty output: spike-22325 | / | uid=10807(u0_a807) ... context=u:r:untrusted_app:s0:c39,c259,c512,c768
API 35  pty: pid=10912 exit=7 elapsed=64ms uid=10360 api=35 abi=arm64-v8a
        pty output: spike-10912 | / | uid=10360(u0_a360) ... context=u:r:untrusted_app:s0:c104,c257,c512,c768
API 37  pty: pid=8878 exit=7 elapsed=34ms uid=10273 api=37 abi=x86_64   (16 KB page size)
        pty output: spike-8878 | / | uid=10273(u0_a273) ... context=u:r:untrusted_app:s0:c17,c257,c512,c768
```

Facts confirmed: the child runs under the plugin uid in the `untrusted_app` domain on every device (D15: sessions
cannot inherit host permissions); the vendored 16 KB-aligned `libjackpal-termexec2.so` loads and forks on a native
16 KB x86_64 system; `cwd` of the child is `/` when no `chdir` is requested (the session launcher must `cd` itself).

## 2. Shared storage follows the plugin's own grant

`cd /storage/emulated/0 && ls` plus a decisive write probe (`echo > /sdcard/three-shell-spike-<ts>.txt`), because
listing `/sdcard` succeeds on some API levels even without the permission.

```text
API 24  before: granted=false  ls: .: Permission denied            write: can't create ...: Permission denied
        after `pm grant READ/WRITE_EXTERNAL_STORAGE`: granted=true  ls lists Alarms | Android | ...  write: __WRITE_OK__
API 28  before: granted=false  ls: .: Permission denied            write: Permission denied
        after `pm grant`: granted=true, write __WRITE_OK__
API 33  granted=false  ls lists the directory names               write: Operation not permitted
API 35  before: granted=false  ls lists the directory names        write: Operation not permitted
        after `appops set <appId> MANAGE_EXTERNAL_STORAGE allow`: Environment.isExternalStorageManager()=true, write __WRITE_OK__
API 37  granted=false  ls lists the directory names               write: Operation not permitted
```

Facts confirmed (D18): the host's grants never apply (the host was installed and granted on every device); on
API < 30 the failure text is `Permission denied`, on API 30+ it is `Operation not permitted` and the top-level
listing still works. `StorageAccess` must therefore classify by the plugin's own permission state
(`isExternalStorageManager()` / runtime permission), not by parsing shell errors, and the "all files access"
banner must trigger on the state, not on the first failed command.

## 3. Node.js launcher of the Node.js Runtime plugin

Query `org.autojs.plugin.nodejs.RUNTIME` on the official package, read the `NODE_CLI_*` meta-data, compare the
signer digests with the official set / our own, exec `nativeLibraryDir/libnodexe.so --version` from the plugin
process.

```text
API 28  node: version=1.5.6 (217) schema=1 executable=libnodexe.so commands=node,npm,npx,corepack,yarn,yarnpkg,pnpm,pnpx
              archive=nodejs/cli/node-cli-24.21.0.bin sha256=ffc84169358de906...
        node: signers=[31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213] official=true own=true
        node: executable=/data/app/io.github.supermonster003.autojs6.plugin.nodejs-.../lib/arm/libnodexe.so exists=true canExecute=true
        node: --version exit=0 elapsed=54ms output=v24.21.0 selinux=u:r:untrusted_app:s0:c94,c259,c512,c768
API 33  node: version=1.5.6 (220) ... lib/arm64/libnodexe.so ... --version exit=0 elapsed=153ms output=v24.21.0
API 35  node: version=1.5.6 (220) ... lib/arm64/libnodexe.so ... --version exit=0 elapsed=205ms output=v24.21.0
API 24 / 37: Node.js Runtime not installed -> test skipped by assumption.
```

Facts confirmed (D17): `untrusted_app` may execute another app's `apk_data_file` native library on all three
physical devices (Sony stock Android 9, Redmi MIUI 14 / Android 13, Xiaomi HyperOS / Android 15); no `ExecDenied`
case was observed, so the `ExecDenied` branch stays a defensive classification with no known trigger. The
official signer digest equals the host's `PluginTrustManager.OFFICIAL_SHA_256`; the debug plugin is signed with
the same key, so `own=true` as well. On the Sony API 28 device the Node.js Runtime plugin is the 32-bit build
(`lib/arm`, versionCode 217): the terminal must pick the launcher ABI from the Node.js plugin's
`nativeLibraryDir`, never from its own process ABI.

## Revisions to D15-D18

None required. Additional notes recorded for P2 / P3: child `cwd` defaults to `/`; storage classification by
state; launcher ABI from the Node.js plugin's `nativeLibraryDir`; `Process.waitFor(timeout)` is API 26+ (the
probe helper polls `exitValue()`), matching the host's `NodeCliProbe` watcher-thread approach.
