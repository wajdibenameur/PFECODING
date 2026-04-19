# Architecture du projet de monitoring multi-source

## 1. Introduction générale

Ce projet est une application de supervision multi-source fondée sur un backend Spring Boot et un frontend Angular. Son objectif est de centraliser la collecte, la persistance, l’agrégation et la visualisation de données issues de plusieurs sources de monitoring hétérogènes, principalement Zabbix, Observium et ZKBio.

L’architecture met en place plusieurs mécanismes complémentaires :

- des contrôleurs REST pour exposer les données et les actions de collecte ;
- des services métier pour orchestrer la logique applicative ;
- des adapters et des clients pour dialoguer avec les systèmes externes ;
- des repositories JPA pour la persistance dans MySQL ;
- un cache Redis pour les lectures unifiées et les snapshots de secours ;
- des mécanismes de résilience avec WebClient et Resilience4j ;
- des schedulers et listeners pour les traitements automatiques ;
- des WebSockets pour la diffusion temps réel vers le frontend Angular.

Cette architecture reste progressive et pragmatique : elle cherche à conserver la structure existante tout en ajoutant des mécanismes de robustesse, de cache et d’unification des sources.

## 2. Architecture globale

L’application est organisée autour de deux grands blocs :

- un backend Java Spring Boot chargé de l’intégration, de la persistance, de l’agrégation et de la diffusion temps réel ;
- un frontend Angular chargé de l’affichage, du pilotage des collectes et de la réception des mises à jour en temps réel.

### 2.1. Vue logique d’ensemble

Le flux fonctionnel principal est le suivant :

1. le frontend appelle les APIs REST du backend ou s’abonne aux flux WebSocket ;
2. les controllers Spring reçoivent les requêtes et délèguent aux services ;
3. les services appellent soit les adapters d’intégration, soit la couche d’agrégation unifiée, soit les repositories JPA ;
4. les adapters s’appuient sur des clients HTTP pour interroger les systèmes externes ;
5. les réponses sont transformées en DTO internes, éventuellement persistées en base, mises en cache dans Redis, puis renvoyées à l’API ou publiées sur WebSocket ;
6. les schedulers et listeners alimentent en parallèle les données persistées et les flux temps réel.

### 2.2. Point d’entrée principal

Le backend démarre depuis la classe `tn.iteam.PfeprojectApplication`, annotée avec :

- `@SpringBootApplication`
- `@EnableAsync`
- `@EnableScheduling`

Cela montre que l’application combine des traitements web classiques, des traitements asynchrones et des tâches planifiées.

## 3. Architecture backend

Le backend est structuré en packages métier et techniques. Les principaux packages observés dans le projet sont :

- `tn.iteam.controller`
- `tn.iteam.service`
- `tn.iteam.service.impl`
- `tn.iteam.adapter`
- `tn.iteam.client`
- `tn.iteam.monitoring`
- `tn.iteam.repository`
- `tn.iteam.domain`
- `tn.iteam.dto`
- `tn.iteam.scheduler`
- `tn.iteam.listener`
- `tn.iteam.websocket`
- `tn.iteam.cache`
- `tn.iteam.config`

### 3.1. Couche controller

La couche controller expose les endpoints REST de l’application. Les principales classes identifiées sont :

- `tn.iteam.controller.MonitoringController`
- `tn.iteam.controller.DashboardController`
- `tn.iteam.controller.ObserviumController`
- `tn.iteam.controller.ZabbixMetricsController`
- `tn.iteam.controller.ZabbixProblemController`
- `tn.iteam.controller.ZkBioController`
- `tn.iteam.controller.TicketController`

Le contrôleur central pour le monitoring multi-source est `MonitoringController`. Il expose notamment :

- `GET /api/monitoring/hosts`
- `GET /api/monitoring/problems`
- `GET /api/monitoring/metrics`
- `GET /api/monitoring/sources/health`
- `POST /api/monitoring/collect`
- `POST /api/monitoring/collect/zabbix`
- `POST /api/monitoring/collect/observium`
- `POST /api/monitoring/collect/zkbio`
- `POST /api/monitoring/collect/camera`

