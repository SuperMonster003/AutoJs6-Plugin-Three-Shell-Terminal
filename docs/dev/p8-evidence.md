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
