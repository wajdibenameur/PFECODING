# Workflows principaux du projet

Ce document decrit les workflows reels du projet apres refactorisation du monitoring. Il est aligne sur l'architecture active :

- `MonitoringStartup` pour le warmup initial
- `integration/*` pour la collecte
- `SnapshotStore` pour les snapshots techniques
- `MonitoringAggregationService` pour les lectures unifiees

Redis n'apparait pas dans ces workflows de reference car il n'est pas utilise dans le chemin metier actif.

## 1. Workflow de startup monitoring

Acteurs :

- Spring Boot
- `MonitoringStartup`
- `IntegrationServiceRegistry`
- services d'integration
- `SnapshotStore`
- publishers WebSocket

Sequence :

1. Spring Boot initialise le contexte ;
2. `MonitoringStartup` est declenche par `@EventListener(ApplicationReadyEvent)` et execute en arriere-plan ;
3. `MonitoringStartup` resout les integrations via `IntegrationServiceRegistry` ;
4. `ZabbixIntegrationService.refreshAsync()` charge hosts, problems et metrics ;
5. `ObserviumIntegrationService.refreshAsync()` charge hosts, problems et metrics ;
6. `ZkBioIntegrationService.refreshAsync()` charge hosts, problems et metrics ;
7. `ZkBioIntegrationService.refreshAttendanceAsync()` charge `attendance`, `devices` et `status` ;
8. `CameraIntegrationService.refreshAsync()` charge les hosts camera ;
9. `MonitoringSnapshotPublicationService` centralise les appels de publication ;
10. les snapshots sont sauvegardes dans `SnapshotStore` ;
11. `MonitoringWebSocketPublisher` republie les snapshots monitoring ;
12. `ZkBioWebSocketPublisher` republie les snapshots ZKBio specifiques.

Lecture courte :

`Spring -> MonitoringStartup -> IntegrationService(s) -> SnapshotStore -> WebSocketPublisher`

## 2. Workflow de refresh periodique

Acteurs :

- scheduler par source
- service d'integration
- `SnapshotStore`
- publishers WebSocket

### 2.1 Zabbix

1. `ZabbixScheduler` declenche `refreshProblems()` puis `refreshMetrics()` ;
2. `ZabbixIntegrationService` collecte et met a jour les snapshots ;
3. `MonitoringWebSocketPublisher` republie `/topic/monitoring/problems` et `/topic/monitoring/metrics`.

### 2.2 Observium

1. `ObserviumScheduler` declenche `refreshProblemsAndMetrics()` ;
2. `ObserviumIntegrationService` rafraichit `problems` et `metrics` ;
3. `MonitoringWebSocketPublisher` republie les snapshots monitoring Observium ;
4. `ObserviumHostsScheduler` rafraichit separement les `hosts`.

### 2.3 ZKBio

1. `ZkBioScheduler` declenche `refreshProblemsAndMetrics()` ;
2. `ZkBioIntegrationService` rafraichit `problems` et `metrics` ;
3. `MonitoringWebSocketPublisher` republie les snapshots monitoring ZKBio ;
4. le second cron ZKBio declenche `refreshAttendanceDevicesAndStatus()` ;
5. `ZkBioIntegrationService` met a jour `attendance`, `devices` et `status` ;
6. `ZkBioWebSocketPublisher` republie les topics ZKBio specifiques.

## 3. Workflow de lecture monitoring unifiee

Acteurs :