Son rôle est volontairement léger. Il ne contient pas la logique métier profonde, mais délègue aux services spécialisés.

### 3.2. Couche service

La couche service assure l’orchestration métier et technique. Les principales classes d’implémentation sont :

- `tn.iteam.service.impl.MonitoringServiceImpl`
- `tn.iteam.monitoring.service.MonitoringAggregationService`
- `tn.iteam.monitoring.service.MonitoringCacheService`
- `tn.iteam.service.impl.SourceAvailabilityServiceImpl`
- `tn.iteam.service.impl.ZabbixLiveSynchronizationServiceImpl`
- `tn.iteam.service.impl.ZabbixMetricsServiceImpl`
- `tn.iteam.service.impl.ZabbixProblemServiceImpl`
- `tn.iteam.service.ZkBioServiceInterface`
- `tn.iteam.service.ZkBioServiceImpl`
- `tn.iteam.service.ZabbixMonitoringService` → `ZabbixMonitoringServiceImpl`
- `tn.iteam.service.ObserviumMonitoringService` → `ObserviumMonitoringServiceImpl`
- `tn.iteam.service.ZkBioMonitoringService` → `ZkBioMonitoringServiceImpl`
- `tn.iteam.service.impl.DashboardServiceImpl`
- `tn.iteam.service.impl.ObserviumSummaryServiceImpl`
- `tn.iteam.service.impl.TicketServiceImpl`

Le rôle général de cette couche est de :

- piloter les collectes ;
- déclencher les synchronisations ;
- construire les réponses unifiées ;
- gérer la disponibilité des sources ;
- manipuler les snapshots persistés ;
- déléguer aux adapters et repositories.

#### MonitoringServiceImpl

`MonitoringServiceImpl` orchestre les collectes manuelles et automatiques par source : Zabbix, Observium, ZKBio et Camera. Il utilise notamment :

- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`
- `ServiceStatusRepository`
- `SourceAvailabilityService`
- `IntegrationExecutionHelper`

Cette classe montre bien le rôle central du service comme coordinateur d’intégrations.

#### MonitoringAggregationService et MonitoringCacheService

L’architecture unifiée du monitoring repose principalement sur :

- `tn.iteam.monitoring.service.MonitoringAggregationService`
- `tn.iteam.monitoring.service.MonitoringCacheService`

Le flux de lecture unifiée suit cette chaîne :

1. `MonitoringController`
2. `MonitoringAggregationService`
3. `MonitoringCacheService`
4. liste de `MonitoringProvider`

`MonitoringAggregationService` reste volontairement léger. Il renvoie une réponse unifiée de type `UnifiedMonitoringResponse` contenant :

- `data`
- `degraded`
- `freshness`
- `coverage` pour certaines réponses comme `/metrics`

`MonitoringCacheService` centralise l’agrégation multi-source et le cache Redis des lectures. Il applique `@Cacheable` sur :

- les hosts
- les problems
- les metrics

Il ne doit mettre en cache que des résultats non dégradés.

#### SourceAvailabilityServiceImpl

`SourceAvailabilityServiceImpl` joue un rôle transverse majeur. Il maintient l’état de santé des sources externes, avec trois états :

- `AVAILABLE`
- `DEGRADED`
- `UNAVAILABLE`

Sa responsabilité est d’indiquer si une source est :

- fonctionnelle en live ;
- indisponible mais encore servie via Redis fallback ;
- complètement indisponible.

Cette classe alimente également la publication WebSocket de l’état des sources.

### 3.3. Couche monitoring.provider

Le package `tn.iteam.monitoring.provider` contient les providers qui exposent chaque source au format de monitoring unifié :

- `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`
- `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`
- `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`

Chaque provider implémente l’interface de monitoring unifié et fournit au minimum :

- `getHosts()`
- `getProblems()`
- `getMetrics()`

Leur rôle est de traduire chaque source vers des DTOs homogènes :

- `UnifiedMonitoringHostDTO`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`

