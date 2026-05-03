# Inventaire Complet des Packages

## 1. Package racine `tn.iteam`
### Role
Point d'entree de l'application et orchestration de warmup initial.

### Classes principales
- `PfeprojectApplication`
- `MonitoringStartup`

### Dependances majeures
- `integration`
- `service.support`
- `monitoring`

## 2. Couche d'exposition HTTP

### `tn.iteam.controller`
### Role
Expose les APIs REST de supervision, ticketing, compatibilite legacy, dashboard et camera.

### Classes
- `MonitoringController`
- `ZkBioController`
- `ZabbixProblemController`
- `ZabbixMetricsController`
- `ObserviumController`
- `DashboardController`
- `CameraController`
- `TicketController`

### Dependances
- `monitoring.service`
- `integration`
- `service`
- `mapper`
- `domain`

### Observation
`Fait confirme`: la coexistence entre `MonitoringController` et plusieurs controleurs de compatibilite traduit une phase de transition d'API.

### `tn.iteam.ml.controller`
### Role
Expose le service de prediction TorchScript.

### Classes
- `TorchScriptPredictionController`

## 3. Couche d'orchestration et d'integration

### `tn.iteam.integration`
### Role
Heberge les services d'integration par source, les abstractions communes et les orchestrateurs legers.

### Classes
- `AsyncIntegrationService`
- `IntegrationService`
- `IntegrationServiceRegistry`
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `ZkBioIntegrationOperations`
- `ZkBioRefreshOrchestrationService`
- `CameraIntegrationService`

### Dependances
- `adapter`
- `service`
- `monitoring.snapshot`
- `repository`
- `websocket`

### Evaluation
`Fait confirme`: `IntegrationServiceRegistry` reste un simple resolver et non un orchestrateur riche.

## 4. Couche d'acces aux systemes externes

### `tn.iteam.adapter`
Sous-packages:
- `adapter.zabbix`
- `adapter.observium`
- `adapter.zkbio`
- `adapter.camera`

### Role
Transformation des flux externes en objets applicatifs. Dans le cas Zabbix, l'adapter est maintenant une facade qui delegue aux collectors.

### Classes
- `ZabbixAdapter` (facade, delegue aux collectors)
- `ZabbixClient` (appels API Zabbix)
- `ZabbixHostCollector` (NOUVEAU: collection hosts)
- `ZabbixProblemCollector` (NOUVEAU: collection problems)
- `ZabbixMetricsCollector` (NOUVEAU: collection metrics/history)
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

### `tn.iteam.client`
### Role
Clients HTTP distincts pour Observium et ZKBio.

### Classes
- `ObserviumClientX`
- `ZkBioClientX`

### Observation
`Fait confirme`: le packaging n'est pas entierement homogene, car `ZabbixClient` reside dans `adapter.zabbix` alors que les autres clients sont dans `client`.

## 5. Couche de service applicatif

### `tn.iteam.service`
### Role
Expose les interfaces de service et certains services historiques.

### Classes
- `ZkBioServiceInterface`
- `ZkBioServiceImpl`
- `ZkBioPersistenceService`
- `ZabbixSyncService`
- `ZabbixProblemService`
- `ZabbixMetricsService`
- `ZabbixDataQualityService`
- `TicketService`
- `ObserviumPersistenceService`
- `MonitoredHostSnapshotService`
- `MonitoredHostPersistenceService`
- `SourceAvailabilityService`
- `ServiceStatusPersistenceService`
- `ObserviumSummaryService`
- `CameraInventoryService`
- `DashboardService`

### `tn.iteam.service.impl`
### Role
Implementations concretes de la plupart des interfaces applicatives.

### Classes
- `CameraInventoryServiceImpl`
- `MonitoredHostSnapshotServiceImpl`
- `MonitoredHostPersistenceServiceImpl`
- `DashboardServiceImpl`
- `ObserviumPersistenceServiceImpl`
- `ObserviumSummaryServiceImpl`
- `ServiceStatusPersistenceServiceImpl`
- `SourceAvailabilityServiceImpl`
- `TicketServiceImpl`
- `ZabbixMetricsServiceImpl`
- `ZkBioPersistenceServiceImpl`
- `ZabbixProblemServiceImpl`

### `tn.iteam.service.support`
### Role
Helpers de support transverses et services de delegation legers.

### Classes
- `MonitoringSnapshotPublicationService`
- `IntegrationExecutionHelper`
- `ZabbixProblemSanitizer`

### Observation
`Fait confirme`: `MonitoringSnapshotPublicationService` reste centre sur la publication. `IntegrationExecutionHelper` reste un helper de wrapping et de logs.

## 6. Couche monitoring unifiee

### `tn.iteam.monitoring`
### Role
Definit les types de source.

### Classes
- `MonitoringSourceType`

