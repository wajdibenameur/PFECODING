# Éléments UML exploitables pour le projet

Ce document prépare les éléments nécessaires à la réalisation de diagrammes UML pour le rapport PFE. Il ne contient pas d’images, mais une structuration directement exploitable pour produire :

- un diagramme de classes ;
- un diagramme de cas d’utilisation ;
- plusieurs diagrammes de séquence.

Les éléments proposés sont fondés sur les classes réellement présentes dans le projet.

## 1. Diagramme de classes

### 1.1. Classes principales à représenter

#### Couche contrôle

- `MonitoringController`
- `ZkBioController`
- `ZabbixMetricsController`
- `ZabbixProblemController`
- `ObserviumController`
- `DashboardController`
- `TicketController`

#### Couche service

- `MonitoringServiceImpl`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SourceAvailabilityServiceImpl`
- `ZabbixLiveSynchronizationServiceImpl`
- `ZabbixMetricsServiceImpl`
- `ZabbixProblemServiceImpl`
- `ZkBioServiceInterface`
- `ZkBioServiceImpl`
- `DashboardServiceImpl`
- `ObserviumSummaryServiceImpl`
- `TicketServiceImpl`
- `IntegrationExecutionHelper`

#### Couche provider

- `ZabbixMonitoringProvider`
- `ObserviumMonitoringProvider`
- `ZkBioMonitoringProvider`
- `MonitoringProvider` (interface)

#### Couche adapter

- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

#### Couche client

- `ZabbixClient`
- `ObserviumClientX`
- `ZkBioClient`

#### Couche persistance

- `MonitoredHostRepository`
- `ServiceStatusRepository`
- `ZabbixMetricRepository`
- `ZabbixProblemRepository`
- `ObserviumProblemRepository`
- `ZkBioProblemRepository`
- `TicketRepository`
- `UserRepository`

#### Entités principales

- `MonitoredHost`
- `ServiceStatus`
- `ZabbixMetric`
- `ZabbixProblem`
- `ObserviumProblem`
- `ZkBioProblem`
- `Ticket`
- `User`
- `Role`
- `Notification`
- `Intervention`

#### DTO / réponses unifiées

- `ServiceStatusDTO`
- `SourceAvailabilityDTO`
- `UnifiedMonitoringHostDTO`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`
- `UnifiedMonitoringResponse`
- `ApiResponse`

#### Couche planification / événements

- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`
- `ZabbixStartupListener`
- `ZabbixMetricsStartupListener`
- `ZabbixProblemStartupListener`

#### Temps réel / cache / config

- `MonitoringWebSocketPublisher`
- `ZabbixWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `IntegrationCacheService`
- `RedisIntegrationCacheService`
- `WebClientConfig`
- `RestTemplateConfig`
- `RedisCacheConfig`

### 1.2. Relations principales à représenter

#### Relations controller -> service

- `MonitoringController --> MonitoringService`
- `MonitoringController --> MonitoringAggregationService`
- `MonitoringController --> SourceAvailabilityService`
- `ZkBioController --> ZkBioServiceInterface`
- `ZabbixMetricsController --> ZabbixMetricsService`
- `ZabbixProblemController --> ZabbixProblemService`
- `DashboardController --> DashboardService`
- `TicketController --> TicketService`

#### Relations service -> adapter / repository

- `MonitoringServiceImpl --> ZabbixAdapter`
- `MonitoringServiceImpl --> ObserviumAdapter`
- `MonitoringServiceImpl --> ZkBioAdapter`
- `MonitoringServiceImpl --> CameraAdapter`
- `MonitoringServiceImpl --> ServiceStatusRepository`
- `MonitoringServiceImpl --> SourceAvailabilityService`
- `MonitoringServiceImpl --> IntegrationExecutionHelper`

- `MonitoringAggregationService --> MonitoringCacheService`
- `MonitoringCacheService --> MonitoringProvider`
- `MonitoringCacheService --> SourceAvailabilityService`

- `ZabbixMetricsServiceImpl --> ZabbixMetricRepository`
- `ZabbixProblemServiceImpl --> ZabbixProblemRepository`
- `ZkBioServiceImpl --> ZkBioAdapter`

#### Relations provider -> service / adapter / repository (PATTERN UNIFIÉ)

