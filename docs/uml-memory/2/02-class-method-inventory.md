# Inventaire des Classes et des Methodes

## Regle de lecture
Le codebase contient un grand nombre de DTOs, entites et repositories purement structurels. Le present document detaille methodologiquement les classes a logique significative. Les classes de transport de donnees sont regroupees et decrites lorsqu'elles n'introduisent pas de comportement particulier.

## I. Bootstrap et orchestration globale

### `tn.iteam.PfeprojectApplication`
#### Responsabilite
Point d'entree Spring Boot et activation de l'infrastructure asynchrone.

#### Annotations
- `@SpringBootApplication`
- `@EnableAsync(proxyTargetClass = true)`

#### Methode principale
`main(String[] args)`
- but: demarrer le contexte Spring
- entree: arguments CLI
- sortie: aucune
- collaborateurs: infrastructure Spring Boot
- effets de bord: creation du conteneur, scan de composants, initialisation de l'application
- role workflow: demarrage global

### `tn.iteam.MonitoringStartup`
#### Responsabilite
Hydrater les snapshots de monitoring au demarrage et publier le premier etat WebSocket.

#### Dependances
- `IntegrationServiceRegistry`
- `ZkBioRefreshOrchestrationService`
- `MonitoringSnapshotPublicationService`

#### Methodes importantes
`warmupInitialSnapshots()`
- but: lancer le warmup initial apres `ApplicationReadyEvent`
- entree: aucune
- sortie: `void`
- collaborateurs:
  - `IntegrationServiceRegistry.getRequired(...)`
  - `ZkBioRefreshOrchestrationService.refreshMonitoringAndAttendanceAsync()`
  - `MonitoringSnapshotPublicationService.publishMonitoringSnapshots(...)`
  - `MonitoringSnapshotPublicationService.publishZkBioSnapshots()`
- effets de bord:
  - declenchement de collectes live
  - alimentation du `SnapshotStore`
  - publication WebSocket
- comportement d'erreur:
  - les erreurs sont attrapees par `refreshSafely(...)`
  - l'application reste disponible
- role workflow:
  - sequence de warmup globale non bloquante

`refreshSafely(String source, Mono<Void> action)`
- but: souscrire a un pipeline de warmup sans bloquer le startup
- entree: etiquette de log, pipeline Reactor
- sortie: `void`
- collaborateurs: `Mono.subscribe(...)`
- effets de bord: consommation reactive, journalisation technique
- comportement d'erreur: log avec stacktrace

## II. Couche controleur

### `tn.iteam.controller.MonitoringController`
#### Responsabilite
Expose l'API unifiee de lecture et de collecte des sources de supervision.

#### Dependances
- `MonitoringAggregationService`
- `SourceAvailabilityService`
- `IntegrationServiceRegistry`
- `ZkBioRefreshOrchestrationService`
- `MonitoringSnapshotPublicationService`

#### Methodes
`getProblems()`
- but: retourner les problemes unifies toutes sources confondues
- sortie: `UnifiedMonitoringResponse<List<UnifiedMonitoringProblemDTO>>`
- collaborateurs: `MonitoringAggregationService.getProblems(null)`
- effets de bord: aucun
- role workflow: lecture frontend dashboard / monitoring

`getMetrics()`
- but: retourner les metriques unifiees
- sortie: `UnifiedMonitoringResponse<List<UnifiedMonitoringMetricDTO>>`
- collaborateurs: `MonitoringAggregationService.getMetrics(null)`

`getHosts()`
- but: retourner les hosts unifies
- sortie: `UnifiedMonitoringResponse<List<UnifiedMonitoringHostDTO>>`
- collaborateurs: `MonitoringAggregationService.getHosts(null)`

`getSourceHealth()`
- but: exposer l'etat de sante de chaque integration
- sortie: `List<SourceAvailabilityDTO>`
- collaborateurs: `SourceAvailabilityService.getAll()`

