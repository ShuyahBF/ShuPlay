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

## Compte rendu par e-mail AUTOMATIQUE après toute livraison (règle du propriétaire, 19/11/2026 : « L'envoi de l'e-mail doit être automatique »)
- L'e-mail HTML à jfrancois.ouoba@gmail.com (outil Gmail, voir règle 12) part TOUT SEUL à la fin de CHAQUE livraison, sans que le
  propriétaire ait à le demander ni à le rappeler, et sans demander confirmation avant l'envoi.
- « Livraison » = tout ce qui atteint le propriétaire ou les utilisateurs, même sans fusion ni déploiement Render : dépôt d'un APK ou d'une
  nouvelle version dans ShuyahStore, publication d'une version, correction, changement de libellé, simple règle ajoutée au CLAUDE.md, fichier
  envoyé au propriétaire. Aucune exception de taille.
- Ne pas conclure « la règle ne s'applique pas, rien n'a été déployé sur Render » : en cas de doute, envoyer l'e-mail.
- Contenu : date et heure, identification (dépôt, branche, commit, version, lot), périmètre, comment vérifier, à activer, points
  d'attention, reste à faire. Jamais de secret (clé, mot de passe, chaîne de connexion) dans l'e-mail.
- Charger l'outil Gmail avec ToolSearch AVANT de dire qu'il manque ; s'il est vraiment indisponible, le dire dans le chat et ne pas clore la livraison.

