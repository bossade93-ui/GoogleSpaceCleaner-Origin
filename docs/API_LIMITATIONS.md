# Limitations des API Google

Ce document consolide toutes les contraintes des API Google découvertes durant
le développement, avec leur impact produit et la solution retenue.

## Google Photos — accès en lecture retiré (31 mars 2025)

**Contrainte** : la Photos Library API ne permet plus l'accès en lecture à
l'ensemble de la bibliothèque d'un utilisateur. Seuls les médias créés par
l'application elle-même restent listables automatiquement.

**Impact** : un scan automatique complet de Google Photos, tel que prévu dans
la demande initiale, est irréalisable.

**Solution retenue** (voir `feature/photos-scan`) :
- **Photos Picker** pour une sélection ponctuelle par album (rapide, temps réel)
- **Import Google Takeout** pour une analyse complète mais hors-ligne
- Dans les deux cas, **la suppression reste manuelle** : l'application ne peut
  pas supprimer un contenu qu'elle n'a pas créé. Elle formule des
  recommandations (`CleanupActionType.MANUAL_RECOMMENDATION_ONLY`), jamais
  d'action automatique sur Photos.

## Gmail — pas de hash de contenu

**Contrainte** : l'API Gmail ne fournit aucun hash/checksum pour les pièces
jointes.

**Impact** : la détection de doublons (`DetectDuplicatesUseCase`) ne peut pas
s'appliquer à Gmail. Seule la détection "volumineux/ancien" est pertinente
pour cette source (seuil abaissé à 10 Mo au lieu de 100 Mo pour Drive, les
pièces jointes étant généralement plus petites que les fichiers Drive).

## Gmail — coût réseau du détail par message

**Contrainte** : l'API de recherche (`messages.list`) ne renvoie que des IDs ;
un appel `messages.get` distinct est nécessaire pour connaître taille, objet
et date de chaque message.

**Solution retenue** : les appels sont parallélisés avec une concurrence
bornée (`Semaphore(10)`) plutôt que séquentiels, pour rester sous le quota de
250 unités/seconde par utilisateur (chaque lecture coûtant 5 unités) tout en
réduisant fortement le temps de scan.

## Drive/Gmail — suppression = corbeille, jamais définitive

**Contrainte volontaire** (choix produit, pas limitation API) : l'application
ne supprime jamais un fichier ou email définitivement. Elle utilise
systématiquement l'action "corbeille" (`trashed: true` pour Drive,
`messages.trash` pour Gmail), avec une rétention standard de 30 jours pendant
laquelle l'utilisateur peut restaurer manuellement depuis Drive/Gmail, ou via
le bouton "Annuler" de `feature:history`.

## Miniatures — authentification requise

**Contrainte** : les URLs de miniatures (`thumbnailLink` Drive,
`mediaFile.baseUrl` Photos Picker) exigent un en-tête `Authorization: Bearer`
valide pour se charger ; sans cela, elles renvoient une erreur 403.

**Solution retenue** : l'`ImageLoader` Coil global (configuré dans
`GsApplication`) réutilise le même client OkHttp que Retrofit (intercepteur
OAuth partagé), afin que les miniatures s'authentifient automatiquement.

## OAuth — vérification Google requise avant publication

**Contrainte** : les scopes sensibles utilisés (`drive.readonly`,
`gmail.readonly`, écriture Drive/Gmail pour la suppression,
`photospicker.mediaitems.readonly`) nécessitent une vérification OAuth par
Google (processus CASA) avant publication publique sur le Play Store. Ce
processus peut prendre plusieurs semaines et doit être initié tôt — voir
`docs/PLAY_STORE_CHECKLIST.md`.
