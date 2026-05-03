# Elements UML exploitables pour le projet

Ce document prepare des elements UML alignes sur l'architecture actuelle du projet.

Note de cadrage importante :

- Redis ne doit plus apparaitre comme coeur du workflow monitoring actif ;
- si un element Redis est mentionne, il doit etre annote comme optionnel ou archive ;
- `InMemorySnapshotStore` reste le stockage technique de reference a faire figurer dans les diagrammes.

## 1. Diagramme de classes

### 1.1 Classes principales a representer

#### Presentation REST

- `MonitoringController`
- `DashboardController`
- `ZkBioController`
- `ZabbixProblemController`
- `ZabbixMetricsController`
- `ObserviumController`
- `TicketController`

#### Startup et orchestration

- `MonitoringStartup`
- `IntegrationServiceRegistry`
- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`

#### Integration

- `IntegrationService`
- `AsyncIntegrationService`
- `ZkBioIntegrationOperations`
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

#### Agregation et snapshots

- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore`
- `InMemorySnapshotStore`
- `StoredSnapshot`
- `MonitoringSourceType`

#### Adapters

- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

#### Clients

- `ZabbixClient`
- `ObserviumClientX`
- `ZkBioClientX`
- `AppRedisProperties` uniquement si un diagramme de configuration optionnelle est souhaite
- `RedisOptionalConfiguration` uniquement comme configuration conditionnelle, pas comme coeur metier

#### Persistance

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

#### Services metier

- `SourceAvailabilityService`
- `SourceAvailabilityServiceImpl`
- `ServiceStatusPersistenceService`
- `ObserviumPersistenceService`
- `ZkBioPersistenceService`
- `ZabbixProblemService`
- `ZabbixMetricsService`
- `ZkBioServiceInterface`
- `ZkBioServiceImpl`
- `ObserviumSummaryService`
- `ObserviumSummaryServiceImpl`
- `DashboardService`
- `DashboardServiceImpl`

#### WebSocket

- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `MonitoringSnapshotPublicationService`

#### DTO principaux

