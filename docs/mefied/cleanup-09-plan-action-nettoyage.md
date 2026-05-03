# Plan d'action nettoyage

## PHASE A - Nettoyage sur
- Supprimer les methodes mortes a forte confiance listees dans `cleanup-02-candidats-code-mort.md`.
- Supprimer les packages vides listes dans `cleanup-11-packages-et-sous-packages-vides.md`.
- Supprimer les methodes WebSocket directes devenues inutiles apres bascule snapshot.
- Supprimer les deux methodes de service Zabbix jamais appelees (`synchronizeAndGetPersisted...`).
- Compiler et lancer les tests apres chaque lot logique.

## PHASE B - Fusions / factorisations sures
- Extraire la persistence metrics commune de `ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl`.
- Extraire la base commune de snapshot/fallback des integration services, sans changer les signatures publiques.
- Extraire les helpers communs de mapping monitoring unifie (normalisation, timestamp, builder metric/probleme).

## PHASE C - Nettoyage de packaging / renommage
- Deplacer `ApiResponse` hors du package `domain`.
- Deplacer `MonitoringStartup` vers un package bootstrap/config.
- Deplacer `ZabbixSyncService` pres du module Zabbix.
- Rapatrier `ZkBioServiceImpl` dans `service.impl` et harmoniser la convention de nommage.
- Renommer `ObserviumClientX` et `ZkBioClientX` pour enlever le suffixe `X` et les rapprocher de leurs modules source.

## PHASE D - Refactors de risque moyen
- Decouper `ZabbixAdapter` et `ZabbixClient`.
- Decouper `ZkBioIntegrationService` en orchestration / refresh datasets / publication.
- Decouper `DashboardServiceImpl`.
- Simplifier certaines interfaces a faible valeur (`IntegrationService`, `ObserviumSummaryService`, `CameraInventoryService`) apres stabilisation du packaging.

## PHASE E - Verification manuelle / runtime necessaire
- Mesurer le trafic de `/api/observium/summary`, `/api/zabbix/metrics`, `/api/zabbix/active`, `/predict`, `/api/zkbio/problems`, `/api/zkbio/attendance/range`, `/api/zkbio/users`.
- Valider avec les parties prenantes si le modele `Permission` / `RoleName` doit vivre avant de le nettoyer.
- Verifier qu'aucun consommateur externe ne depend des endpoints legacy avant suppression.