#### Architecture unifiée des providers

Après le refactoring, tous les providers utilisent un **pattern unifié** :

1. Le provider délègue à un **service de monitoring dédié**
2. Le service de monitoring délègue à l'**adapter** correspondant
3. L'adapter gère le fallback Redis et la transformation des données

Les services de monitoring créés sont :

- `tn.iteam.service.ZabbixMonitoringService` → `ZabbixMonitoringServiceImpl`
- `tn.iteam.service.ObserviumMonitoringService` → `ObserviumMonitoringServiceImpl`
- `tn.iteam.service.ZkBioMonitoringService` → `ZkBioMonitoringServiceImpl`

Les mappers utilisés sont :

- `tn.iteam.mapper.ZabbixMonitoringMapper`
- `tn.iteam.mapper.ObserviumMonitoringMapper`
- `tn.iteam.mapper.ZkBioMonitoringMapper`

Ce pattern assure une cohérence dans le flux de données et facilite la maintenance.

### 3.4. Couche adapter

Les adapters se trouvent sous `tn.iteam.adapter` avec des sous-packages spécialisés :

- `tn.iteam.adapter.zabbix`
- `tn.iteam.adapter.observium`
- `tn.iteam.adapter.zkbio`
- `tn.iteam.adapter.camera`

Les principales classes sont :

- `tn.iteam.adapter.zabbix.ZabbixAdapter`
- `tn.iteam.adapter.observium.ObserviumAdapter`
- `tn.iteam.adapter.zkbio.ZkBioAdapter`
- `tn.iteam.adapter.camera.CameraAdapter`

Les adapters jouent un rôle fondamental de médiation entre les clients HTTP externes et les services internes. Ils assurent :

- la transformation des données externes ;
- le mapping vers les DTOs internes ;
- le pilotage du fallback Redis ;
- la levée d’exceptions métier quand aucune donnée valide n’est disponible ;
- parfois la sauvegarde en base ou la publication WebSocket.

Par exemple, `ObserviumAdapter` utilise `ObserviumClientX`, applique le fallback Redis sur les snapshots d’Observium, transforme les réponses en `ServiceStatusDTO` et `ObserviumProblemDTO`, puis peut publier des problèmes monitoring unifiés.

### 3.5. Couche client

Les clients sont la couche technique la plus proche des APIs externes. Les classes principales sont :

- `tn.iteam.adapter.zabbix.ZabbixClient`
- `tn.iteam.client.ObserviumClientX`
- `tn.iteam.client.ZkBioClient`

Leur rôle est de gérer :

- les appels HTTP ;
- les timeouts ;
- le parsing JSON ;
- l’application de Resilience4j ;
- la sauvegarde des snapshots Redis en cas de succès ;
- les fallbacks Redis côté intégration lorsque cela est nécessaire.

`ObserviumClientX` illustre bien cette responsabilité. Il utilise `WebClient`, applique `@CircuitBreaker` et `@Retry`, appelle l’API Observium, enregistre les snapshots Redis via `IntegrationCacheService`, puis met à jour `SourceAvailabilityService`.

### 3.6. Couche repository

La persistance relationnelle repose sur Spring Data JPA. Les repositories identifiés sont :

- `tn.iteam.repository.MonitoredHostRepository`
- `tn.iteam.repository.ObserviumProblemRepository`
- `tn.iteam.repository.ServiceStatusRepository`
- `tn.iteam.repository.TicketRepository`
- `tn.iteam.repository.UserRepository`
- `tn.iteam.repository.ZabbixMetricRepository`
- `tn.iteam.repository.ZabbixProblemRepository`
- `tn.iteam.repository.ZkBioProblemRepository`

Ces repositories sont utilisés pour lire et écrire les entités persistées dans MySQL.

### 3.7. Couche domain / entity / dto

Le package `tn.iteam.domain` contient les entités JPA et objets persistants. Les principales classes sont :

