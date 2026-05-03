# Chapitre 2 : Architecture et conception

## 2.1 Introduction

Ce chapitre presente l'architecture de l'application de supervision multi-source realisee dans le cadre du PFE. L'objectif de la plateforme est de centraliser la collecte, la persistance, l'agregation et la visualisation de donnees provenant de plusieurs sources heterogenes :

- Zabbix
- Observium
- ZKBio
- Camera pour la partie inventaire d'hotes

L'architecture actuelle repose sur un backend Spring Boot et un frontend Angular. Le backend a ete refactorise autour d'un flux unifie fonde sur :

- `tn.iteam.integration.*`
- `tn.iteam.monitoring.snapshot.SnapshotStore`
- `tn.iteam.monitoring.service.MonitoringAggregationService`
- `tn.iteam.MonitoringStartup`

Cette version remplace l'ancienne orchestration basee sur `MonitoringService`, les providers de monitoring et les listeners de startup multiples, desormais archives dans `depl/`.

## 2.2 Architecture globale du systeme

### 2.2.1 Vue d'ensemble

Le systeme est structure autour de deux blocs complementaires :

- un backend Spring Boot qui collecte, transforme, persiste, agrege et diffuse les donnees ;
- un frontend Angular qui consomme les APIs REST, pilote certaines collectes et recoit les mises a jour temps reel via WebSocket.

Le flux principal est le suivant :

1. le frontend appelle une API REST ou s'abonne a un topic STOMP ;
2. les controllers deleguent soit aux services d'agregation, soit aux services metier, soit aux services d'integration ;
3. les services d'integration interrogent les adapters et clients des sources externes ;
4. les donnees collectees sont transformees puis sauvegardees :
   - en base MySQL pour les donnees persistantes ;
   - dans `SnapshotStore` pour les snapshots techniques utilises par le monitoring unifie et les WebSockets ;
5. les services d'agregation lisent ces snapshots et construisent les reponses unifiees ;
6. les publishers WebSocket republient les snapshots vers le frontend.

### 2.2.2 Stack technologique

Backend :

- Java 21
- Spring Boot 3.2.5
- Spring Web
- Spring WebFlux
- Spring Data JPA
- Spring WebSocket
- Spring Scheduling
- Resilience4j
- MySQL
- Lombok
- Micrometer / Prometheus

Frontend :

- Angular 20
- TypeScript
- RxJS
- STOMP
- SockJS

Redis :

- n'est pas le stockage principal du monitoring actif ;
- reste une capacite optionnelle a reintroduire proprement si necessaire.

## 2.3 Architecture backend

### 2.3.1 Structure des packages

Les packages backend actuellement actifs les plus importants sont :

- `tn.iteam.controller`
- `tn.iteam.integration`
- `tn.iteam.monitoring.service`
- `tn.iteam.monitoring.snapshot`
- `tn.iteam.adapter`
- `tn.iteam.client`
- `tn.iteam.service` et `tn.iteam.service.impl`
- `tn.iteam.repository`
- `tn.iteam.domain`
- `tn.iteam.dto`
- `tn.iteam.scheduler`
- `tn.iteam.websocket`
- `tn.iteam.config`

Des packages historiques existent encore a titre d'archive ou de transition, mais ils ne portent plus le flux principal :

- `tn.iteam.listener` pour le monitoring
- `tn.iteam.cache`
- `tn.iteam.monitoring.provider`
- anciens `MonitoringService*`

### 2.3.2 Couche controller

La couche controller expose les APIs REST. Les controllers les plus importants sont :

- `MonitoringController` : facade principale pour le monitoring unifie ;
- `DashboardController` : endpoints du tableau de bord ;
- `ZkBioController` : endpoints metier ZKBio et collecte manuelle ZKBio ;
- `ZabbixProblemController` et `ZabbixMetricsController` : endpoints de compatibilite temporaire pour d'anciens consommateurs ;
- `ObserviumController` : endpoint de compatibilite temporaire pour le resume Observium.

Le controller principal est `MonitoringController`. Il expose notamment :

- `GET /api/monitoring/hosts`
- `GET /api/monitoring/problems`
- `GET /api/monitoring/metrics`
- `GET /api/monitoring/sources/health`
- `POST /api/monitoring/collect`
- `POST /api/monitoring/collect/zabbix`
- `POST /api/monitoring/collect/observium`
- `POST /api/monitoring/collect/camera`

Le endpoint `POST /api/zkbio/collect` reste porte par `ZkBioController`, car il declenche aussi les publications specifiques ZKBio : attendance, devices et status.

### 2.3.3 Couche integration

La couche `tn.iteam.integration` est le coeur de l'architecture actuelle.

Elle repose sur l'interface `IntegrationService`, qui definit un contrat commun :

- `getSourceType()`
- `refresh()`
- `refreshHosts()`
- `refreshProblems()`
- `refreshMetrics()`
- `refreshAttendance()`

Dans l'etat actuel, l'orchestration s'appuie aussi sur :

