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
