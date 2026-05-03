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
