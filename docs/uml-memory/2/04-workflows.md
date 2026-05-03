# Analyse des Workflows de bout en bout

## 1. Lecture des problemes unifies
### Point d'entree
`GET /api/monitoring/problems`

### Chaine d'appel
`MonitoringController.getProblems()` -> `MonitoringAggregationService.getProblems(null)` -> `MonitoringCacheService` -> `SnapshotStore`

### Transformations
- aucune collecte live
- aggregation de snapshots deja disponibles
- retour via `UnifiedMonitoringResponse<List<UnifiedMonitoringProblemDTO>>`

### Gestion d'erreur
- si snapshot absent, la logique de cache/aggregation sert les donnees disponibles
- `Inference statique`: la robustesse depend des snapshots deja poses par warmup ou scheduler

## 2. Collecte manuelle globale
### Point d'entree
`POST /api/monitoring/collect`

### Chaine d'appel
`MonitoringController.collectAll()` ->
- `ZabbixIntegrationService.refreshAsync()`
- `ObserviumIntegrationService.refreshAsync()`
- `ZkBioRefreshOrchestrationService.refreshMonitoringAndAttendanceAsync()`
- `CameraIntegrationService.refreshAsync()`

Puis:
- `MonitoringSnapshotPublicationService.publishMonitoringSnapshots(...)`
- `MonitoringSnapshotPublicationService.publishZkBioSnapshots()`

### Caracteristiques
- execution parallele par `Mono.whenDelayError(...)`
- publication apres completion du pipeline combine
- la regle ZKBio est sequentielle: monitoring puis attendance

## 3. Warmup au demarrage
### Point d'entree
`MonitoringStartup.warmupInitialSnapshots()`

### Logique
1. verification anti-double lancement via `AtomicBoolean`
2. declenchement des memes pipelines de refresh que `collectAll()`
3. publication des snapshots apres completion
4. journalisation des erreurs sans faire tomber l'application

### Conclusion
`Fait confirme`: le warmup et `collectAll()` partagent des semantiques d'orchestration coherentes.

## 4. Refresh scheduler Zabbix
### Point d'entree
`ZabbixScheduler.fetchAndPublishProblems()` ou `fetchAndPublishMetrics()`

### Chaine d'appel
1. `SourceAvailabilityService.shouldAttempt("ZABBIX", retryBackoffMs)`
2. refresh source-specific
3. publication snapshot

### Particularite
Le scheduler de metriques utilise `refreshMetricsAsync()` puis publie le snapshot uniquement apres completion reactive.

## 5. Refresh scheduler Observium et ZKBio
### Pattern commun
- verification cooldown
- refresh dataset ou source
- publication associee si applicable
- log non fatal en cas d'erreur

### Remarque
`Inference statique`: le cooldown evite les tempetes de polling en cas de panne recurrente.

## 6. Refresh ZKBio source-specific
### Point d'entree
`POST /api/zkbio/collect`

### Chaine d'appel
`ZkBioController.triggerCollection()` -> `ZkBioIntegrationOperations.refreshAllAndPublishAsync()`

### Particularite
Ce workflow combine:
- refresh monitoring ZKBio
- refresh attendance
- publication monitoring et datasets ZKBio

### Observation
Ce chemin est fonctionnellement valide mais recouvre partiellement l'orchestrateur `ZkBioRefreshOrchestrationService` et le service de publication centralise.

## 7. Workflow Ticket depuis un probleme externe
### Point d'entree
`POST /api/tickets/from-problem`

### Chaine d'appel
`TicketController.createFromProblem(...)` -> `TicketService.createFromProblem(...)`

### Etapes
1. chargement du createur
2. conversion severite -> priorite
3. resolution resourceRef
4. persistance du ticket
5. emission WebSocket `/topic/tickets`

### Gestion d'erreur
- utilisateur absent -> `TicketingException`

## 8. Workflow d'affectation et de validation d'un ticket
### Affectation
`assign(...)`:
- charge ticket et user
- affecte le ticket
- bascule `OPEN -> IN_PROGRESS` si necessaire
- cree une intervention
- publie l'evenement

### Validation / rejet
`validate(...)`, `reject(...)`:
- verifient l'automate de transition
- mettent a jour le ticket
- enregistrent l'intervention
- publient l'evenement

## 9. Workflow dashboard
### Overview
`DashboardController.overview()` -> `DashboardServiceImpl.getOverview()`

### Sous-flux
- calcul des predictions
- calcul des anomalies
- distribution des severites
- synthese de qualite de donnees

### Dependances
- repositories Zabbix
- `TorchScriptPredictionService`

## 10. Workflow prediction ML
### Point d'entree
`TorchScriptPredictionController`

### Etapes inferees
1. reception du vecteur de features
2. appel du modele TorchScript
3. retour d'une prediction + probabilite

### Confiance
- moyenne
- raison: le module ML est lisible statiquement, mais son execution depend de fichiers de modele externes
