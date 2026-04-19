# Chapitre 2 : Architecture et Conception

## 2.1. Introduction

Ce chapitre présente l'architecture et la conception de l'application de supervision multi-source développée dans le cadre de ce projet de fin d'études. L'objectif principal de cette application est de centraliser la collecte, la persistance, l'agrégation et la visualisation des données issues de plusieurs systèmes de monitoring hétérogènes, à savoir Zabbix, Observium et ZKBio.

L'architecture retenue repose sur une approche distribuée en couches, combinant un backend Spring Boot pour le traitement des données et l'intégration des systèmes externes, ainsi qu'un frontend Angular pour la présentation et l'interaction utilisateur. Cette conception a été guidée par plusieurs principes fondamentaux : la modularité, la résilience, la maintenabilité et l'extensibilité.

---

## 2.2. Architecture globale du système

### 2.2.1. Vue d'ensemble

L'application est structurée en deux blocs fonctionnels distincts mais complémentaires. Le premier bloc correspond au backend Java Spring Boot, responsable de l'intégration des systèmes externes, de la persistance des données, de l'agrégation des informations et de la diffusion temps réel. Le second bloc représente le frontend Angular, qui assure la présentation des données, le pilotage des collectes et la réception des mises à jour en temps réel via WebSocket.

Le flux fonctionnel principal peut être décrit comme suit : le frontend Angular initie des requêtes vers les APIs REST du backend ou s'abonne aux flux WebSocket pour recevoir les mises à jour en temps réel. Les contrôleurs Spring reçoivent ces requêtes et les délèguent aux services appropriés. Les services orchestrent ensuite les appels vers les adapters d'intégration, la couche d'agrégation unifiée ou les repositories JPA. Les adapters utilisent des clients HTTP pour interroger les systèmes externes. Les réponses sont transformées en objets de transfert de données (DTO) internes, puis éventuellement persistées en base de données MySQL, mises en cache dans Redis, et enfin renvoyées à l'API ou publiées sur WebSocket. Les tâches planifiées (schedulers) et les listeners alimentent en parallèle les données persistées et les flux temps réel.

### 2.2.2. Stack technologique

Le backend utilise les technologies suivantes, définies dans le fichier pom.xml :

- **Java 17** : Langage de programmation orienté objet
- **Spring Boot 3.2.5** : Framework de développement d'applications Java
- **Spring Web** : Framework pour les applications web et REST
- **Spring WebFlux** : Programmation réactive
- **Spring Data JPA** : Couche d'accès aux données
- **Spring WebSocket** : Communication temps réel
- **Spring Cache** : Gestion du cache
- **Spring Data Redis** : Intégration Redis
- **Resilience4j** : Patterns de résilience (CircuitBreaker, Retry)
- **MySQL Connector/J** : Connecteur pour la base de données MySQL
- **Lombok** : Réduction du code boilerplate
- **Micrometer / Prometheus** : Métriques et monitoring

Le frontend s'appuie sur les technologies suivantes, définies dans package.json :

- **Angular 20** : Framework de développement frontend
- **TypeScript** : Langage de programmation typé
- **RxJS** : Programmation réactive
- **STOMP** : Protocole de messagerie
- **SockJS** : WebSocket polyfill

---

## 2.3. Architecture backend

### 2.3.1. Structure des packages

Le backend est organisé en packages métier et techniques distincts, permettant une séparation claire des responsabilités :

- **tn.iteam.controller** : Contrôleurs REST exposant les endpoints
- **tn.iteam.service** et **tn.iteam.service.impl** : Services métier et implémentations
- **tn.iteam.adapter** : Adapters de transformation et d'intégration
- **tn.iteam.client** : Clients HTTP pour les APIs externes
- **tn.iteam.monitoring** : Composants spécifiques au monitoring unifié
- **tn.iteam.repository** : Repositories Spring Data JPA
- **tn.iteam.domain** : Entités et objets métier
- **tn.iteam.dto** : Objets de transfert de données
- **tn.iteam.scheduler** : Tâches planifiées
- **tn.iteam.listener** : Listeners d'événements applicatifs
- **tn.iteam.websocket** : Publishes WebSocket
- **tn.iteam.cache** : Services de cache
- **tn.iteam.config** : Configurations techniques

