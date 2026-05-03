# Inventaire Reel Backend et Frontend

## Dependances principales
### Backend Maven
- Spring Boot `3.2.5`
- Java `17`
- starters principaux: validation, jdbc, web, webflux, data-jpa, websocket, cache, redis, actuator, aop
- resilience: `resilience4j-spring-boot3` `2.2.0`, `resilience4j-micrometer`
- observabilite: `micrometer-registry-prometheus`
- ML: `ai.djl:api` `0.30.0`, `ai.djl.pytorch:pytorch-engine` `0.30.0`
- outillage: Lombok, SpringDoc OpenAPI, MySQL

### Frontend Angular
- Angular `20.3.x`
- `rxjs` `7.8.x`
- `@stomp/stompjs` `7.0.0`
- `sockjs-client` `1.6.1`
- TypeScript `5.9.2`

## Inventaire backend `src/main/java/tn/iteam`
### Totaux verifies
- `171` fichiers Java
- `130` classes
- `30` interfaces
- `5` enums
- `6` records
- `557` methodes detectees statiquement

### Packages et fichiers
#### Racine
- `PfeprojectApplication.java`
- `MonitoringStartup.java`

#### `adapter.camera`
- `CameraAdapter.java`

#### `adapter.observium`
- `ObserviumAdapter.java`

#### `adapter.zabbix`
- `ZabbixAdapter.java`
- `ZabbixClient.java`
- `ZabbixHostCollector.java`
- `ZabbixMetricsCollector.java`
- `ZabbixProblemCollector.java`

#### `adapter.zkbio`
- `ZkBioAdapter.java`

#### `client`
- `ObserviumClientX.java`
- `ZkBioClientX.java`

#### `config`
- `AppRedisProperties.java`
- `AsyncConfig.java`
- `CorsConfig.java`
- `JpaAuditingConfig.java`
- `RedisOptionalConfiguration.java`
- `ResilienceLoggingConfig.java`
- `TicketingBootstrapConfiguration.java`
- `WebClientConfig.java`
- `WebSocketConfig.java`

#### `controller`
- `CameraController.java`
- `DashboardController.java`
- `MonitoringController.java`
- `ObserviumController.java`
- `TicketController.java`
- `ZabbixMetricsController.java`
- `ZabbixProblemController.java`
- `ZkBioController.java`

#### `domain`
- `ApiResponse.java`
- `BaseEntity.java`
- `Intervention.java`
- `MonitoredHost.java`
- `ObserviumMetric.java`
- `ObserviumProblem.java`
- `Role.java`
- `ServiceStatus.java`
- `Ticket.java`
- `User.java`
- `ZabbixMetric.java`
- `ZabbixProblem.java`
- `ZkBioMetric.java`
- `ZkBioProblem.java`

#### `dto`
- `ApiErrorResponse.java`
- `CameraDeviceDTO.java`
- `DashboardAnomalyDTO.java`
- `DashboardOverviewDTO.java`
- `DashboardPredictionDTO.java`
- `ObserviumMetricDTO.java`
- `ObserviumProblemDTO.java`
- `ServiceStatusDTO.java`
- `SourceAvailabilityDTO.java`
- `TicketAssignmentRequestDTO.java`
- `TicketCreateRequestDTO.java`
- `TicketDecisionRequestDTO.java`
- `TicketInterventionDTO.java`
- `TicketInterventionRequestDTO.java`
- `TicketResponseDTO.java`
- `TicketStatusUpdateRequestDTO.java`
- `TicketUserDTO.java`
- `ZabbixMetricDTO.java`
- `ZabbixProblemDTO.java`
- `ZkBioAttendanceDTO.java`
- `ZkBioMetricDTO.java`
- `ZkBioProblemDTO.java`

#### `enums`
- `Permission.java`
- `Priority.java`
- `RoleName.java`
- `TicketStatus.java`

#### `exception`
- `GlobalExceptionHandler.java`
- `IntegrationDataUnavailableException.java`
- `IntegrationException.java`
- `IntegrationResponseException.java`
- `IntegrationTimeoutException.java`
- `IntegrationUnavailableException.java`
- `TicketingException.java`

#### `integration`
- `AsyncIntegrationService.java`
- `CameraIntegrationService.java`
- `IntegrationService.java`
- `IntegrationServiceRegistry.java`
- `ObserviumIntegrationService.java`
- `ZabbixIntegrationService.java`
- `ZkBioIntegrationOperations.java`
- `ZkBioIntegrationService.java`
- `ZkBioRefreshOrchestrationService.java`

#### `mapper`
- `CategoryResolver.java`
- `ObserviumMapper.java`
- `ObserviumMetricMapper.java`
- `ObserviumMonitoringMapper.java`
- `ServiceStatusMapper.java`
- `TicketMapper.java`
- `ZabbixMetricMapper.java`
- `ZabbixMonitoringMapper.java`
- `ZabbixProblemMapper.java`
- `ZkBioAttendanceMapper.java`
- `ZkBioMapper.java`
- `ZkBioMetricMapper.java`
- `ZkBioMonitoringMapper.java`

