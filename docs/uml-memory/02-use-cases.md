# Use Cases

## Actors

- `Monitoring Operator`
  Main frontend user for dashboard, monitoring workspaces, and manual collection.
- `Support Agent`
  Backend/API user for operational follow-up and ticket handling.
- `Administrator`
  Backend/API user for validation and ticket supervision.
- `Zabbix Platform`
  Supporting external system for collection use cases.
- `Observium Platform`
  Supporting external system for collection use cases.
- `ZKBio Platform`
  Supporting external system for collection use cases.
- `Camera Network`
  Supporting external system for collection use cases.

## Use Cases

### `Consult Unified Dashboard`

Allows a monitoring operator to load the multi-source dashboard.
It is justified by `MonitoringDashboardPageComponent`, `MonitoringStore`, and the unified monitoring REST endpoints.

### `View Source Availability`

Included by the dashboard because `MonitoringStore` calls `/api/monitoring/sources/health` and displays source availability through `SourceHealthPanelComponent`.

### `View Unified Asset Inventory`

Included by the dashboard because `MonitoringStore` builds a global inventory from unified hosts, problems, and metrics and exposes it to `AssetInventoryTableComponent`.

### `View Alert Summary`

Included by the dashboard because `MonitoringStore` computes a global problem summary and exposes it to `AlertSummaryPanelComponent`.

### `Consult Zabbix Workspace`

Supported by `MonitoringZabbixPageComponent` and `ZabbixWorkspaceStore`.
The workspace consumes unified monitoring snapshots filtered to source `ZABBIX`.

### `Review Predictions and Anomalies`

Included by the Zabbix workspace because the page loads `/dashboard/predictions`, `/dashboard/anomalies`, and `/dashboard/overview`.

### `Consult Observium Workspace`

Supported by `MonitoringObserviumPageComponent`.
The page filters unified monitoring data to source `OBSERVIUM`.

### `Consult ZKBio Workspace`

Supported by `MonitoringZkBioPageComponent`.
The page combines unified monitoring data with ZKBio-specific business endpoints and websocket topics.

### `Trigger Unified Monitoring Collection`

Supported by `MonitoringController` through:

- `POST /api/monitoring/collect`
- `POST /api/monitoring/collect/zabbix`
- `POST /api/monitoring/collect/observium`
- `POST /api/monitoring/collect/camera`

This use case is associated with the external source platforms because the controller triggers source-specific integration services.

### `Trigger ZKBio Collection`

Supported by `POST /api/zkbio/collect` in `ZkBioController`, which delegates to `ZkBioIntegrationService.refreshAllAndPublish()`.

### `Manage Tickets`

Supported by `TicketController` and `TicketServiceImpl`.
The code supports manual creation, creation from Zabbix problems, assignment, status updates, validation, rejection, comments, listing, and deletion.

## Relationship Justification

- `Consult Unified Dashboard` includes `View Source Availability`, `View Unified Asset Inventory`, and `View Alert Summary` because these sub-flows are always part of the dashboard page composition.
- `Consult Zabbix Workspace` includes `Review Predictions and Anomalies` because the workspace always loads those analytics from `/dashboard/*`.
- No `extend` relation was added because the code does not show optional user-triggered extensions attached to a base use case strongly enough to justify one.

## PlantUML Reference

The corresponding UML use case diagram is in:

- `docs/uml-memory/use-case-diagram.puml`
