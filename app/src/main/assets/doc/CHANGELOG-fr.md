******

### Historique des versions

******

# v1.0.0

###### 2026/10/02

* `Note` Aperçu de développement P2: sessions shell, accès au stockage, intégration Node.js avec vérification des signatures et contrôle des sessions depuis l'hôte sont implémentés. L'écran du terminal, l'API de scripts et les réglages suivront les étapes de ROADMAP.md.
* `Fonctionnalité` Identité du plugin `three-shell-terminal` (engine `terminal`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.TERMINAL` pour la découverte par l'hôte
* `Fonctionnalité` APK séparés par ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus un APK universel, bibliothèques natives alignées sur des pages de 16 Ko
* `Fonctionnalité` README, notice du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Noyau de session porté depuis le terminal de l'hôte: sessions shell sur pty avec un registre au niveau du processus (titre et code de sortie enregistrés pour le Binder), environnement de session et arborescence sous le répertoire de fichiers du plugin, détection du lanceur Node.js avec l'installateur npm / corepack, et service de premier plan qui maintient les sessions avec une notification "Fermer les sessions" (canal `three.shell.terminal.sessions`)
* `Fonctionnalité` Résolution de l'accès au stockage (`StorageAccess`): état des permissions propre au plugin (permissions d'exécution héritées sous API 30, "Accès à tous les fichiers" à partir d'API 30), détection du stockage partagé pour `/sdcard`, `/storage/...` et des dossiers `Android/{data,obb,media}` du plugin, repli du répertoire de départ vers `$HOME` avec `STORAGE_PERMISSION_REQUIRED` ou `DIRECTORY_INACCESSIBLE`, et les Intents de réglages qui ouvrent l'accès à tous les fichiers
* `Fonctionnalité` Intégration Node.js avec confiance du signataire (`NodeCliTrust`, `NodeCliLocator`, `SessionAssembly`): le plugin Node.js Runtime n'est utilisé que s'il est signé par la clé officielle des plugins AutoJs6 ou par la clé propre de ce plugin, l'interrupteur des réglages court-circuite avant toute recherche, chaque résultat est associé aux états `node-cli` du contrat (`available`, `disabled`, `plugin-missing`, `plugin-untrusted`, `plugin-too-old`, `executable-missing`, `exec-denied`, `setup-failed`), et chaque démarrage de session renouvelle les liens de commandes de `usr/bin`, extrait l'archive npm / corepack une seule fois par empreinte et exporte l'environnement npm / corepack
* `Fonctionnalité` AutoJs6 peut créer et contrôler jusqu'à 16 sessions de terminal, recevoir la sortie en direct avec 4 écouteurs par session, lire la sortie récente et consulter l'environnement shell. Les sessions continuent après la fermeture d'AutoJs6; les demandes invalides sont refusées avec un motif précis.
* `Correctif` Les restrictions système sur l'activité en arrière-plan ne font plus planter le plugin au démarrage d'une session. La session continue sans la protection du service de premier plan.
* `Dépendance` Ajout de jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42, libtermexec 1.0, Apache-2.0) pour l'émulation de terminal et les bibliothèques natives pty, verrouillé par hachage dans `locks/vendored-aars.lock`
* `Dépendance` Ajout de `common-plugin-api.aar` et `nodejs-api.aar` (modules AutoJs6 `plugin-api/common-plugin-api` et `plugin-api/nodejs-api`, build hôte 6.8.0 / 5303, MPL 2.0) comme contrat de plugin partagé et contrat de manifeste Node.js, verrouillés par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de `terminal-api.aar` (module AutoJs6 `plugin-api/terminal-api`, build hôte 6.8.0 / 5304, MPL 2.0) comme contrat de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identité, plafonds et codes d'erreur) ; les constantes d'identité du plugin en proviennent désormais, verrouillé par hachage dans `locks/host-api-aars.lock`
