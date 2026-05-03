# Architecture par Module

## 1. Module Monitoring Unifie
### Finalite
Constituer une vue homogenisee des donnees de supervision issues de plusieurs sources heterogenes.

### Points d'entree
- `MonitoringController`
- `MonitoringStartup`
- schedulers de `tn.iteam.scheduler`

### Classes majeures
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore`
- `IntegrationServiceRegistry`
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

### Workflow principal
1. une integration collecte les donnees externes
2. les datasets sont persists ou reconstruits si necessaire
3. un snapshot `live` ou `snapshot_fallback` est enregistre
4. `MonitoringAggregationService` agrege les snapshots par source ou globalement
5. le controleur renvoie `UnifiedMonitoringResponse<T>`

### Forces
- frontiere claire entre lecture agregee et collecte source-specific
- fallback multi-niveaux
- publication WebSocket decouplee de la collecte

### Faiblesses
- duplication de logique entre services d'integration
- couplage technique fort dans l'ecosysteme Zabbix

## 2. Module Integrations Externes
### Finalite
Encapsuler les appels aux sources externes et transformer les reponses brutes.

### Sous-modules
- Zabbix
- Observium
- ZKBio
- Camera

### Particularites
#### Zabbix
- plus mature et plus complexe
- distinction `light` / `heavy` pour la resilience
- forte charge sur les metriques et historiques

#### Observium
- integration HTTP relativement simple
- couche de compatibilite encore presente

#### ZKBio
- double nature:
  - monitoring unifie
  - datasets bruts (attendance, devices, status)

#### Camera
- logique de scan par socket
- pas de client HTTP ni de circuit breaker propre

## 3. Module Ticketing
### Finalite
Transformer des incidents de supervision en tickets puis piloter leur cycle de resolution.

### Points d'entree
- `TicketController`

### Classes majeures
- `TicketServiceImpl`
- `TicketMapper`
- `TicketRepository`
- `InterventionRepository`
- `UserRepository`
- `RoleRepository`

### Forces
- automate de transition explicite
- historisation des interventions
- publication WebSocket des evenements ticket

### Faiblesses
- role model non applique par une couche de securite active
- mode `User` / `Role` surtout bootstrap et persistance, pas enforcement runtime

## 4. Module Dashboard et ML
### Finalite
Produire des indicateurs, anomalies et predictions a partir des donnees persistees Zabbix.

### Points d'entree
- `DashboardController`
- `TorchScriptPredictionController`

### Classes majeures
- `DashboardServiceImpl`
- `TorchScriptPredictionService`
- `ZabbixDataQualityService`

### Forces
- module analytiquement autonome
- fallback implicite sur la persistence

### Faiblesses
- concentration de logique analytique dans `DashboardServiceImpl`
- dependance exclusive aux donnees Zabbix persistees

## 5. Module Resilience et Disponibilite
### Finalite
Assurer la continuite du service malgre des integrations indisponibles ou lentes.

### Elements majeurs
- `WebClientConfig`
- `ResilienceLoggingConfig`
- `IntegrationClientSupport`
- `SourceAvailabilityServiceImpl`
- `MonitoringStartup`
- schedulers avec cooldown

### Mecanismes confirmes
- timeout technique
- retry borne
- circuit breaker
- cooldown scheduler
- fallback snapshot

### Limite
Le comportement degrade est robuste mais disperse entre plusieurs services d'integration.

## 6. Module Configuration et Infrastructure
### Classes majeures
- `AsyncConfig`
- `CorsConfig`
- `JpaAuditingConfig`
- `RedisOptionalConfiguration`
- `WebSocketConfig`
- `TicketingBootstrapConfiguration`

### Evaluation
Module globalement leger, mais `TicketingBootstrapConfiguration` compense l'absence d'un veritable module d'authentification.