### 2.3.2. Couche contrôleur

La couche contrôleur expose les points d'entrée REST de l'application. Le contrôleur central pour le monitoring multi-source est **MonitoringController**, qui expose les endpoints suivants :

- `GET /api/monitoring/hosts` : Lecture des hôtes unifiés
- `GET /api/monitoring/problems` : Lecture des problèmes unifiés
- `GET /api/monitoring/metrics` : Lecture des métriques unifiées
- `GET /api/monitoring/sources/health` : État de santé des sources
- `POST /api/monitoring/collect/{source}` : Déclenchement d'une collecte

Les contrôleurs spécifiques à chaque source (ZabbixMetricsController, ZabbixProblemController, ZkBioController, ObserviumController) complètent cette couche en exposant des endpoints dédiés à chaque système externe.

Le rôle des contrôleurs est volontairement léger : ils ne contiennent pas la logique métier profonde, mais délèguent aux services spécialisés.

### 2.3.3. Couche service

La couche service constitue le cœur de l'orchestration métier et technique. Les principales classes de service sont :

**MonitoringServiceImpl** : Orchestre les collectes manuelles et automatiques par source (Zabbix, Observium, ZKBio, Camera). Il utilise les adapters d'intégration, les repositories et les services de disponibilité des sources.

**MonitoringAggregationService** : Façade légère pour les lectures unifiées du monitoring. Elle délègue à MonitoringCacheService et renvoie une réponse unifiée de type UnifiedMonitoringResponse contenant les données, les indicateurs degraded, freshness et coverage.

**MonitoringCacheService** : Agrège les données des MonitoringProvider, applique le cache Redis Spring sur les lectures unifiées, et transporte les métadonnées degraded et freshness. Ce service vérifie d'abord le cache Redis avant d'interroger les providers.

**SourceAvailabilityServiceImpl** : Maintient l'état de santé des sources externes avec trois états possibles : AVAILABLE (source fonctionnelle en direct), DEGRADED (source indisponible mais servie via le cache Redis), et UNAVAILABLE (source complètement indisponible).

**Services de monitoring dédiés** : Après le refactoring, trois services de monitoring dédiés ont été créés pour assurer une architecture unifiée :

- ZabbixMonitoringService → ZabbixMonitoringServiceImpl
- ObserviumMonitoringService → ObserviumMonitoringServiceImpl
- ZkBioMonitoringService → ZkBioMonitoringServiceImpl

Ces services délèguent aux adapters correspondants et utilisent des mappers pour transformer les données externes en DTOs unifiés.

### 2.3.4. Couche monitoring.provider

Le package tn.iteam.monitoring.provider contient les providers qui exposent chaque source au format de monitoring unifié. Trois providers ont été implémentés :

- **ZabbixMonitoringProvider** : Expose Zabbix dans le format unifié
- **ObserviumMonitoringProvider** : Expose Observium dans le format unifié
- **ZkBioMonitoringProvider** : Expose ZKBio dans le format unifié

Chaque provider implémente l'interface MonitoringProvider et fournit les méthodes getHosts(), getProblems() et getMetrics(). Leur rôle est de traduire chaque source vers des DTOs homogènes : UnifiedMonitoringHostDTO, UnifiedMonitoringProblemDTO et UnifiedMonitoringMetricDTO.

**Pattern unifié des providers** : Après le refactoring, tous les providers utilisent un pattern unifié cohérent :

1. Le provider délègue à un service de monitoring dédié
2. Le service de monitoring délègue à l'adapter correspondant
3. L'adapter gère le fallback Redis et la transformation des données via un mapper

Ce pattern assure une cohérence dans le flux de données et facilite la maintenance.

### 2.3.5. Couche adapter

Les adapters constituent la couche de médiation entre les clients HTTP externes et les services internes. Ils assurent plusieurs responsabilités :

- Transformation des données externes en DTOs internes
- Mapping vers les structures métier
- Pilotage du fallback Redis
- Levée d'exceptions métier en l'absence de données valides
- Publication WebSocket si nécessaire

Les adapters principaux sont :

- **ZabbixAdapter** : Transformation des données Zabbix
- **ObserviumAdapter** : Transformation des données Observium avec fallback Redis
- **ZkBioAdapter** : Transformation des données ZKBio
- **CameraAdapter** : Intégration des caméras

