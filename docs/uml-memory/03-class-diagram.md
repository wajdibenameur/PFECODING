# Class Diagram

## Modeling Scope

The class diagram intentionally focuses on structurally important classes:

- REST controllers
- service contracts and service implementations
- snapshot infrastructure
- websocket publishers
- source adapters and external clients
- repositories and key entities
- high-value DTOs used by the unified monitoring flow

It does not list every mapper, helper, or trivial DTO to keep the diagram readable.

## Selected Classes And Responsibilities

### Controller Layer

- `MonitoringController`
  Exposes unified monitoring REST endpoints and collection endpoints.
- `ZkBioController`
  Exposes ZKBio business endpoints and manual ZKBio collection.
- `DashboardController`
  Exposes overview, prediction, and anomaly endpoints.
- `TicketController`
  Exposes ticket lifecycle endpoints.

### Monitoring Core

- `MonitoringAggregationService`
  Wraps cache results into `UnifiedMonitoringResponse<T>`.
- `MonitoringCacheService`
  Reads snapshots, computes freshness, degraded state, and dataset filtering.
- `SnapshotStore`
  Contract for snapshot persistence.
- `InMemorySnapshotStore`
  Active in-memory snapshot implementation.
- `StoredSnapshot<T>`
  Snapshot value object with `data`, `degraded`, `freshness`, and `updatedAt`.
- `SourceAvailabilityService` / `SourceAvailabilityServiceImpl`
  Maintains source availability state and publishes changes.
- `MonitoringWebSocketPublisher`
  Publishes unified monitoring problems, metrics, and source availability.
- `ZkBioWebSocketPublisher`
  Publishes ZKBio attendance, devices, and status snapshots.

### Integration Layer

- `IntegrationService`
  Common contract for source integration services.
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

These classes refresh snapshots and update source availability.

### Adapters And Clients

- `ZabbixAdapter` -> `ZabbixClient`
- `ObserviumAdapter` -> `ObserviumClientX`
- `ZkBioAdapter` -> `ZkBioClientX`

Adapters translate remote payloads into project DTOs and domain-friendly structures.
Clients encapsulate HTTP communication and resilience behavior.

### Analytics And Ticketing

- `DashboardService` / `DashboardServiceImpl`
  Builds dashboard overview, predictions, and anomalies from repositories and ML service.
- `TorchScriptPredictionService`
  Loads TorchScript model metadata and performs inference when configured.
- `TicketService` / `TicketServiceImpl`
  Manages tickets and websocket notifications.

### Persistence

- `MonitoredHostRepository`
- `ZabbixProblemRepository`
- `ZabbixMetricRepository`
- `TicketRepository`
- `UserRepository`

### Entities And DTOs

- `BaseEntity`
- `MonitoredHost`
- `ZabbixProblem`
- `ZabbixMetric`
- `Ticket`
- `User`
- `UnifiedMonitoringResponse<T>`
- `UnifiedMonitoringHostDTO`
- `UnifiedMonitoringProblemDTO`
- `UnifiedMonitoringMetricDTO`

## Main Relationships

- `MonitoringController` depends on aggregation, source availability, integration services, and websocket publishers.
- `MonitoringAggregationService` depends on `MonitoringCacheService`.
- `MonitoringCacheService` depends on `SnapshotStore`.
- `InMemorySnapshotStore` realizes `SnapshotStore`.
- Source integration services realize `IntegrationService`.
- Source integration services depend on adapters, `SnapshotStore`, and `SourceAvailabilityService`.
- `DashboardServiceImpl` depends on repositories and `TorchScriptPredictionService`.
- `TicketServiceImpl` depends on repositories.
- `Ticket` references `User` in three roles: `createdBy`, `assignedTo`, `validatedBy`.
- Several domain entities inherit from `BaseEntity`.

## Modeling Choices

- `UnifiedMonitoringResponse<T>` is shown because it defines the public REST contract for the unified monitoring API.
- `MonitoringSourceType` is kept in the diagram because it encodes dataset support and metric coverage semantics.
- Ticketing is included even if the Angular frontend still uses placeholders, because the backend API is implemented and reachable.

## Hypotheses

- Repository inheritance from Spring Data JPA is modeled as repository dependencies rather than full generic inheritance trees, to keep the diagram readable.
- `ZkBioWebSocketPublisher.TOPIC_PROBLEMS` exists in code but is not modeled as a central relationship because the active frontend workflow uses unified monitoring problems instead.

## PlantUML Reference

The corresponding UML class diagram is in:

- `docs/uml-memory/class-diagram.puml`
