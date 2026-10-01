******

### Historique des versions

******

# v1.0.0

###### 2026/10/01

* `Note` Aperçu de développement P0 : squelette du dépôt, identité du plugin reconnue par le centre de plugins d'AutoJs6 et spike pty / stockage / lanceur Node.js. Le contrat Binder, le noyau de sessions, l'écran du terminal, l'API de script et la page de réglages suivent les phases de ROADMAP.md.
* `Fonctionnalité` Identité du plugin `three-shell-terminal` (engine `terminal`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.TERMINAL` pour la découverte par l'hôte
* `Fonctionnalité` APK séparés par ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus un APK universel, bibliothèques natives alignées sur des pages de 16 Ko
* `Fonctionnalité` README, notice du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Noyau de session porté depuis le terminal de l'hôte: sessions shell sur pty avec un registre au niveau du processus (titre et code de sortie enregistrés pour le Binder), environnement de session et arborescence sous le répertoire de fichiers du plugin, détection du lanceur Node.js avec l'installateur npm / corepack, et service de premier plan qui maintient les sessions avec une notification "Fermer les sessions" (canal `three.shell.terminal.sessions`)
* `Dépendance` Ajout de jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) pour l'émulation de terminal et les bibliothèques natives pty, verrouillé par hachage dans `locks/vendored-aars.lock`
* `Dépendance` Ajout de `common-plugin-api.aar` et `nodejs-api.aar` (modules AutoJs6 `plugin-api/common-plugin-api` et `plugin-api/nodejs-api`, build hôte 6.8.0 / 5303, MPL 2.0) comme contrat de plugin partagé et contrat de manifeste Node.js, verrouillés par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de `terminal-api.aar` (module AutoJs6 `plugin-api/terminal-api`, build hôte 6.8.0 / 5304, MPL 2.0) comme contrat de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identité, plafonds et codes d'erreur) ; les constantes d'identité du plugin en proviennent désormais, verrouillé par hachage dans `locks/host-api-aars.lock`
