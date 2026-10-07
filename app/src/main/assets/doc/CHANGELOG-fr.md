******

### Historique des versions

******

# v1.1.0

###### 2026/10/07

* `Fonctionnalité` Approuver la signature d'un Node.js Runtime tiers: le diagnostic de l'environnement affiche le SHA-256 d'une signature non officielle et propose "Faire confiance à cette signature"; après une seconde confirmation, les nouvelles sessions peuvent utiliser ses commandes node / npm, et "Signatures Node.js approuvées" dans les paramètres révoque la confiance à tout moment
* `Fonctionnalité` Notifications de fin de commande: lorsqu'une commande sans shell interactif (par exemple un `exec` de script avec `keepOpen: false`) se termine d'elle-même, une notification ouvre la fin de sa sortie pour la copier, la partager ou rouvrir un terminal dans son dossier; les notifications ne citent que la session et le dossier, une rafale n'alerte qu'une fois, et les réglages permettent de les désactiver
* `Fonctionnalité` Couleurs et police du terminal: les réglages ajoutent "Couleurs du terminal" (suivre l'apparence claire / sombre, sombre, clair, teinté par la couleur du thème) et "Police du terminal" (chasse fixe du système ou JetBrains Mono intégrée, sans ligatures); la sélection et l'écran de sortie des commandes utilisent la même police, et le retour au terminal applique le changement sans interrompre la session
* `Fonctionnalité` "Ouvrir dans le terminal" du menu des dossiers du gestionnaire de fichiers est désormais fourni par le plugin (AutoJs6 6.8.0 build 5320 ou ultérieur): AutoJs6 transmet uniquement le chemin du dossier sélectionné sans accorder d'accès supplémentaire; la session la plus récente s'y place, ou une nouvelle session y démarre s'il n'y en a aucune; les builds antérieurs d'AutoJs6 conservent leur propre entrée de menu
* `Correctif` Sur les versions récentes d'Android comme Android 17, un appui long sur le terminal affiche de nouveau les poignées de sélection et la barre Copier / Tout sélectionner, le texte peut donc être sélectionné et copié
* `Correctif` Copier ou partager une très longue transcription ne fait plus planter le plugin (sous Android 7.0, le plantage fermait toutes les sessions); un texte trop long conserve ses 100000 derniers caractères et une notification l'indique
* `Correctif` Le bouton de retour en haut à gauche des écrans Paramètres, À propos, historique des versions, diagnostic de l'environnement et Sortie de la commande fonctionne de nouveau, si bien que les paramètres ouverts depuis le centre de plugins d'AutoJs6 y reviennent
* `Dépendance` Mise à niveau du correctif jackpal emulatorview 1.0.42-p6.1 -> 1.0.42-p8.1: le plugin choisit la police du moteur de rendu, et `Math.ceil` remplace `android.util.FloatMath`, retiré du SDK depuis l'API 23; toutes les autres classes sont identiques octet par octet et `locks/vendored-aars.lock` est mis à jour
* `Dépendance` Ajout de JetBrains Mono NL 2.304 (SIL Open Font License 1.1) comme police facultative du terminal, intégrée sans modification avec sa licence dans l'écran À propos; la source et le SHA-256 sont consignés dans `vendor/fonts/jetbrains-mono/`
* `Dépendance` Ajout de `explorer-action-api.aar` (module AutoJs6 `plugin-api/explorer-action-api`, build hôte 6.8.0 / 5320, MPL 2.0) comme contrat du protocole Explorer Action v23 pour l'action de dossier du gestionnaire de fichiers, avec hachage verrouillé dans `locks/host-api-aars.lock`

# v1.0.0

###### 2026/10/07

* `Note` La version 1.0.0 fournit un terminal autonome, des sessions en arrière-plan, des API de script interactives et les réglages. Les API complètes exigent AutoJs6 6.8.0 build 5315 ou ultérieur; le protocole de base exige le build 5304. Android 7.0 ou ultérieur est pris en charge
* `Fonctionnalité` Sessions multiples : création, bascule, fermeture et gestionnaire de sessions ; un service au premier plan maintient les sessions après avoir quitté l'écran, et sa notification affiche le dossier courant et le nombre de sessions avec une action "Fermer les sessions"
* `Fonctionnalité` Écran du terminal : barre de touches sur deux lignes (Esc / Tab / Ctrl / flèches / symboles courants), sélection de texte native avec Copier / Tout sélectionner, copie et partage de la transcription, taille du texte, coller et effacer
* `Fonctionnalité` Chaîne Node.js : avec le plugin Node.js Runtime (1.5.0+), node / npm / npx / corepack / yarn / pnpm deviennent disponibles, avec les réglages de registre npm et "ignorer les scripts d'installation" et le menu de paquets (npm init / install / run script, etc.)
* `Fonctionnalité` Entrées AutoJs6 : l'interrupteur du tiroir d'accueil (nombre de sessions, tout fermer), "Ouvrir dans le terminal" dans le menu des dossiers du gestionnaire de fichiers et la barre d'outils du projet
* `Fonctionnalité` API de script `terminal` (alias `$terminal`) : gestion des sessions, exécution visible (`exec`, `npm.run`) et objet session avec événements `output` / `exit`, `write` et `waitFor` ; chaque échec est une `TerminalError` au `code` stable
* `Fonctionnalité` Application autonome : l'icône du lanceur ouvre directement le terminal ; page de réglages (apparence suivant AutoJs6, taille du texte, registre npm, intégration Node.js, accès à tous les fichiers, effacer les données du terminal), À propos et historique des versions
* `Fonctionnalité` APK séparés par ABI (arm64-v8a, armeabi-v7a, x86_64, x86) plus un APK universel, bibliothèques natives alignées sur des pages de 16 Ko
* `Fonctionnalité` README, notice du centre de plugins et journal des modifications en 10 langues
* `Correctif` Les restrictions système sur l'activité en arrière-plan ne font plus planter le plugin au démarrage d'une session. La session continue sans la protection du service de premier plan.
* `Correctif` La lecture de longs historiques conserve le texte le plus récent sans dépasser la limite de taille des réponses entre processus.
* `Correctif` La lecture et la reprise de la sortie omettent les lignes vides de remplissage de l'écran, tout en préservant les espaces de l'invite et la séparation avec la sortie suivante
* `Correctif` Une session en cours de démarrage pouvait disparaître brièvement des requêtes de l'hôte et empêcher l'ouverture de son terminal
* `Correctif` Les sorties continues ne bloquent plus la file de messages du terminal; les fermetures pendant le démarrage et les fins naturelles libèrent les pty et les threads d'entrée-sortie
* `Correctif` Ouvrir un terminal existant retente la protection du service de premier plan si les restrictions en arrière-plan ont empêché son démarrage
* `Correctif` La réouverture d'une tâche après la fin du processus du plugin crée un nouveau shell sans réexécuter l'ancienne commande
* `Amélioration` Taille visuelle harmonisée des icônes du lanceur et du Centre de plugins, avec des fonds transparents et des motifs noirs, blancs ou gris neutres
* `Amélioration` Les icônes du centre de plugins utilisent les tailles, positions, images claires et sombres et fonds circulaires réglés dans Icon Studio, avec les sources et paramètres permettant de les reproduire
* `Amélioration` Les icônes des informations d'application Android utilisent les illustrations et les fonds clairs et sombres d'Icon Studio, en conservant les images transparentes du centre de plugins et les choix du lanceur
* `Dépendance` Ajout de jackpal Android-Terminal-Emulator (term 1.0.70, emulatorview 1.0.42-p6.1, libtermexec 1.0, Apache-2.0) pour l'émulation de terminal et les bibliothèques natives pty, verrouillé par hachage dans `locks/vendored-aars.lock`
* `Dépendance` Ajout de `common-plugin-api.aar` et `nodejs-api.aar` (modules AutoJs6 `plugin-api/common-plugin-api` et `plugin-api/nodejs-api`, build hôte 6.8.0 / 5303, MPL 2.0) comme contrat de plugin partagé et contrat de manifeste Node.js, verrouillés par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de `terminal-api.aar` (module AutoJs6 `plugin-api/terminal-api`, build hôte 6.8.0 / 5304, MPL 2.0) comme contrat de terminal V1 (`ITerminalPlugin` / `ITerminalCallback`, identité, plafonds et codes d'erreur) ; les constantes d'identité du plugin en proviennent désormais, verrouillé par hachage dans `locks/host-api-aars.lock`
