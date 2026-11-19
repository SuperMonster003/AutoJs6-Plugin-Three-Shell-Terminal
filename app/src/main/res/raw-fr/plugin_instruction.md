3-Shell Terminal est un terminal interactif sur votre téléphone: il exécute le `/system/bin/sh` du système dans un pty, garde plusieurs sessions ouvertes à la fois et les laisse s'exécuter en arrière-plan quand vous quittez l'écran. Une fois le plugin Node.js Runtime installé, des commandes comme node et npm sont aussi disponibles. L'entrée "Terminal" d'AutoJs6 et l'objet `terminal` des scripts sont tous deux fournis par ce plugin.

### Prise en main

1. Appuyez sur "Terminal" dans le tiroir de l'accueil d'AutoJs6, ou choisissez "Ouvrir dans le terminal" dans le menu d'un dossier du gestionnaire de fichiers; vous pouvez aussi appuyer sur l'icône 3-Shell Terminal de l'écran d'accueil.
2. Saisissez une commande et appuyez sur Entrée pour l'exécuter. La barre de touches en bas de l'écran propose Esc, Tab, Ctrl, les flèches et les symboles courants.
3. Créez, changez ou fermez des sessions depuis le menu "Sessions" de la barre d'outils. Les sessions continuent après que vous avez quitté l'écran; appuyez sur la notification pour ouvrir "Gestion du terminal".
4. Avant d'entrer dans un dossier du stockage partagé comme `/sdcard`, appuyez sur "Autoriser" quand le terminal vous y invite, ou activez "Accès à tous les fichiers" dans les paramètres.

### Fonctionnalités principales

- **Sessions multiples**: créez, changez et fermez des sessions; "Gestion du terminal" les liste toutes. Un service de premier plan garde les sessions actives en arrière-plan, et la notification affiche le dossier courant et le nombre de sessions.
- **Écran du terminal**: barre de touches, sélection et copie du texte, "Partager la transcription", zoom du texte à deux doigts; les paramètres proposent 4 jeux de "Couleurs du terminal" et 2 choix de "Police du terminal".
- **Éditeur de commandes**: quand vous collez une commande longue ou sur plusieurs lignes, le texte complet s'affiche d'abord; vous pouvez le coller dans la session courante, ou l'exécuter dans une nouvelle session et voir le code de sortie.
- **Outils Node.js**: une fois le plugin Node.js Runtime installé, node, npm, npx et corepack (yarn / pnpm) sont disponibles. Dans les paramètres, vous pouvez choisir le "Registre npm" ou activer "Ignorer les scripts de cycle de vie npm".
- **Notifications de fin de commande**: quand une commande lancée par un script sans garder de shell interactif se termine, une notification s'affiche; appuyez dessus pour voir la fin de la sortie. Vous pouvez désactiver "Notifications de fin de commande" dans les paramètres.
- **Paramètres propres**: la langue, le mode nuit et la couleur du thème suivent AutoJs6 par défaut et peuvent aussi être réglés séparément; l'icône du lanceur existe en 4 styles. Quand AutoJs6 le permet, le menu de la page des paramètres propose "Ouvrir les paramètres généraux".

### Utilisation dans les scripts

Les scripts utilisent l'objet global `terminal` (alias `$terminal`) pour créer des sessions, exécuter des commandes, écrire des entrées et écouter la sortie:

```js
if (!terminal.isAvailable()) exit();
let result = terminal.exec('uname -m', {
    show: false,
    keepOpen: false,
    wait: true,
});
console.log(result.exitCode);
let session = terminal.open({ show: false });
session.on('output', (text) => console.log(text));
session.write('ls\n');
session.show();
```

- Par défaut, `exec` garde un shell interactif après la fin de la commande; pour attendre le code de sortie, définissez à la fois `keepOpen: false` et `wait: true`.
- Les méthodes synchrones bloquent le thread du script et ne peuvent pas être appelées sur le thread de l'interface; la plupart des méthodes ont aussi une forme `Async` qui renvoie une Promise.
- Les sessions continuent après la fin du script; appelez `session.close()` quand vous n'en avez plus besoin. Une opération qui échoue lève une `TerminalError` avec un `code`.
- Pour toutes les options et tous les événements, consultez [Documentation AutoJs6 > Terminal](https://docs.autojs6.com/#/terminal).

### Prérequis

- Android 7.0 ou version ultérieure et AutoJs6 6.8.0 ou version ultérieure.
- Les commandes comme node et npm nécessitent le plugin [Node.js Runtime](https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime) 1.5.0 ou version ultérieure.
- Entrer dans les dossiers du stockage partagé nécessite "Accès à tous les fichiers" (Android 11 et versions ultérieures) ou l'autorisation de stockage; l'autorisation des notifications sert aux notifications de session et de fin de commande.
- Les commandes s'exécutent avec l'identité et les autorisations propres à ce plugin, pas celles d'AutoJs6; pour une commande qui a besoin des autorisations d'AutoJs6, utilisez plutôt `shell()` dans un script.

### Questions fréquentes

**Impossible d'entrer dans les dossiers sous `/sdcard`**

Ce plugin a besoin de sa propre autorisation de stockage. Appuyez sur "Autoriser" quand le terminal vous y invite, ou activez "Accès à tous les fichiers" dans les paramètres; sous Android 10 et versions antérieures, accorder l'autorisation de stockage suffit. Une adresse `content://` n'est pas un chemin de fichier et on ne peut pas y entrer.

**La commande node est introuvable**

Vérifiez que le plugin Node.js Runtime est installé et que "Intégration Node.js" est activé dans les paramètres, puis ouvrez "Vérification du lanceur Node.js" pour en voir la raison. Si le plugin n'est pas signé avec la signature officielle, cette page affiche le SHA-256 de sa signature; une fois que vous lui faites confiance, les nouvelles sessions peuvent l'utiliser.

**Le système arrête les sessions en arrière-plan**

Une fois le processus du plugin arrêté par le système, ses sessions ne peuvent pas être restaurées. Des systèmes comme HyperOS et MIUI peuvent limiter l'activité en arrière-plan; dans les paramètres du système, autorisez 3-Shell Terminal à envoyer des notifications et à s'exécuter en arrière-plan.

Les touches, l'interface de script et les autorisations sont détaillées sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal).
