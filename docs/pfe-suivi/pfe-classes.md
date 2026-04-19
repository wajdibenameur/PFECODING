# Classes importantes du projet par couche

Ce document recense les classes importantes du projet en les regroupant par couche. Pour chaque classe, sont indiqués :

- son rôle principal ;
- ses dépendances principales visibles dans le code.

## 1. Controllers

### `tn.iteam.controller.MonitoringController`
- Rôle : point d’entrée REST principal pour le monitoring unifié ; expose les lectures globales (`hosts`, `problems`, `metrics`, `sources/health`) et les endpoints de collecte.
- Dépendances principales : `MonitoringService`, `MonitoringAggregationService`, `SourceAvailabilityService`.

### `tn.iteam.controller.DashboardController`
- Rôle : expose les endpoints du tableau de bord global.
- Dépendances principales : dépend du service de dashboard, principalement `DashboardServiceImpl` via l’interface correspondante.

### `tn.iteam.controller.ObserviumController`
- Rôle : expose les endpoints dédiés à Observium.
- Dépendances principales : services ou adapters liés à Observium.

### `tn.iteam.controller.ZabbixMetricsController`
- Rôle : expose les métriques Zabbix côté API dédiée.
- Dépendances principales : `ZabbixMetricsService`, `MonitoringAggregationService` selon les lectures exposées.

### `tn.iteam.controller.ZabbixProblemController`
- Rôle : expose les problèmes/alertes Zabbix côté API dédiée.
- Dépendances principales : `ZabbixProblemService`, `MonitoringAggregationService` selon les lectures exposées.

### `tn.iteam.controller.ZkBioController`
- Rôle : expose les endpoints spécifiques à ZKBio : statut, devices, attendance, problèmes, collecte.
- Dépendances principales : `ZkBioServiceInterface`, éventuellement `MonitoringService` pour la collecte selon le point d’entrée.

### `tn.iteam.controller.TicketController`
- Rôle : expose les endpoints liés aux tickets.
- Dépendances principales : `TicketServiceImpl` via le service de ticket.

## 2. Services

### `tn.iteam.service.impl.MonitoringServiceImpl`
- Rôle : orchestre la collecte multi-source et les collectes par source.
- Dépendances principales : `ZabbixAdapter`, `ObserviumAdapter`, `ZkBioAdapter`, `CameraAdapter`, `ServiceStatusRepository`, `ServiceStatusMapper`, `TaskExecutor`, `SourceAvailabilityService`, `IntegrationExecutionHelper`.

### `tn.iteam.monitoring.service.MonitoringAggregationService`
- Rôle : façade légère pour les lectures unifiées du monitoring.
- Dépendances principales : `MonitoringCacheService`.

### `tn.iteam.monitoring.service.MonitoringCacheService`
- Rôle : agrège les données des `MonitoringProvider`, applique le cache Redis Spring sur les lectures unifiées, transporte les métadonnées `degraded`, `freshness`.
- Dépendances principales : `List<MonitoringProvider>`, `SourceAvailabilityService`.

### `tn.iteam.service.impl.SourceAvailabilityServiceImpl`
- Rôle : maintient et publie l’état des sources (`AVAILABLE`, `DEGRADED`, `UNAVAILABLE`).
- Dépendances principales : `MonitoringWebSocketPublisher`.

### `tn.iteam.service.impl.ZabbixLiveSynchronizationServiceImpl`
- Rôle : synchronise les données Zabbix en mode live pour les besoins startup et scheduler.
- Dépendances principales : services/adapters Zabbix selon le flux de synchronisation.

### `tn.iteam.service.impl.ZabbixMetricsServiceImpl`
- Rôle : gère les métriques Zabbix persistées et les snapshots associés.
- Dépendances principales : `ZabbixMetricRepository`, composants de mapping et de synchronisation Zabbix.

### `tn.iteam.service.impl.ZabbixProblemServiceImpl`
- Rôle : gère les problèmes Zabbix persistés et les lectures filtrées actives.
- Dépendances principales : `ZabbixProblemRepository`, composants de mapping et de synchronisation Zabbix.

### `tn.iteam.service.ZkBioServiceInterface`
- Rôle : contrat métier pour les fonctionnalités ZKBio (statut serveur, devices, attendance, utilisateurs).
- Dépendances principales : aucune directement, c'est une interface.

### `tn.iteam.service.ZkBioServiceImpl`
- Rôle : implémente les fonctionnalités métier ZKBio (business only - plus de monitoring).
- Dépendances principales : `ZkBioAdapter`, `ZkBioClient`, mappers et repositories ZKBio selon la méthode.

