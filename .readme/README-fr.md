<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-shell-terminal-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Terminal multi-session pour AutoJs6 et ses scripts, exécutant le shell système dans un pty avec sessions en arrière-plan, barre de touches et commandes Node.js</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Shell Terminal reprend le terminal intégré d'AutoJs6 : l'interrupteur "Terminal" du tiroir d'accueil, "Ouvrir dans le terminal" dans le menu des dossiers du gestionnaire de fichiers et la barre d'outils du projet, ainsi que l'objet global `terminal` côté script pour ouvrir, piloter et observer des sessions. Chaque session est un shell système (`/system/bin/sh`) exécuté dans un pty, qui continue en arrière-plan quand on quitte l'écran.

AutoJs6 découvre le plugin via son service Binder, ouvre l'écran du terminal par un Intent explicite et utilise le Binder pour lire le nombre de sessions, fermer toutes les sessions ou piloter les sessions de script ; la sortie revient aux scripts par un tube. Quand le plugin Node.js Runtime est installé, le terminal lit directement son contrat de manifeste, vérifie la signature et le lanceur, puis fournit node / npm / npx / corepack / yarn / pnpm.

******

### État

******

Aperçu de développement P2: sessions shell, accès au stockage, intégration Node.js avec vérification des signatures et contrôle des sessions depuis l'hôte sont implémentés. L'écran du terminal, l'API de scripts et les réglages suivront les étapes de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). AutoJs6 6.8.0 (build 5304+).

******

### Fonctionnalités

******

Le plugin fournit les capacités suivantes:

- Sessions multiples : création, bascule, fermeture et gestionnaire de sessions ; un service au premier plan maintient les sessions après avoir quitté l'écran, et sa notification affiche le dossier courant et le nombre de sessions avec une action "Fermer les sessions".
- Écran du terminal : barre de touches sur deux lignes (Esc / Tab / Ctrl / flèches / symboles courants), sélection de texte native avec Copier / Tout sélectionner, copie et partage de la transcription, taille du texte, coller et effacer.
- Chaîne Node.js : avec le plugin Node.js Runtime (1.5.0+), node / npm / npx / corepack / yarn / pnpm deviennent disponibles, avec les réglages de registre npm et "ignorer les scripts d'installation" et le menu de paquets (npm init / install / run script, etc.).
- Entrées AutoJs6 : l'interrupteur du tiroir d'accueil (nombre de sessions, tout fermer), "Ouvrir dans le terminal" dans le menu des dossiers du gestionnaire de fichiers et la barre d'outils du projet.
- API de script `terminal` (alias `$terminal`) : gestion des sessions, exécution visible (`exec`, `npm.run`) et objet session avec événements `output` / `exit`, `write` et `waitFor` ; chaque échec est une `TerminalError` au `code` stable.
- Application autonome : l'icône du lanceur ouvre directement le terminal ; page de réglages (apparence suivant AutoJs6, taille du texte, registre npm, intégration Node.js, accès à tous les fichiers, effacer les données du terminal), À propos et historique des versions.

******

### Utilisation

******

1. Installez l'APK du plugin correspondant à l'ABI de l'appareil (ou l'APK universel) depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) sur un appareil avec AutoJs6 build 5304 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `3-Shell Terminal` est reconnu et activez-le.
3. Activez "Terminal" dans le tiroir d'accueil d'AutoJs6, choisissez "Ouvrir dans le terminal" sur un dossier du gestionnaire de fichiers, ou appelez `terminal.open(...)` depuis un script. Accordez "Accès à tous les fichiers" quand le plugin le demande pour entrer dans les dossiers du stockage partagé comme `/sdcard`.

******

### Commandes Node.js

******

Comment le terminal obtient node / npm et quelles sont les limites:

- Nécessite le plugin Node.js Runtime 1.5.0 ou plus récent ; le plugin lit son contrat de manifeste, vérifie la signature, le lanceur et l'archive npm / corepack, puis lie les commandes dans `PATH` à chaque démarrage de session. Sans le plugin, ou si la vérification échoue, le terminal reste utilisable sans ces commandes.
- Android interdit l'exécution de fichiers écrits par l'application. npm désactive les liens bin par défaut: `node_modules/.bin/*` et `npx <paquet>` ne peuvent donc pas lancer directement les points d'entrée. Utilisez `node node_modules/<paquet>/<entrée>.js`. Les exécutables natifs des paquets npm échouent avec `EACCES`, et les extensions natives (`.node`) ne peuvent pas être chargées.
- corepack utilise par défaut ses pnpm 11.x et Yarn 1.x intégrés (`COREPACK_DEFAULT_TO_LATEST=0`) et télécharge une version nommée explicitement sur demande ; le registre npm peut être basculé vers npmmirror ou une URL https personnalisée dans les réglages.

******

### Demarrage rapide

******

Un script qui ouvre le dossier du script, installe visiblement les dépendances en attendant le résultat, et pilote une commande interactive (disponible à partir de la phase P4):

```js
// Open the script directory in the terminal; the screen comes to the front and the session keeps running in the background.
let session = terminal.open(files.cwd());
console.log(session.id, terminal.sessions().length);

// Visible execution: install dependencies in a session the user can watch and wait for the exit code (0 = no timeout).
let install = terminal.exec('npm install', { cwd: '/sdcard/Scripts/my-project', wait: true, timeout: 0 });
toastLog('npm install exited with ' + install.exitCode);

// Drive an interactive command: output / exit events, write and waitFor; every failure is a TerminalError with a stable code.
let init = terminal.exec('npm init', { cwd: files.cwd(), show: true });
init.on('output', line => { if (/package name/i.test(line)) init.write('\n'); });
init.waitFor(/Is this OK\?/i, 60e3);
init.write('yes\n');
init.on('exit', code => console.log('npm init exited with ' + code));
terminal.npm.run('build', files.cwd());
```

******

### Compatibilité

******

Faits de plateforme qui délimitent ce que le plugin peut faire:

- Android 7.0 (API 24) et plus récent ; APK pour arm64-v8a, armeabi-v7a, x86_64 et x86 plus un APK universel, bibliothèques natives alignées sur des pages de 16 Ko ; la build hôte et le plugin sont vérifiés ensemble sur la matrice d'appareils de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md).
- Les processus du terminal s'exécutent sous l'uid et les permissions du plugin et n'héritent pas de celles d'AutoJs6 ; utilisez l'API de script `shell()` pour les commandes qui ont besoin des permissions d'AutoJs6.
- Les sessions vivent tant que le processus du plugin existe ; une fois le processus terminé par le système, elles ne peuvent pas être restaurées, ce que le service au premier plan et sa notification rendent improbable.

******

### Questions fréquentes

******

- **Pourquoi `cd /sdcard/Scripts` échoue-t-il ?** Le plugin a besoin de sa propre autorisation de stockage. Ouvrez les réglages du plugin ou suivez la bannière du terminal pour accorder "Accès à tous les fichiers" (Android 11+), ou la permission de stockage sur les systèmes plus anciens.
- **Pourquoi n'y a-t-il pas de commande node ?** Installez le plugin Node.js Runtime (1.5.0+) depuis le centre de plugins d'AutoJs6 ; la "sonde d'environnement" des réglages du plugin indique la raison exacte (absent, trop ancien, signature non approuvée ou lanceur non exécutable).
- **Une commande continue-t-elle après avoir quitté le terminal ?** Oui. Un service au premier plan conserve la session et sa notification affiche le nombre de sessions ; seuls "Fermer les sessions" dans la notification, l'interrupteur du tiroir ou la session elle-même terminent le shell.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Le service Binder et l'entrée d'écran sont protégés par la permission de signature `org.autojs.permission.PLUGIN` et vérifient la signature de l'appelant, donc seul AutoJs6 peut les atteindre ; l'entrée du lanceur ouvre seulement le terminal et n'accepte aucune commande externe.
- La permission de stockage ("Accès à tous les fichiers" sur Android 11+) sert uniquement à entrer dans les dossiers que vous choisissez ; le terminal n'analyse ni n'envoie jamais de fichiers.
- La permission `INTERNET` est utilisée par les commandes que vous exécutez dans le shell (par exemple `npm install`) et par la vérification manuelle des versions via l'API GitHub Releases fixe de ce plugin ; le plugin lui-même ne se connecte jamais en arrière-plan.
- Le lanceur du plugin Node.js Runtime n'est exécuté que si sa signature est l'officielle (ou correspond à ce plugin) ; le plugin ne journalise pas les entrées / sorties des sessions et exclut son stockage privé des sauvegardes.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

