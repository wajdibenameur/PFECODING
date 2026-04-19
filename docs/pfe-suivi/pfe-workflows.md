# Workflows principaux du projet

Ce document décrit les workflows majeurs du projet dans un format clair, directement exploitable pour construire des diagrammes de séquence dans un rapport PFE.

## 1. Workflow de collecte et exposition des données

### 1.1. Collecte manuelle déclenchée depuis le frontend

Acteurs :

- Frontend Angular
- `MonitoringController`
- `MonitoringServiceImpl`
- Adapter source
- Client source
- Redis
- MySQL

Séquence type :

1. le frontend appelle `POST /api/monitoring/collect/{source}` ;
2. `MonitoringController` reçoit la requête ;
3. `MonitoringController` appelle `MonitoringServiceImpl` ;
4. `MonitoringServiceImpl` lance la collecte асynchrone de la source demandée ;
5. le service appelle l’adapter correspondant :
   - `ZabbixAdapter`
   - `ObserviumAdapter`
   - `ZkBioAdapter`
   - `CameraAdapter`
6. l’adapter appelle le client technique de la source ;
7. le client interroge l’API externe ;
8. si l’appel réussit, le client peut sauvegarder un snapshot Redis ;
9. l’adapter transforme la réponse externe en DTOs applicatifs ;
10. le service sauvegarde si nécessaire dans MySQL via les repositories ;
11. `MonitoringController` renvoie une réponse HTTP de succès immédiate ;
12. le frontend planifie ensuite un rafraîchissement de snapshot.

### 1.2. Lecture unifiée des données monitoring

Acteurs :

- Frontend Angular
- `MonitoringController`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `MonitoringProvider`
- Services / Adapters / Repositories
- Redis

Séquence type :

1. le frontend appelle l’un des endpoints :
   - `GET /api/monitoring/hosts`
   - `GET /api/monitoring/problems`
   - `GET /api/monitoring/metrics`
2. `MonitoringController` reçoit la requête ;
3. `MonitoringController` délègue à `MonitoringAggregationService` ;
4. `MonitoringAggregationService` appelle `MonitoringCacheService` ;
5. `MonitoringCacheService` vérifie d’abord le cache Redis Spring si un résultat non dégradé existe ;
6. si le cache n’est pas utilisable, `MonitoringCacheService` interroge les `MonitoringProvider` ;
7. chaque provider récupère les données de sa source :
   - soit via des services persistés JPA ;
   - soit via des adapters d’intégration ;
8. `MonitoringCacheService` agrège les résultats et calcule les métadonnées :
   - `degraded`
   - `freshness`
   - éventuellement `coverage` ;
9. si le résultat n’est pas dégradé, il peut être mis en cache ;
10. `MonitoringAggregationService` renvoie une `UnifiedMonitoringResponse` ;
11. `MonitoringController` retourne cette réponse au frontend ;
12. le frontend l’interprète et met à jour l’interface.

## 2. Workflow source par source

### 2.1. Workflow Zabbix

Chemin principal (monitoring unifié) :

1. `ZabbixMonitoringProvider` est appelé ;
2. il délègue à `ZabbixMonitoringService` ;
3. `ZabbixMonitoringService` s'appuie sur `ZabbixAdapter` ;
4. `ZabbixAdapter` appelle `ZabbixClient` ;
5. `ZabbixClient` interroge l'API Zabbix via un `WebClient` dédié ;
6. si succès, snapshot Redis sauvegardé ;
7. les données sont converties via `ZabbixMonitoringMapper` en DTOs monitoring unifiés ;
8. le résultat remonte vers la réponse unifiée.

Les collectes live et enrichissements sont également assurés par :
- `ZabbixLiveSynchronizationServiceImpl`
- `ZabbixScheduler`

Lecture synthétique pour diagramme :

`Frontend -> MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> ZabbixMonitoringProvider -> ZabbixMonitoringService -> ZabbixAdapter -> ZabbixClient -> API Zabbix -> Redis -> réponse unifiée`

### 2.2. Workflow Observium

Chemin principal (monitoring unifié) :

