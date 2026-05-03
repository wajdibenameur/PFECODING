$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path "frontend/.codex-audit/reports" | Out-Null

$types = Get-Content "frontend/.codex-audit/types.json" | ConvertFrom-Json

function Get-TypeRow($t) {
    $name = $t.Name
    $refs = rg -n --glob '!target/**' --glob '!frontend/dist/**' --glob '!frontend/node_modules/**' --word-regexp $name src/main/java src/test/java 2>$null
    $nonSelf = @($refs | Where-Object { $_ -notmatch [regex]::Escape($t.Relative) })
    $mainCount = @($nonSelf | Where-Object { $_ -like 'src/main/java*' }).Count
    $testCount = @($nonSelf | Where-Object { $_ -like 'src/test/java*' }).Count
    $activation = if ($t.Annotations -match 'RestController|Controller') { 'Endpoint Spring' }
        elseif ($t.Annotations -match 'Scheduled|Component|Service|Repository|Configuration|Bean|ConfigurationProperties') { 'Bean Spring' }
        elseif ($t.Annotations -match 'Entity') { 'Entite JPA' }
        elseif ($t.Kind -eq 'interface') { 'Abstraction' }
        elseif ($t.Package -like 'tn.iteam.dto*' -or $t.Package -like 'tn.iteam.ml.dto*' -or $t.Package -like 'tn.iteam.monitoring.dto*') { 'DTO/Wrapper' }
        else { 'POJO/Support' }
    $note = if ($mainCount -eq 0 -and $testCount -eq 0 -and $activation -notin @('Endpoint Spring', 'Bean Spring', 'Entite JPA')) { 'A verifier: aucune reference statique hors declaration' }
        elseif ($mainCount -eq 0 -and $testCount -gt 0) { 'Utilise seulement par les tests' }
        elseif ($t.Annotations -match 'RestController|Controller|Service|Component|Repository|Configuration|Entity|ConfigurationProperties') { 'Activation indirecte par Spring/JPA possible' }
        else { 'Reference statique detectee' }
    [pscustomobject]@{
        Nom = $t.Name
        Type = $t.Kind
        Package = $t.Package
        Fichier = $t.Relative.Replace('\', '/')
        RefMain = $mainCount
        RefTest = $testCount
        Activation = $activation
        Note = $note
    }
}

$rows = foreach ($t in $types | Sort-Object Package, Name) { Get-TypeRow $t }

$summary = @'
# Resume executif

## Perimetre reel analyse
- Backend principal: `src/main/java/tn/iteam`
- Support croise: `src/test/java/tn/iteam`, `src/main/resources/application.properties`, `pom.xml`, appels du frontend local `frontend/src`
- Verification Spring prise en compte: `@RestController`, `@Service`, `@Component`, `@Repository`, `@Configuration`, `@Bean`, `@Entity`, `@Scheduled`, `@EventListener`, `CommandLineRunner`, WebSocket STOMP, Resilience4j fallback signatures, wiring via interfaces

## Chiffres globaux
- Classes concretes analysees: 122
- Interfaces analysees: 29
- Enums analyses: 5
- Records analyses: 6
- Methodes analysees statiquement: environ 537
- Packages / sous-packages vides detectes: 4

## Constats majeurs
- Le coeur actif de production est centre sur quatre flux: monitoring unifie (`/api/monitoring/*`), ZKBio direct (`/api/zkbio/*`), dashboard (`/dashboard/*`), ticketing (`/api/tickets/*`).
- Les endpoints legacy Zabbix et Observium ne sont plus consommes par le frontend du depot. Ils existent surtout pour compatibilite potentielle externe.
- Le plus gros gisement de duplication se situe dans les services d'integration source par source: `ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService`, `CameraIntegrationService`.
- Le plus gros hotspot de couplage est la pile Zabbix: `ZabbixAdapter` (769 lignes), `ZabbixClient` (643 lignes), `ZabbixIntegrationService`, `ZabbixMetricsServiceImpl`, `ZabbixProblemServiceImpl`, `ZabbixSyncService`.
- Plusieurs methodes sont mortes a forte confiance sans impact Spring: helpers non appeles dans `IntegrationClientSupport`, methodes de mapping non consommees, anciennes methodes WebSocket directes, deux methodes de service Zabbix jamais appelees.
- Le modele role/permission est partiellement dormant: les entites `User` et `Role` sont utilisees pour le ticketing et le bootstrap, mais aucune securite Spring ni enforcement de permissions n'est branche a l'execution.

## Meilleurs candidats SUPPRIMER MAINTENANT
- `IntegrationClientSupport.httpOn(...)`
- `IntegrationClientSupport.timeoutOn(...)`
- `IntegrationClientSupport.invalidJsonOn(...)`
- `IntegrationClientSupport.returnedHttpDuring(...)`
- `IntegrationClientSupport.stableFallbackReason(...)`
- `IntegrationClientSupport.transportErrorOn(...)`
- `IntegrationClientSupport.unexpectedErrorOn(...)`
- `IntegrationClientSupport.duringMessage(...)`
- `IntegrationDataUnavailableException.forZabbix(...)`
- `IntegrationDataUnavailableException.forZkBio(...)`
- `ServiceStatusMapper.toDTO(...)`
- `ZabbixMonitoringMapper.toHost(...)`
- `ZabbixMonitoringMapper.toHostFromServiceStatus(...)`
- `ZabbixMonitoringMapper.toMetricFromDTO(...)`
- `ZabbixMetricsService.synchronizeAndGetPersistedMetricsSnapshot(...)` et son implementation
- `ZabbixProblemService.synchronizeAndGetPersistedFilteredActiveProblems(...)` et son implementation
- `MonitoringWebSocketPublisher.publishProblems(List<...>)`
- `MonitoringWebSocketPublisher.publishMetrics(List<...>)`
- `ZkBioWebSocketPublisher.publishProblems(List<...>)`
- `ZkBioWebSocketPublisher.publishAttendance(List<...>)`
- `ZkBioWebSocketPublisher.publishDevices(List<?>)`
- `ZkBioWebSocketPublisher.publishStatus(Object)`
- `ZkBioWebSocketPublisher.publishProblemsFromSnapshot()`

## Meilleurs candidats FUSIONNER / FACTORISER
- Factoriser la gestion de snapshot/fallback/`saveSnapshot`/`handleRefreshFailure` commune a `ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService`, `CameraIntegrationService`.
- Factoriser `ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl` sur la persistence des metrics et la logique de cloture des problems actifs absents du flux live.
- Unifier les mappers de monitoring (`ObserviumMonitoringMapper`, `ZkBioMonitoringMapper`, parties de `ZabbixMonitoringMapper`) autour d'un socle commun pour `host/problem/metric -> unified monitoring DTO`.
- Rapprocher `ZabbixSyncService` de `MonitoredHostPersistenceServiceImpl`: les deux font de la persistence/merge d'inventaire d'hotes avec des regles proches.
- Rapprocher `ZkBioServiceImpl.getServerStatus()` et `ZkBioAdapter.baseServerStatus()` qui reconstruisent tous deux le statut serveur a partir de `ZkBioClientX.getBaseUri()`.

## Endpoints probablement dormants ou purement de compatibilite
- `/api/zabbix/metrics` via `ZabbixMetricsController`
- `/api/zabbix/active` via `ZabbixProblemController`
- `/api/observium/summary` via `ObserviumController`
- `/predict` via `TorchScriptPredictionController` (aucune consommation detectee dans le frontend du depot)
- `/api/zkbio/problems`, `/api/zkbio/attendance/range`, `/api/zkbio/users` (absents du frontend du depot)

## Packages / sous-packages vides
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`
'@

$cartoHeader = @'
# Cartographie complete des usages

## Methodologie de preuve
- References statiques: recherche recursive sur `src/main/java`, `src/test/java`, annotations, injections, implementations et endpoints.
- Activation indirecte: prise en compte des beans Spring, des entites JPA, des schedulers, des listeners de startup, des fallback Resilience4j et des publishers WebSocket.
- Consommation frontend locale: croisement avec `frontend/src` pour distinguer les endpoints du depot des endpoints potentiellement gardes pour des consommateurs externes.

## Workflows actifs identifies
- Monitoring unifie: `MonitoringController -> MonitoringAggregationService -> MonitoringCacheService -> SnapshotStore`
- Warmup / scheduling: `MonitoringStartup` + `ZabbixScheduler` + `ObserviumScheduler` + `ObserviumHostsScheduler` + `ZkBioScheduler`
- Zabbix: `IntegrationServiceRegistry -> ZabbixIntegrationService -> ZabbixAdapter/ZabbixClient + ZabbixProblemServiceImpl + ZabbixMetricsServiceImpl + ZabbixSyncService`
- Observium: `IntegrationServiceRegistry -> ObserviumIntegrationService -> ObserviumAdapter/ObserviumClientX + ObserviumPersistenceServiceImpl`
- ZKBio: `ZkBioController | MonitoringController | MonitoringStartup -> ZkBioRefreshOrchestrationService / ZkBioIntegrationService -> ZkBioAdapter/ZkBioClientX + ZkBioPersistenceServiceImpl`
- Ticketing: `TicketController -> TicketServiceImpl -> TicketRepository/UserRepository/InterventionRepository + WebSocket ticket topic`
- Dashboard: `DashboardController -> DashboardServiceImpl -> Zabbix repositories + TorchScriptPredictionService`

## Tableau complet des types analyses

| Nom | Type | Package | Fichier | Ref main | Ref test | Activation | Note |
|---|---|---|---|---:|---:|---|---|
'@

$dead = @'
# Candidats code mort

## Candidats a forte confiance

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`IntegrationClientSupport.httpOn(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/util/IntegrationClientSupport.java` / `IntegrationClientSupport` / `httpOn`
- Categorie: Methode non utilisee
- Preuves: recherche exacte `httpOn(` dans `src/main/java`, `src/test/java`
- References trouvees: declaration uniquement dans `IntegrationClientSupport.java:42`
- References non trouvees: aucun appel applicatif ou test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: methode utilitaire pure, sans activation Spring ni reflection
- Action suggeree: supprimer la methode et relancer la compilation
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`IntegrationClientSupport.timeoutOn(...)`, `invalidJsonOn(...)`, `returnedHttpDuring(...)`, `stableFallbackReason(...)`, `transportErrorOn(...)`, `unexpectedErrorOn(...)`, `duringMessage(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/util/IntegrationClientSupport.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `timeoutOn(`, `invalidJsonOn(`, `returnedHttpDuring(`, `stableFallbackReason(`, `transportErrorOn(`, `unexpectedErrorOn(`, `duringMessage(`
- References trouvees: declaration uniquement dans la classe utilitaire
- References non trouvees: aucun appel main/test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: API utilitaire trop large, ces helpers ne participent a aucun workflow actif
- Action suggeree: supprimer les methodes mortes, conserver `mapTransportException(...)` et les helpers reellement consommes
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`IntegrationDataUnavailableException.forZabbix(...)` et `IntegrationDataUnavailableException.forZkBio(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/exception/IntegrationDataUnavailableException.java`
- Categorie: Methodes non utilisees
- Preuves: recherche exacte `forZabbix(` et `forZkBio(`
- References trouvees: declaration uniquement dans la classe d'exception
- References non trouvees: aucun appel applicatif ; seul `forObservium(...)` est consomme par `ObserviumAdapter`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: factories inertes sans couplage runtime
- Action suggeree: supprimer les deux factories inutilisees
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ServiceStatusMapper.toDTO(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/ServiceStatusMapper.java` / `ServiceStatusMapper` / `toDTO`
- Categorie: Methode non utilisee
- Preuves: recherche exacte `statusMapper.toDTO(` et `toDTO(` sur le mapper
- References trouvees: declaration seule `ServiceStatusMapper.java:44`
- References non trouvees: aucun appel main/test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: le flux actif est uniquement DTO -> entity (`toEntity`, `updateEntity`)
- Action suggeree: supprimer `toDTO(...)`
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixMonitoringMapper.toHost(...)`, `toHostFromServiceStatus(...)`, `toMetricFromDTO(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/ZabbixMonitoringMapper.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `toHostFromServiceStatus(`, `toMetricFromDTO(` ; recherche de `toHost(` ne remonte aucune consommation du mapper Zabbix hors declaration
- References trouvees: declaration uniquement dans `ZabbixMonitoringMapper`
- References non trouvees: aucun appel depuis `ZabbixIntegrationService`, `MonitoringAggregationService` ou tests
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: seul `toProblem(...)` et `toMetric(ZabbixMetric)` participent aux snapshots actifs
- Action suggeree: supprimer ces trois methodes et laisser le mapper sur le sous-ensemble reellement utilise
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixMetricsService.synchronizeAndGetPersistedMetricsSnapshot(...)` et `ZabbixProblemService.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ZabbixMetricsService.java`, `src/main/java/tn/iteam/service/impl/ZabbixMetricsServiceImpl.java`, `src/main/java/tn/iteam/service/ZabbixProblemService.java`, `src/main/java/tn/iteam/service/impl/ZabbixProblemServiceImpl.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes des deux signatures
- References trouvees: interface + implementation uniquement
- References non trouvees: aucun controller, scheduler, integration service ou test ne les appelle
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: les workflows actifs utilisent `fetchAndSaveMetrics()`, `getPersistedMetricsSnapshot()`, `synchronizeActiveProblemsFromZabbix()` et `getPersistedFilteredActiveProblems()`
- Action suggeree: supprimer les signatures de l'interface et les implementations mortes
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`MonitoringWebSocketPublisher.publishProblems(List<...>)` et `publishMetrics(List<...>)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/websocket/MonitoringWebSocketPublisher.java`
- Categorie: Methodes non utilisees
- Preuves: recherche exacte `publishProblems(` et `publishMetrics(` hors declaration
- References trouvees: declaration seule ; les appels actifs visent `publishProblemsFromSnapshot(...)` et `publishMetricsFromSnapshot(...)`
- References non trouvees: aucun appel applicatif ni test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: le projet a bascule vers publication depuis `SnapshotStore`
- Action suggeree: supprimer les methodes de publication directe et conserver les variantes snapshot
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZkBioWebSocketPublisher.publishProblems(List<...>)`, `publishAttendance(List<...>)`, `publishDevices(List<?>)`, `publishStatus(Object)`, `publishProblemsFromSnapshot()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/websocket/ZkBioWebSocketPublisher.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `publishAttendance(`, `publishDevices(`, `publishStatus(`, `publishProblemsFromSnapshot()`
- References trouvees: declaration seule ; les appels actifs passent par `publishAttendanceFromSnapshot()`, `publishDevicesFromSnapshot()`, `publishStatusFromSnapshot()`
- References non trouvees: aucun appel applicatif ni test pour les variantes directes ni pour `publishProblemsFromSnapshot()`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: les snapshots ZKBio publies aujourd'hui sont attendance/device/status ; la publication directe des problems est orpheline
- Action suggeree: supprimer ces cinq methodes apres compilation locale
- Verification manuelle necessaire ? (oui/non): non
'@

$dup = @'
# Candidats duplication et fusion

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService`, `CameraIntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/*IntegrationService.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion ; Hotspot de couplage fort
- Preuves: memes blocs `refresh*`, `saveSnapshot`, `handleRefreshFailure`, `safeGetExistingSnapshot`, `safeLoadPersistedFallback`, `saveFallbackSnapshot`, `runStepAsync`, `subscribeSafely`, `safeMessage`, `toException`
- References trouvees: services actifs via `IntegrationServiceRegistry`, schedulers, `MonitoringStartup`, `MonitoringController`, `ZkBioController`
- References non trouvees: aucune abstraction commune pour le socle snapshot/fallback
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: duplication large mais critique ; une extraction partielle est sure si elle laisse chaque source maitriser uniquement `loadPersistedFallback` et ses datasets
- Action suggeree: introduire un support commun `AbstractSnapshotBackedIntegrationService` ou un helper de fallback/snapshot ; garder les specifics source par source en overrides
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/ObserviumPersistenceServiceImpl.java`, `src/main/java/tn/iteam/service/impl/ZkBioPersistenceServiceImpl.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion
- Preuves: meme structure pour `saveProblems(...)`, meme cloture des problems actifs absents du flux live, meme strategy `find existing -> merge -> saveAll`, meme logic pour `saveMetrics(...)`
- References trouvees: services actifs appeles depuis `ObserviumIntegrationService` et `ZkBioIntegrationService`
- References non trouvees: aucune factorisation actuelle
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: la partie metrics est quasiment identique ; la partie problem differe legerement sur les champs (`severity`, `status`, `hostId`)
- Action suggeree: factoriser d'abord la persistence metrics, puis extraire un template pour la cloture des problems et le merge conditionnel
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumMonitoringMapper`, `ZkBioMonitoringMapper`, parties de `ZabbixMonitoringMapper`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/*MonitoringMapper.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion
- Preuves: meme pattern `source + hostId + itemId + timestamp -> UnifiedMonitoringMetricDTO`, meme pattern de construction `UnifiedMonitoringProblemDTO`, normalisation `UNKNOWN` / timestamps de fallback, duplication de `formatTimestamp(...)`
- References trouvees: mappers actifs consommes par les services d'integration
- References non trouvees: aucune base commune de mapping monitoring unifie
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: extraction partielle sure sur les builders communs `metric` et utilitaires `timestamp/normalize`
- Action suggeree: creer un support de mapping monitoring unifie par source, sans toucher tout de suite aux regles Zabbix specifiques
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`MonitoredHostPersistenceServiceImpl` et `ZabbixSyncService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/MonitoredHostPersistenceServiceImpl.java`, `src/main/java/tn/iteam/service/ZabbixSyncService.java`
- Categorie: Services/helpers qui se chevauchent ; Opportunite de fusion
- Preuves: les deux mettent a jour `MonitoredHostRepository` avec des regles proches de merge nom/ip/port ; `ZabbixSyncService.loadHostMap(JsonNode hosts)` reproduit un parcours source-specifique la ou Observium/ZKBio utilisent `MonitoredHostPersistenceService`
- References trouvees: `ZabbixIntegrationService.refreshHosts()` appelle `zabbixSyncService.loadHostMap()`, `ObserviumIntegrationService` et `ZkBioIntegrationService` appellent `MonitoredHostPersistenceService.saveAll(...)`
- References non trouvees: aucune convergence d'API entre le chemin Zabbix et les autres sources
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: FUSIONNER
- Pourquoi: c'est la divergence technique la plus nette entre Zabbix et les autres sources pour un besoin identique
- Action suggeree: extraire la persistence d'hotes Zabbix vers `MonitoredHostPersistenceService`, garder `ZabbixSyncService` uniquement pour le cache transitoire si vraiment necessaire
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZkBioServiceImpl.getServerStatus()` et `ZkBioAdapter.baseServerStatus()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ZkBioServiceImpl.java`, `src/main/java/tn/iteam/adapter/zkbio/ZkBioAdapter.java`
- Categorie: Implementation quasi dupliquee
- Preuves: les deux reconstruisent `ServiceStatusDTO` a partir de `ZkBioClientX.getBaseUri()` et de regles `scheme -> port/protocol`
- References trouvees: `ZkBioServiceImpl.getServerStatus()` alimente le controller ; `ZkBioAdapter.baseServerStatus()` alimente `fetchAll()` et `fetchMetrics()`
- References non trouvees: aucune methode commune partagee
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: petite duplication mais repetee sur un point sensible (URL, port, protocol, nom serveur)
- Action suggeree: extraire un `ZkBioStatusFactory` ou une methode commune dans l'adapter/service apres arbitrage d'ownership
- Verification manuelle necessaire ? (oui/non): non
'@

$interfacesMd = @'
# Analyse interfaces / implementations

## Interfaces a conserver
- `AsyncIntegrationService`: vraie valeur polymorphique. C'est l'API branchee dans `IntegrationServiceRegistry` et consommee par `MonitoringStartup`, `MonitoringController`, `ZabbixScheduler`, `ObserviumScheduler`, `ObserviumHostsScheduler`.
- `ZkBioIntegrationOperations`: utile car `ZkBioScheduler`, `ZkBioController` et `ZkBioRefreshOrchestrationService` exploitent a la fois les variantes sync et async, y compris `refreshAttendanceAsync()` et `refreshAllAndPublishAsync()`.
- Les interfaces Spring Data `*Repository`: a conserver ; elles sont activees par JPA meme avec peu de references directes.

## Interfaces a valeur faible ou triviale

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`IntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/IntegrationService.java` / `IntegrationService`
- Categorie: Interface non utilisee ; Opportunite de simplification
- Preuves: seule reference statique hors declaration = `AsyncIntegrationService extends IntegrationService`; aucune injection, aucun consommateur direct, aucune recherche par type `IntegrationService`
- References trouvees: `AsyncIntegrationService`
- References non trouvees: aucun controller, scheduler, service ou test ne depend de `IntegrationService`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: FUSIONNER
- Pourquoi: l'abstraction utile du projet est `AsyncIntegrationService`, pas le parent synchrone
- Action suggeree: soit supprimer `IntegrationService` et remonter les defaults utiles dans `AsyncIntegrationService`, soit documenter clairement pourquoi garder deux niveaux
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumSummaryService` / `ObserviumSummaryServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ObserviumSummaryService.java`, `src/main/java/tn/iteam/service/impl/ObserviumSummaryServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; Classe utilisee une seule fois ; Opportunite de fusion
- Preuves: un seul consommateur `ObserviumController`; implementation unique ; logique simple basee sur `MonitoringAggregationService`
- References trouvees: `ObserviumController.getSummary()`
- References non trouvees: aucun autre module, aucun test, aucun wiring alternatif
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: FUSIONNER
- Pourquoi: abstraction mince sans variation d'implementation ; le vrai service coeur est deja `MonitoringAggregationService`
- Action suggeree: soit deplacer la logique dans le controller de compatibilite, soit l'integrer a `MonitoringAggregationService` comme vue derivee Observium
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`CameraInventoryService` / `CameraInventoryServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/CameraInventoryService.java`, `src/main/java/tn/iteam/service/impl/CameraInventoryServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; Classe utilisee une seule fois
- Preuves: un seul consommateur `CameraController`; implementation unique ; logique = lecture repository + projection DTO
- References trouvees: `CameraController.getRegisteredCameras()`
- References non trouvees: aucun autre service, aucun test dedie, aucune autre implementation
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: FUSIONNER
- Pourquoi: service tres fin ; peut vivre dans un composant plus proche du module camera
- Action suggeree: deplacer pres du module camera ou absorber dans un query service plus local au controller
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`DashboardService` / `DashboardServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/DashboardService.java`, `src/main/java/tn/iteam/service/impl/DashboardServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; God class / service trop gros
- Preuves: un seul consommateur `DashboardController`; implementation unique ; concentre overview + predictions + anomalies + data quality + ML
- References trouvees: `DashboardController`
- References non trouvees: aucun autre consommateur par interface
- Niveau de confiance: moyen
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: l'interface seule n'apporte pas beaucoup ; le vrai sujet est de decouper `DashboardServiceImpl`
- Action suggeree: avant de supprimer l'interface, extraire `DashboardPredictionService` et `DashboardAnomalyService`
- Verification manuelle necessaire ? (oui/non): oui
'@

$endpoints = @'
# Analyse endpoints et workflows

## Endpoints actifs cotes frontend du depot
- `GET /api/monitoring/hosts`, `GET /api/monitoring/problems`, `GET /api/monitoring/metrics`, `GET /api/monitoring/sources/health`, `POST /api/monitoring/collect*`
- `GET /api/cameras`
- `GET /api/zkbio/status`, `GET /api/zkbio/devices`, `GET /api/zkbio/attendance`, `POST /api/zkbio/collect`
- `GET /dashboard/overview`, `GET /dashboard/predictions`, `GET /dashboard/anomalies`
- `GET/POST/PUT/DELETE /api/tickets/*`

## Endpoints non consommes par le frontend du depot

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumController` / `GET /api/observium/summary`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ObserviumController.java` / `ObserviumController` / `getSummary`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Classe utilisee une seule fois
- Preuves: commentaire de classe parlant explicitement de compatibilite temporaire ; absence d'appel dans `frontend/src`; resume derive du flux unifie
- References trouvees: bean `@RestController`, injection `ObserviumSummaryService`
- References non trouvees: aucune consommation frontend du depot de `/api/observium/summary`
- Niveau de confiance: eleve
- Niveau de risque: moyen-eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: probablement obsolete dans le depot, mais possiblement encore consomme par un client externe
- Action suggeree: journaliser les hits ou mettre l'endpoint en deprecation fonctionnelle avant suppression
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixMetricsController` / `GET /api/zabbix/metrics`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZabbixMetricsController.java` / `ZabbixMetricsController` / `getMetrics`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Endpoint supplanter par un autre
- Preuves: `@Deprecated(since = "2026-04-22")`, commentaire de compatibilite, absence d'usage dans `frontend/src`, donnees deja derivees de `/api/monitoring/metrics`
- References trouvees: bean `@RestController`
- References non trouvees: aucun appel frontend du depot vers `/api/zabbix/metrics`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: supplanté dans le depot, mais endpoint historique potentiellement expose a l'exterieur
- Action suggeree: mesurer les hits HTTP puis supprimer apres migration si zero trafic
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixProblemController` / `GET /api/zabbix/active`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZabbixProblemController.java` / `ZabbixProblemController` / `allActive`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Endpoint supplanter par un autre
- Preuves: `@Deprecated(since = "2026-04-22")`, commentaire de compatibilite, absence d'usage dans `frontend/src`, remplacement evident par `/api/monitoring/problems`
- References trouvees: bean `@RestController`
- References non trouvees: aucun appel frontend du depot vers `/api/zabbix/active`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: meme situation que le controller metrics
- Action suggeree: deprecier activement puis supprimer apres validation trafic
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`TorchScriptPredictionController` / `POST /predict`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/ml/controller/TorchScriptPredictionController.java` / `TorchScriptPredictionController` / `predict`
- Categorie: Fonctionnalite dormante
- Preuves: aucun appel detecte a `/predict` dans `frontend/src`; la fonctionnalite ML consommee par le frontend passe par `DashboardServiceImpl -> TorchScriptPredictionService`
- References trouvees: bean `@RestController`, wiring vers `TorchScriptPredictionService`
- References non trouvees: aucun consommateur interne du depot
- Niveau de confiance: moyen
- Niveau de risque: moyen-eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: endpoint potentiellement reserve a des appels manuels ou externes hors repo
- Action suggeree: mesurer le trafic ; si zero, retirer le controller et exposer seulement le dashboard agrege
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZkBioController.getProblems()`, `getAttendanceLogsByRange()`, `getUsers()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZkBioController.java`
- Categorie: Fonctionnalite dormante ; Methodes possiblement non utilisees dans une classe active
- Preuves: absence d'appels frontend du depot vers `/api/zkbio/problems`, `/api/zkbio/attendance/range`, `/api/zkbio/users`; les problemes ZKBio visibles du frontend passent deja par le monitoring unifie
- References trouvees: exposition HTTP Spring
- References non trouvees: aucune consommation frontend locale
- Niveau de confiance: moyen
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: ces routes peuvent encore etre appelees par des outils externes ou des usages backoffice
- Action suggeree: tracer les hits puis archiver les routes sans trafic
- Verification manuelle necessaire ? (oui/non): oui

## Chevauchements clairs d'API
- `GET /api/zabbix/metrics` est supplanté par `GET /api/monitoring/metrics` + filtrage source `ZABBIX`.
- `GET /api/zabbix/active` est supplanté par `GET /api/monitoring/problems` + filtrage source `ZABBIX`.
- `GET /api/observium/summary` est un derive simplifie des flux `GET /api/monitoring/hosts` et `GET /api/monitoring/problems` pour `OBSERVIUM`.
- `POST /api/zkbio/collect` chevauche partiellement `POST /api/monitoring/collect` et `POST /api/monitoring/collect/zkbio`, avec un enrichissement attendance/device/status propre a ZKBio.
'@

$dto = @'
# Analyse DTO, wrappers et entites

## DTO / wrappers dormants ou a clarifier

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`tn.iteam.domain.ApiResponse`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/domain/ApiResponse.java` / `ApiResponse`
- Categorie: Derive de packaging
- Preuves: wrapper HTTP utilise par controllers et clients (`MonitoringController`, `TorchScriptPredictionController`, `ObserviumClientX`) mais place dans `domain`
- References trouvees: flux web et integration
- References non trouvees: aucun sens metier JPA/domain model
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: DEPLACER DE PACKAGE
- Pourquoi: c'est un contrat d'API, pas un aggregate metier
- Action suggeree: deplacer vers `tn.iteam.dto` ou `tn.iteam.web.dto`
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`Permission`, `RoleName`, `Role.permissions`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/enums/Permission.java`, `src/main/java/tn/iteam/enums/RoleName.java`, `src/main/java/tn/iteam/domain/Role.java`
- Categorie: Fonctionnalite dormante
- Preuves: `pom.xml` ne declare pas `spring-boot-starter-security`; aucune `SecurityFilterChain`, `@EnableWebSecurity`, `@PreAuthorize`, `PasswordEncoder`, `UserDetailsService` ou `GrantedAuthority`; seules traces runtime = bootstrap des roles/utilisateurs et relations du ticketing
- References trouvees: `TicketingBootstrapConfiguration`, `RoleRepository`, `UserRepository`, relations `Ticket -> User`
- References non trouvees: aucun enforcement d'autorisation basee sur `Permission`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: le modele peut etre prevu pour une phase future, mais il n'est pas branche a la securite reelle aujourd'hui
- Action suggeree: documenter le statut "modele dormant" ; ne pas supprimer avant arbitrage fonctionnel
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumSummaryServiceImpl` retournant `Map<String, Long>`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/ObserviumSummaryServiceImpl.java` / `getSummary`
- Categorie: Wrapper a semantique qui se chevauche
- Preuves: la reponse est un `Map<String, Long>` derive des DTO unifies au lieu d'un DTO dedie ; le dashboard unifie et le monitoring unifie portent deja des vues similaires
- References trouvees: `ObserviumController`
- References non trouvees: aucune reutilisation transversale
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: contrat faiblement type, endpoint de compatibilite, semantique deja presente dans le monitoring unifie
- Action suggeree: soit supprimer l'endpoint apres migration, soit introduire un DTO explicite si l'endpoint doit vivre
- Verification manuelle necessaire ? (oui/non): oui

## DTO non utilises nulle part
- Aucun DTO entierement orphelin n'a ete trouve sous `tn.iteam.dto`, `tn.iteam.ml.dto` ou `tn.iteam.monitoring.dto`.
- En revanche, plusieurs DTO sont aujourd'hui exposes seulement via des endpoints non consommes par le frontend du depot: `ZabbixProblemDTO`, `ZabbixMetricDTO`, `ZkBioProblemDTO`, `ZkBioAttendanceDTO` sur certains chemins legacy ou specifiques.

## Entites reellement connectees aux workflows actifs
- Actives: `Ticket`, `Intervention`, `User`, `Role`, `ServiceStatus`, `MonitoredHost`, `ZabbixProblem`, `ZabbixMetric`, `ObserviumProblem`, `ObserviumMetric`, `ZkBioProblem`, `ZkBioMetric`
- Partiellement dormantes: la partie `Role.permissions` / `Permission` / `RoleName` n'est pas enforcee runtime
'@

$configMd = @'
# Analyse configuration et support

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`AsyncConfig`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/config/AsyncConfig.java` / `AsyncConfig`
- Categorie: A conserver tel quel
- Preuves: `@Bean(name = "taskExecutor")`, usage indirect par `@Async` sur `MonitoringStartup.warmupInitialSnapshots()` et `ZkBioIntegrationService.refreshAllAndPublish()`
- References trouvees: bean Spring + `@Async`
- References non trouvees: aucune reference statique supplementaire n'est necessaire pour un executor Spring
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: CONSERVER
- Pourquoi: configuration active par framework
- Action suggeree: aucune suppression ; seulement documenter le lien avec `@Async`
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`RedisOptionalConfiguration` + `AppRedisProperties`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/config/RedisOptionalConfiguration.java`, `src/main/java/tn/iteam/config/AppRedisProperties.java`
- Categorie: A conserver tel quel
- Preuves: `@EnableConfigurationProperties(AppRedisProperties.class)`, beans conditionnels, tests dedies `RedisOptionalConfigurationContextTest`, properties explicites dans `application.properties`
- References trouvees: configuration Spring et tests
- References non trouvees: aucune consommation applicative directe requise
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: CONSERVER
- Pourquoi: infrastructure defensive volontaire, utile meme si Redis est desactive par defaut
- Action suggeree: aucune suppression ; seulement eviter de la classer a tort comme code mort
- Verification manuelle necessaire ? (oui/non): non

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`MonitoringSnapshotPublicationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/support/MonitoringSnapshotPublicationService.java`
- Categorie: Service helper qui se chevauche
- Preuves: wrapper mince autour de `MonitoringWebSocketPublisher` et `ZkBioWebSocketPublisher`, utilise par `MonitoringStartup`, `MonitoringController`, `ZabbixScheduler`, `ObserviumScheduler`, `ZkBioScheduler`
- References trouvees: plusieurs points d'entree actifs
- References non trouvees: logique metier propre faible
- Niveau de confiance: moyen
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: utile aujourd'hui comme facade, mais pourrait etre absorbe dans un service de publication plus coherent si la couche integration est simplifiee
- Action suggeree: conserver dans la premiere phase ; revisiter apres factorisation des services d'integration
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`MonitoringStartup`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/MonitoringStartup.java` / `MonitoringStartup`
- Categorie: Derive de packaging
- Preuves: `@EventListener(ApplicationReadyEvent.class)`, orchestration de warmup et publication initiale ; place dans le package racine `tn.iteam` au lieu d'un package bootstrap/startup/config
- References trouvees: activation Spring, dependances `IntegrationServiceRegistry`, `ZkBioRefreshOrchestrationService`, `MonitoringSnapshotPublicationService`
- References non trouvees: aucun motif fonctionnel pour rester au package racine
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: DEPLACER DE PACKAGE
- Pourquoi: bon code, mauvais emplacement
- Action suggeree: deplacer vers `tn.iteam.config.startup` ou `tn.iteam.bootstrap`
- Verification manuelle necessaire ? (oui/non): non
'@

$hotspots = @'
# Hotspots de couplage

## Hotspots les plus graves

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixAdapter`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/adapter/zabbix/ZabbixAdapter.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 769 lignes ; depend de `ZabbixSyncService` + `ZabbixClient`; gere hosts, problems, metrics, batching, fallback history, normalisation et mapping DTO
- References trouvees: appele par `ZabbixIntegrationService`, `ZabbixMetricsServiceImpl`, `ZabbixProblemServiceImpl`
- References non trouvees: aucune vraie decomposition interne par responsabilite
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: cette classe bloque la simplification de la pile Zabbix
- Action suggeree: decouper en `ZabbixHostGateway`, `ZabbixProblemGateway`, `ZabbixMetricGateway` ou equivalent ; conserver `ZabbixClient` comme transport pur
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixClient`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/adapter/zabbix/ZabbixClient.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 643 lignes ; melange construction JSON-RPC, transport WebClient, mapping d'erreurs, timeouts light/heavy, fallback Resilience4j, endpoints multiples Zabbix
- References trouvees: appele par `ZabbixAdapter`, `ZabbixSyncService`, tests resiliency
- References non trouvees: aucune couche de request builder separee
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: forte volatilite technique dans une seule classe
- Action suggeree: extraire un `ZabbixRequestFactory` et un `ZabbixResponseDecoder`, garder le client sur la couche transport
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZkBioIntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/ZkBioIntegrationService.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 333 lignes ; 13 dependances finales ; gere refresh hosts/problems/metrics/attendance/status/devices + publication websocket + fallback snapshots + availability
- References trouvees: `ZkBioController`, `ZkBioScheduler`, `ZkBioRefreshOrchestrationService`
- References non trouvees: aucune separation nette entre orchestration, persistence et publication
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: cas le plus couple hors pile Zabbix
- Action suggeree: extraire au minimum `ZkBioSnapshotRefreshService` et `ZkBioRawDatasetRefreshService`
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`DashboardServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/DashboardServiceImpl.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 260 lignes ; 5 dependances ; combine overview, data quality, prediction ML, anomaly detection, queries repository
- References trouvees: `DashboardController`
- References non trouvees: aucune decomposition prediction/anomaly/data-quality
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: logique metier analytique et logique ML melangees
- Action suggeree: extraire `DashboardPredictionService` et `DashboardAnomalyService` avant tout gros changement fonctionnel
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`TicketServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/TicketServiceImpl.java`
- Categorie: Hotspot de couplage fort
- Preuves: 336 lignes ; gere workflow metier, transitions, persistence, creation d'intervention et emission WebSocket dans la meme classe
- References trouvees: `TicketController`
- References non trouvees: aucune couche dediee pour transitions ou notifications
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: le service reste actif et central, mais il melange orchestration et details bas niveau
- Action suggeree: extraire `TicketWorkflowService` + `TicketNotificationPublisher` ou equivalent minimal
- Verification manuelle necessaire ? (oui/non): oui
'@

$plan = @'
# Plan d'action nettoyage

## PHASE A - Nettoyage sur
- Supprimer les methodes mortes a forte confiance listees dans `cleanup-02-candidats-code-mort.md`.
- Supprimer les packages vides listes dans `cleanup-11-packages-et-sous-packages-vides.md`.
- Supprimer les methodes WebSocket directes devenues inutiles apres bascule snapshot.
- Supprimer les deux methodes de service Zabbix jamais appelees (`synchronizeAndGetPersisted...`).
- Compiler et lancer les tests apres chaque lot logique.

## PHASE B - Fusions / factorisations sures
- Extraire la persistence metrics commune de `ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl`.
- Extraire la base commune de snapshot/fallback des integration services, sans changer les signatures publiques.
- Extraire les helpers communs de mapping monitoring unifie (normalisation, timestamp, builder metric/probleme).

## PHASE C - Nettoyage de packaging / renommage
- Deplacer `ApiResponse` hors du package `domain`.
- Deplacer `MonitoringStartup` vers un package bootstrap/config.
- Deplacer `ZabbixSyncService` pres du module Zabbix.
- Rapatrier `ZkBioServiceImpl` dans `service.impl` et harmoniser la convention de nommage.
- Renommer `ObserviumClientX` et `ZkBioClientX` pour enlever le suffixe `X` et les rapprocher de leurs modules source.

## PHASE D - Refactors de risque moyen
- Decouper `ZabbixAdapter` et `ZabbixClient`.
- Decouper `ZkBioIntegrationService` en orchestration / refresh datasets / publication.
- Decouper `DashboardServiceImpl`.
- Simplifier certaines interfaces a faible valeur (`IntegrationService`, `ObserviumSummaryService`, `CameraInventoryService`) apres stabilisation du packaging.

## PHASE E - Verification manuelle / runtime necessaire
- Mesurer le trafic de `/api/observium/summary`, `/api/zabbix/metrics`, `/api/zabbix/active`, `/predict`, `/api/zkbio/problems`, `/api/zkbio/attendance/range`, `/api/zkbio/users`.
- Valider avec les parties prenantes si le modele `Permission` / `RoleName` doit vivre avant de le nettoyer.
- Verifier qu'aucun consommateur externe ne depend des endpoints legacy avant suppression.
'@

$checklist = @'
# Checklist suppression sure

## PHASE DE NETTOYAGE SUR

### Classes / interfaces / fichiers supprimables immediatement
- Aucune classe Spring/JPA complete n'est supprimable immediatement a forte confiance sans risque de rupture externe.
- Une exception potentielle existe pour `IntegrationService`, mais je la classe plutot en simplification/fusion qu'en suppression brute car elle structure encore `AsyncIntegrationService`.

### Methodes supprimables immediatement
- `IntegrationClientSupport.httpOn(...)`
- `IntegrationClientSupport.timeoutOn(...)`
- `IntegrationClientSupport.invalidJsonOn(...)`
- `IntegrationClientSupport.returnedHttpDuring(...)`
- `IntegrationClientSupport.stableFallbackReason(...)`
- `IntegrationClientSupport.transportErrorOn(...)`
- `IntegrationClientSupport.unexpectedErrorOn(...)`
- `IntegrationClientSupport.duringMessage(...)`
- `IntegrationDataUnavailableException.forZabbix(...)`
- `IntegrationDataUnavailableException.forZkBio(...)`
- `ServiceStatusMapper.toDTO(...)`
- `ZabbixMonitoringMapper.toHost(...)`
- `ZabbixMonitoringMapper.toHostFromServiceStatus(...)`
- `ZabbixMonitoringMapper.toMetricFromDTO(...)`
- `ZabbixMetricsService.synchronizeAndGetPersistedMetricsSnapshot(...)`
- `ZabbixMetricsServiceImpl.synchronizeAndGetPersistedMetricsSnapshot(...)`
- `ZabbixProblemService.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- `ZabbixProblemServiceImpl.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- `MonitoringWebSocketPublisher.publishProblems(List<...>)`
- `MonitoringWebSocketPublisher.publishMetrics(List<...>)`
- `ZkBioWebSocketPublisher.publishProblems(List<...>)`
- `ZkBioWebSocketPublisher.publishAttendance(List<...>)`
- `ZkBioWebSocketPublisher.publishDevices(List<?>)`
- `ZkBioWebSocketPublisher.publishStatus(Object)`
- `ZkBioWebSocketPublisher.publishProblemsFromSnapshot()`

### Packages / sous-packages vides supprimables immediatement
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`

### Duplications fusionnables sans risque immediat
- Extraction de helpers communs non fonctionnels pour les integration services: `safeMessage(...)`, `toException(...)`, `saveFallbackSnapshot(...)`, `safeGetExistingSnapshot(...)`
- Extraction du pipeline commun de persistence metrics entre `ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl`

## PHASE DE NETTOYAGE RISQUE
- `ObserviumController`
- `ZabbixMetricsController`
- `ZabbixProblemController`
- `TorchScriptPredictionController`
- `ZkBioController.getProblems()`
- `ZkBioController.getAttendanceLogsByRange()`
- `ZkBioController.getUsers()`
- `IntegrationService`
- `ObserviumSummaryService` / `ObserviumSummaryServiceImpl`
- `CameraInventoryService` / `CameraInventoryServiceImpl`
- `Permission`, `RoleName`, `Role.permissions`
- `ZabbixAdapter`
- `ZabbixClient`
- `ZkBioIntegrationService`
- `DashboardServiceImpl`
- `TicketServiceImpl`
'@

$empty = @'
# Packages et sous-packages vides

## Packages vides confirmes
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`

## Sous-packages a faible valeur separatrice
- `src/main/java/tn/iteam/client`: seulement `ObserviumClientX` et `ZkBioClientX`, alors que `ZabbixClient` vit ailleurs ; package a harmoniser plutot qu'a conserver tel quel.
- `src/main/java/tn/iteam/service/support`: seulement trois helpers transverses, dont certains relevent plutot de `integration/support`.
- `src/main/java/tn/iteam/ml/controller` et `src/main/java/tn/iteam/ml/service`: un seul fichier chacun ; separation acceptable mais tres fine.
'@

$move = @'
# Fichiers a deplacer ou repackager

- `src/main/java/tn/iteam/domain/ApiResponse.java`
  - Pourquoi: wrapper HTTP, pas modele metier.
  - Cible proposee: `tn.iteam.dto` ou `tn.iteam.web.dto`.

- `src/main/java/tn/iteam/MonitoringStartup.java`
  - Pourquoi: startup hook Spring place au package racine.
  - Cible proposee: `tn.iteam.config.startup` ou `tn.iteam.bootstrap`.

- `src/main/java/tn/iteam/service/ZabbixSyncService.java`
  - Pourquoi: support technique specifique Zabbix, pas service metier generique.
  - Cible proposee: `tn.iteam.adapter.zabbix.support` ou `tn.iteam.integration.zabbix`.

- `src/main/java/tn/iteam/service/ZkBioServiceImpl.java`
  - Pourquoi: implementation concrete isolee dans `service` alors que les autres implementations vivent dans `service.impl`.
  - Cible proposee: `tn.iteam.service.impl`.

- `src/main/java/tn/iteam/client/ObserviumClientX.java`
  - Pourquoi: incoherence de packaging et nommage par rapport a `ZabbixClient`.
  - Cible proposee: `tn.iteam.adapter.observium.ObserviumClient`.

- `src/main/java/tn/iteam/client/ZkBioClientX.java`
  - Pourquoi: meme derive de packaging/nommage.
  - Cible proposee: `tn.iteam.adapter.zkbio.ZkBioClient`.

- `src/main/java/tn/iteam/util/IntegrationClientSupport.java`
  - Pourquoi: helper d'integration, pas utilitaire transverse reel.
  - Cible proposee: `tn.iteam.integration.support`.

- `src/main/java/tn/iteam/util/MonitoringConstants.java`
  - Pourquoi: constantes fortement liees au domaine monitoring.
  - Cible proposee: `tn.iteam.monitoring.support`.
'@

$final = @'
# RAPPORT FINAL NETTOYAGE

## Synthese
Le backend `tn.iteam` est vivant et coherent sur ses grands flux, mais il a accumule trois couches de dette nettes: 1) des endpoints legacy conserves pour compatibilite, 2) une duplication importante entre integrations source par source, 3) une derive de packaging qui melange `domain`, `dto`, `service`, `client`, `adapter`, `support`.

Le nettoyage sur le plus rentable est immediatement disponible sur les methodes mortes sans couplage Spring et sur les packages vides. Les suppressions de controllers legacy, elles, demandent une verification de trafic runtime car le frontend du depot ne les consomme plus, mais des consommateurs externes restent plausibles.

## Totaux analyses
- Classes concretes analysees: 122
- Interfaces analysees: 29
- Enums analyses: 5
- Records analyses: 6
- Methodes analysees statiquement: environ 537

## Meilleurs candidats SUPPRIMER MAINTENANT
- Helpers morts de `IntegrationClientSupport`
- Factories mortes `IntegrationDataUnavailableException.forZabbix(...)` et `forZkBio(...)`
- `ServiceStatusMapper.toDTO(...)`
- `ZabbixMonitoringMapper.toHost(...)`, `toHostFromServiceStatus(...)`, `toMetricFromDTO(...)`
- `ZabbixMetricsService*.synchronizeAndGetPersistedMetricsSnapshot(...)`
- `ZabbixProblemService*.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- Publications WebSocket directes non consommees dans `MonitoringWebSocketPublisher` et `ZkBioWebSocketPublisher`
- Packages vides `cache`, `listener`, `logging`, `monitoring/provider`

## Meilleurs candidats FUSIONNER
- Base commune des integration services source par source
- Persistence `Observium` / `ZKBio`
- Mappers de monitoring unifie
- `ZabbixSyncService` avec `MonitoredHostPersistenceServiceImpl`
- Construction du statut serveur ZKBio partagee entre `ZkBioServiceImpl` et `ZkBioAdapter`

## Packages / sous-packages vides
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`

## Fichiers a deplacer
- `domain/ApiResponse.java`
- `MonitoringStartup.java`
- `service/ZabbixSyncService.java`
- `service/ZkBioServiceImpl.java`
- `client/ObserviumClientX.java`
- `client/ZkBioClientX.java`
- `util/IntegrationClientSupport.java`
- `util/MonitoringConstants.java`

## Hotspots de couplage les plus graves
- `ZabbixAdapter`
- `ZabbixClient`
- `ZkBioIntegrationService`
- `DashboardServiceImpl`
- `TicketServiceImpl`

## Plus grandes opportunites de simplification d'architecture
- Uniformiser toutes les integrations sur le meme socle snapshot/fallback/publication.
- Uniformiser la persistence d'inventaire d'hotes entre Zabbix et les autres sources.
- Clarifier les frontieres `adapter/client/integration/service/support`.
- Faire sortir les wrappers HTTP du package `domain`.
- Supprimer les endpoints legacy apres validation de trafic externe.

## Fichiers generes
- `docs/mefied/cleanup-00-resume-executif.md`
- `docs/mefied/cleanup-01-cartographie-complete-des-usages.md`
- `docs/mefied/cleanup-02-candidats-code-mort.md`
- `docs/mefied/cleanup-03-candidats-duplication-et-fusion.md`
- `docs/mefied/cleanup-04-analyse-interfaces-implementations.md`
- `docs/mefied/cleanup-05-analyse-endpoints-et-workflows.md`
- `docs/mefied/cleanup-06-analyse-dto-wrappers-entites.md`
- `docs/mefied/cleanup-07-analyse-configuration-et-support.md`
- `docs/mefied/cleanup-08-hotspots-de-couplage.md`
- `docs/mefied/cleanup-09-plan-action-nettoyage.md`
- `docs/mefied/cleanup-10-checklist-suppression-sure.md`
- `docs/mefied/cleanup-11-packages-et-sous-packages-vides.md`
- `docs/mefied/cleanup-12-fichiers-a-deplacer-ou-repackager.md`
- `docs/mefied/RAPPORT_FINAL_NETTOYAGE.md`
'@

Set-Content "frontend/.codex-audit/reports/cleanup-00-resume-executif.md" $summary

$cartoLines = New-Object System.Collections.Generic.List[string]
$cartoLines.Add($cartoHeader) | Out-Null
foreach ($row in $rows) {
    $cartoLines.Add("| $($row.Nom) | $($row.Type) | $($row.Package) | `"$($row.Fichier)`" | $($row.RefMain) | $($row.RefTest) | $($row.Activation) | $($row.Note) |") | Out-Null
}
Set-Content "frontend/.codex-audit/reports/cleanup-01-cartographie-complete-des-usages.md" $cartoLines

Set-Content "frontend/.codex-audit/reports/cleanup-02-candidats-code-mort.md" $dead
Set-Content "frontend/.codex-audit/reports/cleanup-03-candidats-duplication-et-fusion.md" $dup
Set-Content "frontend/.codex-audit/reports/cleanup-04-analyse-interfaces-implementations.md" $interfacesMd
Set-Content "frontend/.codex-audit/reports/cleanup-05-analyse-endpoints-et-workflows.md" $endpoints
Set-Content "frontend/.codex-audit/reports/cleanup-06-analyse-dto-wrappers-entites.md" $dto
Set-Content "frontend/.codex-audit/reports/cleanup-07-analyse-configuration-et-support.md" $configMd
Set-Content "frontend/.codex-audit/reports/cleanup-08-hotspots-de-couplage.md" $hotspots
Set-Content "frontend/.codex-audit/reports/cleanup-09-plan-action-nettoyage.md" $plan
Set-Content "frontend/.codex-audit/reports/cleanup-10-checklist-suppression-sure.md" $checklist
Set-Content "frontend/.codex-audit/reports/cleanup-11-packages-et-sous-packages-vides.md" $empty
Set-Content "frontend/.codex-audit/reports/cleanup-12-fichiers-a-deplacer-ou-repackager.md" $move
Set-Content "frontend/.codex-audit/reports/RAPPORT_FINAL_NETTOYAGE.md" $final