### `tn.iteam.service.ZabbixMonitoringService` → `ZabbixMonitoringServiceImpl`
- Rôle : service dédié pour les opérations de monitoring Zabbix (hosts, problems, metrics).
- Dépendances principales : `ZabbixAdapter`, `ZabbixMonitoringMapper`.

### `tn.iteam.service.ObserviumMonitoringService` → `ObserviumMonitoringServiceImpl`
- Rôle : service dédié pour les opérations de monitoring Observium (hosts, problems).
- Dépendances principales : `ObserviumAdapter`, `ObserviumMonitoringMapper`.

### `tn.iteam.service.ZkBioMonitoringService` → `ZkBioMonitoringServiceImpl`
- Rôle : service dédié pour les opérations de monitoring ZKBio (hosts, problems).
- Dépendances principales : `ZkBioAdapter`, `ZkBioMonitoringMapper`.

### `tn.iteam.service.impl.DashboardServiceImpl`
- Rôle : construit les données de synthèse utilisées par le dashboard.
- Dépendances principales : services de monitoring, repositories et agrégation selon les indicateurs exposés.

### `tn.iteam.service.impl.ObserviumSummaryServiceImpl`
- Rôle : gère les vues de synthèse liées à Observium.
- Dépendances principales : composants Observium et repositories associés.

### `tn.iteam.service.impl.TicketServiceImpl`
- Rôle : porte la logique métier liée aux tickets.
- Dépendances principales : `TicketRepository`, éventuellement `UserRepository` ou autres composants métier.

### `tn.iteam.service.support.IntegrationExecutionHelper`
- Rôle : centralise une partie de l’exécution robuste des collectes d’intégration.
- Dépendances principales : utilisé par `MonitoringServiceImpl` avec `SourceAvailabilityService` et un logger.

## 3. Monitoring Providers

### `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`
- Rôle : expose Zabbix dans le format monitoring unifié.
- Dépendances principales : `ZabbixMonitoringService` (nouveau pattern unifié).

### `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`
- Rôle : expose Observium dans le format monitoring unifié.
- Dépendances principales : `ObserviumMonitoringService` (nouveau pattern unifié).

### `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`
- Rôle : expose ZKBio dans le format monitoring unifié.
- Dépendances principales : `ZkBioMonitoringService` (nouveau pattern unifié).

## 4. Adapters

### `tn.iteam.adapter.zabbix.ZabbixAdapter`
- Rôle : transforme les réponses Zabbix, gère les lectures monitoring et le fallback Redis Zabbix.
- Dépendances principales : `ZabbixClient`, mappers Zabbix, services/repositories Zabbix, `IntegrationCacheService`, `SourceAvailabilityService` selon les méthodes.

### `tn.iteam.adapter.observium.ObserviumAdapter`
- Rôle : transforme les réponses Observium en DTOs applicatifs, gère le fallback Redis et la sauvegarde/publishing des problèmes.
- Dépendances principales : `ObserviumClientX`, `ObserviumMapper`, `ObserviumProblemRepository`, `MonitoringWebSocketPublisher`, `IntegrationCacheService`, `SourceAvailabilityService`.

### `tn.iteam.adapter.zkbio.ZkBioAdapter`
- Rôle : transforme les réponses ZKBio, gère le fallback Redis et alimente les DTOs monitoring / service status.
- Dépendances principales : `ZkBioClient`, mappers ZKBio, repositories ou services ZKBio selon les méthodes, `IntegrationCacheService`, `SourceAvailabilityService`.

### `tn.iteam.adapter.camera.CameraAdapter`
- Rôle : scanne et convertit l’état des caméras dans le format interne.
- Dépendances principales : logique réseau/scan, DTOs `ServiceStatusDTO`, services de monitoring selon le flux.

## 5. Clients

### `tn.iteam.adapter.zabbix.ZabbixClient`
- Rôle : client technique Zabbix pour les appels HTTP/JSON-RPC, avec résilience et snapshots Redis.
- Dépendances principales : `WebClient` ou `RestTemplate` selon l’implémentation en place, `ObjectMapper`, `IntegrationCacheService`, `SourceAvailabilityService`.

### `tn.iteam.client.ObserviumClientX`
- Rôle : client technique Observium ; exécute les appels live, applique `CircuitBreaker`/`Retry`, sauvegarde les snapshots Redis et met à jour la disponibilité.
- Dépendances principales : `WebClient`, `ObjectMapper`, `IntegrationCacheService`, `SourceAvailabilityService`, configuration Observium (`observium.url`, `observium.token`).

