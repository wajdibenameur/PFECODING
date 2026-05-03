# Monitoring Refactor Memory - 2026-04-20

## Goal

Consolidate backend monitoring around:

- `integration/*`
- `SnapshotStore`
- `MonitoringAggregationService`

and remove compiled legacy leftovers.

## Implemented

- Added `CameraIntegrationService` and removed active usage of legacy `MonitoringService`.
- Updated `MonitoringStartup` to warm up all active sources, including Zabbix and camera, from the integration layer.
- Updated `MonitoringController` to use `CameraIntegrationService` instead of `MonitoringService`.
- Unified active ZKBio monitoring client usage on `ZkBioClient`; `ZkBioClientX` was archived to `depl`.
- Fixed monitoring source semantics:
  - `CAMERA` now participates only in `hosts`
  - `OBSERVIUM` and `ZKBIO` metrics are exposed as `synthetic`
  - `ZABBIX` metrics are exposed as `native`
- Archived compiled legacy classes to `src/main/java/depl/replaced-classes/2026-04-20-monitoring-refactor/`.
- Deleted dead placeholders/utilities:
  - `AppConfig`
  - `LoggingConfig`
  - `ZabbixProblemLogger`
  - `RateLimitedLogger`
  - `DateUtils`
  - `NetworkUtils`
  - `JobStatus`
  - `TicketDTO`

## Validation

- `mvn -q -DskipTests compile` OK
- `mvn -q test` OK
- `PfeprojectApplicationTests` was disabled explicitly because it requires external MySQL/Redis services not present in the local workspace.

## Follow-up reality check - 2026-04-22

- Fixed a startup blocker caused by JDK dynamic proxies around `ZkBioIntegrationService`.
- `PfeprojectApplication` now enables async with `@EnableAsync(proxyTargetClass = true)` so injections by concrete class keep working.
- `AsyncConfig` no longer duplicates `@EnableAsync`; async activation is centralized at application entry point.
- Current Angular frontend no longer consumes `/api/zabbix/active` or `/api/zabbix/metrics`.
- Compatibility controllers still exist in backend for legacy or external consumers, but not for the active frontend flow.