- `tn.iteam.domain.ServiceStatus`
- `tn.iteam.domain.MonitoredHost`
- `tn.iteam.domain.ZabbixMetric`
- `tn.iteam.domain.ZabbixProblem`
- `tn.iteam.domain.ObserviumProblem`
- `tn.iteam.domain.ZkBioProblem`
- `tn.iteam.domain.Ticket`
- `tn.iteam.domain.User`
- `tn.iteam.domain.Role`
- `tn.iteam.domain.Notification`
- `tn.iteam.domain.Intervention`
- `tn.iteam.domain.ApiResponse`

Les DTOs applicatifs sont répartis dans :

- `tn.iteam.dto`
- `tn.iteam.monitoring.dto`

Cette séparation permet de distinguer :

- le modèle persistant MySQL ;
- le modèle d’échange entre couches ;
- le modèle unifié exposé par l’API monitoring.

### 3.8. Couche scheduler

Les traitements automatiques planifiés se trouvent dans `tn.iteam.scheduler` :

- `tn.iteam.scheduler.ZabbixScheduler`
- `tn.iteam.scheduler.ObserviumScheduler`
- `tn.iteam.scheduler.ObserviumHostsScheduler`
- `tn.iteam.scheduler.ZkBioScheduler`

Leur rôle est de :

- lancer des collectes périodiques ;
- rafraîchir les snapshots persistés ;
- republier des données sur WebSocket ;
- assurer une continuité de service même sans intervention utilisateur.

Par exemple, `ZabbixScheduler` essaie d’abord de synchroniser les données live, puis publie soit les données persistées, soit un fallback via l’adapter si nécessaire.

### 3.9. Couche listener

Les listeners d’application se trouvent dans `tn.iteam.listener` :

- `tn.iteam.listener.ZabbixStartupListener`
- `tn.iteam.listener.ZabbixMetricsStartupListener`
- `tn.iteam.listener.ZabbixProblemStartupListener`

Ils écoutent le démarrage de l’application, notamment via `ApplicationReadyEvent`, pour effectuer un préchargement ou une synchronisation initiale. Cela permet d’avoir des données exploitables plus rapidement après le lancement du backend.

### 3.10. Couche websocket

La publication temps réel est gérée dans `tn.iteam.websocket` :

- `tn.iteam.websocket.MonitoringWebSocketPublisher`
- `tn.iteam.websocket.ZabbixWebSocketPublisher`
- `tn.iteam.websocket.ZkBioWebSocketPublisher`

`MonitoringWebSocketPublisher` publie notamment sur les topics :

- `/topic/monitoring/problems`
- `/topic/monitoring/metrics`
- `/topic/monitoring/sources`

Le backend utilise ici `SimpMessagingTemplate` pour pousser les événements vers le frontend Angular.

### 3.11. Configuration technique

Les classes de configuration importantes sont :

- `tn.iteam.config.WebClientConfig`
- `tn.iteam.config.RestTemplateConfig`
- `tn.iteam.config.RedisCacheConfig`

#### WebClientConfig

`WebClientConfig` définit :

- un `WebClient` principal pour les intégrations standard ;
- un `WebClient` spécifique à ZKBio nommé `zkbioUnsafeTlsWebClientForInternalUseOnly`.

Ce second client désactive la validation TLS. Il est explicitement réservé à ZKBio, ce qui constitue un compromis technique transitoire.

#### RestTemplateConfig

`RestTemplateConfig` maintient des `RestTemplate` avec timeouts configurés. Cela montre que l’application utilise actuellement à la fois :

- `WebClient` pour plusieurs intégrations modernes ;
- `RestTemplate` pour compatibilité avec certaines parties existantes.

#### RedisCacheConfig

`RedisCacheConfig` configure le cache Spring avec Redis. Les caches nommés sont :

- `monitoring:hosts`
- `monitoring:metrics`
- `monitoring:problems`
- `monitoring:source-health`

Les TTL courts sont lus depuis `application.properties`.

## 4. Intégrations externes

