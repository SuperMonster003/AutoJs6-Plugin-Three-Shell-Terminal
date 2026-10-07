# JetBrains Mono NL (bundled terminal font)

The optional terminal font of roadmap P8. `provenance.json` records the release archive, its SHA-256,
the entry copied into `app/src/main/res/font/jetbrains_mono_nl_regular.ttf` and the digest of both files.
`OFL.txt` is the license from the same archive; the `bundleLegalAssets` Gradle task packages it so the
About screen can show it offline.

- Version 2.304 (released 2023-01-14), SIL Open Font License 1.1, unmodified.
- The NL variant has no programming ligatures. A terminal draws every character in its own cell, and
  ligatures such as `->` would merge glyphs across cells.
- The release asset has no published digest. The archive size matches the GitHub release metadata, and
  both copied files are byte-identical to the files at tag `v2.304` in the upstream repository.
- Glyphs the font does not have (for example CJK) fall back to the system fonts.

Update by replacing both files from a new release, then update `provenance.json`,
`THIRD_PARTY_NOTICES.md` and the `dependency` changelog entry together. `BundledFontTest` checks the
recorded digests.
