# Guide de configuration développeur

## 1. Prérequis

- Android Studio (dernière version stable) ou build en ligne de commande avec JDK 17
- Un compte Google Cloud avec facturation activée (requis même pour le quota gratuit des API)

## 2. Créer le projet Google Cloud

1. Créer un projet sur [console.cloud.google.com](https://console.cloud.google.com)
2. Activer les API suivantes :
   - Google Drive API
   - Gmail API
   - Google Photos Picker API
3. Configurer l'écran de consentement OAuth (type "Externe" pour les tests, "Interne" si compte Workspace)

## 3. Créer les identifiants OAuth

1. **Identifiants > Créer des identifiants > ID client OAuth**
2. Type d'application : **Android**
3. Renseigner le nom de package (`com.googlespacecleaner.app`) et l'empreinte SHA-1 de votre certificat de debug :
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
4. Télécharger le fichier `google-services.json` et le placer dans `app/`

## 4. Scopes à déclarer dans l'écran de consentement

| Scope | Usage | Sensibilité |
|---|---|---|
| `drive.readonly` | Scan Drive | Sensible |
| `drive.file` | Suppression (corbeille) Drive | Sensible |
| `gmail.readonly` | Scan Gmail | Sensible |
| `gmail.modify` | Suppression (corbeille) Gmail | Restreint — vérification Google requise |
| `photospicker.mediaitems.readonly` | Sélection Photos Picker | Sensible |

⚠️ Les scopes **sensibles** et **restreints** nécessitent une vérification
Google (processus CASA) avant publication publique. Voir
`docs/API_LIMITATIONS.md` et `docs/PLAY_STORE_CHECKLIST.md`.

## 5. Build local

⚠️ **Étape unique avant le premier build** : le fichier binaire du Gradle
Wrapper (`gradlew`) n'est pas fourni dans ce dépôt (il ne peut pas être généré
sans exécuter Gradle au moins une fois). Deux options :

- **Android Studio** : ouvrez simplement le dossier du projet — l'IDE détecte
  l'absence du wrapper et propose de le régénérer automatiquement.
- **Ligne de commande**, si vous avez déjà Gradle installé sur votre machine
  (`brew install gradle` ou équivalent) :
  ```bash
  gradle wrapper --gradle-version 8.9
  ```
  Cette commande n'est à exécuter qu'une seule fois ; elle génère `gradlew`,
  `gradlew.bat` et `gradle/wrapper/gradle-wrapper.jar`, qui doivent ensuite
  être committés dans le dépôt.

Une fois le wrapper généré :

```bash
./gradlew assembleDebug
```

**Sur GitHub Actions**, ce problème ne se pose pas : le workflow CI installe
Gradle directement sur le runner (`gradle/actions/setup-gradle`) sans dépendre
du wrapper local.

## 6. Lancer les tests

```bash
./gradlew testDebugUnitTest        # tests unitaires (JVM)
./gradlew connectedAndroidTest      # tests instrumentés (nécessite un appareil/émulateur)
```

## 7. Variables sensibles

Aucune clé API n'est codée en dur dans le dépôt. `google-services.json` est
ignoré par `.gitignore` (à créer si absent) — ne jamais le committer.
