# Classes importantes du projet par couche

Ce document recense les classes principales encore actives dans le projet. Il privilegie l'architecture actuelle et signale explicitement les couches de compatibilite ou les reliquats archives.

## 1. Controllers

### `tn.iteam.controller.MonitoringController`
- Role : point d'entree REST principal du monitoring unifie.
- Dependances principales : `MonitoringAggregationService`, `SourceAvailabilityService`, `IntegrationServiceRegistry`, `ZkBioIntegrationOperations`, `MonitoringSnapshotPublicationService`.

### `tn.iteam.controller.DashboardController`
- Role : expose les endpoints `/dashboard/overview`, `/dashboard/predictions`, `/dashboard/anomalies`.
- Dependances principales : `DashboardService`.

### `tn.iteam.controller.ZkBioController`
- Role : expose les endpoints metier ZKBio (`status`, `devices`, `attendance`, `users`, `problems`) et la collecte manuelle ZKBio.
- Dependances principales : `ZkBioServiceInterface`, `ZkBioIntegrationOperations`.

### `tn.iteam.controller.ZabbixProblemController`
- Role : endpoint de compatibilite temporaire pour `/api/zabbix/active`, conserve pour anciens consommateurs et non pour le frontend Angular courant.
- Dependances principales : `MonitoringAggregationService`.

### `tn.iteam.controller.ZabbixMetricsController`
- Role : endpoint de compatibilite temporaire pour `/api/zabbix/metrics`, conserve pour anciens consommateurs et non pour le frontend Angular courant.
- Dependances principales : `MonitoringAggregationService`.

### `tn.iteam.controller.ObserviumController`
- Role : endpoint de compatibilite temporaire pour `/api/observium/summary`.
- Dependances principales : `ObserviumSummaryService`.

## 2. Startup et schedulers

### `tn.iteam.MonitoringStartup`
- Role : warmup initial unique du monitoring.
- Dependances principales : `IntegrationServiceRegistry`, `ZkBioIntegrationOperations`, `MonitoringSnapshotPublicationService`.

### `tn.iteam.scheduler.ZabbixScheduler`
- Role : rafraichissement periodique des problemes et metriques Zabbix.
- Dependances principales : `IntegrationServiceRegistry`, `MonitoringSnapshotPublicationService`.

### `tn.iteam.scheduler.ObserviumScheduler`
- Role : rafraichissement periodique des problemes et metriques Observium.
- Dependances principales : `IntegrationServiceRegistry`, `MonitoringSnapshotPublicationService`.

### `tn.iteam.scheduler.ObserviumHostsScheduler`
- Role : rafraichissement periodique des hosts Observium.
- Dependances principales : `IntegrationServiceRegistry`.

### `tn.iteam.scheduler.ZkBioScheduler`
- Role : rafraichissement periodique des problemes, metriques, attendance, devices et status ZKBio.
- Dependances principales : `ZkBioIntegrationOperations`, `MonitoringSnapshotPublicationService`.

## 3. Couche integration

### `tn.iteam.integration.IntegrationService`
- Role : contrat commun des integrations de monitoring.
- Methodes principales : `refresh`, `refreshHosts`, `refreshProblems`, `refreshMetrics`, `refreshAttendance`.

### `tn.iteam.integration.AsyncIntegrationService`
- Role : extension asynchrone du contrat d'integration.
- Methodes principales : `refreshAsync`, `refreshHostsAsync`, `refreshProblemsAsync`, `refreshMetricsAsync`.

### `tn.iteam.integration.ZkBioIntegrationOperations`
- Role : contrat specialise pour les operations ZKBio supplementaires.
- Methodes principales : `refreshAttendanceAsync`, `refreshAllAndPublishAsync`.

### `tn.iteam.integration.IntegrationServiceRegistry`
- Role : registre de resolution des integrations par `MonitoringSourceType`.
- Dependances principales : `List<AsyncIntegrationService>`.
- Note : ne doit contenir ni logique metier, ni logique de persistance, ni logique d'orchestration.

### `tn.iteam.integration.ZabbixIntegrationService`
- Role : collecte Zabbix, persistance metier et mise a jour des snapshots `hosts`, `problems`, `metrics`.
- Dependances principales : `ZabbixAdapter`, `ZabbixMonitoringMapper`, `ServiceStatusPersistenceService`, `ZabbixProblemService`, `ZabbixMetricsService`, `SnapshotStore`, `SourceAvailabilityService`.

