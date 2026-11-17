# ShuPlay

Lecteur audio et vidéo pour Android (propriétaire : ShuyahBF). Paquet `com.shuyah.shuplay`, Android 7.0 et plus.

## Fonctions (version 1.0)
- Lecture de **tous les formats usuels** audio et vidéo (moteur ExoPlayer / Media3) : fichiers du téléphone, « Ouvrir avec ShuPlay », liens directs http(s).
- Commandes standard : lecture / pause, piste précédente / suivante, **retour et avance rapides de 10 s**, barre de progression, plein écran, vitesse, pistes audio, sous-titres.
- **Liste de lecture** : ajout, réordonnancement (flèches), retrait, lecture **aléatoire**, **répétition** (aucune / liste / piste), listes **nommées enregistrées**, liste en cours **enregistrée automatiquement**.
- **Reprise** : chaque piste repart là où elle s'était arrêtée.
- **La lecture continue** quand la fenêtre perd le focus, quand on change d'application ou que l'écran s'éteint (service de lecture + notification de contrôle, boutons du casque, écran verrouillé). Pause automatique pendant un appel ou si le casque est débranché.
- Aucune donnée personnelle collectée ; tout reste sur le téléphone. Seul un signal de présence anonyme (version, modèle du téléphone) est envoyé à SAWALI (règle 4).

## Ce que ShuPlay ne fait pas
- Il ne télécharge ni n'extrait de vidéos YouTube (conditions d'utilisation de YouTube).

## Structure
| Fichier | Rôle |
|---|---|
| `app/src/main/java/com/shuyah/shuplay/ServiceLecture.kt` | Service qui porte le lecteur (ExoPlayer + session média) |
| `MainActivity.kt` | Écran unique : lecteur, actions, liste |
| `Logique.kt` | Logique pure (titres, durées, reprise, liens, JSON) — testée |
| `Stockage.kt` | Mémoire locale (liste en cours, listes nommées, positions) |
| `AdaptateurListe.kt` | Affichage de la liste (piste en cours : fond orange `#f6a35b`, police bleu nuit `#0A1E3D`) |
| `Presence.kt` | Signal de présence à SAWALI |

## Compiler (sans Visual Studio : Android Studio ou ligne de commande)
```
gradle :app:testDebugUnitTest          # tests de la logique
gradle :app:assembleRelease -PsignerAvecCleLocale=true   # APK signé avec la clé de test locale
```
Clé permanente : variables d'environnement `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` (jamais dans le dépôt).

## Version et lot (règles 1 et 2)
Source unique : `versionName` / `versionCode` et `LOT` dans `app/build.gradle.kts`. À chaque livraison : version +1, lot = numéro de la PR fusionnée. L'écran affiche « Version X · déployée le JJ/MM/AAAA HH:MM ».