1. `ObserviumMonitoringProvider` est appelé ;
2. il délègue à `ObserviumMonitoringService` ;
3. `ObserviumMonitoringService` s'appuie sur `ObserviumAdapter` ;
4. `ObserviumAdapter` appelle `ObserviumClientX` ;
5. `ObserviumClientX` appelle l'API Observium via `WebClient` ;
6. si succès, snapshot Redis sauvegardé ;
7. les données sont converties via `ObserviumMonitoringMapper` en DTOs monitoring unifiés ;
8. le résultat remonte vers la réponse unifiée.

Lecture synthétique pour diagramme :

`Frontend -> MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> ObserviumMonitoringProvider -> ObserviumMonitoringService -> ObserviumAdapter -> ObserviumClientX -> API Observium -> Redis -> réponse unifiée`

### 2.3. Workflow ZKBio

Chemin principal (monitoring unifié) :

1. `ZkBioMonitoringProvider` est appelé ;
2. il délègue à `ZkBioMonitoringService` ;
3. `ZkBioMonitoringService` s'appuie sur `ZkBioAdapter` ;
4. `ZkBioAdapter` appelle `ZkBioClient` ;
5. `ZkBioClient` interroge l'API ZKBio via un `WebClient` dédié ;
6. si succès, snapshot Redis sauvegardé ;
7. les données sont converties via `ZkBioMonitoringMapper` en DTOs monitoring unifiés ;
8. le résultat remonte vers la réponse unifiée.

Chemin secondaire (business ZKBio) :

1. `ZkBioController` est appelé pour les opérations métier ;
2. il délègue à `ZkBioServiceInterface` / `ZkBioServiceImpl` ;
3. `ZkBioServiceImpl` s'appuie sur `ZkBioAdapter` pour les opérations business (devices, attendance, users).

Lecture synthétique pour diagramme (monitoring) :

`Frontend -> MonitoringController -> ZkBioMonitoringProvider -> ZkBioMonitoringService -> ZkBioAdapter -> ZkBioClient -> API ZKBio -> Redis -> réponse unifiée`

## 3. Workflow de fallback Redis

Acteurs :

- Client source
- Adapter source
- `RedisIntegrationCacheService`
- `SourceAvailabilityServiceImpl`
- `MonitoringCacheService`

Objectif :

Permettre à l’application de continuer à servir des données si une API externe est indisponible, à condition qu’un snapshot Redis valide existe.

### 3.1. Séquence générale du fallback Redis

1. un client tente un appel live vers une API externe ;
2. l’appel échoue à cause d’une indisponibilité, d’un timeout ou d’une erreur de transport ;
3. le client ou l’adapter tente de relire le dernier snapshot Redis valide ;
4. si un snapshot est trouvé :
   - les données sont renvoyées ;
   - `SourceAvailabilityServiceImpl` marque la source en `DEGRADED` ;
   - l’application continue à servir la donnée ;
5. si aucun snapshot n’existe :
   - une exception explicite est levée ;
   - `SourceAvailabilityServiceImpl` marque la source en `UNAVAILABLE`.

### 3.2. Séquence de succès live puis fallback futur

1. appel live réussi ;
2. parsing réussi ;
3. snapshot Redis sauvegardé ;
4. à un appel ultérieur, si l’API tombe, ce snapshot peut être utilisé comme dernier état connu.

### 3.3. Effet sur l’agrégation unifiée

1. si une source fournit encore un snapshot Redis valide, elle peut contribuer à la réponse ;
2. si une source ne peut fournir ni live ni Redis, elle déclenche un état dégradé côté agrégateur ;
3. `MonitoringCacheService` marque alors la réponse agrégée avec `degraded=true` ;
4. un résultat dégradé n’est pas mis en cache dans Redis Spring.

Lecture synthétique pour diagramme :

`Provider/Adapter -> Client -> API externe KO -> RedisIntegrationCacheService -> SourceAvailabilityServiceImpl(DEGRADED) -> réponse fallback`

ou

`Provider/Adapter -> Client -> API externe KO -> Redis vide -> SourceAvailabilityServiceImpl(UNAVAILABLE) -> exception`

## 4. Workflow WebSocket

Acteurs :

- Scheduler ou service backend
- Publisher WebSocket Spring
- Broker STOMP
- Frontend Angular
- Store frontend