### `tn.iteam.client.ZkBioClient`
- Rôle : client technique ZKBio ; interroge l’API ZKBio, gère les snapshots Redis et la disponibilité.
- Dépendances principales : `WebClient` dédié à ZKBio, `ObjectMapper`, `IntegrationCacheService`, `SourceAvailabilityService`, configuration ZKBio.

## 6. Repositories

### `tn.iteam.repository.MonitoredHostRepository`
- Rôle : accès aux hôtes supervisés persistés.
- Dépendances principales : entité `MonitoredHost`.

### `tn.iteam.repository.ServiceStatusRepository`
- Rôle : persistance des états de service/équipement collectés.
- Dépendances principales : entité `ServiceStatus`.

### `tn.iteam.repository.ZabbixMetricRepository`
- Rôle : persistance des métriques Zabbix.
- Dépendances principales : entité `ZabbixMetric`.

### `tn.iteam.repository.ZabbixProblemRepository`
- Rôle : persistance des problèmes Zabbix.
- Dépendances principales : entité `ZabbixProblem`.

### `tn.iteam.repository.ObserviumProblemRepository`
- Rôle : persistance des problèmes Observium.
- Dépendances principales : entité `ObserviumProblem`.

### `tn.iteam.repository.ZkBioProblemRepository`
- Rôle : persistance des problèmes ZKBio.
- Dépendances principales : entité `ZkBioProblem`.

### `tn.iteam.repository.TicketRepository`
- Rôle : persistance des tickets.
- Dépendances principales : entité `Ticket`.

### `tn.iteam.repository.UserRepository`
- Rôle : accès aux utilisateurs.
- Dépendances principales : entité `User`.

## 7. DTO et entités

### Entités principales

#### `tn.iteam.domain.ServiceStatus`
- Rôle : représente l’état collecté d’un service ou équipement.
- Dépendances principales : utilisée par `ServiceStatusRepository`, `MonitoringServiceImpl`, mappers de statut.

#### `tn.iteam.domain.MonitoredHost`
- Rôle : représente un hôte supervisé persisté.
- Dépendances principales : `MonitoredHostRepository`, `ZabbixMonitoringProvider`.

#### `tn.iteam.domain.ZabbixMetric`
- Rôle : représente une métrique Zabbix persistée.
- Dépendances principales : `ZabbixMetricRepository`, `ZabbixMetricsServiceImpl`, `ZabbixMonitoringProvider`.

#### `tn.iteam.domain.ZabbixProblem`
- Rôle : représente un problème Zabbix persisté.
- Dépendances principales : `ZabbixProblemRepository`, `ZabbixProblemServiceImpl`.

#### `tn.iteam.domain.ObserviumProblem`
- Rôle : représente un problème Observium persisté.
- Dépendances principales : `ObserviumProblemRepository`, `ObserviumAdapter`.

#### `tn.iteam.domain.ZkBioProblem`
- Rôle : représente un problème ZKBio persisté.
- Dépendances principales : `ZkBioProblemRepository`, services/adapters ZKBio.

#### `tn.iteam.domain.ApiResponse`
- Rôle : wrapper générique de réponse pour certaines APIs backend.
- Dépendances principales : controllers, adapters, clients et frontend via la structure d’échange.

### DTO principaux

#### `tn.iteam.dto.ServiceStatusDTO`
- Rôle : DTO interne pour transporter l’état d’un service/équipement entre adapter, service et persistance.
- Dépendances principales : adapters multi-source, `MonitoringServiceImpl`, mappers.

#### `tn.iteam.dto.SourceAvailabilityDTO`
- Rôle : DTO exposé pour la santé des sources (`AVAILABLE`, `DEGRADED`, `UNAVAILABLE`).
- Dépendances principales : `SourceAvailabilityServiceImpl`, `MonitoringController`, `MonitoringWebSocketPublisher`.

#### `tn.iteam.monitoring.dto.UnifiedMonitoringHostDTO`
- Rôle : DTO unifié des hôtes monitoring.
- Dépendances principales : `MonitoringProvider`, `MonitoringAggregationService`, frontend Angular.

#### `tn.iteam.monitoring.dto.UnifiedMonitoringProblemDTO`
- Rôle : DTO unifié des problèmes monitoring.
- Dépendances principales : `MonitoringProvider`, `MonitoringAggregationService`, WebSocket monitoring.