`collectAll()`
- but: lancer une collecte manuelle multi-sources coherente
- entree: aucune
- sortie: `Mono<ResponseEntity<ApiResponse<Void>>>`
- collaborateurs:
  - `IntegrationServiceRegistry.getRequired(ZABBIX).refreshAsync()`
  - `IntegrationServiceRegistry.getRequired(OBSERVIUM).refreshAsync()`
  - `ZkBioRefreshOrchestrationService.refreshMonitoringAndAttendanceAsync()`
  - `IntegrationServiceRegistry.getRequired(CAMERA).refreshAsync()`
  - `MonitoringSnapshotPublicationService.publishMonitoringSnapshots(...)`
  - `MonitoringSnapshotPublicationService.publishZkBioSnapshots()`
- effets de bord:
  - collecte live
  - eventuelle persistance via services d'integration
  - publication websocket
- comportement d'erreur:
  - `Mono.whenDelayError(...)` agrege les echecs
  - la publication ne se produit qu'apres completion de la sequence combinee
- role workflow:
  - flux d'administration / support pour forcer une synchronisation

`collectZabbix()`, `collectObservium()`, `collectCamera()`
- but: collecte par source
- sorties: `Mono<ResponseEntity<ApiResponse<Void>>>`
- particularite:
  - Zabbix et Observium publient ensuite le snapshot correspondant
  - Camera ne publie pas de snapshot monitoring unifie supplementaire

### `tn.iteam.controller.ZkBioController`
#### Responsabilite
Expose les operations ZKBio specifiques a la source.

#### Dependances
- `ZkBioServiceInterface`
- `ZkBioIntegrationOperations`

#### Methodes principales
`getServerStatus()`
- but: retourner le statut serveur ZKBio
- sortie: `ApiResponse<Map<String,Object>>`
- collaborateurs: `ZkBioServiceInterface.getServerStatus()`
- role workflow: vue support / diagnostic source

`getDevices()`
- but: recuperer la liste des devices ZKBio
- collaborateurs: `ZkBioServiceInterface.getDevices()`

`getProblems()`
- but: retourner les alertes/problemes ZKBio source-specific
- collaborateurs: `ZkBioServiceInterface.getProblems()`

`getAttendanceLogs()`, `getAttendanceLogsByRange(...)`
- but: exposer les journaux de presence
- collaborateurs: `ZkBioServiceInterface.getAttendanceLogs...`

`triggerCollection()`
- but: forcer une collecte ZKBio complete avec publication
- collaborateurs: `ZkBioIntegrationOperations.refreshAllAndPublishAsync()`
- comportement d'erreur: reactive; laisse remonter vers `GlobalExceptionHandler` si non absorbe en aval

`getUsers()`
- but apparent: renvoyer des utilisateurs ZKBio
- observation: retourne `List<ZkBioAttendanceDTO>`; il s'agit d'un probleme de nommage / modelisation

### `tn.iteam.controller.TicketController`
#### Responsabilite
Expose le workflow de vie des tickets.

#### Dependances
- `TicketService`
- `TicketMapper`

#### Methodes structurantes
`createFromProblem(...)`
- but: creer un ticket a partir d'un probleme de monitoring
- collaborateurs: `TicketService.createFromProblem(...)`, `TicketMapper`
- effets de bord: insertion ticket + publication `/topic/tickets`

`createManual(...)`
- but: creer un ticket manuel
- effets de bord: insertion ticket, statut OPEN, publication WebSocket

`assign(...)`
- but: affecter un ticket a un utilisateur
- effets de bord: modification de statut eventuelle, journal d'intervention

`updateStatus(...)`, `validate(...)`, `reject(...)`
- but: faire progresser ou terminer le ticket
- comportement d'erreur: `TicketingException` si transition invalide ou entite absente