- `ZabbixMonitoringProvider --> ZabbixMonitoringService`
- `ZabbixMonitoringService --> ZabbixAdapter`
- `ZabbixAdapter --> ZabbixMonitoringMapper`
- `ObserviumMonitoringProvider --> ObserviumMonitoringService`
- `ObserviumMonitoringService --> ObserviumAdapter`
- `ObserviumAdapter --> ObserviumMonitoringMapper`
- `ZkBioMonitoringProvider --> ZkBioMonitoringService`
- `ZkBioMonitoringService --> ZkBioAdapter`
- `ZkBioAdapter --> ZkBioMonitoringMapper`

#### Relations adapter -> client / cache / repository / websocket

- `ZabbixAdapter --> ZabbixClient`
- `ZabbixAdapter --> IntegrationCacheService`
- `ZabbixAdapter --> SourceAvailabilityService`

- `ObserviumAdapter --> ObserviumClientX`
- `ObserviumAdapter --> ObserviumMapper`
- `ObserviumAdapter --> ObserviumProblemRepository`
- `ObserviumAdapter --> MonitoringWebSocketPublisher`
- `ObserviumAdapter --> IntegrationCacheService`
- `ObserviumAdapter --> SourceAvailabilityService`

- `ZkBioAdapter --> ZkBioClient`
- `ZkBioAdapter --> IntegrationCacheService`
- `ZkBioAdapter --> SourceAvailabilityService`

#### Relations client -> services techniques

- `ObserviumClientX --> WebClient`
- `ObserviumClientX --> IntegrationCacheService`
- `ObserviumClientX --> SourceAvailabilityService`
- `ObserviumClientX --> ObjectMapper`

- `ZkBioClient --> WebClient`
- `ZkBioClient --> IntegrationCacheService`
- `ZkBioClient --> SourceAvailabilityService`
- `ZkBioClient --> ObjectMapper`

- `ZabbixClient --> IntegrationCacheService`
- `ZabbixClient --> SourceAvailabilityService`
- `ZabbixClient --> ObjectMapper`

#### Relations repository -> entity

- `MonitoredHostRepository --> MonitoredHost`
- `ServiceStatusRepository --> ServiceStatus`
- `ZabbixMetricRepository --> ZabbixMetric`
- `ZabbixProblemRepository --> ZabbixProblem`
- `ObserviumProblemRepository --> ObserviumProblem`
- `ZkBioProblemRepository --> ZkBioProblem`
- `TicketRepository --> Ticket`
- `UserRepository --> User`

#### Relations scheduler / listener

- `ZabbixScheduler --> ZabbixProblemService`
- `ZabbixScheduler --> ZabbixMetricsService`
- `ZabbixScheduler --> ZabbixLiveSynchronizationService`
- `ZabbixScheduler --> ZabbixAdapter`
- `ZabbixScheduler --> MonitoringWebSocketPublisher`
- `ZabbixScheduler --> ZabbixWebSocketPublisher`
- `ZabbixScheduler --> SourceAvailabilityService`

- `ObserviumScheduler --> MonitoringService`
- `ObserviumHostsScheduler --> MonitoringService`
- `ZkBioScheduler --> ZkBioServiceImpl`
- `ZkBioScheduler --> ZkBioWebSocketPublisher`

- `ZabbixStartupListener --> ZabbixLiveSynchronizationService`
- `ZabbixMetricsStartupListener --> ZabbixMetricsService`
- `ZabbixProblemStartupListener --> ZabbixProblemService`

#### Relations temps réel / cache

- `SourceAvailabilityServiceImpl --> MonitoringWebSocketPublisher`
- `RedisIntegrationCacheService ..|> IntegrationCacheService`
- `MonitoringWebSocketPublisher --> SimpMessagingTemplate`
- `ZabbixWebSocketPublisher --> SimpMessagingTemplate`
- `ZkBioWebSocketPublisher --> SimpMessagingTemplate`

### 1.3. Vision simplifiée du diagramme de classes

Une représentation UML synthétique peut être organisée en cinq blocs :

1. présentation REST : controllers ;
2. orchestration métier : services ;
3. intégration externe : providers, adapters, clients ;
4. persistance : repositories et entités ;
5. infrastructure transversale : cache Redis, WebSocket, schedulers, listeners.

## 2. Diagramme de cas d’utilisation

### 2.1. Acteurs principaux