### `tn.iteam.integration.ObserviumIntegrationService`
- Role : collecte Observium, persistance des problemes et metriques, alimentation des snapshots unifies.
- Dependances principales : `ObserviumAdapter`, `ObserviumMonitoringMapper`, `ObserviumPersistenceService`, `ServiceStatusPersistenceService`, `SnapshotStore`, `SourceAvailabilityService`.

### `tn.iteam.integration.ZkBioIntegrationService`
- Role : collecte monitoring et jeux de donnees metier ZKBio, puis publication des snapshots quand necessaire.
- Dependances principales : `ZkBioServiceInterface`, `ZkBioAdapter`, `ZkBioMonitoringMapper`, `ZkBioPersistenceService`, `ServiceStatusPersistenceService`, `SnapshotStore`, `SourceAvailabilityService`, publishers WebSocket.

### `tn.iteam.integration.CameraIntegrationService`
- Role : collecte la partie Camera et alimente uniquement le dataset `hosts`.
- Dependances principales : `CameraAdapter`, `ServiceStatusPersistenceService`, `SnapshotStore`.

## 4. Aggregation et snapshots

### `tn.iteam.monitoring.snapshot.SnapshotStore`
- Role : interface de stockage des snapshots techniques par dataset et source.

### `tn.iteam.monitoring.snapshot.InMemorySnapshotStore`
- Role : implementation en memoire du stockage des snapshots.
- Note : reste le fallback garanti et l'implementation active par defaut.

### `tn.iteam.monitoring.snapshot.StoredSnapshot`
- Role : representation d'un snapshot avec donnees, etat degrade, freshness et horodatage.

### `tn.iteam.monitoring.service.MonitoringCacheService`
- Role : lit et agrege les snapshots par dataset.
- Dependances principales : `SnapshotStore`.

### `tn.iteam.monitoring.service.MonitoringAggregationService`
- Role : transforme les `FetchResult` en `UnifiedMonitoringResponse`.
- Dependances principales : `MonitoringCacheService`.

### `tn.iteam.monitoring.MonitoringSourceType`
- Role : enum des sources et de leurs capacites.
- Donnees principales : support des datasets et `metricsCoverage`.

## 5. Adapters

### `tn.iteam.adapter.zabbix.ZabbixAdapter`
- Role : traduit les reponses Zabbix en DTO applicatifs et alimente les services Zabbix.
- Dependances principales : `ZabbixClient`, mappers Zabbix.

### `tn.iteam.adapter.observium.ObserviumAdapter`
- Role : traduit les reponses Observium en `ServiceStatusDTO`, `ObserviumProblemDTO` et `ObserviumMetricDTO`.
- Dependances principales : `ObserviumClientX`, `ObserviumMapper`.

### `tn.iteam.adapter.zkbio.ZkBioAdapter`
- Role : traduit les reponses ZKBio en DTO metier et monitoring.
- Dependances principales : `ZkBioClientX`, `ZkBioMapper`.

### `tn.iteam.adapter.camera.CameraAdapter`
- Role : collecte l'etat des cameras sur le reseau cible.

## 6. Clients

### `tn.iteam.adapter.zabbix.ZabbixClient`
- Role : client technique Zabbix base sur `WebClient` et Resilience4j.
- Dependances principales : `WebClient`, `ObjectMapper`.

### `tn.iteam.client.ObserviumClientX`
- Role : client technique Observium base sur `WebClient` et Resilience4j.
- Dependances principales : `WebClient`, `ObjectMapper`.

### `tn.iteam.client.ZkBioClientX`
- Role : client technique ZKBio base sur `WebClient` dedie et Resilience4j.
- Dependances principales : `WebClient`, `ObjectMapper`.

## 7. Services metier et persistance

### `tn.iteam.service.SourceAvailabilityService`
- Role : maintient l'etat des sources (`AVAILABLE`, `DEGRADED`, `UNAVAILABLE`).

### `tn.iteam.service.impl.SourceAvailabilityServiceImpl`
- Role : implementation concrete et publication temps reel de l'etat des sources.
- Dependances principales : `MonitoringWebSocketPublisher`.

### `tn.iteam.service.ServiceStatusPersistenceService`
- Role : persistance des `ServiceStatusDTO` vers les entites associees.

### `tn.iteam.service.ObserviumPersistenceService`
- Role : persistance des problemes et metriques Observium.

### `tn.iteam.service.ZkBioPersistenceService`
- Role : persistance des problemes et metriques ZKBio.

### `tn.iteam.service.ZabbixProblemService`
- Role : synchronisation et lecture des problemes Zabbix persistants.

