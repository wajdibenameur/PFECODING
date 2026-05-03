# Assumptions And Gaps

## Confirmed Assumptions Used In The UML

- `InMemorySnapshotStore` is the active snapshot implementation because it is the only active `SnapshotStore` bean in the code.
- Redis is optional and non-blocking because only optional Redis bean configuration exists; there is no active Redis-backed snapshot store.
- The monitoring frontend primarily consumes unified monitoring endpoints and unified monitoring websocket topics.
- ZKBio business websocket topics for attendance, devices, and status are active.
- Compatibility controllers under `/api/zabbix/*` and `/api/observium/*` still exist, but they are not the target architecture.

## Areas Of Uncertainty

- The production deployment topology is not fully derivable from the repository alone.
- Authentication and authorization flows are not modeled because the current frontend uses a no-op auth context and no active security configuration was found in the inspected code.
- The exact operator roles used in production are not explicit in code; actor names were inferred from implemented responsibilities.
- The presence of `ZkBioWebSocketPublisher.TOPIC_PROBLEMS` is real in code, but it does not appear to be the principal active frontend path anymore.

## Elements Not Fully Deductible From Code Alone

- real network topology between backend and external systems
- production hosting and infrastructure boundaries
- external reverse proxy or load balancer behavior
- operational observability stack outside Spring Actuator
- exact business ownership of placeholder frontend routes

## Deliberate Simplifications

- Not every mapper, DTO, helper, and support class is shown in the class diagram.
- Repository generic inheritance trees are simplified to keep the diagram readable.
- The architecture diagram groups source-specific adapters and clients into a compact integration layer.
- The use case diagram emphasizes user-observable capabilities and major backend APIs instead of every single endpoint.

## Known Code Gaps Worth Tracking

- `ObserviumController`, `ZabbixProblemController`, and `ZabbixMetricsController` are temporary compatibility controllers that should be revisited later.
- Angular ticket routes are currently placeholders even though the backend ticket API is implemented.
- Some tests still use historical freshness values such as `redis_fallback` or `persisted`; those literals do not fully match the current runtime semantics centered on `live`, `snapshot_fallback`, and `snapshot_missing`.
- `ZkBioWebSocketPublisher` still exposes `/topic/zkbio/problems`, while the active unified monitoring flow uses `/topic/monitoring/problems`.

## Recommendations For Future UML Enrichment

- Add a dedicated deployment diagram only when the real production topology is documented.
- Add a dedicated ticketing class and sequence diagram if the frontend starts consuming ticket APIs.
- Add a dedicated ML sequence diagram if `/predict` becomes a first-class user-facing workflow.
- Add a future Redis snapshot diagram only if a real `RedisSnapshotStore` or composite snapshot strategy is introduced into production code.
