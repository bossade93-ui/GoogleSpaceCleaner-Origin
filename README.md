# Google Space Cleaner

Application Android permettant d'analyser et de nettoyer l'espace de stockage
d'un compte Google (Drive, Photos, Gmail), avec une priorité donnée à la
sécurité et au contrôle de l'utilisateur : aucune suppression sans
prévisualisation, double confirmation, et suppression réversible (corbeille)
partout où l'API Google le permet.

## Documentation

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — architecture multi-module, flux de données, règles de conception
- **[docs/API_LIMITATIONS.md](docs/API_LIMITATIONS.md)** — contraintes des API Google découvertes durant le développement et leur impact produit
- **[docs/SETUP.md](docs/SETUP.md)** — configuration Google Cloud, OAuth, build local
- **[docs/PRIVACY_POLICY_DRAFT.md](docs/PRIVACY_POLICY_DRAFT.md)** — ébauche de politique de confidentialité (à valider par un juriste)

## État du projet

Les 7 modules `feature/*` du cahier des charges sont développés :
Auth, Dashboard, Drive, Photos, Gmail, Cleanup, History.

**Étapes de la méthode de travail :**
1. ✅ Analyse des besoins
2. ✅ Cahier des charges
3. ✅ Architecture
4. ✅ Structure des dossiers
5. ✅ Maquettes UI
6. ✅ Base du projet
7. ✅ Développement de chaque module
8. ✅ Tests — 13 fichiers de test couvrant les repositories réseau (Drive/Gmail/Photos), la logique de suppression/annulation, et les ViewModels (Cleanup/Dashboard/History)
9. ✅ Optimisation — voir le détail dans l'historique de développement
10. ✅ Documentation
11. ⬜ Préparation à la publication sur le Google Play Store

## Limitations connues et assumées

- **Google Photos** ne peut pas être scanné automatiquement (restriction Google du 31/03/2025) — voir `docs/API_LIMITATIONS.md`
- **Gmail** n'a pas de détection de doublons (l'API ne fournit aucun hash de contenu)
- Tests d'UI instrumentés (Compose) non encore écrits — la couverture actuelle est unitaire (JVM), pas d'automatisation de bout en bout
- Aucune Migration Room réelle n'existe encore (`fallbackToDestructiveMigration`) — acceptable tant qu'aucune version n'est publiée avec des utilisateurs réels ; **à remplacer avant toute publication**

## Build

```bash
./gradlew assembleDebug
```

Nécessite une configuration préalable — voir `docs/SETUP.md`.

## Tests

```bash
./gradlew testDebugUnitTest
```
