# Politique de confidentialité — Ébauche

> ⚠️ Ce document est une **ébauche technique** destinée à accélérer la
> rédaction finale. Il doit être relu et validé par un juriste avant toute
> publication publique — Claude n'est pas juriste et ce texte ne constitue
> pas un conseil juridique.

## Données traitées

Google Space Cleaner analyse les métadonnées suivantes de votre compte Google,
avec votre autorisation explicite (OAuth) :
- **Google Drive** : nom, taille, type, dates, empreinte de contenu des fichiers
- **Google Photos** : uniquement les albums/médias que vous sélectionnez
  explicitement (Picker), ou que vous importez vous-même (export Takeout)
- **Gmail** : objet, expéditeur, taille des pièces jointes volumineuses

## Traitement des données

**Aucune donnée n'est envoyée à un serveur tiers.** Toute l'analyse (détection
de doublons, calcul d'espace récupérable) est effectuée localement sur votre
appareil. Les métadonnées mises en cache localement sont chiffrées
(SQLCipher, clé stockée dans l'Android Keystore).

## Actions de suppression

L'application ne supprime jamais définitivement un fichier ou un email : elle
utilise systématiquement la corbeille (rétention 30 jours), et affiche une
double confirmation avant toute action. Pour Google Photos, l'application ne
peut techniquement pas supprimer de contenu qu'elle n'a pas créé (restriction
imposée par Google) — elle formule uniquement des recommandations.

## Conservation et suppression des données

- Le cache local peut être effacé à tout moment (réglages de l'application ou désinstallation)
- Aucune donnée n'est conservée sur un serveur, il n'y a donc rien à supprimer côté serveur
- Les tokens d'authentification sont supprimés lors de la déconnexion

## Partage avec des tiers

Aucun. L'application n'intègre aucun SDK publicitaire ni analytique tiers.

## Contact

[à compléter : adresse email de contact du développeur/éditeur]

## Dernière mise à jour

[à compléter à la date de publication]
