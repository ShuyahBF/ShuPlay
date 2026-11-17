# ShuPlay — règles du dépôt (propriétaire ShuyahBF)

Les règles permanentes du propriétaire (voir le CLAUDE.md de `ShuyahBF/Aizenta-Analyse-Qualit-Gestion-Stocks`) s'appliquent ici. Rappel :
- Commits : auteur ET committer `ShuyahBF <jfrancois.ouoba@gmail.com>`, messages et commentaires en français, aucune mention d'IA ni de Co-Authored-By.
- Commentaires clairs sur chaque bloc de code (le propriétaire vient de WinDev). Aucun secret ni clé de signature dans le dépôt.
- Après chaque livraison : compte rendu par e-mail HTML à jfrancois.ouoba@gmail.com (date, identification, périmètre, comment vérifier, à activer, points d'attention, reste à faire).
- Version et lot mis à jour à chaque livraison (`app/build.gradle.kts`) ; version affichée dans l'application.
- Pictogramme + légende courte sur toute action ; enregistrement jamais « deviné » (liste en cours : mention « enregistrée automatiquement ») ; menus avec une majuscule à chaque mot.
- Ligne sélectionnée : fond `#f6a35b`, police `#0A1E3D`. Présence déclarée à SAWALI (`Presence.kt`), jamais bloquante.
- APK livré à CHAQUE livraison (règle du propriétaire) : release, nom `shuplay-X.Y.apk` (sans « debug » ni « release »), version +1, envoyé avec `SendUserFile` ; Android refuse parfois la mise à jour si la signature diffère et la désinstallation efface les données locales.

## Plateforme
- Application Android seule (décision du 17/11/2026 : pas de version Windows). Kotlin, Media3 (ExoPlayer, MediaSession), vues Android classiques.
- Le lecteur vit dans `ServiceLecture` ; l'écran n'est qu'une télécommande (`MediaController`) : la lecture continue quand la fenêtre perd le focus.
- Tests avant livraison : `gradle :app:testDebugUnitTest` ; build : `gradle :app:assembleRelease -PsignerAvecCleLocale=true`.
