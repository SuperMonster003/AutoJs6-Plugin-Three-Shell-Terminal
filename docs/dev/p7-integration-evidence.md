# P7 documentation and publication preparation

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
packaged ABI, CRC32 filenames and SHA-256. This candidate is preparation, not a published Release.

## Remaining publication sequence

1. Commit the recovery refinement and acceptance evidence, matching VERSION_BUILD to HEAD.
2. Rebuild/verify the final signed APK set and complete checks on the exact release commit.
3. Push the release source, wait for all GitHub workflows, create `v1.0.0` and the stable Release
   with five APKs plus SHA-256 sums.
4. Add the repository and exact tagged-source/artifact admission record to the official index,
   generate/validate it, push it, wait for index CI and rescan Icon Studio.

The index must not advertise a downloadable stable entry before its real Release exists.
