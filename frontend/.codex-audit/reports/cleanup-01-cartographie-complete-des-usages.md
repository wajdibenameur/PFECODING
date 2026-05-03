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
| MonitoringStartup | class | tn.iteam | "src/main/java/tn/iteam/MonitoringStartup.java" | 6 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| PfeprojectApplication | class | tn.iteam | "src/main/java/tn/iteam/PfeprojectApplication.java" | 2 | 0 | POJO/Support | Reference statique detectee |
| CameraAdapter | class | tn.iteam.adapter.camera | "src/main/java/tn/iteam/adapter/camera/CameraAdapter.java" | 5 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumAdapter | class | tn.iteam.adapter.observium | "src/main/java/tn/iteam/adapter/observium/ObserviumAdapter.java" | 3 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixAdapter | class | tn.iteam.adapter.zabbix | "src/main/java/tn/iteam/adapter/zabbix/ZabbixAdapter.java" | 8 | 5 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixClient | class | tn.iteam.adapter.zabbix | "src/main/java/tn/iteam/adapter/zabbix/ZabbixClient.java" | 6 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioAdapter | class | tn.iteam.adapter.zkbio | "src/main/java/tn/iteam/adapter/zkbio/ZkBioAdapter.java" | 5 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumClientX | class | tn.iteam.client | "src/main/java/tn/iteam/client/ObserviumClientX.java" | 4 | 3 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioClientX | class | tn.iteam.client | "src/main/java/tn/iteam/client/ZkBioClientX.java" | 6 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| AppRedisProperties | class | tn.iteam.config | "src/main/java/tn/iteam/config/AppRedisProperties.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| AsyncConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/AsyncConfig.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| CorsConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/CorsConfig.java" | 2 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| JpaAuditingConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/JpaAuditingConfig.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| RedisOptionalConfiguration | class | tn.iteam.config | "src/main/java/tn/iteam/config/RedisOptionalConfiguration.java" | 1 | 1 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ResilienceLoggingConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/ResilienceLoggingConfig.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| TicketingBootstrapConfiguration | class | tn.iteam.config | "src/main/java/tn/iteam/config/TicketingBootstrapConfiguration.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| WebClientConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/WebClientConfig.java" | 2 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| WebSocketConfig | class | tn.iteam.config | "src/main/java/tn/iteam/config/WebSocketConfig.java" | 2 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| CameraController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/CameraController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| DashboardController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/DashboardController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| MonitoringController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/MonitoringController.java" | 1 | 4 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| ObserviumController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/ObserviumController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| TicketController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/TicketController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMetricsController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/ZabbixMetricsController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| ZabbixProblemController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/ZabbixProblemController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| ZkBioController | class | tn.iteam.controller | "src/main/java/tn/iteam/controller/ZkBioController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| BaseEntity | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/BaseEntity.java" | 13 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| Intervention | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/Intervention.java" | 13 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| MonitoredHost | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/MonitoredHost.java" | 29 | 2 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ObserviumMetric | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ObserviumMetric.java" | 11 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ObserviumProblem | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ObserviumProblem.java" | 16 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| Role | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/Role.java" | 13 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ServiceStatus | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ServiceStatus.java" | 21 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| Ticket | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/Ticket.java" | 57 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| User | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/User.java" | 28 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ZabbixMetric | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ZabbixMetric.java" | 57 | 7 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ZabbixProblem | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ZabbixProblem.java" | 30 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ZkBioMetric | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ZkBioMetric.java" | 11 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ZkBioProblem | class | tn.iteam.domain | "src/main/java/tn/iteam/domain/ZkBioProblem.java" | 14 | 0 | Entite JPA | Activation indirecte par Spring/JPA possible |
| ApiErrorResponse | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ApiErrorResponse.java" | 7 | 0 | DTO/Wrapper | Reference statique detectee |
| CameraDeviceDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/CameraDeviceDTO.java" | 8 | 0 | DTO/Wrapper | Reference statique detectee |
| ObserviumMetricDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ObserviumMetricDTO.java" | 17 | 3 | DTO/Wrapper | Reference statique detectee |
| ObserviumProblemDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ObserviumProblemDTO.java" | 20 | 3 | DTO/Wrapper | Reference statique detectee |
| ServiceStatusDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ServiceStatusDTO.java" | 79 | 3 | DTO/Wrapper | Reference statique detectee |
| SourceAvailabilityDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/SourceAvailabilityDTO.java" | 13 | 3 | DTO/Wrapper | Reference statique detectee |
| TicketAssignmentRequestDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketAssignmentRequestDTO.java" | 3 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketCreateRequestDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketCreateRequestDTO.java" | 3 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketDecisionRequestDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketDecisionRequestDTO.java" | 4 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketInterventionDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketInterventionDTO.java" | 5 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketInterventionRequestDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketInterventionRequestDTO.java" | 3 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketResponseDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketResponseDTO.java" | 16 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketStatusUpdateRequestDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketStatusUpdateRequestDTO.java" | 3 | 0 | DTO/Wrapper | Reference statique detectee |
| TicketUserDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/TicketUserDTO.java" | 10 | 0 | DTO/Wrapper | Reference statique detectee |
| ZabbixMetricDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ZabbixMetricDTO.java" | 34 | 3 | DTO/Wrapper | Reference statique detectee |
| ZabbixProblemDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ZabbixProblemDTO.java" | 46 | 3 | DTO/Wrapper | Reference statique detectee |
| ZkBioAttendanceDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ZkBioAttendanceDTO.java" | 24 | 2 | DTO/Wrapper | Reference statique detectee |
| ZkBioMetricDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ZkBioMetricDTO.java" | 18 | 3 | DTO/Wrapper | Reference statique detectee |
| ZkBioProblemDTO | class | tn.iteam.dto | "src/main/java/tn/iteam/dto/ZkBioProblemDTO.java" | 26 | 3 | DTO/Wrapper | Reference statique detectee |
| Permission | enum | tn.iteam.enums | "src/main/java/tn/iteam/enums/Permission.java" | 24 | 0 | POJO/Support | Reference statique detectee |
| Priority | enum | tn.iteam.enums | "src/main/java/tn/iteam/enums/Priority.java" | 15 | 0 | POJO/Support | Reference statique detectee |
| RoleName | enum | tn.iteam.enums | "src/main/java/tn/iteam/enums/RoleName.java" | 11 | 0 | POJO/Support | Reference statique detectee |
| TicketStatus | enum | tn.iteam.enums | "src/main/java/tn/iteam/enums/TicketStatus.java" | 33 | 0 | POJO/Support | Reference statique detectee |
| GlobalExceptionHandler | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/GlobalExceptionHandler.java" | 2 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| IntegrationDataUnavailableException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/IntegrationDataUnavailableException.java" | 11 | 0 | POJO/Support | Reference statique detectee |
| IntegrationException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/IntegrationException.java" | 17 | 0 | POJO/Support | Reference statique detectee |
| IntegrationResponseException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/IntegrationResponseException.java" | 28 | 0 | POJO/Support | Reference statique detectee |
| IntegrationTimeoutException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/IntegrationTimeoutException.java" | 16 | 6 | POJO/Support | Reference statique detectee |
| IntegrationUnavailableException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/IntegrationUnavailableException.java" | 31 | 4 | POJO/Support | Reference statique detectee |
| TicketingException | class | tn.iteam.exception | "src/main/java/tn/iteam/exception/TicketingException.java" | 8 | 0 | POJO/Support | Reference statique detectee |
| AsyncIntegrationService | interface | tn.iteam.integration | "src/main/java/tn/iteam/integration/AsyncIntegrationService.java" | 11 | 6 | Abstraction | Reference statique detectee |
| CameraIntegrationService | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/CameraIntegrationService.java" | 1 | 1 | Bean Spring | Activation indirecte par Spring/JPA possible |
| IntegrationService | interface | tn.iteam.integration | "src/main/java/tn/iteam/integration/IntegrationService.java" | 2 | 0 | Abstraction | Reference statique detectee |
| IntegrationServiceRegistry | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/IntegrationServiceRegistry.java" | 13 | 4 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumIntegrationService | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/ObserviumIntegrationService.java" | 1 | 1 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixIntegrationService | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/ZabbixIntegrationService.java" | 2 | 1 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioIntegrationOperations | interface | tn.iteam.integration | "src/main/java/tn/iteam/integration/ZkBioIntegrationOperations.java" | 8 | 3 | Abstraction | Reference statique detectee |
| ZkBioIntegrationService | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/ZkBioIntegrationService.java" | 2 | 1 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioRefreshOrchestrationService | class | tn.iteam.integration | "src/main/java/tn/iteam/integration/ZkBioRefreshOrchestrationService.java" | 7 | 6 | Bean Spring | Activation indirecte par Spring/JPA possible |
| CategoryResolver | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/CategoryResolver.java" | 4 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ObserviumMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumMetricMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ObserviumMetricMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumMonitoringMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ObserviumMonitoringMapper.java" | 3 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ServiceStatusMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ServiceStatusMapper.java" | 4 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| TicketMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/TicketMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMetricMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZabbixMetricMapper.java" | 3 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMonitoringMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZabbixMonitoringMapper.java" | 3 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixProblemMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZabbixProblemMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioAttendanceMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZkBioAttendanceMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZkBioMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioMetricMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZkBioMetricMapper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioMonitoringMapper | class | tn.iteam.mapper | "src/main/java/tn/iteam/mapper/ZkBioMonitoringMapper.java" | 3 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MlTorchScriptConfig | class | tn.iteam.ml.config | "src/main/java/tn/iteam/ml/config/MlTorchScriptConfig.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| TorchScriptPredictionController | class | tn.iteam.ml.controller | "src/main/java/tn/iteam/ml/controller/TorchScriptPredictionController.java" | 1 | 0 | Endpoint Spring | Activation indirecte par Spring/JPA possible |
| TorchScriptPredictionRequest | class | tn.iteam.ml.dto | "src/main/java/tn/iteam/ml/dto/TorchScriptPredictionRequest.java" | 3 | 0 | DTO/Wrapper | Reference statique detectee |
| TorchScriptPredictionService | class | tn.iteam.ml.service | "src/main/java/tn/iteam/ml/service/TorchScriptPredictionService.java" | 5 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MonitoringSourceType | enum | tn.iteam.monitoring | "src/main/java/tn/iteam/monitoring/MonitoringSourceType.java" | 128 | 45 | POJO/Support | Reference statique detectee |
| UnifiedMonitoringHostDTO | class | tn.iteam.monitoring.dto | "src/main/java/tn/iteam/monitoring/dto/UnifiedMonitoringHostDTO.java" | 37 | 5 | DTO/Wrapper | Reference statique detectee |
| UnifiedMonitoringMetricDTO | class | tn.iteam.monitoring.dto | "src/main/java/tn/iteam/monitoring/dto/UnifiedMonitoringMetricDTO.java" | 26 | 11 | DTO/Wrapper | Reference statique detectee |
| UnifiedMonitoringProblemDTO | class | tn.iteam.monitoring.dto | "src/main/java/tn/iteam/monitoring/dto/UnifiedMonitoringProblemDTO.java" | 28 | 18 | DTO/Wrapper | Reference statique detectee |
| MonitoringAggregationService | class | tn.iteam.monitoring.service | "src/main/java/tn/iteam/monitoring/service/MonitoringAggregationService.java" | 10 | 6 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MonitoringCacheService | class | tn.iteam.monitoring.service | "src/main/java/tn/iteam/monitoring/service/MonitoringCacheService.java" | 8 | 13 | Bean Spring | Activation indirecte par Spring/JPA possible |
| InMemorySnapshotStore | class | tn.iteam.monitoring.snapshot | "src/main/java/tn/iteam/monitoring/snapshot/InMemorySnapshotStore.java" | 1 | 15 | Bean Spring | Activation indirecte par Spring/JPA possible |
| SnapshotStore | interface | tn.iteam.monitoring.snapshot | "src/main/java/tn/iteam/monitoring/snapshot/SnapshotStore.java" | 23 | 8 | Abstraction | Reference statique detectee |
| InterventionRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/InterventionRepository.java" | 3 | 0 | Abstraction | Reference statique detectee |
| MonitoredHostRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/MonitoredHostRepository.java" | 9 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumMetricRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ObserviumMetricRepository.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ObserviumProblemRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ObserviumProblemRepository.java" | 5 | 2 | Abstraction | Reference statique detectee |
| RoleRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/RoleRepository.java" | 4 | 0 | Abstraction | Reference statique detectee |
| ServiceStatusRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ServiceStatusRepository.java" | 7 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| TicketRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/TicketRepository.java" | 3 | 0 | Abstraction | Reference statique detectee |
| UserRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/UserRepository.java" | 6 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMetricRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ZabbixMetricRepository.java" | 7 | 2 | Abstraction | Reference statique detectee |
| ZabbixProblemRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ZabbixProblemRepository.java" | 7 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioMetricRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ZkBioMetricRepository.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ZkBioProblemRepository | interface | tn.iteam.repository | "src/main/java/tn/iteam/repository/ZkBioProblemRepository.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ObserviumHostsScheduler | class | tn.iteam.scheduler | "src/main/java/tn/iteam/scheduler/ObserviumHostsScheduler.java" | 2 | 3 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumScheduler | class | tn.iteam.scheduler | "src/main/java/tn/iteam/scheduler/ObserviumScheduler.java" | 1 | 3 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixScheduler | class | tn.iteam.scheduler | "src/main/java/tn/iteam/scheduler/ZabbixScheduler.java" | 2 | 3 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioScheduler | class | tn.iteam.scheduler | "src/main/java/tn/iteam/scheduler/ZkBioScheduler.java" | 2 | 3 | Bean Spring | Activation indirecte par Spring/JPA possible |
| CameraInventoryService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/CameraInventoryService.java" | 5 | 0 | Abstraction | Reference statique detectee |
| DashboardService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/DashboardService.java" | 5 | 0 | Abstraction | Reference statique detectee |
| MonitoredHostPersistenceService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/MonitoredHostPersistenceService.java" | 7 | 2 | Abstraction | Reference statique detectee |
| MonitoredHostSnapshotService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/MonitoredHostSnapshotService.java" | 9 | 2 | Abstraction | Reference statique detectee |
| ObserviumPersistenceService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ObserviumPersistenceService.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ObserviumSummaryService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ObserviumSummaryService.java" | 5 | 0 | Abstraction | Reference statique detectee |
| ServiceStatusPersistenceService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ServiceStatusPersistenceService.java" | 11 | 2 | Abstraction | Reference statique detectee |
| SourceAvailabilityService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/SourceAvailabilityService.java" | 29 | 8 | Abstraction | Reference statique detectee |
| TicketService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/TicketService.java" | 5 | 0 | Abstraction | Reference statique detectee |
| ZabbixDataQualityService | class | tn.iteam.service | "src/main/java/tn/iteam/service/ZabbixDataQualityService.java" | 8 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMetricsService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ZabbixMetricsService.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ZabbixProblemService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ZabbixProblemService.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ZabbixSyncService | class | tn.iteam.service | "src/main/java/tn/iteam/service/ZabbixSyncService.java" | 6 | 4 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioPersistenceService | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ZkBioPersistenceService.java" | 5 | 2 | Abstraction | Reference statique detectee |
| ZkBioServiceImpl | class | tn.iteam.service | "src/main/java/tn/iteam/service/ZkBioServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioServiceInterface | interface | tn.iteam.service | "src/main/java/tn/iteam/service/ZkBioServiceInterface.java" | 6 | 2 | Abstraction | Reference statique detectee |
| CameraInventoryServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/CameraInventoryServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| DashboardServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/DashboardServiceImpl.java" | 2 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MonitoredHostPersistenceServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/MonitoredHostPersistenceServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MonitoredHostSnapshotServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/MonitoredHostSnapshotServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumPersistenceServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ObserviumPersistenceServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ObserviumSummaryServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ObserviumSummaryServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ServiceStatusPersistenceServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ServiceStatusPersistenceServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| SourceAvailabilityServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/SourceAvailabilityServiceImpl.java" | 2 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| TicketServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/TicketServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixMetricsServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ZabbixMetricsServiceImpl.java" | 2 | 2 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixProblemServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ZabbixProblemServiceImpl.java" | 2 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioPersistenceServiceImpl | class | tn.iteam.service.impl | "src/main/java/tn/iteam/service/impl/ZkBioPersistenceServiceImpl.java" | 1 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| IntegrationExecutionHelper | class | tn.iteam.service.support | "src/main/java/tn/iteam/service/support/IntegrationExecutionHelper.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| MonitoringSnapshotPublicationService | class | tn.iteam.service.support | "src/main/java/tn/iteam/service/support/MonitoringSnapshotPublicationService.java" | 13 | 4 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZabbixProblemSanitizer | class | tn.iteam.service.support | "src/main/java/tn/iteam/service/support/ZabbixProblemSanitizer.java" | 3 | 0 | Bean Spring | Activation indirecte par Spring/JPA possible |
| IntegrationClientSupport | class | tn.iteam.util | "src/main/java/tn/iteam/util/IntegrationClientSupport.java" | 38 | 0 | POJO/Support | Reference statique detectee |
| MonitoringConstants | class | tn.iteam.util | "src/main/java/tn/iteam/util/MonitoringConstants.java" | 129 | 0 | POJO/Support | Reference statique detectee |
| MonitoringWebSocketPublisher | class | tn.iteam.websocket | "src/main/java/tn/iteam/websocket/MonitoringWebSocketPublisher.java" | 10 | 10 | Bean Spring | Activation indirecte par Spring/JPA possible |
| ZkBioWebSocketPublisher | class | tn.iteam.websocket | "src/main/java/tn/iteam/websocket/ZkBioWebSocketPublisher.java" | 6 | 8 | Bean Spring | Activation indirecte par Spring/JPA possible |