### `tn.iteam.service.ZabbixMetricsService`
- Role : synchronisation et lecture des metriques Zabbix persistantes.

### `tn.iteam.service.ZkBioServiceInterface`
- Role : contrat metier ZKBio pour `status`, `devices`, `attendance`, `users`, `problems`.

### `tn.iteam.service.ZkBioServiceImpl`
- Role : implementation metier ZKBio hors aggregation unifiee.
- Dependances principales : `ZkBioAdapter`, `ZkBioClientX`, mappers et repositories ZKBio.

### `tn.iteam.service.DashboardService`
- Role : construit la vue dashboard.

### `tn.iteam.service.impl.ObserviumSummaryServiceImpl`
- Role : fournit le resume Observium a partir du flux unifie.
- Dependances principales : `MonitoringAggregationService`.

## 8. Repositories

Repositories les plus importants :

- `MonitoredHostRepository`
- `ServiceStatusRepository`
- `ZabbixMetricRepository`
- `ZabbixProblemRepository`
- `ObserviumMetricRepository`
- `ObserviumProblemRepository`
- `ZkBioMetricRepository`
- `ZkBioProblemRepository`
- `TicketRepository`
- `UserRepository`

## 9. WebSocket

### `tn.iteam.websocket.MonitoringWebSocketPublisher`
- Role : publie les problemes, metriques et etats de source du monitoring unifie.
- Dependances principales : `SimpMessagingTemplate`, `SnapshotStore`.

### `tn.iteam.websocket.ZkBioWebSocketPublisher`
- Role : publie les jeux de donnees ZKBio specifiques.
- Dependances principales : `SimpMessagingTemplate`, `SnapshotStore`.

### `tn.iteam.service.support.MonitoringSnapshotPublicationService`
- Role : centralise les appels de publication WebSocket monitoring et ZKBio.
- Dependances principales : `MonitoringWebSocketPublisher`, `ZkBioWebSocketPublisher`.
- Note : reste un delegateur de publication sans logique metier.

## 10. DTO et reponses principales

### DTO monitoring unifies

- `UnifiedMonitoringHostDTO`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`
- `UnifiedMonitoringResponse`

### DTO metier importants

- `ServiceStatusDTO`
- `SourceAvailabilityDTO`
- `ObserviumProblemDTO`
- `ObserviumMetricDTO`
- `ZabbixProblemDTO`
- `ZabbixMetricDTO`
- `ZkBioProblemDTO`
- `ZkBioMetricDTO`
- `ZkBioAttendanceDTO`

## 11. Classes ou couches retirees du flux principal

Les elements suivants ne doivent plus etre documentes comme classes pivot du systeme :

- `MonitoringServiceImpl`
- `MonitoringProvider`
- `tn.iteam.monitoring.provider.*`
- `IntegrationCacheService`
- `RedisIntegrationCacheService`
- `ZabbixStartupListener`
- anciens `*MonitoringService*`

Ils ont ete retires du chemin principal et archives dans `depl/`.

## 12. Configuration Redis optionnelle

### `tn.iteam.config.AppRedisProperties`
- Role : centralise les proprietes Redis optionnelles (`app.redis.*`).

### `tn.iteam.config.RedisOptionalConfiguration`
- Role : cree les beans Redis uniquement si `app.redis.enabled=true`.
- Dependances principales : `AppRedisProperties`, `RedisConnectionFactory`, `StringRedisTemplate`.

### Classes Redis non actives dans le metier courant

- aucune integration active n'injecte directement `RedisTemplate`, `StringRedisTemplate` ou `RedisConnectionFactory` ;
- aucun repository Redis actif n'est utilise par le flux monitoring courant.

## 13. Design patterns effectivement utilises

- Adapter :
  - `ZabbixAdapter`, `ObserviumAdapter`, `ZkBioAdapter`, `CameraAdapter`
- Strategy :
  - `IntegrationService` + implementations par source
- Facade :
  - `MonitoringController`
  - `ObserviumSummaryServiceImpl` joue aussi une petite facade de compatibilite sur le flux unifie
  - `MonitoringSnapshotPublicationService` joue une facade technique de publication
- Repository :
  - l'ensemble des repositories Spring Data JPA
- Observer / Publisher-Subscriber :
  - `MonitoringWebSocketPublisher`, `ZkBioWebSocketPublisher`
- Store :
  - `SnapshotStore`, `InMemorySnapshotStore`, `MonitoringStore` cote frontend