Le projet intègre principalement trois systèmes externes.

### 4.1. Zabbix

Zabbix est la source la plus complète dans le projet. Les classes principales liées à cette intégration sont :

- client : `tn.iteam.adapter.zabbix.ZabbixClient`
- adapter : `tn.iteam.adapter.zabbix.ZabbixAdapter`
- provider : `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`
- services : `tn.iteam.service.impl.ZabbixMetricsServiceImpl`, `tn.iteam.service.impl.ZabbixProblemServiceImpl`, `tn.iteam.service.impl.ZabbixLiveSynchronizationServiceImpl`
- scheduler : `tn.iteam.scheduler.ZabbixScheduler`

Zabbix fournit actuellement :

- des hosts unifiés ;
- des problems unifiés ;
- des metrics unifiées ;
- des snapshots persistés en base ;
- des publications WebSocket dédiées ;
- des fallbacks Redis sur les appels externes.

### 4.2. Observium

Observium est utilisé surtout pour les équipements et les alertes. Les classes principales sont :

- client : `tn.iteam.client.ObserviumClientX`
- adapter : `tn.iteam.adapter.observium.ObserviumAdapter`
- provider : `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`
- scheduler : `tn.iteam.scheduler.ObserviumScheduler`
- scheduler de hosts : `tn.iteam.scheduler.ObserviumHostsScheduler`

Observium fournit actuellement :

- des hosts unifiés ;
- des problems unifiés ;
- des snapshots Redis de fallback ;
- des appels live via WebClient.

Ambiguïté importante à noter :

- Observium ne fournit pas encore de metrics unifiées. Dans `ObserviumMonitoringProvider`, `getMetrics()` retourne actuellement une liste vide. L’endpoint `/api/monitoring/metrics` n’est donc pas encore homogène entre sources.

### 4.3. ZKBio

ZKBio est une source spécialisée, surtout orientée contrôle d’accès et présence. Les classes principales sont :

- client : `tn.iteam.client.ZkBioClient`
- adapter : `tn.iteam.adapter.zkbio.ZkBioAdapter`
- service métier : `tn.iteam.service.ZkBioServiceInterface`
- implémentation : `tn.iteam.service.ZkBioServiceImpl`
- provider : `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`
- scheduler : `tn.iteam.scheduler.ZkBioScheduler`
- publisher : `tn.iteam.websocket.ZkBioWebSocketPublisher`

ZKBio fournit actuellement :

- des hosts unifiés ;
- des problems unifiés ;
- des endpoints dédiés pour le statut, les devices et l’attendance ;
- des snapshots Redis de fallback ;
- un `WebClient` dédié avec TLS permissif.

Comme Observium, ZKBio ne fournit pas encore de metrics unifiées.

## 5. Redis, WebClient et Resilience4j

### 5.1. Redis

Redis est utilisé à deux niveaux distincts.

#### a. Cache Spring sur les lectures unifiées

Le cache Redis sert à éviter de recalculer trop souvent les lectures unifiées via `MonitoringCacheService`. Les TTL configurés dans `application.properties` sont courts :

- `app.cache.ttl.hosts=30s`
- `app.cache.ttl.metrics=30s`
- `app.cache.ttl.problems=30s`
- `app.cache.ttl.source-health=15s`

#### b. Snapshots d’intégration pour fallback

Redis sert aussi de stockage temporaire de snapshots techniques grâce à :

- `tn.iteam.cache.IntegrationCacheService`
- `tn.iteam.cache.RedisIntegrationCacheService`

Le principe est :

1. un client externe réussit ;
2. il sauvegarde un snapshot Redis ;
3. si l’API devient indisponible, l’application essaie de servir le dernier snapshot valide.

### 5.2. WebClient et RestTemplate

Le projet utilise à la fois :

- `WebClient` pour plusieurs intégrations récentes ;
- `RestTemplate` pour compatibilité avec des parties plus anciennes.

Cela traduit une architecture hybride et progressive.

### 5.3. Resilience4j

