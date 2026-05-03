# Duplication, Redondance et Dette

## Doublons backend confirmes
### 1. Pattern d'integration quasi duplique
Constate dans:

- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

Elements repetes:
- `refresh`, `refreshHosts`, `refreshProblems`, `refreshMetrics`
- `subscribeSafely(...)`
- `safeMessage(Exception|Throwable)`
- `tryPersistToDatabase(...)`
- conversion `Throwable -> Exception`
- logique `live -> snapshot -> persisted -> empty` ou `live -> snapshot -> empty`

Impact:
- cout de maintenance eleve
- risque d'ecarts subtils de comportement
- tres forte probabilite de regressions asymetriques

### 2. Signatures backend repetitives
Les repetitions les plus visibles sont:

- `safeMessage(Throwable throwable)`: `10` occurrences
- `fetchProblems()`: `7`
- `normalizeText(String value)`: `7`
- `refreshHosts()`: `6`
- `fetchAll()`: `6`
- `refreshAsync()`: `5`
- `getProblems()`: `5`
- `getMetrics()`: `5`
- `getHosts()`: `5`
- `normalizeIp(String value)`: `5`

### 3. Signatures qui portent vraisemblablement la meme fonction
- `normalizeText(String value)` dans `ObserviumMonitoringMapper`, `ZabbixMonitoringMapper`, `ZkBioMonitoringMapper`, `ZabbixSyncService`, `InMemoryZabbixHostSyncService`, `MonitoredHostPersistenceServiceImpl`, `MonitoredHostSnapshotServiceImpl`
- `normalizeIp(String value)` dans les memes familles de classes
- `formatTimestamp(Long epoch)` dans `ObserviumMapper`, `ObserviumMonitoringMapper`, `ZkBioMapper`, `ZkBioMonitoringMapper`
- `buildHostMap(JsonNode hosts)` dans `ZabbixAdapter`, `ZabbixHostCollector`, `ZabbixProblemCollector`
- `extractMainIp(JsonNode hostNode)` et `extractMainPort(JsonNode hostNode)` dans plusieurs classes Zabbix
- `mapCircuitBreakerException(...)` dans `ZabbixClient`, `ObserviumClientX`, `ZkBioClientX`

## Redondances de classes ou de responsabilites
### Implementations "in memory" paralleles
- `InMemoryDashboardService` vs `DashboardServiceImpl`
- `InMemoryCameraInventoryService` vs `CameraInventoryServiceImpl`
- `InMemoryMonitoredHostPersistenceService` vs `MonitoredHostPersistenceServiceImpl`
- `InMemoryZabbixHostSyncService` vs `ZabbixSyncService`

Constat:
- ces couples ne sont pas des doublons morts a eux seuls
- ils font la meme famille de fonction et doivent etre documentes comme variantes runtime ou offline
- plusieurs documents existants les sous-expliquent

### Endpoints de compatibilite a fonction recouverte
- `ObserviumController`
- `ZabbixMetricsController`
- `ZabbixProblemController`

Recouvrement principal:
- les usages frontend actuels passent deja par `MonitoringController` et les reponses unifiees

### ZKBio a double orchestration
- `ZkBioRefreshOrchestrationService`
- `ZkBioIntegrationService.refreshAllAndPublishAsync()`

Constat:
- les deux expriment une orchestration composee
- la seconde ajoute la publication WebSocket
- la frontiere fonctionnelle existe, mais la responsabilite est proche et doit etre clarifiee

## Redondances frontend confirmees
### 1. Deux gros stores qui reimplementent les memes mecanismes
- `MonitoringStore`
- `ZabbixWorkspaceStore`

Redondances observees:
- `loadSnapshot()`
- `bindRealtime()`
- `triggerCollection()`
- `mergeProblems(...)`
- `mergeMetrics(...)`
- `mergeSourceAvailability(...)`
- `scheduleSnapshotRefresh()`
- `clearScheduledRefreshes()`

### 2. Logique de page encore autonome au lieu d'un store partage
`MonitoringObserviumPageComponent` embarque encore:

- chargement snapshot
- binding temps reel
- merge de problemes
- merge de metriques
- gestion des erreurs
- delais de rafraichissement

Cette logique recoupe partiellement `MonitoringStore` et aurait interet a etre extraite dans un store source-specific comme Zabbix.

### 3. Duplication de presentation
- formatage de labels metriques dans plusieurs composants/stores
- lecture des metadonnees `freshness` / `coverage` repetee
- logique de timeout `1500` / `5000` repetee

## Hotspots prioritaires
### Priorite 1
- base abstraite ou helper commun pour les services d'integration
- helper commun de normalisation `text/ip/timestamp`
- converger sur une seule strategie de fallback documentee

### Priorite 2
- factoriser le workflow temps reel / snapshot des stores Angular
- sortir `MonitoringObserviumPageComponent` de la logique de donnees lourde

### Priorite 3
- clarifier les variantes `InMemory*` et les isoler comme mode offline ou profil de secours
