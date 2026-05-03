# Resume executif

## Perimetre reel analyse
- Backend principal: `src/main/java/tn/iteam`
- Support croise: `src/test/java/tn/iteam`, `src/main/resources/application.properties`, `pom.xml`, appels du frontend local `frontend/src`
- Verification Spring prise en compte: `@RestController`, `@Service`, `@Component`, `@Repository`, `@Configuration`, `@Bean`, `@Entity`, `@Scheduled`, `@EventListener`, `CommandLineRunner`, WebSocket STOMP, Resilience4j fallback signatures, wiring via interfaces

## Chiffres globaux
- Classes concretes analysees: 130
- Interfaces analysees: 30
- Enums analyses: 5
- Records analyses: 6
- Methodes analysees statiquement: 557
- Packages / sous-packages vides detectes: 4

## Verification complementaire au 2026-04-28
- Frontend Angular verifie en plus du backend: 62 fichiers TypeScript applicatifs, 28 classes, 27 interfaces, 186 methodes detectees statiquement.
- `npm run build` reussit avec warnings de budget et dependances CommonJS.
- `./mvnw.cmd -q test` execute la suite mais echoue actuellement sur 2 erreurs Mockito `UnnecessaryStubbingException`.
- Voir `docs/mefied/verif` pour l'inventaire reel, les duplications confirmees et les ecarts documentaires corriges.

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
