# P7 integration and v1.0.0 publication

Updated 2026-10-07. Publication of the plugin, its tag and official index entry is authorized
by the maintainer. P6 device acceptance is complete; the signed candidate matrix and final
process-restoration checks are recorded in the three P6 evidence documents.

## Related repositories

| Repository | Completed work / evidence |
| --- | --- |
| AutoJs6-Documentation | `614b49f` adds the four Terminal pages, navigation and the reciprocal shell() explanation. Current full generation/freshness check passes for 156 modules |
| AutoJs6-TypeScript-Declarations | `24b44eb` adds terminal/$terminal, module/session/options/error/event types and consumer tests. The 4.32.0 changelog includes Terminal; both TypeScript 5.1.3 and 6.0.3 pass the strict corpus with 12 expected invalid calls |
| AutoJs6-Plugin-Ace-Editor | `c5d77a5` imports declarations 4.32.0, including Terminal, in the 1.25.0 development line. `generateAutoJs6LspDeclarations` and `verifyAutoJs6LspRuntime` pass. Other editor work continues independently |
| AutoJs6-Plugin-Offline-Docs | `1167c9e` bundles the Terminal references with the current documentation snapshot. Comparison of all 212 source-site files finds no difference after line-ending normalization; the 156-page normalization check is a no-op |
| AutoJs6-Plugin-NodeJs-Runtime | `f56fc4ba` updates the current terminal consumer, UID, private layout, signer trust and Android npm restrictions. The manifest schema remains 1. Markdown and the locked CLI archive checks pass |
| AutoJs6 host | `11780d593e` records the release matrix test and final protocol references in an isolated local branch; the same files also reached the main checkout through merge `2e14309df7`. The harness passed 2/2 on Xiaomi API 35. Current 6.8.0 changelogs cover plugin migration, entry points and script APIs in all ten languages |

The host build/check used an isolated checkout based on `9c8c736271` because other work was
editing and compiling the main workspace. No unrelated edits or merge conflicts were reset.
Host history was not pushed as part of this plugin publication task.

## Plugin documents and artifacts

The ten README and changelog sources now describe the 1.0.0 feature set, compatible host
builds, Node.js requirements, storage permissions and OEM foreground restrictions. Beta
publication wording from the abandoned early preparation was removed. All 36 generated
documentation artifacts and all 22 Icon Studio resources pass their reproduction checks.

The patched emulatorview recipe preserves the original AAR/source and emits canonical ZIP
headers on Windows and Linux. Its exact compiler and Android stub digest are in provenance;
the rebuild check passes with those recorded inputs.

Signed candidate collection produces four ABI APKs and a universal APK. The local verifier
checks official signer, package identity, version code/name, exactly two native libraries per
packaged ABI, CRC32 filenames and SHA-256. The final build 35 set was published after the complete acceptance matrix passed.

## Published release and official index

- [v1.0.0 stable Release](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases/tag/v1.0.0): build 35, tag/source
  `7af6fc27984546f0b8032af03c5254a77558c769`, five signed APKs plus `SHA256SUMS`.
  GitHub upload digests and byte counts match the exact files installed in the final device matrix.
- [Release-source CI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/actions/runs/37589278646)
  passed JVM/build/lint/alignment and complete API 24/API 35 suites. Its first API 24 attempt
  stopped making progress after 65/78 tests (7 skipped, no reported assertion failure), was
  cancelled and retained for diagnosis. A full rerun of the same commit passed; no tests were
  removed or reclassified. [Markdown CI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/actions/runs/37589278620)
  also passed. The tag-triggered [Build integrity](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/actions/runs/37592693346), Markdown, Icon geometry and Icon Studio workflows all passed as well.
- Official index publication: commit `5de2fcd91bc47fce938d90e27d955468df0ba7ed`,
  [index CI](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/actions/runs/37595512640) passed. Inventory contains 46 official projects.
  `release-manifests/io.github.supermonster003.autojs6.plugin.three.shell.terminal/35.json`
  binds the tag, signer and all five artifact hashes. The live entry exposes stable 1.0.0 / 35,
  minimum host build 5304, four ABI choices plus universal and 16384-byte native-page metadata.
- The published index and installed plugin use identical day/night catalog artwork and fixed
  #FAFAFA / #212121 surfaces. Icon Studio's catalog scan recognizes the project as published.

The final local gates passed: 176 JVM tests, Temurin platform selection, debug/androidTest and
signed release assembly, debug/release lint, native alignment, 36 generated documents and
22 Icon Studio resources. Each of the 13 final device installations was hash-matched to its
Release asset; the controlled process-termination check also passed against the signed APK.

This receipt is a documentation-only follow-up to the published tag. Its required commit-count
increment does not replace the immutable build 35 APKs or claim an additional application release.
The related host and editor tasks retain their own workspaces and publication scopes.