### 2.3.6. Couche client

Les clients représentent la couche technique la plus proche des APIs externes. Ils gèrent :

- Les appels HTTP via WebClient
- Les timeouts et la gestion des connexions
- Le parsing JSON des réponses
- L'application des mécanismes Resilience4j (CircuitBreaker, Retry)
- La sauvegarde des snapshots Redis en cas de succès

Les clients principaux sont :

- **ZabbixClient** : Client pour l'API Zabbix
- **ObserviumClientX** : Client pour l'API Observium
- **ZkBioClient** : Client pour l'API ZKBio

### 2.3.7. Couche persistance

La persistance relationnelle repose sur Spring Data JPA avec MySQL. Les repositories principaux sont :

- **MonitoredHostRepository** : Gestion des hôtes supervisés
- **ServiceStatusRepository** : Gestion des statuts de service
- **ZabbixMetricRepository** : Gestion des métriques Zabbix
- **ZabbixProblemRepository** : Gestion des problèmes Zabbix
- **ObserviumProblemRepository** : Gestion des problèmes Observium
- **ZkBioProblemRepository** : Gestion des problèmes ZKBio
- **TicketRepository** : Gestion des tickets
- **UserRepository** : Gestion des utilisateurs

### 2.3.8. Couche scheduler et listener

Les **schedulers** (ZabbixScheduler, ObserviumScheduler, ZkBioScheduler) lancent des collectes périodiques, rafraîchissent les snapshots persistés, republient des données sur WebSocket et assurent une continuité de service.

Les **listeners** (ZabbixStartupListener, ZabbixMetricsStartupListener, ZabbixProblemStartupListener) écoutent le démarrage de l'application via ApplicationReadyEvent pour effectuer un préchargement ou une synchronisation initiale.

### 2.3.9. Couche WebSocket

La publication temps réel est assurée par plusieurs publishers :

- **MonitoringWebSocketPublisher** : Publie sur /topic/monitoring/problems, /topic/monitoring/metrics, /topic/monitoring/sources
- **ZabbixWebSocketPublisher** : Publie sur /topic/zabbix/problems, /topic/zabbix/metrics
- **ZkBioWebSocketPublisher** : Publie sur /topic/zkbio/problems, /topic/zkbio/attendance, /topic/zkbio/devices, /topic/zkbio/status

Le backend utilise SimpMessagingTemplate pour pousser les événements vers le frontend Angular.

---

## 2.4. Architecture frontend Angular

### 2.4.1. Structure des packages

Le frontend Angular est structuré en plusieurs packages dans frontend/src/app :

- **core** : Briques transverses (config, http, models, realtime, auth)
- **features** : Fonctionnalités métier (monitoring)
- **layout** : Structure visuelle (shell, navbar, sidebar, user-panel)
- **shared** : Composants réutilisables

### 2.4.2. Couche core

Le package core contient les éléments transverses réutilisables dans toute l'application :

- **stomp-client.service.ts** : Client WebSocket STOMP
- **Modèles TypeScript** : source-availability.model.ts, monitoring-host.model.ts, zabbix-metric.model.ts
- **APP_CONFIG** : Configuration des URLs API

### 2.4.3. Couche features/monitoring

La fonctionnalité principale est organisée en trois sous-packages :

**Data** : MonitoringApiService (appels REST) et MonitoringRealtimeService (abonnements WebSocket)

**State** : MonitoringStore, façade d'état qui charge les snapshots initiaux, fusionne les flux temps réel, calcule des KPIs et pilote les rafraîchissements

**UI** : Composants de page (MonitoringDashboardPage, MonitoringZabbixPage, MonitoringObserviumPage, MonitoringZkbioPage)

### 2.4.4. Routage

Les routes principales définies dans app.routes.ts sont :

- /dashboard
- /monitoring/zabbix
- /monitoring/observium
- /monitoring/zkbio

---

## 2.5. Mécanismes de résilience et de cache

### 2.5.1. Cache Redis

Redis est utilisé à deux niveaux distincts :

**Cache Spring sur les lectures unifiées** : MonitoringCacheService utilise @Cacheable pour mettre en cache les hosts, problems et metrics. Les TTL configurés sont courts (30 secondes pour hosts, metrics et problems, 15 secondes pour source-health).

