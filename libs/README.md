# Staged AAR artifacts

This directory contains the exact, hash-locked binary artifacts consumed by the plugin. Gradle never
resolves them from sibling repositories or from `mavenLocal()`.

## Host protocol AARs (`libs/`, lock `../locks/host-api-aars.lock`)

Stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants)
- `nodejs-api.aar` (host module `plugin-api/nodejs-api`: `NodeJsPluginActions`, `NodeJsPluginCapabilityKeys`, the `NODE_CLI_*` manifest contract read from the Node.js Runtime plugin)
- `terminal-api.aar` (host module `plugin-api/terminal-api`: `ITerminalPlugin` / `ITerminalCallback` AIDL and the `Terminal*` constants of the terminal contract V1; the plugin identity constants are read from it)

All three must come from the same host contract line. Current provenance: `common-plugin-api.aar` and
`nodejs-api.aar` are the release AARs assembled from AutoJs6 6.8.0 / 5303 (host commit `9545a7f4aa`,
2026-10-01); `terminal-api.aar` is the release AAR of host commit `b8f4d6c939` (6.8.0 / 5304, the commit that
added the module, plugin roadmap P1.1; the host build 5304 recorded as `requiresHostVersion` was confirmed in
P1.4 together with the host protocol document `docs/dev/terminal-plugin-protocol-v1.md`). `common-plugin-api.aar` is byte-identical to the artifact staged by the
other official plugins (module unchanged since host commit `9c3ba2e520`).

`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock entries and
digest mismatches during configuration. Do not commit locally assembled debug AARs or rename debug outputs to
bypass this policy.

## Vendored terminal AARs (`libs/jackpal/`, lock `../locks/vendored-aars.lock`)

- `term-1_0_70.aar` (jackpal `term` 1.0.70; upstream only publishes the `term-debug` distribution, so the debug check is not applied to this set)
- `emulatorview-1_0_42.aar` (jackpal `emulatorview` 1.0.42)
- `libtermexec-1_0.aar` (jackpal `libtermexec` 1.0 with the 16 KB aligned native libraries; its SHA-256 must equal `aarSha256` in `../native/jackpal-termexec/provenance.json`)

The files originate from the AutoJs6 host `libs/` directory (roadmap D13). Since P6, emulatorview carries
the bounded I/O and shutdown patch `1.0.42-p6.1`; its original input, patched source, offline rebuild script
and exact provenance are in `../vendor/emulatorview/`. The other two AARs are unchanged. Rebuilding the native
libraries is done with `../native/jackpal-termexec/build.py`, which writes a fresh AAR plus provenance; update the
lock, the provenance and `../THIRD_PARTY_NOTICES.md` together.

Record the lowercase SHA-256 of every staged artifact in the matching lock file. Licenses are listed in
`../THIRD_PARTY_NOTICES.md`.