- Administrateur / opérateur de supervision
- Système externe Zabbix
- Système externe Observium
- Système externe ZKBio
- Base MySQL
- Redis

### 2.2. Cas d’utilisation métier principaux

#### Acteur : Administrateur / opérateur

- Consulter les hôtes supervisés
- Consulter les problèmes/alertes
- Consulter les métriques disponibles
- Consulter la santé des sources
- Déclencher une collecte globale
- Déclencher une collecte Zabbix
- Déclencher une collecte Observium
- Déclencher une collecte ZKBio
- Consulter les données spécifiques ZKBio
- Recevoir les mises à jour en temps réel
- Consulter le tableau de bord global
- Gérer les tickets

#### Acteur : Systèmes externes

- Fournir les données Zabbix
- Fournir les données Observium
- Fournir les données ZKBio

#### Acteur : Redis

- Fournir un snapshot de fallback
- Mettre en cache les lectures unifiées

#### Acteur : MySQL

- Stocker les snapshots persistés
- Fournir les données persistées

### 2.3. Relations de cas d’utilisation

Cas principal :

- `Consulter les données de monitoring` inclut :
  - `Consulter les hosts`
  - `Consulter les problems`
  - `Consulter les metrics`
  - `Consulter la santé des sources`

Cas d’extension :

- `Consulter les données de monitoring` peut étendre :
  - `Utiliser le fallback Redis` en cas de panne API
  - `Recevoir une réponse dégradée` si une source est indisponible

Autres cas importants :

- `Déclencher une collecte` inclut :
  - `Appeler un adapter`
  - `Interroger une API externe`
  - `Sauvegarder en base`
  - `Sauvegarder un snapshot Redis`

- `Recevoir des mises à jour temps réel` inclut :
  - `S’abonner au WebSocket`
  - `Recevoir un événement publié`

## 3. Diagrammes de séquence

## 3.1. Séquence : lecture unifiée des hosts / problems / metrics

Participants :

- Frontend Angular
- `MonitoringController`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `MonitoringProvider`
- Services / adapters / repositories
- Redis

Séquence UML textuelle :

1. `Frontend -> MonitoringController : GET /api/monitoring/{resource}`
2. `MonitoringController -> MonitoringAggregationService : getHosts()/getProblems()/getMetrics()`
3. `MonitoringAggregationService -> MonitoringCacheService : get...()`
4. `MonitoringCacheService -> Redis(Spring Cache) : lire cache`
5. `alt cache valide`
6. `Redis --> MonitoringCacheService : résultat non dégradé`
7. `MonitoringCacheService --> MonitoringAggregationService : FetchResult`
8. `MonitoringAggregationService --> MonitoringController : UnifiedMonitoringResponse`
9. `MonitoringController --> Frontend : réponse JSON`
10. `else cache absent ou non exploitable`
11. `MonitoringCacheService -> MonitoringProvider : collecter les données`
12. `MonitoringProvider -> Service/Adapter/Repository : lecture source`
13. `Service/Adapter/Repository --> MonitoringProvider : données`
14. `MonitoringProvider --> MonitoringCacheService : données unifiées`
15. `MonitoringCacheService : calcule degraded/freshness/coverage`
16. `opt résultat non dégradé`
17. `MonitoringCacheService -> Redis(Spring Cache) : sauvegarde cache`
18. `end`
19. `MonitoringCacheService --> MonitoringAggregationService : FetchResult`
20. `MonitoringAggregationService --> MonitoringController : UnifiedMonitoringResponse`
21. `MonitoringController --> Frontend : réponse JSON`

## 3.2. Séquence : collecte manuelle

Participants :

- Frontend Angular
- `MonitoringController`
- `MonitoringServiceImpl`
- Adapter source
- Client source
- API externe
- Redis
- MySQL

Séquence UML textuelle :

1. `Frontend -> MonitoringController : POST /api/monitoring/collect/{source}`
2. `MonitoringController -> MonitoringServiceImpl : collect{Source}()`
3. `MonitoringServiceImpl -> Adapter : fetchAll()/fetchProblems()`
4. `Adapter -> Client : appel API source`
5. `Client -> API externe : requête HTTP`
6. `API externe --> Client : réponse`
7. `Client -> RedisIntegrationCacheService : saveSnapshot()`
8. `Client --> Adapter : données brutes`
9. `Adapter --> MonitoringServiceImpl : DTOs applicatifs`
10. `MonitoringServiceImpl -> Repository : save/saveAll`
11. `Repository -> MySQL : persistance`
12. `MonitoringController --> Frontend : ApiResponse success`