- `UnifiedMonitoringHostDTO`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`
- `UnifiedMonitoringResponse`
- `SourceAvailabilityDTO`
- `ServiceStatusDTO`
- `ZabbixProblemDTO`
- `ZabbixMetricDTO`
- `ObserviumProblemDTO`
- `ObserviumMetricDTO`
- `ZkBioProblemDTO`
- `ZkBioMetricDTO`
- `ZkBioAttendanceDTO`

### 1.2 Relations principales a representer

#### Controllers vers services

- `MonitoringController --> MonitoringAggregationService`
- `MonitoringController --> SourceAvailabilityService`
- `MonitoringController --> IntegrationServiceRegistry`
- `MonitoringController --> ZkBioIntegrationOperations`
- `MonitoringController --> MonitoringSnapshotPublicationService`
- `ZkBioController --> ZkBioServiceInterface`
- `ZkBioController --> ZkBioIntegrationOperations`
- `ZabbixProblemController --> MonitoringAggregationService`
- `ZabbixMetricsController --> MonitoringAggregationService`
- `ObserviumController --> ObserviumSummaryService`
- `DashboardController --> DashboardService`

#### Startup et schedulers

- `MonitoringStartup --> IntegrationServiceRegistry`
- `MonitoringStartup --> ZkBioIntegrationOperations`
- `MonitoringStartup --> MonitoringSnapshotPublicationService`

- `ZabbixScheduler --> IntegrationServiceRegistry`
- `ZabbixScheduler --> MonitoringSnapshotPublicationService`
- `ObserviumScheduler --> IntegrationServiceRegistry`
- `ObserviumScheduler --> MonitoringSnapshotPublicationService`
- `ObserviumHostsScheduler --> IntegrationServiceRegistry`
- `ZkBioScheduler --> ZkBioIntegrationOperations`
- `ZkBioScheduler --> MonitoringSnapshotPublicationService`

#### Integration vers adapter / persistance / snapshot

- `ZabbixIntegrationService --> ZabbixAdapter`
- `ZabbixIntegrationService --> ZabbixProblemService`
- `ZabbixIntegrationService --> ZabbixMetricsService`
- `ZabbixIntegrationService --> ServiceStatusPersistenceService`
- `ZabbixIntegrationService --> SnapshotStore`
- `ZabbixIntegrationService --> SourceAvailabilityService`

- `ObserviumIntegrationService --> ObserviumAdapter`
- `ObserviumIntegrationService --> ObserviumPersistenceService`
- `ObserviumIntegrationService --> ServiceStatusPersistenceService`
- `ObserviumIntegrationService --> SnapshotStore`
- `ObserviumIntegrationService --> SourceAvailabilityService`

- `ZkBioIntegrationService --> ZkBioAdapter`
- `ZkBioIntegrationService --> ZkBioServiceInterface`
- `ZkBioIntegrationService --> ZkBioPersistenceService`
- `ZkBioIntegrationService --> ServiceStatusPersistenceService`
- `ZkBioIntegrationService --> SnapshotStore`
- `ZkBioIntegrationService --> SourceAvailabilityService`

- `CameraIntegrationService --> CameraAdapter`
- `CameraIntegrationService --> ServiceStatusPersistenceService`
- `CameraIntegrationService --> SnapshotStore`

#### Agregation

- `MonitoringAggregationService --> MonitoringCacheService`
- `MonitoringCacheService --> SnapshotStore`
- `MonitoringAggregationService --> MonitoringSourceType`

#### Adapters vers clients

- `ZabbixAdapter --> ZabbixClient`
- `ObserviumAdapter --> ObserviumClientX`
- `ZkBioAdapter --> ZkBioClientX`

#### WebSocket

- `SourceAvailabilityServiceImpl --> MonitoringWebSocketPublisher`
- `MonitoringWebSocketPublisher --> SnapshotStore`
- `MonitoringWebSocketPublisher --> SimpMessagingTemplate`
- `ZkBioWebSocketPublisher --> SnapshotStore`
- `ZkBioWebSocketPublisher --> SimpMessagingTemplate`
- `MonitoringSnapshotPublicationService --> MonitoringWebSocketPublisher`
- `MonitoringSnapshotPublicationService --> ZkBioWebSocketPublisher`

### 1.3 Vision simplifiee du diagramme de classes

Une vue UML synthese peut etre decoupee en cinq blocs :

1. presentation REST ;
2. startup et schedulers ;
3. integration par source ;
4. snapshots et agregation ;
5. persistance et diffusion temps reel.

## 2. Diagramme de cas d'utilisation

### 2.1 Acteurs principaux

- Administrateur / operateur
- Systeme Zabbix
- Systeme Observium
- Systeme ZKBio
- Systeme Camera / reseau local
- Base MySQL

### 2.2 Cas d'utilisation principaux

#### Acteur humain

- Consulter les hosts unifies
- Consulter les problems unifies
- Consulter les metrics unifiees
- Consulter la sante des sources
- Declencher une collecte globale
- Declencher une collecte par source
- Consulter les donnees ZKBio specifiques
- Consulter le dashboard
- Recevoir les mises a jour temps reel

#### Acteurs systemes externes

- Fournir les donnees Zabbix
- Fournir les donnees Observium
- Fournir les donnees ZKBio
- Fournir les hosts Camera

### 2.3 Cas d'utilisation de compatibilite

- Consommateur legacy : `/api/zabbix/active`
- Consommateur legacy : `/api/zabbix/metrics`
- Consommateur legacy ou externe : `/api/observium/summary`

Ces cas existent encore, mais ne representent plus le coeur de l'architecture.

Le frontend Angular courant n'utilise plus ces APIs ; elles doivent donc etre annotees comme compatibilite legacy ou consommation externe.

## 3. Diagrammes de sequence

### 3.1 Sequence : warmup initial

Participants :

- Spring Boot
- `MonitoringStartup`
- services d'integration
- `SnapshotStore`
- publishers WebSocket

Sequence textuelle :

1. `Spring -> MonitoringStartup : @EventListener(ApplicationReadyEvent)`
2. `MonitoringStartup -> IntegrationServiceRegistry : getRequired(source)`
3. `MonitoringStartup -> ZabbixIntegrationService : refreshAsync()`
4. `MonitoringStartup -> ObserviumIntegrationService : refreshAsync()`
5. `MonitoringStartup -> ZkBioIntegrationService : refreshAsync()`
6. `MonitoringStartup -> ZkBioIntegrationService : refreshAttendanceAsync()`
7. `MonitoringStartup -> CameraIntegrationService : refreshAsync()`
8. `IntegrationService -> SnapshotStore : save(dataset, source, snapshot)`
9. `MonitoringStartup -> MonitoringSnapshotPublicationService : publish...`

### 3.2 Sequence : lecture monitoring unifiee

Participants :

- Frontend
- `MonitoringController`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore`