- frontend Angular
- `MonitoringController`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore`

Sequence :

1. le frontend appelle :
   - `GET /api/monitoring/hosts`
   - ou `GET /api/monitoring/problems`
   - ou `GET /api/monitoring/metrics`
2. `MonitoringController` recoit la requete ;
3. il delegue a `MonitoringAggregationService` ;
4. `MonitoringAggregationService` delegue a `MonitoringCacheService` ;
5. `MonitoringCacheService` lit les snapshots par source dans `SnapshotStore` ;
6. les donnees sont fusionnees ;
7. `degraded` et `freshness` sont calcules ;
8. pour les metriques, `coverage` est ajoutee ;
9. `UnifiedMonitoringResponse` est renvoyee au frontend.

Lecture courte :

`Frontend -> MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> SnapshotStore -> UnifiedMonitoringResponse`

## 4. Workflow de collecte manuelle

### 4.1 Collecte globale ou par source via MonitoringController

Acteurs :

- frontend Angular
- `MonitoringController`
- `IntegrationServiceRegistry`
- services d'integration
- publishers WebSocket

Sequence :

1. le frontend appelle `POST /api/monitoring/collect` ou `POST /api/monitoring/collect/{source}` ;
2. `MonitoringController` resout le service d'integration correspondant via `IntegrationServiceRegistry` ;
3. le service collecte, persiste et met a jour les snapshots ;
4. `MonitoringController` delegue la republication a `MonitoringSnapshotPublicationService` ;
5. une `ApiResponse` de succes est renvoyee.

### 4.2 Collecte manuelle ZKBio

Acteurs :

- frontend Angular
- `ZkBioController`
- `ZkBioIntegrationService`
- publishers WebSocket

Sequence :

1. le frontend appelle `POST /api/zkbio/collect` ;
2. `ZkBioController` delegue a `ZkBioIntegrationService.refreshAllAndPublish()` ;
3. le service recharge monitoring + attendance ;
4. les snapshots sont republies ;
5. le frontend recoit ensuite les mises a jour temps reel.

## 5. Workflow source par source

### 5.1 Zabbix

Chemin principal :

`MonitoringController / Scheduler -> ZabbixIntegrationService -> ZabbixAdapter -> ZabbixClient -> API Zabbix -> Repository + SnapshotStore`

Resultats :

- hosts unifies
- problems unifies
- metrics unifiees

### 5.2 Observium

Chemin principal :

`MonitoringController / Scheduler -> ObserviumIntegrationService -> ObserviumAdapter -> ObserviumClientX -> API Observium -> Repository + SnapshotStore`

Resultats :

- hosts unifies
- problems unifies
- metrics synthetiques

### 5.3 ZKBio

Chemin principal monitoring :

`MonitoringController / Scheduler -> ZkBioIntegrationService -> ZkBioAdapter -> ZkBioClientX -> API ZKBio -> Repository + SnapshotStore`

Chemin metier ZKBio :

`ZkBioController -> ZkBioServiceImpl -> ZkBioAdapter / ZkBioClientX`

Resultats :

- hosts unifies
- problems unifies
- metrics synthetiques
- attendance
- devices
- status
- users

### 5.4 Camera

Chemin principal :

`MonitoringController / Startup -> CameraIntegrationService -> CameraAdapter -> SnapshotStore`

Resultat :

- hosts camera uniquement

## 6. Workflow WebSocket

Acteurs :

- service ou scheduler backend
- publisher WebSocket
- broker STOMP
- frontend Angular
- `MonitoringRealtimeService`
- `monitoring.store`

Sequence :

1. un service ou scheduler met a jour un snapshot ;
2. un publisher WebSocket lit ce snapshot ;
3. `SimpMessagingTemplate.convertAndSend(...)` publie le message ;
4. Angular recoit le message via STOMP ;
5. `MonitoringRealtimeService` l'expose ;
6. le store frontend fusionne l'evenement dans l'etat courant ;
7. l'UI est rafraichie.

Topics principaux :

- `/topic/monitoring/problems`
- `/topic/monitoring/metrics`
- `/topic/monitoring/sources`
- `/topic/zkbio/attendance`
- `/topic/zkbio/devices`
- `/topic/zkbio/status`

Note :

- le topic actif pour les problemes ZKBio est actuellement `/topic/monitoring/problems` avec filtrage par source ;
- `/topic/zkbio/problems` existe encore comme reliquat de compatibilite mais n'est pas branche dans le workflow actif.

## 7. Workflow des APIs de compatibilite

Ces APIs existent encore cote backend, mais elles ne sont plus consommees par le frontend Angular courant.

Certaines APIs restent exposees temporairement pour des consommateurs legacy ou externes.

### 7.1 Compatibilite Zabbix

`Consommateur legacy -> ZabbixProblemController / ZabbixMetricsController -> MonitoringAggregationService -> SnapshotStore`

Les endpoints conserves sont :

- `/api/zabbix/active`
- `/api/zabbix/metrics`

### 7.2 Compatibilite Observium

`Frontend ou consommateur externe -> ObserviumController -> ObserviumSummaryServiceImpl -> MonitoringAggregationService -> SnapshotStore`

Endpoint concerne :

- `/api/observium/summary`

## 8. Workflows qui ne sont plus la reference

Les descriptions suivantes ne correspondent plus au flux principal et ne doivent plus etre utilisees dans le rapport comme workflow de reference :

- `MonitoringController -> MonitoringServiceImpl`
- `MonitoringProvider -> MonitoringService dedie`
- listeners startup monitoring multiples
- fallback Redis comme chemin central du monitoring unifie

Le flux de reference est desormais :

`Startup ou Scheduler ou Controller -> IntegrationService -> SnapshotStore -> Aggregation/WebSocket`

## 9. Workflow de fallback boot et runtime

### 9.1 Boot fallback

Acteurs :

- Spring Boot
- configuration Redis optionnelle
- `InMemorySnapshotStore`

Sequence :

1. Spring initialise le contexte ;
2. si `app.redis.enabled` est absent ou `false`, aucun bean Redis n'est requis ;
3. `InMemorySnapshotStore` reste disponible ;
4. `MonitoringStartup`, les schedulers, les publishers et les controllers peuvent demarrer ;
5. l'application reste operationnelle.

### 9.2 Runtime fallback

Acteurs :

- couche metier monitoring
- `InMemorySnapshotStore`
- Redis optionnel si active

Sequence :

1. l'application a deja demarre ;
2. Redis peut etre inaccessible ou tombe en panne ;
3. le code metier actif continue a lire et ecrire via `SnapshotStore` memoire ;
4. `MonitoringCacheService`, les services d'integration, le startup, les schedulers, les publishers et les controllers ne remontent pas d'erreur fatale liee a Redis ;
5. le monitoring continue au moins sur la memoire.

## 10. Validation par tests

Commandes validees :

- `mvn -q "-Dtest=RedisOptionalConfigurationContextTest,MonitoringCacheServiceInMemoryTest,IntegrationServicesWithoutRedisTest,MonitoringRuntimeIsolationTest" test`
- `mvn -q test`
- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=MonitoringRuntimeIsolationTest,MonitoringControllerWebMvcTest" test`

Classes de test et preuve associee :

- `RedisOptionalConfigurationContextTest`
  - demarrage sans Redis
  - demarrage avec Redis desactive
  - demarrage avec Redis active mais inaccessible
- `MonitoringCacheServiceInMemoryTest`
  - fonctionnement du monitoring unifie avec memoire seule
- `IntegrationServicesWithoutRedisTest`
  - fonctionnement de Zabbix, Observium, ZKBio et Camera sans Redis
- `MonitoringRuntimeIsolationTest`
  - absence d'echec fatal au niveau warmup, schedulers, publishers et controller de collecte