**Snapshots d'intégration pour fallback** : IntegrationCacheService et RedisIntegrationCacheService sauvegardent des snapshots techniques. En cas d'indisponibilité d'une API externe, l'application peut servir le dernier snapshot valide.

### 2.5.2. Résilience avec Resilience4j

Les appels externes utilisent Resilience4j avec deux mécanismes :

- **CircuitBreaker** : Empêche les appels répétés vers un service défaillant
- **Retry** : Réessaye les appels en cas d'échec temporaire

Les instances configurées sont zabbixApi, observiumApi et zkbioApi.

---

## 2.6. Flux de données principaux

### 2.6.1. Flux de lecture unifiée

1. Le frontend appelle /api/monitoring/hosts, /problems ou /metrics
2. MonitoringController reçoit la requête
3. MonitoringAggregationService délègue à MonitoringCacheService
4. MonitoringCacheService interroge les MonitoringProvider
5. Chaque provider appelle son service de monitoring dédié
6. Le service délègue à l'adapter qui appelle le client externe
7. Les données sont agrégées, enrichies avec degraded, freshness et coverage
8. La réponse est renvoyée au frontend

### 2.6.2. Flux de collecte manuelle

1. Le frontend déclenche POST /api/monitoring/collect/{source}
2. MonitoringController appelle MonitoringServiceImpl
3. MonitoringServiceImpl lance la collecte asynchrone
4. L'adapter appelle le client externe
5. Les données sont transformées, sauvegardées si nécessaire
6. MonitoringController renvoie une réponse HTTP de succès

### 2.6.3. Flux de publication temps réel

1. Un scheduler ou un service produit une donnée ou un état
2. Le publisher WebSocket envoie le message sur un topic
3. Angular reçoit ce message via STOMP
4. Le store frontend fusionne l'événement dans l'état courant

---

## 2.7. Intégrations externes

### 2.7.1. Zabbix

Zabbix constitue la source la plus complète du projet. Elle fournit :

- Des hosts unifiés
- Des problèmes unifiés
- Des métriques unifiées
- Des snapshots persistés en base MySQL
- Des publications WebSocket dédiées
- Des fallbacks Redis sur les appels externes

### 2.7.2. Observium

Observium est utilisé principalement pour les équipements et les alertes. Elle fournit :

- Des hosts unifiés
- Des problèmes unifiés
- Des snapshots Redis de fallback
- Des appels live via WebClient

Note : Observium ne fournit pas encore de métriques unifiées.

### 2.7.3. ZKBio

ZKBio est une source spécialisée orientée contrôle d'accès et présence. Elle fournit :

- Des hosts unifiés
- Des problèmes unifiés
- Des endpoints dédiés pour le statut, les devices et l attendance
- Des snapshots Redis de fallback
- Un WebClient dédié avec TLS permissif

Note : Comme Observium, ZKBio ne fournit pas encore de métriques unifiées.

---

## 2.8. Synthèse architecturale

L'architecture présentée dans ce chapitre démontre une approche distribuée en couches, adaptée au contexte de supervision multi-source. Le backend Spring Boot joue le rôle de noyau d'intégration, de persistance et de diffusion des données, tandis que le frontend Angular constitue la couche de présentation et d'exploitation utilisateur.

Les points forts de cette architecture sont :

- Une séparation claire entre contrôleurs, services, adapters, clients et persistance
- Une capacité d'intégration de plusieurs systèmes externes hétérogènes
- La mise en place d'un cache Redis et de fallbacks techniques pour la résilience
- La résilience des appels externes grâce à Resilience4j
- La diffusion temps réel via WebSocket
- Une structure frontend modulaire par fonctionnalités
- Un pattern unifié pour les providers de monitoring

Le projet présente une hétérogénéité entre les sources : Zabbix est la source la plus complète (métriques, snapshots persistés), tandis qu'Observium et ZKBio restent plus limités sur certains aspects. Cette situation reflète une architecture évolutive construite progressivement, où chaque source a été intégrée selon ses spécificités et ses capacités.

Cette architecture peut être présentée comme une solution de supervision centralisée, modulaire et résiliente, capable de fédérer plusieurs sources de monitoring au sein d'une même plateforme web temps réel.