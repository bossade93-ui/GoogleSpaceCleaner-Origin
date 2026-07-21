# Architecture

## Vue d'ensemble

Clean Architecture en 3 couches, organisée en modules Gradle multi-module :

```
app/                    → assemblage (DI graph, navigation, thème)
core/
  ├── core-ui           → composants Compose partagés, thème M3, ItemThumbnail (Coil)
  ├── core-domain        → modèles, interfaces de repository, use cases (Kotlin/JVM pur)
  ├── core-data           → Room (chiffré SQLCipher), mappers, SelectionRepository
  ├── core-network         → Retrofit (Drive/Gmail/Photos Picker), OAuth
  └── core-security          → TokenManager, DbKeyProvider (Android Keystore)
feature/
  ├── auth                 → Google Sign-In, auth incrémentale par scope
  ├── dashboard              → vue d'ensemble, statistiques agrégées
  ├── drive-scan               → scan + détection Drive
  ├── photos-scan                → Picker + import Takeout
  ├── gmail-scan                   → recherche pièces jointes volumineuses
  ├── cleanup                        → prévisualisation, suppression, historique
  └── history                          → annulation des suppressions
```

**Règle stricte** : un module `feature/*` ne dépend **jamais** directement d'un
autre module `feature/*`. Toute donnée partagée entre écrans de scan et écran
de nettoyage transite par une interface de `core-domain` (`SelectionRepository`,
`AuthRepository`), implémentée dans le module qui la possède et liée via Hilt
`@Binds`/`@Provides`. Cela permet à Hilt d'agréger les bindings au niveau de
l'application sans introduire de couplage de compilation entre features.

## Flux de données type (exemple : scan Drive)

```
DriveScanScreen (Compose)
  → DriveScanViewModel (StateFlow<DriveScanUiState>)
    → DriveRepositoryImpl (implémente ScanRepository)
      → DriveApiService (Retrofit, intercepteur OAuth)
      → DetectDuplicatesUseCase / DetectLargeAndOldFilesUseCase (core-domain)
      → ScannedItemDao (Room, chiffré SQLCipher)
```

## Authentification incrémentale

Un seul scope (email/profil) est demandé à la connexion. Chaque module de scan
(Drive/Photos/Gmail) demande son propre scope Google via
`AuthRepository.requestScope()` **au moment où il en a réellement besoin**
(juste avant le premier scan), conformément aux recommandations Google plutôt
que de tout demander d'un coup à l'écran de connexion.

## Suppression et annulation

`CleanupRepository.executeCleanup()` reçoit les `ScannedItem` complets (pas de
simples IDs) car leur champ `source` détermine l'API à appeler :
- **DRIVE** → `trashFile()` (corbeille Drive, rétention 30 jours)
- **GMAIL** → `trashMessage()` (corbeille Gmail, rétention 30 jours)
- **PHOTOS_PICKER / PHOTOS_TAKEOUT** → aucun appel API, marqué
  `MANUAL_RECOMMENDATION_ONLY` (voir `docs/API_LIMITATIONS.md`)

Chaque `CleanupAction` conserve `itemSources: Map<String, DataSource>` (et non
un simple `itemIds: List<String>`) : au moment d'une annulation, les items ont
déjà été retirés du cache Room, donc la source de chaque item ne peut plus être
redemandée au cache — elle doit être conservée dans l'historique lui-même.

## Stockage local

- **Room + SQLCipher** : cache des métadonnées de fichiers scannés et
  historique des suppressions, chiffrés avec une clé 256 bits générée au
  premier lancement et stockée dans `EncryptedSharedPreferences`
  (Android Keystore). Voir `DbKeyProvider`.
- **Aucune donnée n'est envoyée à un serveur tiers** : tout le traitement
  (détection de doublons, calcul d'espace récupérable) est effectué on-device.

## Modules non fonctionnels transverses

- `NetworkAuthModule` (core-network) : source **unique** du client OkHttp
  partagé (intercepteur OAuth) et de l'`ImageLoader` Coil global — ne jamais
  dupliquer ce provider ailleurs, Hilt lèverait une erreur de binding dupliqué.
- `SecurityModule` (core-security) : source unique de `TokenManager` et
  `DbKeyProvider`.
