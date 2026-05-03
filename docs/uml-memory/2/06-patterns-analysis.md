# Analyse des Patterns Architecturaux et de Conception

## 1. Architecture en couches
### Presence
`Fait confirme`

### Incarnations
- `controller`
- `integration` / `service`
- `adapter` / `client`
- `repository`
- `domain` / `dto`

### Evaluation
Pattern globalement respecte. Les derivees principales concernent le packaging et certaines classes trop larges.

## 2. Pattern Adapter
### Classes
- `ZabbixAdapter`
- `ObserviumAdapter`
- `ZkBioAdapter`
- `CameraAdapter`

### Role
Traduire une source externe en structures applicatives.

### Evaluation
- bien applique pour Observium, ZKBio et Camera
- applique de maniere plus lourde pour Zabbix, où l'adapter absorbe aussi de l'orchestration technique

## 3. Pattern Client / Gateway
### Classes
- `ZabbixClient`
- `ObserviumClientX`
- `ZkBioClientX`

### Role
Encapsuler le boundary HTTP, la resilience, le timeout et le mapping d'erreurs transport.

### Evaluation
Pattern sain et conforme a l'architecture cible.

## 4. Pattern Strategy + Resolver
### Classes
- `AsyncIntegrationService`
- `IntegrationServiceRegistry`
- services d'integration par source

### Role
Selectionner dynamiquement l'integration adapte a un `MonitoringSourceType`.

### Evaluation
Implementation legere et propre; pas de logique cachee dans le registry.

## 5. Pattern Orchestration Service
### Classes
- `MonitoringStartup`
- `ZkBioRefreshOrchestrationService`
- `TicketServiceImpl`

### Role
Coordonner plusieurs collaborateurs pour accomplir un cas d'usage composite.

### Evaluation
Correctement present. `ZkBioRefreshOrchestrationService` est un bon exemple de micro-orchestrateur utile.

## 6. Pattern Snapshot / Fallback
### Classes
- `SnapshotStore`
- `InMemorySnapshotStore`
- services d'integration

### Role
Maintenir un etat utilisable meme en cas de defaillance d'une integration.

### Evaluation
Pattern central de la resilience metier. Il est efficace mais duplique dans plusieurs classes les memes etapes de degradation.

## 7. Pattern Publication centralisee
### Classes
- `MonitoringSnapshotPublicationService`
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`

### Role
Separer la publication temps reel des pipelines de collecte.

### Evaluation
Service de publication correctement maintenu comme delegateur leger.

## 8. Pattern Scheduler-driven refresh
### Classes
- `ZabbixScheduler`
- `ObserviumScheduler`
- `ObserviumHostsScheduler`
- `ZkBioScheduler`

### Role
Pilotage periodique des collectes.

### Evaluation
L'ajout du cooldown via `SourceAvailabilityService.shouldAttempt(...)` renforce la robustesse.

## 9. Pattern Resilience
### Elements
- `WebClientConfig`
- `ResilienceLoggingConfig`
- `IntegrationClientSupport`
- annotations Resilience4j sur les clients

### Evaluation
Le pattern est clairement present. Zabbix beneficie d'un raffinement supplementaire avec des profils `light` et `heavy`.

## 10. Pattern Repository
### Presence
`Fait confirme`

Les repositories Spring Data structurent correctement la persistance. La complexite metier reste principalement dans les services.

## Recommandation transversale
Conserver ces patterns, mais extraire a moyen terme un helper commun de fallback snapshot pour reduire la duplication sans detruire les frontieres actuelles.
