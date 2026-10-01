3-Shell Terminal reprend le terminal intégré d'AutoJs6 : l'interrupteur "Terminal" du tiroir d'accueil, "Ouvrir dans le terminal" dans le menu des dossiers du gestionnaire de fichiers et la barre d'outils du projet, ainsi que l'objet global `terminal` côté script pour ouvrir, piloter et observer des sessions. Chaque session est un shell système (`/system/bin/sh`) exécuté dans un pty, qui continue en arrière-plan quand on quitte l'écran.

Aperçu de développement P2: sessions shell, accès au stockage, intégration Node.js avec vérification des signatures et contrôle des sessions depuis l'hôte sont implémentés. L'écran du terminal, l'API de scripts et les réglages suivront les étapes de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md). AutoJs6 6.8.0 (build 5304+).

### Utilisation

1. Installez l'APK du plugin correspondant à l'ABI de l'appareil (ou l'APK universel) depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/releases) sur un appareil avec AutoJs6 build 5304 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `3-Shell Terminal` est reconnu et activez-le.
3. Activez "Terminal" dans le tiroir d'accueil d'AutoJs6, choisissez "Ouvrir dans le terminal" sur un dossier du gestionnaire de fichiers, ou appelez `terminal.open(...)` depuis un script. Accordez "Accès à tous les fichiers" quand le plugin le demande pour entrer dans les dossiers du stockage partagé comme `/sdcard`.

### Commandes Node.js

- Nécessite le plugin Node.js Runtime 1.5.0 ou plus récent ; le plugin lit son contrat de manifeste, vérifie la signature, le lanceur et l'archive npm / corepack, puis lie les commandes dans `PATH` à chaque démarrage de session. Sans le plugin, ou si la vérification échoue, le terminal reste utilisable sans ces commandes.
- Android interdit l'exécution de fichiers écrits par l'application. npm désactive les liens bin par défaut: `node_modules/.bin/*` et `npx <paquet>` ne peuvent donc pas lancer directement les points d'entrée. Utilisez `node node_modules/<paquet>/<entrée>.js`. Les exécutables natifs des paquets npm échouent avec `EACCES`, et les extensions natives (`.node`) ne peuvent pas être chargées.
- corepack utilise par défaut ses pnpm 11.x et Yarn 1.x intégrés (`COREPACK_DEFAULT_TO_LATEST=0`) et télécharge une version nommée explicitement sur demande ; le registre npm peut être basculé vers npmmirror ou une URL https personnalisée dans les réglages.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal/blob/master/ROADMAP.md) pour le guide d'installation et l'avancement actuel.