#### `ml.config`
- `MlTorchScriptConfig.java`
- `MlTorchScriptProperties.java`

#### `ml.controller`
- `TorchScriptPredictionController.java`

#### `ml.dto`
- `TorchScriptPredictionRequest.java`
- `TorchScriptPredictionResponse.java`

#### `ml.service`
- `TorchScriptPredictionService.java`

#### `monitoring`
- `MonitoringSourceType.java`

#### `monitoring.dto`
- `UnifiedMonitoringHostDTO.java`
- `UnifiedMonitoringMetricDTO.java`
- `UnifiedMonitoringProblemDTO.java`
- `UnifiedMonitoringResponse.java`

#### `monitoring.service`
- `MonitoringAggregationService.java`
- `MonitoringCacheService.java`

#### `monitoring.snapshot`
- `InMemorySnapshotStore.java`
- `SnapshotStore.java`
- `StoredSnapshot.java`

#### `repository`
- `InterventionRepository.java`
- `MonitoredHostRepository.java`
- `ObserviumMetricRepository.java`
- `ObserviumProblemRepository.java`
- `RoleRepository.java`
- `ServiceStatusRepository.java`
- `TicketRepository.java`
- `UserRepository.java`
- `ZabbixMetricRepository.java`
- `ZabbixProblemRepository.java`
- `ZkBioMetricRepository.java`
- `ZkBioProblemRepository.java`

#### `scheduler`
- `ObserviumHostsScheduler.java`
- `ObserviumScheduler.java`
- `ZabbixScheduler.java`
- `ZkBioScheduler.java`

#### `service`
- `CameraInventoryService.java`
- `DashboardService.java`
- `MonitoredHostPersistenceService.java`
- `MonitoredHostSnapshotService.java`
- `ObserviumPersistenceService.java`
- `ObserviumSummaryService.java`
- `ServiceStatusPersistenceService.java`
- `SourceAvailabilityService.java`
- `TicketService.java`
- `ZabbixDataQualityService.java`
- `ZabbixHostSyncService.java`
- `ZabbixMetricsService.java`
- `ZabbixProblemService.java`
- `ZabbixSyncService.java`
- `ZkBioPersistenceService.java`
- `ZkBioServiceImpl.java`
- `ZkBioServiceInterface.java`

#### `service.impl`
- `CameraInventoryServiceImpl.java`
- `DashboardServiceImpl.java`
- `InMemoryCameraInventoryService.java`
- `InMemoryDashboardService.java`
- `InMemoryMonitoredHostPersistenceService.java`
- `InMemoryZabbixHostSyncService.java`
- `MonitoredHostPersistenceServiceImpl.java`
- `MonitoredHostSnapshotServiceImpl.java`
- `ObserviumPersistenceServiceImpl.java`
- `ObserviumSummaryServiceImpl.java`
- `ServiceStatusPersistenceServiceImpl.java`
- `SourceAvailabilityServiceImpl.java`
- `TicketServiceImpl.java`
- `ZabbixMetricsServiceImpl.java`
- `ZabbixProblemServiceImpl.java`
- `ZkBioPersistenceServiceImpl.java`

#### `service.support`
- `DatabaseAvailabilityService.java`
- `IntegrationExecutionHelper.java`
- `MonitoringSnapshotPublicationService.java`
- `ZabbixProblemSanitizer.java`

#### `util`
- `IntegrationClientSupport.java`
- `MonitoringConstants.java`

#### `websocket`
- `MonitoringWebSocketPublisher.java`
- `ZkBioWebSocketPublisher.java`

### Classes backend les plus chargees en methodes
- `ZabbixClient`: `39`
- `ZkBioIntegrationService`: `26`
- `TicketServiceImpl`: `21`
- `ZkBioClientX`: `20`
- `ObserviumIntegrationService`: `19`
- `SourceAvailabilityServiceImpl`: `19`
- `ZabbixIntegrationService`: `19`
- `AppRedisProperties`: `18`
- `CameraIntegrationService`: `16`

## Inventaire frontend `frontend/src/app`
### Totaux verifies
- `62` fichiers TypeScript applicatifs
- `28` classes
- `27` interfaces
- `1` type exporte
- `186` methodes detectees statiquement

### Repertoires et fichiers
#### `app`
- `app.config.ts`
- `app.html`
- `app.routes.ts`
- `app.scss`
- `app.spec.ts`
- `app.ts`

#### `core/auth`
- `auth-context.port.ts`
- `noop-auth-context.service.ts`

#### `core/config`
- `app-config.token.ts`

#### `core/http`
- `auth-header.interceptor.ts`
- `http-error.utils.ts`