La résilience des appels externes repose sur Resilience4j. Les instances configurées dans `application.properties` sont :

- `zabbixApi`
- `observiumApi`
- `zkbioApi`

Les mécanismes activés sont :

- `CircuitBreaker`
- `Retry`

Les clients concernés incluent au moins :

- `ZabbixClient`
- `ObserviumClientX`
- `ZkBioClient`

L’objectif est de limiter les effets d’une API externe indisponible, tout en permettant un fallback Redis ou une remontée d’exception contrôlée.

## 6. Flux principaux de données

### 6.1. Flux de lecture unifiée

Le flux standard pour les endpoints monitoring unifiés est :

1. Angular appelle `/api/monitoring/hosts`, `/problems` ou `/metrics` ;
2. `MonitoringController` reçoit la requête ;
3. `MonitoringAggregationService` délègue à `MonitoringCacheService` ;
4. `MonitoringCacheService` interroge les `MonitoringProvider` ;
5. chaque provider appelle soit des services persistés, soit des adapters d’intégration ;
6. les données sont agrégées, enrichies avec `degraded`, `freshness` et parfois `coverage`, puis renvoyées au frontend.

### 6.2. Flux de collecte manuelle

1. le frontend déclenche `POST /api/monitoring/collect/{source}` ;
2. `MonitoringController` appelle `MonitoringServiceImpl` ;
3. `MonitoringServiceImpl` lance la collecte asynchrone ;
4. l’adapter appelle le client externe ;
5. les données sont transformées, sauvegardées si nécessaire, puis republiees ou rendues disponibles pour lecture.

### 6.3. Flux de fallback Redis

1. un client externe tente un appel live ;
2. en cas de succès, il sauvegarde un snapshot Redis ;
3. en cas d’échec ultérieur, l’adapter ou le client tente de relire le snapshot Redis ;
4. si le snapshot existe, l’état de la source passe typiquement à `DEGRADED` ;
5. sinon une exception explicite est levée.

### 6.4. Flux de publication temps réel

1. un scheduler ou un service produit une donnée ou un état ;
2. `MonitoringWebSocketPublisher` ou un publisher spécialisé envoie le message sur un topic ;
3. Angular reçoit ce message via STOMP ;
4. le store frontend fusionne l’événement dans l’état courant.

## 7. Architecture frontend Angular

Le frontend Angular est structuré autour de plusieurs packages dans `frontend/src/app` :

- `core`
- `features`
- `layout`
- `shared`

### 7.1. Couche core

Le package `core` regroupe les briques transverses :

- `core/config`
- `core/http`
- `core/models`
- `core/realtime`
- `core/auth`

On y trouve notamment :

- `stomp-client.service.ts`
- les modèles TypeScript comme `source-availability.model.ts`, `monitoring-host.model.ts`, `zabbix-metric.model.ts`
- la configuration d’URL API via `APP_CONFIG`

Cette couche fournit les éléments réutilisables dans toute l’application.

### 7.2. Couche features/monitoring

La fonctionnalité principale du projet Angular est `features/monitoring`, organisée en :

- `data`
- `state`
- `ui`

#### Data

Le package `data` contient :

- `monitoring-api.service.ts`
- `monitoring-realtime.service.ts`

`MonitoringApiService` centralise les appels REST vers :

- `/api/monitoring`
- `/api/zabbix`
- `/api/zkbio`
- `/dashboard`

`MonitoringRealtimeService` encapsule les abonnements aux topics WebSocket.

#### State

Le package `state` contient :

- `monitoring.store.ts`

`MonitoringStore` joue le rôle de façade d’état côté Angular. Il :

- charge les snapshots initiaux ;
- fusionne les flux temps réel ;
- calcule des KPIs et des vues dérivées ;
- pilote les rafraîchissements après une collecte.

#### UI

Le package `ui` contient les pages d’affichage :

- `monitoring-dashboard-page.component.ts`
- `monitoring-zabbix-page.component.ts`
- `monitoring-observium-page.component.ts`
- `monitoring-zkbio-page.component.ts`

