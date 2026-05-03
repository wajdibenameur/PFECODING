# Sequence Diagrams

## Scenario 1 - Global Dashboard Main Flow

- Name: `Global Dashboard Main Flow`
- Objective: show how the Angular dashboard loads unified snapshots and then subscribes to realtime updates.
- Preconditions:
  - backend is started
  - snapshots may already exist in `SnapshotStore`
  - websocket endpoint `/ws` is available
- Actors / Participants:
  - `Monitoring Operator`
  - `MonitoringDashboardPageComponent`
  - `MonitoringStore`
  - `MonitoringApiService`
  - `MonitoringRealtimeService`
  - `StompClientService`
  - `MonitoringController`
  - `MonitoringAggregationService`
  - `MonitoringCacheService`
  - `SnapshotStore`
  - `SourceAvailabilityService`
- Nominal Flow:
  - the page calls `loadSnapshot()`
  - the store performs parallel REST calls for hosts, problems, metrics, and source health
  - the backend resolves monitoring datasets from `SnapshotStore`
  - the store computes dashboard view models
  - the page binds realtime subscriptions to monitoring problems, metrics, and source availability
- Exceptions / Variants:
  - when a snapshot is missing, freshness becomes `snapshot_missing`
  - when a source is degraded, the source health note reflects the fallback state
  - realtime updates only change freshness, not coverage
- PlantUML:
  - `docs/uml-memory/sequence-01-main-flow.puml`

## Scenario 2 - Unified Monitoring Collection Flow

- Name: `Unified Monitoring Collection Flow`
- Objective: show how a manual collection request triggers integrations, snapshot persistence, and websocket publication.
- Preconditions:
  - operator is on a monitoring page
  - collection endpoints are reachable
- Actors / Participants:
  - `Monitoring Operator`
  - `MonitoringDashboardPageComponent`
  - `MonitoringStore`
  - `MonitoringApiService`
  - `MonitoringController`
  - `ZabbixIntegrationService`
  - `ObserviumIntegrationService`
  - `ZkBioIntegrationService`
  - `CameraIntegrationService`
  - `SnapshotStore`
  - `SourceAvailabilityService`
  - `MonitoringWebSocketPublisher`
  - `ZkBioWebSocketPublisher`
- Nominal Flow:
  - the user triggers `collectAll`
  - the controller refreshes source-specific integrations
  - integrations persist snapshots and mark availability
  - publishers emit unified monitoring topics and ZKBio side topics
  - the frontend schedules a delayed REST refresh
- Exceptions / Variants:
  - on live fetch failure with an existing snapshot, the integration keeps a degraded snapshot with freshness `snapshot_fallback`
  - on failure without a snapshot, the source becomes unavailable
- PlantUML:
  - `docs/uml-memory/sequence-02-monitoring-flow.puml`

## Scenario 3 - ZKBio Manual Refresh And Publication

- Name: `ZKBio Manual Refresh And Publication`
- Objective: show the dedicated ZKBio collection flow, including business data publication.
- Preconditions:
  - `POST /api/zkbio/collect` is triggered
  - the ZKBio page may already be subscribed to realtime topics
- Actors / Participants:
  - `Monitoring Operator`
  - `MonitoringZkBioPageComponent`
  - `MonitoringApiService`
  - `ZkBioController`
  - `ZkBioIntegrationService`
  - `ZkBioAdapter`
  - `ZkBioServiceImpl`
  - `ZkBioClientX`
  - `SnapshotStore`
  - `MonitoringWebSocketPublisher`
  - `ZkBioWebSocketPublisher`
  - `MonitoringRealtimeService`
  - `StompClientService`
- Nominal Flow:
  - the controller delegates to `refreshAllAndPublish()`
  - the integration refreshes ZKBio monitoring snapshots and business snapshots
  - snapshots are stored under `hosts`, `problems`, `metrics`, `attendance`, `devices`, and `status`
  - unified monitoring topics and ZKBio-specific topics are published
  - the page receives updates through existing subscriptions
- Exceptions / Variants:
  - if a live call fails and a previous snapshot exists, a degraded snapshot is kept
  - if the ML or ticketing subsystems are unavailable, this flow is unaffected because they are not on the ZKBio critical path
- PlantUML:
  - `docs/uml-memory/sequence-03-zkbio-flow.puml`