Sequence textuelle :

1. `Frontend -> MonitoringController : GET /api/monitoring/{dataset}`
2. `MonitoringController -> MonitoringAggregationService : getHosts/getProblems/getMetrics`
3. `MonitoringAggregationService -> MonitoringCacheService : get...`
4. `MonitoringCacheService -> SnapshotStore : get(dataset, source)`
5. `SnapshotStore --> MonitoringCacheService : StoredSnapshot`
6. `MonitoringCacheService --> MonitoringAggregationService : FetchResult`
7. `MonitoringAggregationService --> MonitoringController : UnifiedMonitoringResponse`
8. `MonitoringController --> Frontend : JSON`

### 3.3 Sequence : collecte manuelle

Participants :

- Frontend
- controller
- service d'integration
- adapter
- client
- API externe
- repository
- `SnapshotStore`

Sequence textuelle :

1. `Frontend -> Controller : POST collect`
2. `Controller -> IntegrationService : refresh() / refreshAllAndPublish()`
3. `IntegrationService -> Adapter : fetch...`
4. `Adapter -> Client : appel technique`
5. `Client -> API externe : HTTP`
6. `API externe --> Client : reponse`
7. `Adapter --> IntegrationService : DTO`
8. `IntegrationService -> Repository : save...`
9. `IntegrationService -> SnapshotStore : save(...)`
10. `Controller --> Frontend : ApiResponse`

### 3.4 Sequence : refresh periodique

Participants :

- scheduler
- service d'integration
- `SnapshotStore`
- publisher WebSocket
- frontend

Sequence textuelle :

1. `Scheduler -> IntegrationService : refresh dataset`
2. `IntegrationService -> SnapshotStore : save updated snapshot`
3. `Scheduler -> WebSocketPublisher : publish from snapshot`
4. `WebSocketPublisher --> Frontend : topic update`

### 3.5 Sequence : flux ZKBio metier

Participants :

- Frontend
- `ZkBioController`
- `ZkBioServiceInterface`
- `ZkBioServiceImpl`
- `ZkBioAdapter`
- `ZkBioClientX`

Sequence textuelle :

1. `Frontend -> ZkBioController : GET /api/zkbio/{resource}`
2. `ZkBioController -> ZkBioServiceInterface : fetch...`
3. `ZkBioServiceImpl -> ZkBioAdapter : fetch...`
4. `ZkBioAdapter -> ZkBioClientX : call`
5. `ZkBioClientX --> ZkBioAdapter : JsonNode`
6. `ZkBioAdapter --> ZkBioServiceImpl : DTO`
7. `ZkBioServiceImpl --> ZkBioController : DTO list`
8. `ZkBioController --> Frontend : JSON`

## 4. Conseils UML pour le rapport

### 4.1 Diagramme de classes

Mettre l'accent sur les relations suivantes :

- `Controller -> Integration/Aggregation`
- `Integration -> Adapter -> Client`
- `Integration -> SnapshotStore`
- `Aggregation -> SnapshotStore`
- `Scheduler/Startup -> Integration`
- `Publisher -> SnapshotStore`

### 4.2 Diagrammes de sequence prioritaires

Les sequences les plus representatives pour le rapport sont :

- warmup initial avec `MonitoringStartup`
- lecture monitoring unifiee
- collecte manuelle
- refresh periodique
- flux ZKBio metier

## 5. Classes a ne plus mettre au centre des diagrammes

Pour rester fidele a l'etat reel du code, il ne faut plus construire les diagrammes autour de :

- `MonitoringServiceImpl`
- `MonitoringProvider`
- `ZabbixStartupListener`
- `IntegrationCacheService`
- `RedisIntegrationCacheService`

Ces elements appartiennent a l'ancien flux ou a des archives `depl/`.

## 6. Notes UML sur Redis optionnel

Si Redis doit etre represente dans le rapport, le diagramme doit montrer :

- `InMemorySnapshotStore` comme chemin principal ;
- Redis comme extension conditionnelle ;
- aucune fleche directe `Controller -> Redis` ;
- aucune fleche directe `Scheduler -> Redis` ;
- aucune fleche directe `Publisher -> Redis`.

Le bon message architectural est :

- `Integration -> SnapshotStore`
- `Aggregation -> SnapshotStore`
- `Publisher -> SnapshotStore`
- et non pas un monitoring central construit autour de Redis.
