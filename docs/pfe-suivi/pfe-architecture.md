# Architecture du projet de monitoring multi-source

## 1. Vue d'ensemble

Le projet est une plateforme de supervision multi-source construite sur :

- un backend Spring Boot ;
- un frontend Angular.

Son objectif est de centraliser la collecte, la persistance, l'agregation et la visualisation de donnees provenant de Zabbix, Observium, ZKBio et Camera.

L'architecture active au 21 avril 2026 est fondee sur quatre briques principales :

- `tn.iteam.integration.*`
- `tn.iteam.monitoring.snapshot.*`
- `tn.iteam.monitoring.service.*`
- `tn.iteam.MonitoringStartup`

## 2. Point d'entree backend

L'application demarre depuis `tn.iteam.PfeprojectApplication` avec :

- `@SpringBootApplication`
- `@EnableAsync(proxyTargetClass = true)`
- `@EnableScheduling`

Le choix `proxyTargetClass = true` reste un garde-fou Spring utile, mais l'orchestration monitoring active a ete refactorisee pour s'appuyer principalement sur des abstractions plutot que sur l'injection des classes concretes.

Le warmup monitoring initial n'est plus distribue dans plusieurs listeners. Il est centralise dans :

- `tn.iteam.MonitoringStartup`

## 3. Architecture backend active

### 3.1 Controllers

Controllers principaux :

- `MonitoringController`
- `DashboardController`
- `ZkBioController`
- `ZabbixProblemController`
- `ZabbixMetricsController`
- `ObserviumController`
- `TicketController`

Remarque importante :

- `ZabbixProblemController`, `ZabbixMetricsController` et `ObserviumController` sont conserves comme couche de compatibilite temporaire ;
- le flux principal de lecture reste `MonitoringController` ;
- le frontend Angular courant n'utilise plus ces endpoints de compatibilite.

### 3.2 Services d'integration

Le coeur du monitoring est porte par :

- `IntegrationService`
- `AsyncIntegrationService`
- `IntegrationServiceRegistry`
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

Chaque service d'integration :

- collecte les donnees de sa source ;
- transforme ou mappe les donnees via adapter et mapper ;
- persiste si necessaire ;
- met a jour `SnapshotStore` ;
- marque l'etat de la source via `SourceAvailabilityService`.

`IntegrationServiceRegistry` joue un role volontairement limite :

- resoudre le bon service d'integration par `MonitoringSourceType` ;
- ne pas porter de logique metier ;
- ne pas porter de logique de persistance ;
- ne pas porter de logique d'orchestration.

### 3.3 SnapshotStore

Le stockage technique central du monitoring unifie est :

- `SnapshotStore`
- `InMemorySnapshotStore`
- `StoredSnapshot`

Regle importante documentee apres audit :

- `InMemorySnapshotStore` est aujourd'hui le chemin actif et garanti ;
- Redis n'est pas le stockage principal ;
- Redis ne peut etre qu'un complement optionnel active explicitement.

Datasets principaux :

- `hosts`
- `problems`
- `metrics`
- `attendance`
- `devices`
- `status`

Les snapshots servent a deux usages :

- lecture unifiee REST ;
- republication WebSocket.

### 3.4 Services d'agregation

Les lectures unifiees reposent sur :

- `MonitoringCacheService`
- `MonitoringAggregationService`

`MonitoringCacheService` :

- lit les snapshots par dataset et par source ;
- fusionne les jeux de donnees ;
- calcule `degraded` et `freshness`.

`MonitoringAggregationService` :

- emballe le resultat dans `UnifiedMonitoringResponse` ;
- ajoute `coverage` pour les metriques.

### 3.5 Schedulers et startup

Warmup initial :

- `MonitoringStartup`

Schedulers periodiques :

- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`

Regle architecturale :

- startup = warmup initial ;
- schedulers = refresh periodique.

Il n'existe plus de listener monitoring actif parallele au startup principal.

### 3.6 Adapters

Adapters actifs :

- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

Ils assurent la traduction entre les clients techniques et les DTO utilises par les services d'integration.

### 3.7 Clients

Clients actifs :

- `tn.iteam.adapter.zabbix.ZabbixClient`
- `tn.iteam.client.ObserviumClientX`
- `tn.iteam.client.ZkBioClientX`

Le placement n'est pas encore homogene, mais il est important de documenter l'etat reel du projet.

### 3.8 Persistance metier

La persistance s'appuie sur JPA/MySQL.

Services de persistance dedies :

- `ServiceStatusPersistenceService`
- `ObserviumPersistenceService`
- `ZkBioPersistenceService`

Repositories principaux :

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

### 3.9 WebSocket

Publishers actifs :

- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `MonitoringSnapshotPublicationService`

Topics principaux :

- `/topic/monitoring/problems`
- `/topic/monitoring/metrics`
- `/topic/monitoring/sources`
- `/topic/zkbio/attendance`
- `/topic/zkbio/devices`
- `/topic/zkbio/status`

Note importante :

- `/topic/zkbio/problems` existe encore dans une classe publisher, mais n'est pas le topic effectivement publie par le workflow actif startup/scheduler/collecte ;
- le flux probleme ZKBio temps reel actif passe aujourd'hui par `/topic/monitoring/problems`.
- `MonitoringSnapshotPublicationService` reste un simple delegateur de publication et ne porte ni regle metier ni logique de fallback.

## 4. Flux principaux

### 4.1 Lecture monitoring unifiee

`Frontend -> MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> SnapshotStore -> UnifiedMonitoringResponse`

### 4.2 Collecte manuelle

`Frontend -> Controller -> IntegrationService -> Adapter -> Client -> API externe -> Repository + SnapshotStore`

### 4.3 Warmup initial

`Spring -> MonitoringStartup -> IntegrationService(s) -> SnapshotStore -> WebSocketPublisher`

### 4.4 Refresh periodique

`Scheduler -> IntegrationService -> SnapshotStore -> WebSocketPublisher`

## 5. Frontend Angular

Le frontend est organise en :

- `core`
- `features`
- `layout`
- `shared`

Le domaine `features/monitoring` contient :

- `data/monitoring-api.service.ts`
- `data/monitoring-realtime.service.ts`
- `state/monitoring.store.ts`
- `ui/*`

Le frontend Angular courant consomme actuellement :

- `/api/monitoring/*`
- `/api/zkbio/*`
- `/dashboard/*`

Compatibilites temporaires encore exposees cote backend, mais plus utilisees par le frontend courant :

- `/api/zabbix/active`
- `/api/zabbix/metrics`
- `/api/observium/summary`

## 6. Ce qui n'est plus le coeur de l'architecture

Les elements suivants appartiennent a l'ancien flux ou a la transition et ne doivent plus etre presentes comme noyau de l'architecture :

- `MonitoringServiceImpl`
- `MonitoringProvider`
- `tn.iteam.monitoring.provider.*`
- `IntegrationCacheService`
- `RedisIntegrationCacheService`
- `ZabbixStartupListener`
- autres listeners startup monitoring

Ces reliquats ont ete retires du chemin principal et archives dans `depl/`.

## 6.1 Politique Redis optionnel

Redis n'est pas utilise par le code metier actif du monitoring.

L'etat de reference est :

- snapshots actifs dans `InMemorySnapshotStore`
- lectures unifiees via `MonitoringCacheService`
- publications WebSocket a partir de `SnapshotStore`

Le risque principal etait l'infrastructure Spring autour de Redis, pas le metier.

Les garde-fous appliques sont :

- exclusion de l'auto-configuration Redis Spring Boot ;
- `management.health.redis.enabled=false` ;
- `spring.cache.type=simple` ;
- activation Redis uniquement par `app.redis.enabled=true`.

Comportement attendu :

- Redis absent -> application demarre ;
- Redis desactive -> application demarre ;
- Redis active mais inaccessible -> application demarre quand meme et continue a fonctionner sur la memoire.

## 6.2 Avant / Apres / Raison

| Avant | Apres | Raison |
|---|---|---|
| Redis pouvait sembler transversal au monitoring | `InMemorySnapshotStore` est clairement le chemin principal | Refleter le code reel |
| Actuator pouvait faire remonter Redis comme risque global | Redis health est neutralise dans la sante globale | Redis est auxiliaire |
| La doc melangeait flux actif et fallback Redis historique | Redis est documente comme option future explicite | Eviter les contresens d'exploitation |

## 7. Conclusion

L'architecture actuelle est plus coherente et plus simple a expliquer :

- une integration par source ;
- un stockage technique commun des snapshots ;
- une agregation unifiee centralisee ;
- un startup unique ;
- des schedulers specialises ;
- une couche de compatibilite API maintenue temporairement pour des consommateurs legacy ou externes.

## 8. Design patterns utilises

- Adapter :
  - `ZabbixAdapter`, `ObserviumAdapter`, `ZkBioAdapter`, `CameraAdapter`
- Strategy :
  - `IntegrationService` et ses implementations par source
- Facade :
  - `MonitoringController` pour l'acces unifie au monitoring
  - `MonitoringSnapshotPublicationService` comme facade technique de publication
- Repository :
  - repositories Spring Data JPA du domaine monitoring et ticketing
- Observer / Publisher-Subscriber :
  - `MonitoringWebSocketPublisher`, `ZkBioWebSocketPublisher`, topics STOMP et `MonitoringRealtimeService` cote frontend
- Store centralise :
  - `SnapshotStore` / `InMemorySnapshotStore` cote backend, `MonitoringStore` cote frontend