- `AsyncIntegrationService` pour les operations asynchrones communes ;
- `IntegrationServiceRegistry` pour resoudre les integrations par source sans injecter directement toutes les implementations concretes dans les controllers et schedulers.

Les implementations principales sont :

- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

Responsabilites de cette couche :

- collecter les donnees de chaque source ;
- appeler les adapters et services specialisees ;
- persister les donnees utiles en base ;
- sauvegarder des snapshots dans `SnapshotStore` ;
- marquer la disponibilite de la source via `SourceAvailabilityService`.

`IntegrationServiceRegistry` n'est pas un orchestrateur metier :

- il resout un service ;
- il n'accede pas aux repositories ;
- il ne manipule pas les snapshots ;
- il ne contient pas de branchements metier par source.

### 2.3.4 SnapshotStore et agregation unifiee

Le monitoring unifie ne passe plus par des providers actifs ni par un cache Redis Spring.

Le flux actuel repose sur :

- `SnapshotStore`
- `InMemorySnapshotStore`
- `MonitoringCacheService`
- `MonitoringAggregationService`

`SnapshotStore` est un stockage technique cle/valeur par couple `(dataset, source)`.

Dans l'etat actuel du backend :

- `InMemorySnapshotStore` est l'implementation active ;
- Redis n'est pas utilise dans le code metier actif ;
- le fallback garanti est la memoire, pas Redis.

Les datasets utilises sont principalement :

- `hosts`
- `problems`
- `metrics`
- `attendance`
- `devices`
- `status`

`MonitoringCacheService` lit les snapshots, agrege les donnees par source et calcule :

- `degraded`
- `freshness`

`MonitoringAggregationService` transforme ensuite ce resultat en `UnifiedMonitoringResponse`. Pour `/metrics`, il ajoute aussi la `coverage` issue de `MonitoringSourceType` :

- `ZABBIX -> native`
- `OBSERVIUM -> synthetic`
- `ZKBIO -> synthetic`
- `CAMERA -> not_applicable`

### 2.3.5 Startup et schedulers

Le warmup initial est centralise par une seule classe :

- `MonitoringStartup`

Son role est clair :

- faire un chargement initial des snapshots au demarrage ;
- publier un premier etat WebSocket exploitable ;
- eviter les multiples listeners startup paralleles.

Les rafraichissements periodiques restent assures par des schedulers separes par source :

- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`

La regle architecturale actuelle est la suivante :

- `MonitoringStartup` = warmup initial unique ;
- `scheduler` = refresh periodique.

Les anciens listeners de startup monitoring ont ete retires du flux actif.

### 2.3.6 Couche adapter

Les adapters assurent la mediation entre les clients externes et les DTO internes :

- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

Ils transforment les reponses externes en DTO applicatifs et servent d'appui aux services d'integration.

### 2.3.7 Couche client

Les clients techniques actifs sont :

- `tn.iteam.adapter.zabbix.ZabbixClient`
- `tn.iteam.client.ObserviumClientX`
- `tn.iteam.client.ZkBioClientX`

Cette repartition n'est pas encore totalement homogene, mais elle correspond a l'etat reel du code.

Leur role est de :

- gerer les appels HTTP ;
- appliquer `Retry` et `CircuitBreaker` de Resilience4j ;
- parser les reponses JSON ;
- lever des exceptions d'integration coherentes.

### 2.3.8 Persistance

La persistance principale repose sur Spring Data JPA et MySQL.

Repositories majeurs :

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

Des services de persistance dedies ont ete introduits pour clarifier les responsabilites :

- `ServiceStatusPersistenceService`
- `ObserviumPersistenceService`
- `ZkBioPersistenceService`

### 2.3.9 WebSocket

La diffusion temps reel est assuree par :

- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `MonitoringSnapshotPublicationService`

Les topics actifs les plus importants sont :

- `/topic/monitoring/problems`
- `/topic/monitoring/metrics`
- `/topic/monitoring/sources`
- `/topic/zkbio/attendance`
- `/topic/zkbio/devices`
- `/topic/zkbio/status`

Les publications s'appuient directement sur `SnapshotStore`.

`MonitoringSnapshotPublicationService` joue un role de facade technique legere pour regrouper les appels de publication sans embarquer de logique metier.

Note :

- `/topic/zkbio/problems` n'est pas le topic effectivement publie par le workflow actif ; les problemes ZKBio passent actuellement par `/topic/monitoring/problems`.

### 2.3.10 Resilience et cache

L'architecture conserve des mecanismes de resilience via Resilience4j dans les clients.

En revanche, le chemin principal du monitoring unifie ne repose plus sur :

- `MonitoringProvider`
- `IntegrationCacheService`
- `RedisIntegrationCacheService`
- le cache Spring `@Cacheable`

Le stockage technique central du flux unifie est aujourd'hui `InMemorySnapshotStore`.

Redis doit etre considere comme auxiliaire et optionnel.

Les garde-fous appliques sont :

- exclusion de l'auto-configuration Redis Spring Boot ;
- activation seulement par `app.redis.enabled=true` ;
- `management.health.redis.enabled=false` ;
- `spring.cache.type=simple`.

Effet recherche :

- Redis absent -> application UP ;
- Redis desactive -> application UP ;
- Redis active mais inaccessible -> application UP avec continuite memoire.

## 2.4 Architecture frontend

Le frontend Angular est organise dans `frontend/src/app` autour de quatre blocs :

- `core`
- `features`
- `layout`
- `shared`

Le domaine principal est `features/monitoring`, structure en :

- `data`
- `state`
- `ui`

Les services pivots sont :

- `monitoring-api.service.ts`
- `monitoring-realtime.service.ts`
- `monitoring.store.ts`

Le frontend Angular actif consomme actuellement :

- endpoints unifies : `/api/monitoring/*`
- endpoints ZKBio metier : `/api/zkbio/*`
- endpoints dashboard : `/dashboard/*`

Les endpoints de compatibilite Zabbix et Observium restent exposes cote backend, mais ils ne sont plus consommes par le frontend courant.

## 2.5 Flux de donnees principaux

### 2.5.1 Flux de lecture unifiee

1. Angular appelle `MonitoringController` ;
2. `MonitoringController` delegue a `MonitoringAggregationService` ;
3. `MonitoringAggregationService` delegue a `MonitoringCacheService` ;
4. `MonitoringCacheService` lit `SnapshotStore` ;
5. les snapshots par source sont agreges ;
6. une `UnifiedMonitoringResponse` est renvoyee.

### 2.5.2 Flux de collecte manuelle

1. Angular declenche une collecte REST ;
2. le controller appelle un service d'integration ;
3. le service d'integration collecte, persiste, puis met a jour `SnapshotStore` ;
4. le controller renvoie une reponse de succes ;
5. les publishers WebSocket peuvent republier le snapshot.

### 2.5.3 Flux de startup

1. Spring initialise le contexte ;
2. `MonitoringStartup` effectue le warmup initial ;
3. chaque source charge ses snapshots principaux ;
4. les premiers messages WebSocket sont publies ;
5. les schedulers prennent ensuite le relais pour les refresh periodiques.

## 2.6 Synthese

L'architecture actuelle est plus simple et plus lisible que la version initiale. Elle est desormais organisee autour d'un noyau clair :

- integration par source ;
- snapshot technique commun ;
- aggregation unifiee ;
- startup unique ;
- schedulers periodiques ;
- endpoints de compatibilite maintenus temporairement.

Cette architecture reste modulaire, evolutive et plus facile a documenter dans un rapport PFE, car elle separe nettement :

- la collecte ;
- le stockage technique temporaire ;
- la persistance metier ;
- l'exposition REST ;
- la diffusion WebSocket.

## 2.7 Design patterns utilises et ou ils sont utilises

Les patterns ci-dessous correspondent au code reel actuellement actif.

- Adapter :
  - `ZabbixAdapter`, `ObserviumAdapter`, `ZkBioAdapter`, `CameraAdapter`
  - Utilisation : traduction des reponses des clients techniques vers les DTO internes exploites par les services d'integration.
- Strategy :
  - `IntegrationService` + `ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService`, `CameraIntegrationService`
  - Utilisation : chaque source implemente la meme strategie de refresh avec ses propres regles de collecte, persistance et snapshot.
- Facade :
  - `MonitoringController`
  - Utilisation : point d'entree unique du monitoring unifie pour le frontend via `/api/monitoring/*`.
  - `MonitoringSnapshotPublicationService`
  - Utilisation : facade technique de publication WebSocket reutilisable par controller, startup et schedulers.
- Repository :
  - `ZabbixProblemRepository`, `ObserviumMetricRepository`, `ZkBioProblemRepository`, `TicketRepository`, etc.
  - Utilisation : abstraction de l'acces aux donnees JPA/MySQL.
- Observer / Publisher-Subscriber :
  - `MonitoringWebSocketPublisher`, `ZkBioWebSocketPublisher`, topics STOMP et `MonitoringRealtimeService` cote frontend
  - Utilisation : publication puis consommation temps reel des snapshots et etats de source.
- Store centralise :
  - `SnapshotStore` / `InMemorySnapshotStore` cote backend, `MonitoringStore` cote frontend
  - Utilisation : centralisation de l'etat technique backend et de l'etat d'interface frontend.

### Tableau Avant / Apres / Raison

| Avant | Apres | Raison |
|---|---|---|
| Redis pouvait etre percu comme cache central du monitoring | `InMemorySnapshotStore` est identifie comme chemin principal | S'aligner sur le code actif |
| La sante Redis pouvait etre interpretee comme critique | Redis est documente comme optionnel et non bloquant | Eviter un faux KO global |
| Les workflows pouvaient laisser penser a un fallback Redis central | La memoire est le fallback garanti au boot et au runtime | Proteger la disponibilite de l'application |
