# Architecture Diagram

## Chosen UML View

The architecture view is modeled as a UML Component Diagram.
This is the most appropriate standard UML representation for the current repository because:

- the logical component boundaries are visible in code
- deployment topology is only partially deducible
- the main value lies in controller/service/store/client/repository relationships

## Main Blocks

### Angular Frontend

- monitoring pages
- `MonitoringApiService`
- `MonitoringRealtimeService`
- `StompClientService`
- `MonitoringStore`
- `ZabbixWorkspaceStore`

### REST API Layer

- `MonitoringController`
- `ZkBioController`
- `DashboardController`
- `TicketController`
- compatibility controllers for legacy consumers

### Monitoring Core

- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore`
- `InMemorySnapshotStore`
- `SourceAvailabilityServiceImpl`
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `MonitoringStartup`
- scheduled refresh components

### Integration Layer

- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`
- source adapters
- source HTTP clients

### Persistence And Analytics

- JPA repositories
- MySQL-backed entities
- `DashboardServiceImpl`
- `TorchScriptPredictionService`
- `TicketServiceImpl`

### Optional Redis Configuration

- `RedisOptionalConfiguration`
- `AppRedisProperties`

Redis is modeled as optional because:

- no active `RedisSnapshotStore` exists in the current code
- `InMemorySnapshotStore` remains the active snapshot path
- Redis beans are only created when `app.redis.enabled=true`

## Major Flows

- REST snapshot flow: frontend -> controllers -> aggregation/cache -> snapshot store
- realtime flow: frontend -> SockJS/STOMP -> `/topic/*`
- source refresh flow: startup/schedulers/controllers -> integration services -> adapters/clients -> external systems
- persistence flow: source services and analytics services -> repositories -> MySQL
- analytics flow: dashboard endpoints -> repositories + TorchScript prediction service
- ticket flow: ticket endpoints -> ticket service -> repositories -> `/topic/tickets`

## Major Dependencies

- frontend depends on unified monitoring REST and STOMP contracts
- aggregation depends on snapshot retrieval rather than live integration clients
- source integrations depend on external adapters and source availability tracking
- analytics depend on persisted Zabbix metrics/problems
- optional Redis configuration does not sit on the runtime critical path of monitoring

## PlantUML Reference

The corresponding UML component diagram is in:

- `docs/uml-memory/architecture-overview.puml`