`addComment(...)`, `addIntervention(...)`
- but: completer l'historique d'un ticket
- effets de bord: persistance `Intervention`

### Controleurs de compatibilite
`ZabbixProblemController`, `ZabbixMetricsController`, `ObserviumController`
- role: compatibilite avec anciens contrats REST
- statut:
  - `ZabbixProblemController` et `ZabbixMetricsController` sont `@Deprecated`
  - `ObserviumController` est commente comme temporaire
- inference:
  - ces controleurs ne sont pas centraux dans l'architecture cible

### `DashboardController` et `CameraController`
- `DashboardController`: facade read-only vers `DashboardService`
- `CameraController`: exposition de l'inventaire camera persiste

## III. Orchestration d'integration et resilience metier

### `IntegrationServiceRegistry`
#### Responsabilite
Resolver les integrations par `MonitoringSourceType`.

#### Methodes
`IntegrationServiceRegistry(List<AsyncIntegrationService>)`
- but: indexer les implementations a partir de leur `getSourceType()`

`getRequired(MonitoringSourceType sourceType)`
- but: retourner le service requis ou lever une erreur
- comportement d'erreur: `IllegalArgumentException` si aucune implementation
- conclusion: service leger, sans logique metier cachee

### `ZkBioRefreshOrchestrationService`
#### Responsabilite
Rendre explicite la regle de sequencing ZKBio.

#### Methode
`refreshMonitoringAndAttendanceAsync()`
- but: lancer d'abord le refresh monitoring ZKBio puis le refresh attendance
- sortie: `Mono<Void>`
- collaborateurs: `ZkBioIntegrationOperations.refreshAsync()`, `refreshAttendanceAsync()`
- role workflow: alignement startup / collectAll
- comportement d'erreur: laisse remonter l'erreur au pipeline appelant

### `ZabbixIntegrationService`
#### Responsabilite
Orchestration par dataset de la supervision Zabbix avec fallback snapshot.

#### Collaborateurs majeurs
- `ZabbixAdapter`
- `ZabbixMonitoringMapper`
- `ServiceStatusPersistenceService`
- `ZabbixProblemService`
- `ZabbixMetricsService`
- `SnapshotStore`
- `SourceAvailabilityService`
- `MonitoredHostSnapshotService`
- `ZabbixSyncService`

#### Methodes majeures
`refresh()`
- but: point d'entree synchrone, delegue au pipeline async
- effets: subscription defensive

`refreshHosts()`
- but: synchroniser les hosts Zabbix
- collaborateurs:
  - `ZabbixAdapter.fetchAll()`
  - `ServiceStatusPersistenceService.saveAll(...)`
  - `ZabbixSyncService.loadHostMap()`
  - `MonitoredHostSnapshotService.loadHosts(...)`
  - `saveSnapshot(...)`
- effets de bord:
  - persistance des statuts
  - rechargement map host
  - mise a jour du `SnapshotStore`
- comportement d'erreur:
  - bascule vers `handleRefreshFailure("hosts", ...)`

`refreshProblems()`
- but: synchroniser les problemes actifs Zabbix
- collaborateurs:
  - `ZabbixProblemService.synchronizeActiveProblemsFromZabbix()`
  - `ZabbixMonitoringMapper.toProblem(...)`
  - `saveSnapshot(...)`

`refreshMetrics()`
- but: wrapper synchrone du pipeline metrics

`refreshAsync()`
- but: enchainer `refreshHosts`, `refreshProblems`, `refreshMetricsAsync`
- role workflow: pipeline complet Zabbix

`refreshMetricsAsync()`
- but: recuperer, persister et snapshoter les metriques Zabbix
- collaborateurs: `ZabbixMetricsService.fetchAndSaveMetrics()`
- comportement d'erreur:
  - `onErrorResume` vers `handleRefreshFailure(...)`
  - retourne `Mono.empty()` apres fallback

