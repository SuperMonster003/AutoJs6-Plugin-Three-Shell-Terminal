# P8 evidence (1.1.0 candidates)

Recorded from 2026-10-07. Each section names the change, its tests and the devices that ran them.

## Trust this signer (roadmap P8, D34 Q3)

- Rule: `NodeCliTrust.evaluate` keeps the D17 order (official key, then this plugin's key) and only then
  accepts a user confirmation. A confirmation covers one package and exactly one set of current signers.
  A rotated key, a lineage entry, a partial multi-signer set or another package is not covered.
- Storage: `node_trusted_signers` in the terminal preferences holds `package|sha256[,sha256]` lines, oldest
  first, at most 8; malformed lines are ignored. Trusting or revoking clears the Node CLI resolution cache,
  so the next session plan and the Binder environment document use the new verdict at once. Running shells
  keep the environment they started with.
- Flow: the terminal banner for an untrusted signer opens the environment probe screen. The probe report
  lists every digest seen and, when it differs, the current signers. "Trust this signer" opens a
  destructive-styled confirmation that names the package, the exact current digests and the permissions the
  commands would run with; nothing is stored before its positive button. The settings row "Trusted Node.js
  signers" lists the confirmed packages and revokes one after choose-then-confirm. A trusted launcher reports
  `Trust: confirmed in the settings (...)` in the probe report; the contract state stays `available`.

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `node/NodeCliTrustTest` (+4: exact current set, lineage not covered, precedence, order-independent multi-signer, record validation) and `core/TerminalPreferencesTest` (+1: order, bound, malformed values) | 186 passed |
| DEVICE | `ui/settings/SettingsActivityInstrumentationTest`, API 37 x86_64 AVD and Redmi 22120RN86C API 33 | 12 / 12 each (the `ui.settings` package) |
| DEVICE | `node` package on Redmi API 33 with the official Node.js Runtime 1.5.6 | 5 passed, 2 explicit skips; the official signer still resolves as available |

The device test injects the probe screen's resolution, because no third-party signed Node.js Runtime exists
on the test devices. The trust rule itself runs through the JVM table.

## Command completion notices (roadmap P8)

- Scope: a session the registry created with a command and `keepOpen: false` (a script's `exec`, the Binder
  `openSession` with that flag). Interactive shells never notify.
- Capture: such a session sets `keepFinalTranscript`; `TerminalPtySession.finish` keeps the last 64 KiB of the
  transcript before the emulator releases it. The notice waits up to 2 seconds for that finish (a grandchild can hold
  the pty open), then reads the live tail. The terminal's own "[Process completed (code N)]" line is dropped from the
  kept output because the result screen shows the exit code, and because the reaper can append that line before
  the last output was drained (see the follow-up below).
- Notice: channel `three.shell.terminal.commands` (default importance). The title is "Command finished" or "Command
  failed (exit code N)" and the text names only the session number and directory. Notices share one group whose
  summary alerts once, so a script running many commands does not ring for each. No notice when the switch is off,
  the notification permission is missing, `TerminalSessionManager.close` ended the session, or the resumed terminal
  shows that session.
- Result screen: `CommandResultActivity` (not exported, own task affinity) shows the exit code, directory and kept
  output, with copy / share (the same 100000-character bound as the terminal) and "Open terminal here". Results live
  in memory only (`FinishedCommands`, at most 8); after a process restart the screen says the output is gone.

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `core/CommandNoticesTest` (decision table, id range and summary id, bounded store, completion line removal), `ManifestContractTest`, `TerminalPreferencesTest` | 190 passed |
| DEVICE | `service/CommandNotificationsInstrumentationTest` on API 37 x86_64 AVD, Redmi 22120RN86C API 33, Sony G8441 API 28 | 2 / 2 each |
| DEVICE | `binder` package on the API 37 AVD; `SettingsActivityInstrumentationTest` (16 rows) on API 37 and API 33 | 18 / 18; 7 / 7 each |
| DEVICE | real notice and result screen on the API 37 AVD (screenshots reviewed, not committed) | title "Command failed (exit code 2)", text "Session 1 in ~", output and three actions shown |

Follow-up observed: when a non-interactive command exits right after printing, the reaper appends
"[Process completed (code N)]" before the reader has drained the final output, so the terminal screen and
`readTranscript` can show that line above the last output lines. This predates P8; the pty lifecycle was left
unchanged here because P6 stabilized it under load.

## Terminal color schemes and font (roadmap P8)

- Color schemes: `terminal_color_scheme` is `follow` (default, the neutral pair of the current light / dark
  appearance), `dark`, `light` (the neutral pair of the other mode) or `theme` (HCT tones 10 / 92 in dark and
  96 / 14 in light of the theme hue at chroma 16, neutral for gray seeds). The palette carries its scheme; a
  screen resumes, sees a different stored scheme and recreates itself, and the terminal reattaches the same
  session. Because no single accent can be readable on both a light surface and a dark terminal (or the
  reverse), the accent is corrected against the terminal background only when both are on the same side.
- Font: `terminal_font` is `system` (default, `Typeface.MONOSPACE`) or `jetbrains-mono` (bundled JetBrains Mono
  NL 2.304, SIL OFL 1.1, the variant without ligatures). The emulatorview patch `1.0.42-p8.1` gives
  `PaintRenderer` a default typeface that the plugin sets through the same-package `TerminalTypeface`; each view
  remembers the face its renderer was built with and rebuilds it on resume when the face changed. The selection
  overlay and the command output screen draw with the same face, so the overlay still wraps like the emulator.
  The same patch replaces `android.util.FloatMath.ceil` (removed from the SDK in API 23) with `Math.ceil`.
  Rebuilding the AAR with the recorded JDK 21.0.12.1 and android-37.0 jar changes only `PaintRenderer.class`.
- Size: the signed release universal APK is 2037690 bytes against 1882323 bytes for v1.0.0 (+8.3 percent, all of
  P8 including the font), below the 10 percent investigation threshold.

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM | `ui/TerminalPaletteTest` (theme pair >= 7:1 for ten seeds in both modes, gray seed neutral, always-dark / always-light keep the accent readable on the surfaces), `BundledFontTest`, `TerminalPreferencesTest`, `ReleaseHistoryTest` | 192 passed |
| BUILD | `vendor/emulatorview/build.py --check` with JDK 21.0.12.1 and the recorded android.jar; old and new AAR compared class by class | reproducible; only `PaintRenderer.class` differs |
| DEVICE | `ui/TerminalAppearanceInstrumentationTest` on the API 37 x86_64 AVD and Redmi 22120RN86C API 33 | 3 / 3 each (same shell pid across three recreations; the font face swapped; settings dialogs write both choices) |
| DEVICE | signed release x86_64 APK on the API 37 AVD, choices made through the settings page | theme-tinted background and JetBrains Mono shown after returning; R8 keeps the patch working |
| DEVICE | screenshots on the API 37 AVD (reviewed, not committed) | `->`, `!=`, `=>` stay separate cells, box drawing renders, CJK falls back as wide cells |

## Complete suite at e85e2f9 (build 43)

The whole instrumentation suite ran on the final P8 source, debug APKs rebuilt at that commit:

| Device | Result |
| --- | --- |
| API 37 x86_64 AVD, 16 KiB pages | OK (87 tests): 84 passed, 3 explicit skips |
| Redmi 22120RN86C, API 33, arm64-v8a, Node.js Runtime installed | OK (87 tests): 84 passed, 3 explicit skips |

The skips are the no-runtime branch (both devices have a Node.js Runtime), the coordinated Node.js lifecycle
case that needs a disposable device, and the API 24-only split-screen case. v1.0.0 had 78 cases; P8 and the
review added the parity, trust, command notice and appearance cases.

## Explorer Action directory action (roadmap P8.1, appendix D Q10 option B)

- Host (AutoJs6 commit `9fe2c84202`, build 5320): Explorer Action protocol v23 adds the optional catalog flag
  `filesystemPath`. The host accepts it only for a single-directory, read-only, overflow Activity action without
  output, matchers, probes or session capabilities, and rejects the whole action otherwise. For an accepted action
  the single target Bundle carries the canonical directory path (at most 4096 characters) and no host file session
  is created. The path grants nothing. The hard-coded "Open in terminal" directory menu item is removed; the drawer
  item and the project toolbar keep `TerminalLauncher`. Protocol document: host `docs/dev/explorer-action-protocol-v23.md`.
- Plugin: `ThreeShellTerminalExplorerActionService` answers the Explorer Action identity (engine `explorer-action`,
  capabilities `requiresHostVersion` 5320 and protocol 23 only, without computing the Node.js state) and a catalog
  with `three-shell-terminal.open-directory`. `ThreeShellTerminalExplorerActivity` is exported behind the PLUGIN
  permission, checks the caller it can name, accepts only the execute action, this action id, protocol 23, exactly
  one directory target and an absolute normalized path within both ceilings, keeps the path out of the log, and
  opens the terminal like the former host item: the most recent session changes into the directory, or a new
  session starts there. Without storage access the existing fallback to `HOME` with the storage banner applies.
- Compatibility: hosts 5304-5319 advertise at most protocol 22, never list the action and keep their own item, so
  the menu never shows two entries. The plugin center keeps a single entry (the INFO identity).

| Evidence | Configuration | Result |
| --- | --- | --- |
| JVM (host) | `ExplorerActionCatalogPolicyTest` v23 table (accepted shape, protocol gate, file / mixed / multiple targets, write access, primary placement, host presentation and matchers rejected), protocol version frozen at 23 | 114 explorer tests passed |
| JVM (plugin) | `explorer/TerminalExplorerActionTest`, `ManifestContractTest`, `VendoredAarLockTest` | 199 passed |
| BUILD | debug, androidTest, release (R8, resource shrinking), lintDebug and lintRelease (0 errors), 16 KB alignment; `aapt2` shows the kept title string and both components in the release APK | passed |
| DEVICE | `explorer` package on the API 37 x86_64 AVD, Redmi 22120RN86C API 33 and Sony G8441 API 28 | 4 / 4 each |
| DEVICE | host 5320 debug with plugin debug and signed release on the API 37 AVD: directory menu, "Open in terminal" | terminal opens in `/storage/emulated/0/Scripts/<dir>` in its own task; Back returns to the file manager |
| DEVICE | host 5319 with plugin 1.1.0 on the Sony G8441 API 28 | only the host's own "Open in terminal" item |
| DEVICE | whole suite on the API 37 AVD | OK (91 tests): 88 passed, 3 existing explicit skips |

- Size: keeping the whole Explorer Action module gave a 2064230-byte universal release APK (+9.7 percent against
  1882323 bytes for v1.0.0). Only the discovery AIDL is kept now, which gives 2047846 bytes (+8.8 percent); the
  signed release was verified again on the device.
- Device note: during the whole-suite run the host stayed on its Plugins tab and an injected test tap switched
  3-Shell Terminal off in the Plugin Center, which hid the action in later checks. It was switched back on through
  the host UI, and device runs now start from the home screen.