#### `core/models`
- `api-error.model.ts`
- `api-response.model.ts`
- `camera-device.model.ts`
- `collection-target.model.ts`
- `dashboard-anomaly.model.ts`
- `dashboard-overview.model.ts`
- `dashboard-prediction.model.ts`
- `monitoring-host.model.ts`
- `monitoring-problem.model.ts`
- `observium-metric.model.ts`
- `page-response.model.ts`
- `service-status.model.ts`
- `source-availability.model.ts`
- `ticket.model.ts`
- `ticket-intervention.model.ts`
- `ticket-user.model.ts`
- `unified-monitoring-metric.model.ts`
- `unified-monitoring-response.model.ts`
- `zabbix-metric.model.ts`
- `zabbix-problem.model.ts`
- `zkbio-attendance.model.ts`
- `zkbio-metric.model.ts`
- `zkbio-problem.model.ts`

#### `core/realtime`
- `realtime-connection.store.ts`
- `stomp-client.service.ts`

#### `features/monitoring/data`
- `monitoring-api.service.ts`
- `monitoring-realtime.service.ts`
- `monitoring-source.utils.ts`

#### `features/monitoring/state`
- `global-monitoring.models.ts`
- `monitoring.store.ts`
- `zabbix-workspace.models.ts`
- `zabbix-workspace.store.ts`

#### `features/monitoring/ui`
- `monitoring-camera-page.component.ts`
- `monitoring-dashboard-page.component.ts`
- `monitoring-observium-page.component.ts`
- `monitoring-zabbix-page.component.ts`
- `monitoring-zkbio-page.component.ts`
- leurs fichiers `.html`
- leurs fichiers `.scss`

#### `features/tickets/data`
- `ticket-manager-api.service.ts`

#### `features/tickets/ui`
- `ticket-add-page.component.ts`
- `ticket-list-page.component.ts`
- `ticket-tracking-page.component.ts`
- leurs fichiers `.html`
- leurs fichiers `.scss`

#### `layout`
- `navbar`
- `shell`
- `sidebar`
- `user-panel`

#### `shared/pages`
- `placeholder-page.component.ts`
- `placeholder-page.component.html`
- `placeholder-page.component.scss`

#### `shared/ui`
- `alert-summary-panel`
- `asset-inventory-table`
- `collection-control-bar`
- `data-coverage-notice`
- `global-kpi-strip`
- `source-health-panel`

### Classes frontend les plus chargees en methodes
- `ZabbixWorkspaceStore`: `65`
- `MonitoringStore`: `48`
- `MonitoringObserviumPageComponent`: `17`
- `MonitoringZabbixPageComponent`: `14`
- `MonitoringZkBioPageComponent`: `11`
- `TicketTrackingPageComponent`: `7`

## Routes frontend utilisees
- `/dashboard`
- `/monitoring/zabbix`
- `/monitoring/observium`
- `/monitoring/camera`
- `/monitoring/zkbio`
- `/monitoring/access-point`
- `/equipment`
- `/tickets/list`
- `/tickets/add`
- `/tickets/tracking`
- `/users`

## Endpoints backend consommes par le frontend
### Monitoring
- `GET /api/monitoring/hosts`
- `GET /api/monitoring/problems`
- `GET /api/monitoring/metrics`
- `GET /api/monitoring/sources/health`
- `POST /api/monitoring/collect`
- `POST /api/monitoring/collect/zabbix`
- `POST /api/monitoring/collect/observium`
- `POST /api/monitoring/collect/camera`

### Sources specifiques
- `GET /api/cameras`
- `GET /api/zkbio/status`
- `GET /api/zkbio/devices`
- `GET /api/zkbio/attendance`
- `POST /api/zkbio/collect`

### Dashboard et ticketing
- `GET /dashboard/overview`
- `GET /dashboard/predictions`
- `GET /dashboard/anomalies`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `GET /api/tickets/users`
- `POST /api/tickets`
- `PUT /api/tickets/{id}/assign`
- `PUT /api/tickets/{id}/status`
- `POST /api/tickets/{id}/interventions`
- `PUT /api/tickets/{id}/validate`
- `PUT /api/tickets/{id}/reject`

## Verification d'execution
### Backend
- `./mvnw.cmd -q test` lance bien la suite
- echec actuel: `2` erreurs Mockito dans `IntegrationServicesWithoutRedisTest`
- nature de l'echec: `UnnecessaryStubbingException`
- conclusion: le backend est compilable/testable, mais la suite n'est pas totalement verte a l'etat actuel

### Frontend
- `npm run build`: succes
- warnings:
  - budget initial depasse: `587.66 kB` pour un budget `500 kB`
  - budgets SCSS depasses sur `monitoring-observium-page`, `monitoring-zabbix-page`, `monitoring-zkbio-page`
  - dependances CommonJS: `@stomp/stompjs` et `sockjs-client`