#### Helpers structurants
`runLegacyStepAsync(...)`
- but: executer une etape synchrone sur `boundedElastic`

`saveSnapshot(...)`
- but: enregistrer un snapshot `live`
- effets: `availabilityService.markAvailable(source)`

`handleRefreshFailure(...)`
- but: appliquer l'escalier de degradation
- ordre confirme:
  1. snapshot memoire existant
  2. fallback persiste
  3. snapshot vide

#### Role dans le module
Classe-pivot du monitoring Zabbix. Elle combine acquisition, persistence indirecte, fallback et marquage de disponibilite.

### `ObserviumIntegrationService`
#### Responsabilite
Equivalent structurel du service Zabbix pour la source Observium.

#### Particularites
- meme patron de fallback
- persistence via `ObserviumPersistenceService` et repositories Observium
- duplication elevee des helpers `safeMessage`, `saveSnapshot`, `safeLoadPersistedFallback`

### `ZkBioIntegrationService`
#### Responsabilite
Service d'integration le plus large du projet.

#### Collaborateurs majeurs
- `ZkBioServiceInterface`
- `ZkBioAdapter`
- `ZkBioMonitoringMapper`
- `ZkBioPersistenceService`
- `ServiceStatusPersistenceService`
- `SnapshotStore`
- `SourceAvailabilityService`
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`

#### Methodes majeures
`refreshAsync()`
- but: pipeline monitoring ZKBio

`refreshAttendanceAsync()`
- but: rafraichir les journaux de presence

`refreshAllAndPublishAsync()`
- but: lancer monitoring + attendance + publication
- observation:
  - chevauchement conceptuel avec `ZkBioRefreshOrchestrationService` + `MonitoringSnapshotPublicationService`
  - conserve toutefois un sens fonctionnel pour l'endpoint `/api/zkbio/collect`

`refreshRawDatasetAsync(...)`
- but: factoriser la collecte des datasets bruts ZKBio
- effets:
  - snapshot dataset-specifique
  - publication potentielle suivant l'appelant

#### Evaluation
`Fait confirme`: cette classe porte plus de responsabilites que ses homologues Zabbix et Observium.

### `CameraIntegrationService`
#### Responsabilite
Service d'integration minimal pour les hotes camera.

#### Methodes
`refreshAsync()`
- but: executer la collecte camera en mode async
- collaborateurs: `CameraAdapter.fetchAll()`, `MonitoredHostSnapshotService.loadHosts(...)`, `SnapshotStore`

`handleRefreshFailure(...)`
- but: fallback snapshot/empty similaire aux autres integrations

## IV. Services applicatifs et metier

### `TicketServiceImpl`
#### Responsabilite
Encapsuler le cycle de vie complet des tickets.

#### Collaborateurs
- `TicketRepository`
- `UserRepository`
- `InterventionRepository`
- `SimpMessagingTemplate`

#### Methodes principales
`createFromProblem(ZabbixProblemDTO problem, Long creatorId)`
- but: convertir un incident de monitoring en ticket
- entree: DTO de probleme, id createur
- sortie: `Ticket`
- collaborateurs:
  - `getUserOrThrow(...)`
  - `mapSeverity(...)`
  - `resolveResourceRef(...)`
  - `ticketRepository.save(...)`
  - `notify("NEW_TICKET", saved)`
- effets:
  - creation ticket
  - publication WebSocket
- erreurs:
  - `TicketingException` si utilisateur introuvable

`createManual(Ticket ticket, Long creatorId)`
- but: creer un ticket manuel en imposant `OPEN`
- effets:
  - initialise `creationDate`, `createdBy`, `interventions`

`assign(Long ticketId, Long userId)`
- but: affecter le ticket
- effets:
  - statut `OPEN -> IN_PROGRESS`
  - insertion d'une `Intervention`

`updateStatus(...)`
- but: gerer les transitions standards
- point d'attention:
  - le log affiche le statut apres mutation de l'objet; la trace "from -> to" n'est pas strictement fiable

`validate(...)`, `reject(...)`
- but: finaliser le ticket avec validation ou rejet
- effets:
  - `validatedBy`
  - intervention historique

`search(...)`
- but: construire une `Specification<Ticket>` dynamique
- role workflow: listing et filtrage backoffice

`ensureTransitionAllowed(...)`
- but: centraliser l'automate d'etat du ticket
- comportement d'erreur:
  - `TicketingException(BAD_REQUEST, INVALID_TICKET_TRANSITION, ...)`

`recordIntervention(...)`
- but: persister une action historisee

### `DashboardServiceImpl`
#### Responsabilite
Assembler la vue dashboard a partir des donnees Zabbix persistees et du moteur ML.

#### Collaborateurs
- `ZabbixProblemRepository`
- `ZabbixMetricRepository`
- `MonitoredHostRepository`
- `TorchScriptPredictionService`
- `ZabbixDataQualityService`

#### Methodes majeures
`getOverview()`
- but: produire une synthese incluant problemes, predictions, anomalies et qualite de donnees
- role workflow: endpoint `/dashboard/overview`

`getPredictions()`
- but: produire un score de risque par host
- appels:
  - `loadHostContexts()`
  - `buildPredictionFeatures(...)`
  - `TorchScriptPredictionService.predict(...)`
- erreurs:
  - plusieurs exceptions ML sont journalisees et absorbees

`getAnomalies()`
- but: detecter des anomalies statistiques a partir des metriques persistees
- appels:
  - `computeAnomaly(...)`

`buildPredictionFeatures(Long hostId)`
- but: construire le vecteur de features dans l'ordre attendu par le modele
- effets: aucun
- inference:
  - piece clef de la logique dashboard / ML

`loadHostContexts()`
- but: determiner l'ensemble des hosts Zabbix a analyser
- fallback:
  - `MonitoredHostRepository` d'abord
  - `ZabbixMetricRepository.findDistinctHostIds()` ensuite

### `SourceAvailabilityServiceImpl`
#### Responsabilite
Maintenir l'etat de disponibilite courant des sources.

#### Methodes essentielles
`markAvailable(...)`, `markDegraded(...)`, `markUnavailable(...)`
- but: mettre a jour l'etat source
- effets:
  - enregistrement memoire
  - publication WebSocket si changement significatif

`shouldAttempt(String source, long retryBackoffMs)`
- but: controler le cooldown scheduler-level
- role workflow: resilience de polling

`isRetryCooldownActive(...)`
- but: verifier si la source reste en periode de retenue

### `ZabbixMetricsServiceImpl`
#### Responsabilite
Synchroniser, fusionner et persister les metriques Zabbix.

#### Collaborateurs
- `ZabbixAdapter`
- `ZabbixMetricMapper`
- `ZabbixMetricRepository`
- `SourceAvailabilityService`
- `ZabbixDataQualityService`
- `TransactionTemplate`

#### Methodes principales
`getPersistedMetricsSnapshot()`
- but: charger le snapshot persiste complet

`synchronizeAndGetPersistedMetricsSnapshot()`
- but: rafraichir les metriques puis relire le snapshot persiste
- observation:
  - candidate de methode faiblement referencee statiquement

`fetchAndSaveMetrics()`, `fetchAndSaveMetrics(JsonNode hosts)`
- but: point d'entree principal de collecte lourde
- comportement:
  - evite les recouvrements via `AtomicBoolean metricsRefreshInProgress`
  - bascule sur le snapshot persiste si un refresh lourd est deja en cours

`doFetchAndSaveMetrics(...)`
- but: orchestrer validation, merge, persistence transactionnelle

`resolveEmptyMetricsResult()`
- but: reutiliser le snapshot persiste si rien n'a ete collecte live

`loadExistingMetricsByKey(...)`
- but: charger les tuples existants pour faire un merge idempotent

`mapDtosToEntities(...)`
- but: transformer et filtrer les lignes invalides

`persistMetricsInTransaction(...)`
- but: ecrire en base et journaliser la qualite des donnees

## V. Adapters, clients et boundaries techniques

### `ZabbixAdapter`
#### Responsabilite
Acces technique le plus riche du projet pour Zabbix.

#### Fonctions majeures
- collecte hosts
- collecte problems
- collecte metrics
- batch history
- mapping vers DTOs applicatifs

#### Observation
`Fait confirme`: il s'agit du principal hotspot de couplage et de dette technique.

### `ZabbixClient`
#### Responsabilite
Boundary HTTP + Resilience4j pour Zabbix.

#### Caracteristiques
- profils `zabbixApiLight` et `zabbixApiHeavy`
- timeout distinct pour flux legers et lourds
- mapping des erreurs transport vers le modele d'exception `IntegrationException`

### `ObserviumClientX`
#### Responsabilite
Boundary HTTP Observium.

#### Methodes structurantes
- `getDevices()`
- `getAlerts()`
- `callApiLive(...)`
- `mapCircuitBreakerException(...)`

#### Comportement
- applique timeout Reactor
- applique circuit breaker / retry
- mappe les erreurs transport via `IntegrationClientSupport`

### `ZkBioClientX`
#### Responsabilite
Boundary HTTP ZKBio.

#### Particularites
- mapping explicite des timeouts et indisponibilites
- preserve les causes pour `IntegrationTimeoutException` et `IntegrationUnavailableException`

### `CameraAdapter`
#### Responsabilite
Scanner les endpoints camera via socket.

#### Particularites
- pas de WebClient
- timeout au niveau `Socket.connect(...)`
- mapping d'erreur global pour les echec de scan de haut niveau

## VI. Exceptions, schedulers, publication et support

### `GlobalExceptionHandler`
#### Responsabilite
Normaliser les erreurs REST.

#### Methodes
`handleIntegrationException(...)`
- distingue timeout / unavailable et autres erreurs d'integration
- journalisation technique avec stacktrace

`handleTicketingException(...)`
- reponse metier normalisee

`handleAllExceptions(...)`
- filet de securite global

### Schedulers
`ZabbixScheduler`, `ObserviumScheduler`, `ObserviumHostsScheduler`, `ZkBioScheduler`
- role:
  - planifier les refresh periodiques
  - verifier `SourceAvailabilityService.shouldAttempt(...)`
  - publier les snapshots apres refresh quand approprie

### `MonitoringSnapshotPublicationService`
#### Responsabilite
Centraliser la publication de snapshots vers WebSocket.

#### Methodes
- `publishMonitoringSnapshots(MonitoringSourceType sourceType)`
- `publishMonitoringSnapshots(Iterable<MonitoringSourceType> sourceTypes)`
- `publishProblemsSnapshot(...)`
- `publishMetricsSnapshot(...)`
- `publishZkBioSnapshots()`

#### Evaluation
`Fait confirme`: le service reste leger et sans logique metier cachee.

## VII. Classes structurelles resumees

### Entites et DTOs
Les classes suivantes sont majoritairement des supports de donnees:

- `domain.*`
- `dto.*`
- `monitoring.dto.*`
- `ml.dto.*`

Elles portent:

- l'etat persiste JPA
- les contrats REST
- les contrats WebSocket
- les contrats internes de mapping

### Repositories
Les repositories Spring Data sont structurels. Leur role est purement persistants, a l'exception de quelques methodes de projection utiles aux workflows:

- `ZabbixMetricRepository.findDistinctHostIds()`
- `findAllByHostIdInAndItemIdInAndTimestampIn(...)`

### Mappers
Les mappers sont majoritairement des composants de transformation pure. `CategoryResolver` ajoute une petite logique de normalisation metier pour le classement des ressources.
