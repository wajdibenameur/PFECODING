# Suivi PFE - Refactor Monitoring - 2026-04-20

## Demande traitée

- Consolider monitoring autour de `integration/* + SnapshotStore + MonitoringAggregationService`
- Isoler le legacy restant dans `depl`
- Retirer `MonitoringService`
- Unifier les clients ZKBio
- Corriger la sémantique metrics Observium/ZKBio et le rôle de `CAMERA`
- Supprimer les placeholders/utilitaires morts

## Résultat

- Flux actif recentré sur les services d'intégration.
- `CameraIntegrationService` introduit pour sortir la caméra de l'ancien orchestrateur transverse.
- `MonitoringStartup` devient le warmup principal pour toutes les sources.
- Legacy archivé dans `src/main/java/depl/replaced-classes/2026-04-20-monitoring-refactor/`.
- Tests unitaires/WebMvc repassés; test d'intégration global désactivé car dépendant d'une DB externe.

## Commandes de vérification exécutées

```powershell
mvn -q -DskipTests compile
mvn -q test
```

## Point d'attention

- Le workspace contient encore d'autres changements déjà présents avant ce tour de refactor; ils n'ont pas été revert.