#### `tn.iteam.monitoring.dto.UnifiedMonitoringMetricDTO`
- Rôle : DTO unifié des métriques monitoring.
- Dépendances principales : `MonitoringProvider`, `MonitoringAggregationService`, WebSocket monitoring.

#### `tn.iteam.monitoring.dto.UnifiedMonitoringResponse`
- Rôle : wrapper de réponse unifiée contenant `data`, `degraded`, `freshness` et éventuellement `coverage`.
- Dépendances principales : `MonitoringAggregationService`, `MonitoringController`, frontend Angular.

## 8. Schedulers et listeners

### `tn.iteam.scheduler.ZabbixScheduler`
- Rôle : exécute périodiquement la synchronisation et la publication WebSocket des problèmes et métriques Zabbix.
- Dépendances principales : `ZabbixProblemService`, `ZabbixMetricsService`, `ZabbixLiveSynchronizationService`, `ZabbixAdapter`, `ZabbixMetricMapper`, `ZabbixWebSocketPublisher`, `MonitoringWebSocketPublisher`, `SourceAvailabilityService`.

### `tn.iteam.scheduler.ObserviumScheduler`
- Rôle : exécute périodiquement la collecte et/ou publication Observium.
- Dépendances principales : `MonitoringService`, `ObserviumAdapter`, publishers WebSocket selon l’implémentation exacte.

### `tn.iteam.scheduler.ObserviumHostsScheduler`
- Rôle : planifie le rafraîchissement des hosts Observium.
- Dépendances principales : `MonitoringService` ou composants Observium dédiés.

### `tn.iteam.scheduler.ZkBioScheduler`
- Rôle : planifie les rafraîchissements ZKBio et les publications associées.
- Dépendances principales : `ZkBioServiceImpl`, `ZkBioMonitoringService`, `ZkBioWebSocketPublisher`, `SourceAvailabilityService` selon le flux.

### `tn.iteam.listener.ZabbixStartupListener`
- Rôle : déclenche un warmup/sync Zabbix au démarrage de l’application.
- Dépendances principales : `ZabbixLiveSynchronizationService`.

### `tn.iteam.listener.ZabbixMetricsStartupListener`
- Rôle : prépare ou synchronise les métriques Zabbix au démarrage.
- Dépendances principales : services Zabbix liés aux métriques.

### `tn.iteam.listener.ZabbixProblemStartupListener`
- Rôle : prépare ou synchronise les problèmes Zabbix au démarrage.
- Dépendances principales : services Zabbix liés aux problèmes.

## 9. Classes techniques transverses

### `tn.iteam.config.WebClientConfig`
- Rôle : configure les `WebClient` applicatifs, y compris le client TLS permissif réservé à ZKBio.
- Dépendances principales : propriétés `integration.webclient.*`, Reactor Netty, SSL context.

### `tn.iteam.config.RestTemplateConfig`
- Rôle : configure les `RestTemplate` avec timeouts et, pour un bean spécifique, sans validation SSL.
- Dépendances principales : Apache HttpClient, propriétés `integration.http.*`, `LoggingInterceptor`.

### `tn.iteam.config.RedisCacheConfig`
- Rôle : configure le cache Spring Redis et ses TTL.
- Dépendances principales : `RedisConnectionFactory`, `ObjectMapper`, propriétés `app.cache.ttl.*`.

### `tn.iteam.cache.IntegrationCacheService`
- Rôle : contrat de stockage des snapshots d’intégration dans Redis.
- Dépendances principales : aucune directe, c’est une interface.

### `tn.iteam.cache.RedisIntegrationCacheService`
- Rôle : implémentation Redis des snapshots de fallback d’intégration.
- Dépendances principales : `StringRedisTemplate`, `ObjectMapper`, propriété `app.cache.ttl.integration-snapshot`.

### `tn.iteam.websocket.MonitoringWebSocketPublisher`
- Rôle : publie les problèmes, métriques et états de santé monitoring vers le frontend.
- Dépendances principales : `SimpMessagingTemplate`.

### `tn.iteam.websocket.ZabbixWebSocketPublisher`
- Rôle : publie les flux temps réel spécifiques à Zabbix.
- Dépendances principales : `SimpMessagingTemplate`.

### `tn.iteam.websocket.ZkBioWebSocketPublisher`
- Rôle : publie les flux temps réel spécifiques à ZKBio.
- Dépendances principales : `SimpMessagingTemplate`.