```text
application id: io.github.supermonster003.autojs6.plugin.three.shell.terminal
plugin id: three-shell-terminal
engine: terminal
variant: default
service action: org.autojs.plugin.TERMINAL
service category: terminal
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.terminal.api.ITerminalPlugin
minimum host build: 5304 (6.8.0)
```

`ThreeShellTerminalPluginService` répond à `org.autojs.plugin.TERMINAL` (category `terminal`) et implémente le contrat terminal-api de l'hôte `org.autojs.plugin.terminal.api.ITerminalPlugin` à partir de la phase P2. `ThreeShellTerminalPluginInfoService` répond à `org.autojs.plugin.INFO` avec PluginInfo. `WakeActivity` permet à l'hôte d'activer le plugin ; l'écran du terminal s'ouvre via `org.autojs.plugin.TERMINAL_OPEN`.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/10/02_

- `Note` Aperçu de développement P2: sessions shell, accès au stockage, intégration Node.js avec vérification des signatures et contrôle des sessions depuis l'hôte sont implémentés. L'écran du terminal, l'API de scripts et les réglages suivront les étapes de ROADMAP.md.
- `Fonctionnalité` Identité du plugin `three-shell-terminal` (engine `terminal`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.TERMINAL` pour la découverte par l'hôte
- `Fonctionnalité` APK séparés par ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus un APK universel, bibliothèques natives alignées sur des pages de 16 Ko
- `Fonctionnalité` README, notice du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` Noyau de session porté depuis le terminal de l'hôte: sessions shell sur pty avec un registre au niveau du processus (titre et code de sortie enregistrés pour le Binder), environnement de session et arborescence sous le répertoire de fichiers du plugin, détection du lanceur Node.js avec l'installateur npm / corepack, et service de premier plan qui maintient les sessions avec une notification "Fermer les sessions" (canal `three.shell.terminal.sessions`)
- `Fonctionnalité` Résolution de l'accès au stockage (`StorageAccess`): état des permissions propre au plugin (permissions d'exécution héritées sous API 30, "Accès à tous les fichiers" à partir d'API 30), détection du stockage partagé pour `/sdcard`, `/storage/...` et des dossiers `Android/{data,obb,media}` du plugin, repli du répertoire de départ vers `$HOME` avec `STORAGE_PERMISSION_REQUIRED` ou `DIRECTORY_INACCESSIBLE`, et les Intents de réglages qui ouvrent l'accès à tous les fichiers
- `Fonctionnalité` Intégration Node.js avec confiance du signataire (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): le plugin Node.js Runtime n'est utilisé que s'il est signé par la clé officielle des plugins AutoJs6 ou par la clé propre de ce plugin, l'interrupteur des réglages court-circuite avant toute recherche, chaque résultat est associé aux états `node-cli` du contrat (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), et chaque démarrage de session renouvelle les liens de commandes de `usr/bin`, extrait l'archive npm / corepack une seule fois par empreinte et exporte l'environnement npm / corepack
- `Fonctionnalité` AutoJs6 peut créer et contrôler jusqu'à 16 sessions de terminal, recevoir la sortie en direct avec 4 écouteurs par session, lire la sortie récente et consulter l'environnement shell. Les sessions continuent après la fermeture d'AutoJs6; les demandes invalides sont refusées avec un motif précis.
- `Fonctionnalité` La gestion des paquets prend en charge npm init, l'installation de dépendances et de paquets, la liste et l'exécution des scripts package.json, les commandes Yarn / pnpm et la recherche npm. Les registres npmjs, npmmirror et HTTPS personnalisés sont disponibles, avec une option pour ignorer les scripts d'installation. L'effacement ferme toutes les sessions avant de réinitialiser home / usr et conserve les réglages et projets externes. Les menus et l'interface de réglages suivront.
- `Fonctionnalité` Ecran de terminal (`TerminalActivity`) : l'interface du terminal de l'hote portee sur le theme Material 3 propre au plugin, avec la barre de touches (Echap / Tab / Ctrl / Alt / fleches / pagination), la taille du texte par pincement, la selection de texte par appui long avec copie, les menus sessions / texte / gestion des paquets / parametres / aide, un sous-titre de barre d'outils qui affiche le repertoire du shell et le copie au toucher, et une banniere Node.js qui explique un Node.js Runtime absent, non approuve, obsolete ou desactive avec les actions installer / mettre a jour / activer / details ; une banniere de stockage apparait lorsqu'un repertoire du stockage partage ne peut pas etre ouvert et propose "Accorder" et "Rentrer dans le repertoire" ; l'ecran suit la langue, le mode nuit et la couleur de theme de l'hote via le fournisseur de parametres de l'hote et revient aux valeurs du systeme avec la couleur commune `#FFDEAD`
- `Fonctionnalité` Gestionnaire de sessions : une boite de dialogue avec des sections repliables etat / commandes / sessions / parametres qui liste chaque session en cours avec son repertoire, son PID et sa duree, ouvre ou ferme une session, en demarre une nouvelle, ferme tout, affiche les details d'une session avec une action de copie et propose les reglages de taille du texte, de registre npm et d'ignore-scripts ; il suit le meme registre de sessions que `onSessionsChanged` de l'hote, s'ouvre depuis le menu du terminal, depuis la notification de session (appui) et, pour l'entree `manager=true` de l'hote, via une `TerminalManagerActivity` transparente qui ne laisse aucun terminal derriere elle une fois fermee
- `Correctif` Les restrictions système sur l'activité en arrière-plan ne font plus planter le plugin au démarrage d'une session. La session continue sans la protection du service de premier plan.
- `Correctif` La lecture de longs historiques conserve le texte le plus récent sans dépasser la limite de taille des réponses entre processus.
- `Dépendance` Ajout de jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) pour l'émulation de terminal et les bibliothèques natives pty, verrouillé par hachage dans `locks/vendored-aars.lock`
- `Dépendance` Ajout de `common-plugin-api.aar` et `nodejs-api.aar` (modules AutoJs6 `plugin-api/common-plugin-api` et `plugin-api/nodejs-api`, build hôte 6.8.0 / 5303, MPL 2.0) comme contrat de plugin partagé et contrat de manifeste Node.js, verrouillés par hachage dans `locks/host-api-aars.lock`
- `Dépendance` Ajout de `terminal-api.aar` (module AutoJs6 `plugin-api/terminal-api`, build hôte 6.8.0 / 5304, MPL 2.0) comme contrat de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identité, plafonds et codes d'erreur) ; les constantes d'identité du plugin en proviennent désormais, verrouillé par hachage dans `locks/host-api-aars.lock`

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources ; les utilisateurs ordinaires peuvent simplement installer l'APK préconstruit depuis la page Releases.

Compiler un APK de débogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Exécuter les tests unitaires JVM et compiler l'APK de tests d'instrumentation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collecter l'artefact de release et ajouter la version et le condensé CRC32 à son nom de fichier:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Vérifier que les sources de documentation multilingues et les artefacts générés sont synchronisés (également appliqué par la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilation nécessite JDK 21 ou ultérieur et Android SDK 37 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

******

### Localisation et génération de la documentation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Les fichiers JSON de langue sous `.readme/` et `.changelog/` sont la source unique du README, des instructions du centre de plugins et du journal des modifications. Modifiez toujours ces sources JSON et relancez `py .python/generate_markdown.py` ; les artefacts README, `plugin_instruction.md` et journal des modifications générés ne sont jamais édités à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier tous les artefacts générés.

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Documentation du module terminal: https://docs.autojs6.com/#/terminal
- Plugin Node.js Runtime: https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime
- jackpal Android-Terminal-Emulator (émulation de terminal et bibliothèques natives pty, Apache-2.0): https://github.com/jackpal/Android-Terminal-Emulator
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/THIRD_PARTY_NOTICES.md