## 3.3. Séquence : fallback Redis

Participants :

- Adapter
- Client
- API externe
- `RedisIntegrationCacheService`
- `SourceAvailabilityServiceImpl`

Séquence UML textuelle :

1. `Adapter -> Client : appel source`
2. `Client -> API externe : requête HTTP`
3. `API externe --> Client : erreur / timeout / indisponibilité`
4. `Client -> RedisIntegrationCacheService : getSnapshot()`
5. `alt snapshot Redis trouvé`
6. `RedisIntegrationCacheService --> Client : snapshot`
7. `Client -> SourceAvailabilityServiceImpl : markUnavailable(... fallback Redis ...)`
8. `SourceAvailabilityServiceImpl : état = DEGRADED`
9. `Client --> Adapter : données fallback`
10. `Adapter --> couche appelante : réponse dégradée mais exploitable`
11. `else aucun snapshot`
12. `RedisIntegrationCacheService --> Client : vide`
13. `Client -> SourceAvailabilityServiceImpl : markUnavailable(... no usable Redis snapshot ...)`
14. `SourceAvailabilityServiceImpl : état = UNAVAILABLE`
15. `Client --> Adapter : exception explicite`
16. `Adapter --> couche appelante : exception`

## 3.4. Séquence : publication WebSocket

Participants :

- Scheduler ou service
- Publisher WebSocket
- STOMP broker
- Frontend Angular
- `MonitoringRealtimeService`
- `MonitoringStore`

Séquence UML textuelle :

1. `Scheduler/Service -> WebSocketPublisher : publish(...)`
2. `WebSocketPublisher -> STOMP topic : convertAndSend()`
3. `STOMP topic --> Frontend : message`
4. `Frontend STOMP client --> MonitoringRealtimeService : événement reçu`
5. `MonitoringRealtimeService --> MonitoringStore : next(data)`
6. `MonitoringStore --> UI : mise à jour automatique`

## 3.5. Séquence : warmup au démarrage

Participants :

- Spring Boot
- Listener
- Service de synchronisation
- Adapter / client
- API externe
- MySQL / Redis

Séquence UML textuelle :

1. `Spring Boot -> StartupListener : ApplicationReadyEvent`
2. `StartupListener -> Service de synchronisation : synchronizeForStartup()`
3. `Service -> Adapter/Client : lecture initiale`
4. `Adapter/Client -> API externe : appel`
5. `API externe --> Adapter/Client : réponse`
6. `Adapter/Client -> Redis : saveSnapshot()`
7. `Service -> Repository : saveAll()`
8. `Repository -> MySQL : persistance initiale`

## 4. Conseils de représentation UML

### 4.1. Diagramme de classes

Il est conseillé de le construire en trois niveaux :

- niveau 1 : controllers et services ;
- niveau 2 : providers, adapters, clients ;
- niveau 3 : repositories, entités, cache et infrastructure.

### 4.2. Diagramme de cas d’utilisation

Le diagramme peut être centré sur l’acteur principal « Administrateur / opérateur », avec les systèmes externes et Redis/MySQL comme acteurs secondaires.

### 4.3. Diagrammes de séquence

Les séquences les plus pertinentes pour un rapport PFE sont :

- lecture monitoring unifiée ;
- collecte manuelle ;
- fallback Redis ;
- publication WebSocket ;
- warmup au démarrage.

## 5. Résumé compact pour insertion rapide

### Classes pivots

- `MonitoringController`
- `MonitoringServiceImpl`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `ZabbixMonitoringProvider`
- `ObserviumMonitoringProvider`
- `ZkBioMonitoringProvider`
- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `ZabbixClient`
- `ObserviumClientX`
- `ZkBioClient`
- `SourceAvailabilityServiceImpl`
- `MonitoringWebSocketPublisher`
- `RedisIntegrationCacheService`

### Cas d’utilisation pivots

- Consulter le monitoring unifié
- Déclencher une collecte
- Recevoir les mises à jour temps réel
- Continuer à servir des données via fallback Redis

### Séquences pivots

- lecture unifiée
- collecte
- fallback Redis
- WebSocket
- startup warmup
