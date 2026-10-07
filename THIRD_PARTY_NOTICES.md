# Third-party notices

3-Shell Terminal (`AutoJs6-Plugin-Three-Shell-Terminal`) is licensed under the Mozilla Public License 2.0
(see `LICENSE`). The components below are distributed with the plugin or used to build it; each keeps its own
license, reproduced in full in the distribution of the respective project.

## Host contract artifacts (staged in `libs/`, hash-locked in `locks/host-api-aars.lock`)

| Artifact | Origin | Version | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `common-plugin-api.aar` | AutoJs6 module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | host build 6.8.0 / 5303, commit `9545a7f4aa` (module unchanged since `9c3ba2e520`) | MPL 2.0 | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |
| `explorer-action-api.aar` | AutoJs6 module `plugin-api/explorer-action-api` (`IExplorerActionPlugin` AIDL and the `ExplorerAction*` constants: Explorer Action protocol v23) | host build 6.8.0 / 5320, commit `9fe2c84202` | MPL 2.0 | `80a7635a7757ebed20046e1410f9f6fe4ec43565a16e46ac1015614fb3320e71` |
| `nodejs-api.aar` | AutoJs6 module `plugin-api/nodejs-api` (`NodeJsPluginActions`, `NodeJsPluginCapabilityKeys`, the `NODE_CLI_*` manifest contract) | host build 6.8.0 / 5303, commit `9545a7f4aa` | MPL 2.0 | `4334b94a6f86e8ef8ff2912b6ace817dbf9bca1e887be7d615918f990f57ab71` |
| `terminal-api.aar` | AutoJs6 module `plugin-api/terminal-api` (`ITerminalPlugin` / `ITerminalCallback` AIDL, `TerminalActions`, `TerminalIds`, `TerminalContract`, `TerminalCapabilityKeys`, `TerminalErrorCodes`: the terminal contract V1) | host build 6.8.0 / 5304, commit `b8f4d6c939` | MPL 2.0 | `98b43a9deb236d642f0d3b18696a4da9a9c4b399374d6e398d80d3abf0c86bc6` |

## Vendored terminal libraries (staged in `libs/jackpal/`, hash-locked in `locks/vendored-aars.lock`)

The terminal emulation, the pty bridge and the native helpers come from jackpal's Android-Terminal-Emulator
(https://github.com/jackpal/Android-Terminal-Emulator, Apache License 2.0, commit
`35188f8a8b57989a4a4ec9485e11187b46be26d9`). The AARs originate from the AutoJs6 host
(`libs/` of the host repository); the host keeps its own copies for the script `shell()` API (roadmap D2).
The plugin's emulatorview AAR carries the P6 bounded-input and shutdown patch and the P8 typeface
patch. Its original AAR, original sources, modified `TermSession` and `PaintRenderer` sources,
deterministic offline rebuild script and SHA-256 provenance are preserved in `vendor/emulatorview/`. All other AAR classes/resources and the other
two libraries retain their original content. The modified source retains the Apache-2.0 license.

| Artifact | Upstream module | Version | Content | SHA-256 |
| --- | --- | --- | --- | --- |
| `term-1_0_70.aar` | `term` (`jackpal.androidterm`) | 1.0.70 | `Exec` JNI declarations (`setPtyWindowSizeInternal`, `setPtyUTF8ModeInternal`), `GenericTermSession`; only the `term-debug` distribution exists upstream | `75d47c8ca207ac8c3afb5820019e5203b361f5977b2631fd1c95ecd9f308dce1` |
| `emulatorview-1_0_42.aar` | `emulatorview` (`jackpal.androidterm.emulatorview`) | 1.0.42-p8.1 | `TermSession`, `EmulatorView`, the VT100 emulator and renderers; bounded main-thread input and complete worker/pty cleanup (P6), a selectable renderer typeface without the removed `android.util.FloatMath` (P8) | `acde6014fb455a214264f79c04c5221292acfbefcf0662be0d71b812fa007f09` |
| `libtermexec-1_0.aar` | `libtermexec` (`jackpal.androidterm.libtermexec`) | 1.0 | `TermExec` (`createSubprocess` / `waitFor` / `sendSignal` JNI), `ITerminal`, and `libjackpal-androidterm5.so` + `libjackpal-termexec2.so` for arm64-v8a, armeabi-v7a, x86_64 and x86, rebuilt with NDK 28.2.13676358 against platform 24 for 16 KB page alignment | `46c6aea86dc45908a3d2555635a6f31d2fcb74c7af01a9b1d80a9f3cb5522fc4` |

`native/jackpal-termexec/` holds the reproducible rebuild recipe for the native libraries (`build.py`,
`CMakeLists.txt`, `upstream.lock.json`, `provenance.json` with per-library SHA-256 and load alignment) together
with the upstream `LICENSE` and `NOTICE`, which are also bundled into the APK under `assets/legal/`.

## Bundled font

| Component | Version | License | Source | SHA-256 |
| --- | --- | --- | --- | --- |
| JetBrains Mono NL Regular (`res/font/jetbrains_mono_nl_regular.ttf`) | 2.304 | SIL Open Font License 1.1 | https://github.com/JetBrains/JetBrainsMono (release `JetBrainsMono-2.304.zip`, entry `fonts/ttf/JetBrainsMonoNL-Regular.ttf`) | `fb3b2575d7b0657359707993288f12a7360344d39387bb26050e276d61f6bd2a` |

The optional terminal font (roadmap P8) is unmodified. Its license text (`OFL.txt`, SHA-256
`30f0c136e3c88e422d0791acd97238870f9054a9729bc34cf2ff0d4ed8cac4ad`) is bundled under `assets/legal/fonts/jetbrains-mono/`
and shown on the About screen; `vendor/fonts/jetbrains-mono/` records the provenance.

## Sources migrated from the AutoJs6 host

The terminal session core, the pty bridge (`jackpal/androidterm/PtyBridge.java`, kept in the `jackpal.androidterm`
package to reach package-private APIs), the terminal screen, the Node.js launcher integration and the related
resources are migrated from the AutoJs6 host repository (https://github.com/SuperMonster003/AutoJs6, MPL 2.0) in
roadmap phases P2 and P3 and keep that license.

## Runtime dependencies (Gradle)

| Component | Coordinates | Version | License | Purpose |
| --- | --- | --- | --- | --- |
| AndroidX AppCompat | `androidx.appcompat:appcompat` | 1.7.1 | Apache License 2.0 | Activity and theme support |
| Material Components for Android | `com.google.android.material:material` | 1.13.0 | Apache License 2.0 | Material 3 widgets and themes |
| Gson | `com.google.code.gson:gson` | 2.13.2 | Apache License 2.0 | JSON documents of the Binder contract |
| Kotlin standard library | `org.jetbrains.kotlin:kotlin-stdlib` | managed by the platform versions plugin | Apache License 2.0 | Language runtime |

Test-only dependencies (JUnit 4, AndroidX Test runner / rules / ext-junit, Apache License 2.0 or EPL 1.0) are
not shipped in the APK.

## Build tooling (not distributed)

Gradle, the Android Gradle Plugin, the Kotlin Gradle Plugin, `io.github.supermonster003.autojs6-platform-versions`,
`io.github.supermonster003.autojs6-native-alignment`, Apache Commons Compress and XZ for Java are used by
`build-logic/` and the Gradle build only. The Android NDK and CMake are used only by `native/jackpal-termexec/build.py`
when the native libraries are rebuilt.