### 4.1. Publication WebSocket côté backend

1. un scheduler ou un service produit une mise à jour ;
2. la donnée est transformée en DTO de sortie ;
3. un publisher WebSocket appelle `SimpMessagingTemplate.convertAndSend(...)` ;
4. le message est publié sur un topic STOMP.

Publishers principaux :

- `MonitoringWebSocketPublisher`
- `ZabbixWebSocketPublisher`
- `ZkBioWebSocketPublisher`

Topics importants :

- `/topic/monitoring/problems`
- `/topic/monitoring/metrics`
- `/topic/monitoring/sources`
- `/topic/zabbix/problems`
- `/topic/zabbix/metrics`
- `/topic/zkbio/problems`
- `/topic/zkbio/status`
- `/topic/zkbio/devices`
- `/topic/zkbio/attendance`

### 4.2. Réception WebSocket côté Angular

1. Angular initialise `StompClientService` ;
2. la connexion SockJS/STOMP est établie ;
3. `MonitoringRealtimeService` s’abonne aux topics nécessaires ;
4. `MonitoringStore` ou les pages spécifiques reçoivent les événements ;
5. le state frontend fusionne les nouvelles données dans l’état courant ;
6. l’interface est actualisée automatiquement.

Lecture synthétique pour diagramme :

`Scheduler/Service -> WebSocketPublisher -> STOMP topic -> StompClientService -> MonitoringRealtimeService -> MonitoringStore -> UI`

## 5. Workflow Listener au démarrage

Acteurs :

- Spring Boot
- Listener de démarrage
- Service de synchronisation
- API externe
- MySQL / Redis

### 5.1. Séquence générale

1. Spring Boot termine le démarrage ;
2. un `ApplicationReadyEvent` est émis ;
3. un listener capte cet événement ;
4. le listener déclenche une synchronisation initiale ;
5. les services concernés interrogent les intégrations et préparent les snapshots nécessaires.

Listeners principaux :

- `ZabbixStartupListener`
- `ZabbixMetricsStartupListener`
- `ZabbixProblemStartupListener`

Lecture synthétique pour diagramme :

`Spring Boot -> StartupListener -> Service de synchronisation -> Adapter/Client -> API externe -> MySQL/Redis`

## 6. Workflow complet demandé : listener -> service -> adapter -> client -> cache -> controller -> frontend

Cette chaîne complète n’est pas toujours exécutée exactement dans cet ordre dans tous les cas du projet, mais elle peut être décrite comme un workflow conceptuel représentatif.

### 6.1. Version complète synthétique

1. un `listener` ou un `scheduler` déclenche une synchronisation ;
2. un `service` métier orchestre le traitement ;
3. l’`adapter` appelle le `client` externe ;
4. le `client` interroge l’API distante ;
5. si succès, un snapshot est enregistré en `cache Redis` ;
6. les données peuvent être persistées dans MySQL ;
7. plus tard, un `controller` expose la lecture unifiée ou spécifique ;
8. le `frontend Angular` consomme soit l’API REST, soit le flux WebSocket résultant.

### 6.2. Séquence diagramme textuelle

`Listener/Scheduler -> Service -> Adapter -> Client -> API externe -> Redis -> MySQL -> Controller -> Frontend`

Variante temps réel :

`Listener/Scheduler -> Service -> Adapter -> Client -> API externe -> Redis -> WebSocketPublisher -> Frontend`

## 7. Résumé des workflows les plus importants

### Workflow A : lecture monitoring unifiée

`Frontend -> MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> MonitoringProvider -> Service/Adapter/Repository -> réponse`

### Workflow B : collecte manuelle

`Frontend -> MonitoringController -> MonitoringServiceImpl -> Adapter -> Client -> API externe -> Redis/MySQL`

### Workflow C : fallback Redis

`Client -> API externe KO -> Redis snapshot -> SourceAvailabilityServiceImpl -> réponse dégradée`

### Workflow D : temps réel WebSocket

`Scheduler/Service -> WebSocketPublisher -> Topic STOMP -> Angular -> Store -> UI`

### Workflow E : warmup au démarrage

`Spring Boot -> Listener -> Service de synchronisation -> Adapter/Client -> API externe -> snapshots initiaux`