### `tn.iteam.monitoring.dto`
### Role
Contrats unifies renvoyes au frontend.

### Classes
- `UnifiedMonitoringResponse`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`
- `UnifiedMonitoringHostDTO`

### `tn.iteam.monitoring.service`
### Role
Agregation de snapshots et cache de lecture.

### Classes
- `MonitoringAggregationService`
- `MonitoringCacheService`

### `tn.iteam.monitoring.snapshot`
### Role
Stockage memoire des snapshots par dataset et source.

### Classes
- `SnapshotStore`
- `InMemorySnapshotStore`
- `StoredSnapshot`

### `tn.iteam.monitoring.provider`
### Role
Package reserve, actuellement vide.

### Observation
`Fait confirme`: package vide, candidat de nettoyage documentaire ou suppression.

## 7. Couche persistence

### `tn.iteam.domain`
### Role
Entites JPA et enveloppe reponse generique.

### Classes
- `BaseEntity`
- `ApiResponse`
- `MonitoredHost`
- `ServiceStatus`
- `ObserviumMetric`
- `ObserviumProblem`
- `ZabbixMetric`
- `ZabbixProblem`
- `ZkBioMetric`
- `ZkBioProblem`
- `Ticket`
- `Intervention`
- `User`
- `Role`

### `tn.iteam.repository`
### Role
Repositories Spring Data JPA.

### Classes
- `MonitoredHostRepository`
- `ServiceStatusRepository`
- `ObserviumMetricRepository`
- `ObserviumProblemRepository`
- `ZabbixMetricRepository`
- `ZabbixProblemRepository`
- `ZkBioMetricRepository`
- `ZkBioProblemRepository`
- `TicketRepository`
- `InterventionRepository`
- `UserRepository`
- `RoleRepository`

## 8. Couche DTO et mapping

### `tn.iteam.dto`
### Role
DTOs historiques, metier et compatibilite.

### Familles
- monitoring source-specific: `ZabbixMetricDTO`, `ZabbixProblemDTO`, `ObserviumMetricDTO`, `ObserviumProblemDTO`, `ZkBioMetricDTO`, `ZkBioProblemDTO`, `ZkBioAttendanceDTO`, `ServiceStatusDTO`, `SourceAvailabilityDTO`, `CameraDeviceDTO`
- dashboard: `DashboardOverviewDTO`, `DashboardPredictionDTO`, `DashboardAnomalyDTO`
- ticketing: `Ticket*DTO`
- erreurs: `ApiErrorResponse`

### `tn.iteam.mapper`
### Role
Transformations entre entites, DTOs source-specific et DTOs unifies.

### Classes
- `TicketMapper`
- `ServiceStatusMapper`
- `ObserviumMapper`
- `ObserviumMetricMapper`
- `ObserviumMonitoringMapper`
- `ZabbixMetricMapper`
- `ZabbixProblemMapper`
- `ZabbixMonitoringMapper`
- `ZkBioMapper`
- `ZkBioMetricMapper`
- `ZkBioAttendanceMapper`
- `ZkBioMonitoringMapper`
- `CategoryResolver`

## 9. Scheduling, WebSocket et configuration

### `tn.iteam.scheduler`
### Role
Rafraichissement periodique par source avec cooldown de reprise.

### Classes
- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`

### `tn.iteam.websocket`
### Role
Publication STOMP/WebSocket.

### Classes
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`

### `tn.iteam.config`
### Role
Configuration Spring, WebClient, Redis, WebSocket, bootstrap ticketing, logging resilience.

### Classes
- `AsyncConfig`
- `CorsConfig`
- `JpaAuditingConfig`
- `RedisOptionalConfiguration`
- `WebSocketConfig`
- `WebClientConfig`
- `TicketingBootstrapConfiguration`
- `ResilienceLoggingConfig`
- `AppRedisProperties`

## 10. Exceptions, utilitaires, ML et packages vides

### `tn.iteam.exception`
### Role
Modele d'exceptions metier et techniques centralise.

### Classes
- `IntegrationException`
- `IntegrationTimeoutException`
- `IntegrationUnavailableException`
- `IntegrationResponseException`
- `IntegrationDataUnavailableException`
- `TicketingException`
- `GlobalExceptionHandler`

### `tn.iteam.util`
### Role
Utilitaires transverses.

### Classes
- `IntegrationClientSupport`
- `MonitoringConstants`

### `tn.iteam.ml`
Sous-packages:
- `ml.config`
- `ml.controller`
- `ml.dto`
- `ml.service`

### Role
Prediction TorchScript et configuration associee.

### `tn.iteam.cache`, `tn.iteam.listener`, `tn.iteam.logging`
### Statut
Packages vides detectes statiquement.

### Evaluation
- confiance: elevee
- interpretation: residus de refactorings ou emplacements reserves
