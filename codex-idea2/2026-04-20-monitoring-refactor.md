# Monitoring Refactor Ideas - 2026-04-20

## Next cleanups after this refactor

- Decide whether source-specific compatibility controllers (`/api/zabbix/*`) can be archived after external consumer confirmation.
  - Current Angular frontend is already migrated to `/api/monitoring/*`.
- Add a dedicated camera scheduler if camera hosts must stay fresh without manual collection.
- Replace string dataset keys (`hosts`, `metrics`, `problems`) with a small enum to reduce semantic drift.
- Consider moving source capability metadata from `MonitoringSourceType` into a richer contract if unified monitoring grows.
- Consider consolidating ZKBio raw business endpoints and integration endpoints behind one explicit service facade.
- Revisit `PfeprojectApplicationTests` with a true test profile using embedded infrastructure or Testcontainers.