Ces composants s’appuient sur les services data et sur le store pour présenter les tableaux de bord.

### 7.3. Couche layout

Le package `layout` contient la structure visuelle de l’application :

- `shell`
- `navbar`
- `sidebar`
- `user-panel`

Le `ShellComponent` sert de conteneur principal des routes.

### 7.4. Couche shared

Le package `shared` contient les composants, directives et utilitaires réutilisables, notamment :

- `source-health-panel`
- `collection-control-bar`
- `asset-inventory-table`
- `data-coverage-notice`
- `alert-summary-panel`
- `global-kpi-strip`

Ces composants permettent de mutualiser l’affichage du monitoring.

### 7.5. Routage Angular

Le routage est défini dans `frontend/src/app/app.routes.ts`. Les routes principales sont :

- `/dashboard`
- `/monitoring/zabbix`
- `/monitoring/observium`
- `/monitoring/zkbio`

D’autres écrans sont encore des placeholders, ce qui suggère une architecture extensible en cours d’évolution.

## 8. Interaction backend / frontend

L’interaction repose sur deux canaux complémentaires.

### 8.1. Canal REST

Le frontend Angular consomme les données via `HttpClient`, principalement dans `MonitoringApiService`. Le backend renvoie :

- des snapshots unifiés ;
- des données spécifiques à chaque source ;
- des réponses de collecte ;
- des états de santé des sources.

### 8.2. Canal WebSocket

Le temps réel est assuré par STOMP et SockJS côté Angular via `StompClientService`, et par les publishers Spring côté backend.

Le frontend s’abonne à des topics comme :

- `/topic/zabbix/problems`
- `/topic/zabbix/metrics`
- `/topic/monitoring/problems`
- `/topic/monitoring/sources`
- `/topic/zkbio/problems`
- `/topic/zkbio/attendance`
- `/topic/zkbio/devices`
- `/topic/zkbio/status`

Cela permet d’actualiser dynamiquement les écrans sans attendre un rechargement complet.

## 9. Technologies utilisées

### Backend

Selon `pom.xml`, les principales technologies backend sont :

- Java 17
- Spring Boot 3.2.5
- Spring Web
- Spring WebFlux
- Spring Data JPA
- Spring WebSocket
- Spring Cache
- Spring Data Redis
- Resilience4j
- Micrometer / Prometheus
- Lombok
- MySQL Connector/J

### Frontend

Selon `frontend/package.json`, les principales technologies frontend sont :

- Angular 20
- TypeScript
- RxJS
- STOMP
- SockJS

### Infrastructure logique

- MySQL pour la persistance
- Redis pour le cache et les snapshots de fallback
- WebSocket pour le temps réel
- WebClient / RestTemplate pour les intégrations externes

## 10. Conclusion pour le rapport PFE

L’architecture de ce projet est une architecture distribuée en couches, pensée pour un contexte de supervision multi-source. Le backend Spring Boot joue le rôle de noyau d’intégration, de persistance et de diffusion des données, tandis que le frontend Angular constitue la couche de présentation et d’exploitation utilisateur.

Les points forts de cette architecture sont :

- une séparation claire entre contrôleurs, services, adapters, clients et persistance ;
- une capacité d’intégration de plusieurs systèmes externes ;
- la mise en place d’un cache Redis et de fallbacks techniques ;
- la résilience des appels grâce à Resilience4j ;
- la diffusion temps réel via WebSocket ;
- une structure frontend modulaire par fonctionnalités.

Le projet présente toutefois une hétérogénéité réelle entre les sources : Zabbix est actuellement la source la plus complète, notamment pour les métriques et les snapshots persistés, alors que Observium et ZKBio restent plus limités sur certains axes. Cette situation n’est pas une incohérence architecturale majeure, mais plutôt l’expression d’une architecture évolutive construite progressivement.

Dans un rapport PFE, cette architecture peut être présentée comme une solution de supervision centralisée, modulaire et résiliente, capable de fédérer plusieurs sources de monitoring au sein d’une même plateforme web.

