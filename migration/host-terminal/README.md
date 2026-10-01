# Host terminal snapshot (roadmap P1.3)

Verbatim copy of the AutoJs6 host terminal taken at host commit `1dcb393104` right before
roadmap P1.3 deleted it from the host (2026-10-01). It exists so that P2.1 / P3.1 / P5.1 can port
the code without digging through host history; files are removed from here as they are ported and
the directory is deleted once P3 closes. Nothing in here is compiled, tested or scanned.

| Directory | Host origin | Ported by |
| --- | --- | --- |
| `src/core/terminal/` (17) | `app/src/main/java/org/autojs/autojs/core/terminal/` | P2.1 ported all 17 (removed from here): `core/` 9, `node/` 7, `service/` 1 (`TerminalSessionService` became `ThreeShellTerminalSessionService` + `SessionNotifications`); P2.2 (storage), P2.3 (`NodeCliTrust`) and P2.5 build on them |
| `src/ui/terminal/` (7) | `app/src/main/java/org/autojs/autojs/ui/terminal/` | P2.5 extracted the non-UI npm actions into `core/TerminalNpmActions`; P3.1 ported `TerminalActivity`, `TerminalEmulatorView`, `TerminalToolbarView`, `TerminalTextSelection`, `TerminalSettingsDialogs` and the dialog shell of `TerminalNpmActions` (now `ui/TerminalNpmDialogs`) onto the plugin's own `UiKit` / `TerminalPalette` instead of the host's MaterialDialog and utilities (removed from here); `TerminalManagerDialog.kt` stays for P3.2 |
| `src/ui/settings/` (3) | `app/src/main/java/org/autojs/autojs/ui/settings/Terminal*Preference.kt` | P2.5 extracted registry / ignore-scripts / clear-data logic into `core/TerminalSettingsActions`; preference widgets remain for P5.1 |
| `src/jackpal/androidterm/` (2) | `app/src/main/java/jackpal/androidterm/` | `PtyBridge.java` already lived in the plugin (P0.2); `TerminalSelectionSnapshot.java` ported in P2.1; both removed from here |
| `test/core/terminal/` (10) | `app/src/test/java/org/autojs/autojs/core/terminal/` | P2.1 ported all 10 (removed from here): `core/` 6, `node/` 4; package names changed, resource names untouched |
| `androidTest/core/terminal/` (1) | `app/src/androidTest/java/org/autojs/autojs/core/terminal/` | the Activity-free cases were recreated in P2.1 as `core/TerminalSessionsInstrumentationTest`; P3.1 ported `switchingSessionsPreservesShellStateAndUnsubmittedInput` and the subtitle / clipboard part of the manager case into `ui/TerminalActivityInstrumentationTest`; the manager case itself stays here for P3.2 |
| `res/layout/`, `res/menu/`, `res/values/ids_terminal.xml` | `app/src/main/res/` | P3.1 (removed from here): `activity_terminal.xml` (edge-to-edge spacer, Material toolbar, two banner includes), `include_terminal_node_banner.xml` -> `include_terminal_banner.xml` (shared by the Node.js and storage banners), `menu_terminal.xml` (manager item deferred to P3.2, settings / about items to P5.1), `ids.xml` |
| `res/values*/strings_terminal.xml` (11 locales) | every `*terminal*` entry of the host `strings.xml`, including the five the host keeps (D27) and the P1.2 plugin-state strings | P2.1 (`text_terminal_notification_*` x 5 -> `notification_*`); P3.1 ported 41 more per locale (`text_terminal_*` -> `terminal_*`, `text_terminal` -> `terminal_title`, `entry_terminal_npm_registry_*` -> `terminal_npm_registry_*`, `hint_terminal_npm_registry_custom_url` -> `terminal_npm_registry_custom_url_hint`; `terminal_tips_content` reworded for the plugin's own uid) and added 20 plugin-only keys; the 25 remaining per locale are the host-side plugin-state prompts (D27, stay in the host), the manager strings (P3.2) and the settings summaries / clear-data strings (P5.1) |
| `res/values/strings_donottranslate_terminal.xml`, `arrays_terminal.xml` | host preference keys and npm registry arrays | P5.1 |
| `res/xml/preferences_terminal_fragments.xml` | the host settings category and developer option (D8) | P5.1 |
| `AndroidManifest_terminal.xml` | the two host components | P2.1 (`ThreeShellTerminalSessionService`, D15) / P3.1 (`TerminalActivity`, own task affinity, not exported); both done, file removed |

String entry counts per locale: values 71, values-en 71, values-zh 71, values-zh-rHK 71, values-zh-rTW 71, values-ja 71, values-ko 71, values-fr 71, values-es 71, values-ru 71, values-ar 71.