## 11. Classes principales concernées

### Backend

- `tn.iteam.PfeprojectApplication`
- `tn.iteam.controller.MonitoringController`
- `tn.iteam.controller.ZkBioController`
- `tn.iteam.controller.ZabbixMetricsController`
- `tn.iteam.controller.ZabbixProblemController`
- `tn.iteam.controller.ObserviumController`
- `tn.iteam.service.impl.MonitoringServiceImpl`
- `tn.iteam.monitoring.service.MonitoringAggregationService`
- `tn.iteam.monitoring.service.MonitoringCacheService`
- `tn.iteam.service.impl.SourceAvailabilityServiceImpl`
- `tn.iteam.service.impl.ZabbixLiveSynchronizationServiceImpl`
- `tn.iteam.service.impl.ZabbixMetricsServiceImpl`
- `tn.iteam.service.impl.ZabbixProblemServiceImpl`
- `tn.iteam.service.ZkBioServiceInterface`
- `tn.iteam.service.ZkBioServiceImpl`
- `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`
- `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`
- `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`
- `tn.iteam.adapter.zabbix.ZabbixAdapter`
- `tn.iteam.adapter.observium.ObserviumAdapter`
- `tn.iteam.adapter.zkbio.ZkBioAdapter`
- `tn.iteam.adapter.camera.CameraAdapter`
- `tn.iteam.adapter.zabbix.ZabbixClient`
- `tn.iteam.client.ObserviumClientX`
- `tn.iteam.client.ZkBioClient`
- `tn.iteam.repository.MonitoredHostRepository`
- `tn.iteam.repository.ServiceStatusRepository`
- `tn.iteam.repository.ZabbixMetricRepository`
- `tn.iteam.repository.ZabbixProblemRepository`
- `tn.iteam.repository.ObserviumProblemRepository`
- `tn.iteam.repository.ZkBioProblemRepository`
- `tn.iteam.domain.ServiceStatus`
- `tn.iteam.domain.MonitoredHost`
- `tn.iteam.domain.ZabbixMetric`
- `tn.iteam.domain.ZabbixProblem`
- `tn.iteam.domain.ObserviumProblem`
- `tn.iteam.domain.ZkBioProblem`
- `tn.iteam.scheduler.ZabbixScheduler`
- `tn.iteam.scheduler.ObserviumScheduler`
- `tn.iteam.scheduler.ObserviumHostsScheduler`
- `tn.iteam.scheduler.ZkBioScheduler`
- `tn.iteam.listener.ZabbixStartupListener`
- `tn.iteam.listener.ZabbixMetricsStartupListener`
- `tn.iteam.listener.ZabbixProblemStartupListener`
- `tn.iteam.websocket.MonitoringWebSocketPublisher`
- `tn.iteam.websocket.ZabbixWebSocketPublisher`
- `tn.iteam.websocket.ZkBioWebSocketPublisher`
- `tn.iteam.config.WebClientConfig`
- `tn.iteam.config.RestTemplateConfig`
- `tn.iteam.config.RedisCacheConfig`
- `tn.iteam.cache.IntegrationCacheService`
- `tn.iteam.cache.RedisIntegrationCacheService`

### Frontend

- `frontend/src/app/app.routes.ts`
- `frontend/src/app/features/monitoring/data/monitoring-api.service.ts`
- `frontend/src/app/features/monitoring/data/monitoring-realtime.service.ts`
- `frontend/src/app/core/realtime/stomp-client.service.ts`
- `frontend/src/app/features/monitoring/state/monitoring.store.ts`
- `frontend/src/app/features/monitoring/ui/monitoring-dashboard-page.component.ts`
- `frontend/src/app/features/monitoring/ui/monitoring-zabbix-page.component.ts`
- `frontend/src/app/features/monitoring/ui/monitoring-observium-page.component.ts`
- `frontend/src/app/features/monitoring/ui/monitoring-zkbio-page.component.ts`
