# Codex Memory

## Purpose

This file stores persistent technical memory for the project.
It is intended to help future Codex sessions keep the same mental model of:

- the current backend architecture
- the integration strategy
- resilience and cache rules
- important implementation conventions

This file is documentation only. It must not be treated as executable configuration.

## Current Architecture

The backend is a Spring Boot multi-source monitoring application.
It combines:

- REST controllers
- business services
- external integration clients
- adapters that normalize external data
- monitoring providers for unified endpoints
- schedulers for periodic collection and live refresh
- JPA persistence for stored monitoring data
- WebSocket publication for near real-time updates

The architecture has been evolved progressively. The project intentionally avoids massive refactors and keeps the existing layering as much as possible.

## Main Layers

### Controllers

Controllers expose REST endpoints.

Important entry points:

- `tn.iteam.controller.MonitoringController`
- source-specific controllers such as ZKBio and other integration controllers

`MonitoringController` is the unified monitoring entry point under `/api/monitoring`.

### Services

Services orchestrate collection, persistence, aggregation and availability tracking.

Important services include:

- `tn.iteam.service.impl.MonitoringServiceImpl`
- `tn.iteam.monitoring.service.MonitoringAggregationService`
- `tn.iteam.monitoring.service.MonitoringCacheService`
- `tn.iteam.service.impl.SourceAvailabilityServiceImpl`
- `tn.iteam.service.impl.ZabbixLiveSynchronizationServiceImpl`
- `tn.iteam.service.impl.ZkBioServiceImpl`
- `tn.iteam.service.ZabbixMonitoringService` → `ZabbixMonitoringServiceImpl`
- `tn.iteam.service.ObserviumMonitoringService` → `ObserviumMonitoringServiceImpl`
- `tn.iteam.service.ZkBioMonitoringService` → `ZkBioMonitoringServiceImpl`

### Adapters

Adapters convert raw external data into project DTOs and domain-friendly structures.
They are also the preferred place for fallback orchestration when the client fails but a cached snapshot may still be usable.

Important adapters:

- `tn.iteam.adapter.zabbix.ZabbixAdapter`
- `tn.iteam.adapter.observium.ObserviumAdapter`
- `tn.iteam.adapter.zkbio.ZkBioAdapter`
- `tn.iteam.adapter.camera.CameraAdapter`

### Clients

Clients are the direct HTTP integration layer.
They are the preferred place for transport concerns:

- network calls
- timeout handling
- circuit breaker
- retry
- raw Redis snapshot save after successful calls

Important clients:

- `tn.iteam.adapter.zabbix.ZabbixClient`
- `tn.iteam.client.ObserviumClientX`
- `tn.iteam.client.ZkBioClient`

### Monitoring Providers

Monitoring providers expose each source in the unified monitoring format.

Important providers:

- `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`
- `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`
- `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`

These providers are consumed by `MonitoringCacheService` and `MonitoringAggregationService`.

## Integrations

### Zabbix

Zabbix is the most complete monitoring source in the project.

It currently provides:

- unified hosts
- unified problems
- unified metrics
- persisted snapshots in MySQL
- Redis snapshots for external-call fallback

Important classes:

- client: `tn.iteam.adapter.zabbix.ZabbixClient`
- adapter: `tn.iteam.adapter.zabbix.ZabbixAdapter`
- service: `tn.iteam.service.ZabbixMonitoringService` → `ZabbixMonitoringServiceImpl`
- mapper: `tn.iteam.mapper.ZabbixMonitoringMapper`
- provider: `tn.iteam.monitoring.provider.ZabbixMonitoringProvider`

Architecture note:

- All providers now use the **unified pattern**: Provider → MonitoringService → Adapter → Client
- `ZabbixMonitoringService` delegates to `ZabbixAdapter` which handles fallback Redis and data transformation

### Observium

Observium is used mainly for devices and alerts.

It currently provides:

- unified hosts
- unified problems
- Redis fallback snapshots
- live API calls through WebClient

Important classes:

- client: `tn.iteam.client.ObserviumClientX`
- adapter: `tn.iteam.adapter.observium.ObserviumAdapter`
- service: `tn.iteam.service.ObserviumMonitoringService` → `ObserviumMonitoringServiceImpl`
- mapper: `tn.iteam.mapper.ObserviumMonitoringMapper`
- provider: `tn.iteam.monitoring.provider.ObserviumMonitoringProvider`

Current note:

- Observium does not currently provide unified metrics

### ZKBio

ZKBio is integrated as a specialized source.

It currently provides:

- unified hosts
- unified problems
- dedicated status, devices and attendance endpoints (business logic)
- live API calls through a dedicated WebClient
- Redis fallback snapshots

Important classes:

- client: `tn.iteam.client.ZkBioClient`
- adapter: `tn.iteam.adapter.zkbio.ZkBioAdapter`
- business service: `tn.iteam.service.ZkBioServiceInterface` → `ZkBioServiceImpl`
- monitoring service: `tn.iteam.service.ZkBioMonitoringService` → `ZkBioMonitoringServiceImpl`
- mapper: `tn.iteam.mapper.ZkBioMonitoringMapper`
- provider: `tn.iteam.monitoring.provider.ZkBioMonitoringProvider`

Architecture note:

- Business logic (status, devices, attendance) uses `ZkBioServiceInterface` / `ZkBioServiceImpl`
- Monitoring logic (hosts, problems) uses `ZkBioMonitoringService` / `ZkBioMonitoringServiceImpl`
- This separation ensures single responsibility principle

Current note:

- ZKBio does not currently provide unified metrics

## Unified Monitoring Endpoints

Unified monitoring endpoints are exposed under `/api/monitoring`.

Main endpoints:

- `GET /api/monitoring/hosts`
- `GET /api/monitoring/problems`
- `GET /api/monitoring/metrics`
- `GET /api/monitoring/sources/health`
- `POST /api/monitoring/collect`
- `POST /api/monitoring/collect/zabbix`
- `POST /api/monitoring/collect/observium`
- `POST /api/monitoring/collect/zkbio`
- `POST /api/monitoring/collect/camera`

Main class:

- `tn.iteam.controller.MonitoringController`

Important design note:

- unified endpoints expose normalized DTOs
- source health is exposed separately through `SourceAvailabilityService`
- the freshness model is not fully uniform across all sources
- `GET /api/monitoring/hosts`, `/problems` and `/metrics` expose a minimal wrapper with `data`, `degraded` and per-source `freshness`
- `GET /api/monitoring/metrics` also exposes a lightweight per-source `coverage` metadata block so clients can see that metrics are currently supported mainly by Zabbix

## Aggregation Strategy

The current unified read path is:

1. `MonitoringController`
2. `MonitoringAggregationService`
3. `MonitoringCacheService`
4. `MonitoringProvider` list

### MonitoringAggregationService

`MonitoringAggregationService` is intentionally thin.
It delegates unified reads to `MonitoringCacheService` and preserves the aggregated `degraded` flag in a minimal response wrapper.

Its role is:

- expose simple `getHosts`
- expose simple `getProblems`
- expose simple `getMetrics`
- keep the public aggregation API stable while transporting `data`, `degraded`, `freshness` and endpoint-level clarification metadata when needed

### MonitoringCacheService

`MonitoringCacheService` exists to solve two things:

- centralize aggregation logic
- avoid Spring self-invocation problems on `@Cacheable`

It computes aggregated data across providers and returns a wrapper result containing:

- `data`
- `degraded`
- `freshness`

Only non-degraded results are intended to be cached.

## Resilience Strategy

The project uses a layered resilience strategy.

General rule:

1. try live API
2. if live API fails, try Redis snapshot
3. if Redis is unavailable or empty, keep the existing local behavior of the calling layer

### Redis Fallback

Redis is used as short-lived fallback storage for external integration snapshots.

Main service:

- `tn.iteam.cache.RedisIntegrationCacheService`

Interface:

- `tn.iteam.cache.IntegrationCacheService`

Rules:

- store DTO-like or JSON-like payloads only
- do not store JPA entities
- keep TTL short
- do not turn Redis into the primary business source
- a Redis fallback may return a normal success response only when a usable snapshot actually exists
- if both live API and Redis snapshot are unavailable, the integration must raise an explicit exception instead of synthesizing an empty success-like result

### Circuit Breaker and Retry

Resilience4j is applied at the external-client level.

Current instances:

- `zabbixApi`
- `observiumApi`
- `zkbioApi`

Current behavior in `application.properties`:

- sliding window type: `COUNT_BASED`
- sliding window size: `10`
- minimum number of calls: `5`
- failure rate threshold: `50`
- open-state wait duration: `30s`
- permitted half-open calls: `3`
- retry max attempts: `3`
- retry wait duration: `1s`
- exponential backoff enabled

Important rule:

- resilience stays close to the clients
- business services should not be globally refactored just to host circuit breaker logic

## Cache Strategy

There are two distinct cache usages in the project.

### 1. Spring Cache for Unified Monitoring Reads

Configured in:

- `tn.iteam.config.RedisCacheConfig`

Named caches:

- `monitoring:hosts`
- `monitoring:metrics`
- `monitoring:problems`
- `monitoring:source-health`

Configured TTLs from `application.properties`:

- `app.cache.ttl.hosts=30s`
- `app.cache.ttl.metrics=30s`
- `app.cache.ttl.problems=30s`
- `app.cache.ttl.source-health=15s`

### Unified Freshness Metadata

Unified monitoring responses expose per-source freshness metadata with these minimal values:

- `live`
- `persisted`
- `redis_fallback`

Current determination rule:

- `ZABBIX` unified data is currently marked `persisted`
- `OBSERVIUM` and `ZKBIO` are marked `live` when `SourceAvailabilityService` says the source is available
- `OBSERVIUM` and `ZKBIO` are marked `redis_fallback` when data is still served while `SourceAvailabilityService` says the source is unavailable

### Metrics Coverage Metadata

`GET /api/monitoring/metrics` exposes a lightweight `coverage` metadata block to avoid implying fully homogeneous multi-source support.

Current values:

- `ZABBIX= supported`
- `OBSERVIUM= not_supported`
- `ZKBIO= not_supported`

### 2. Redis Snapshots for Integration Fallback

Configured through:

- `tn.iteam.cache.RedisIntegrationCacheService`

Snapshot TTL:

- `app.cache.ttl.integration-snapshot=60s`

### Degraded Rules

`MonitoringCacheService` must only cache non-degraded aggregation results.

Target rule:

- if all relevant providers succeed, the result may be cached
- if at least one relevant provider is degraded, the result must not be cached

Important nuance:

- this rule only works if provider failure is not silently hidden
- a provider that swallows an exception and returns an empty list can make a degraded result look healthy
- `ObserviumMonitoringProvider` must let adapter failures propagate so `MonitoringCacheService` can mark the aggregated result as degraded and skip caching
- `ObserviumAdapter.fetchAll()` and `fetchProblems()` must throw an explicit integration exception when neither live API data nor a usable Redis snapshot is available
- `ZkBioAdapter.fetchAll()` and `fetchProblems()` must throw an explicit integration exception when neither live API data nor a usable Redis snapshot is available
- `ZkBioServiceImpl.fetchMonitoringHosts()` and `fetchMonitoringProblems()` must rely directly on adapter methods that propagate integration failures, so the unified aggregation path cannot silently downgrade them into endpoint-friendly fallback values
- the `degraded` flag produced by `MonitoringCacheService` must be transported through `MonitoringAggregationService` and exposed by `MonitoringController`, never discarded
- freshness metadata must be transported through the same chain and exposed to clients without changing the existing business DTO payloads

## SourceAvailabilityService Strategy

`SourceAvailabilityService` is the central source-health tracker.

Current implementation:

- `tn.iteam.service.impl.SourceAvailabilityServiceImpl`

Current modeled states:

- `AVAILABLE`
- `DEGRADED`
- `UNAVAILABLE`

Current intended meaning:

- live API success -> source becomes `AVAILABLE`
- live API failure with usable Redis fallback -> source becomes `DEGRADED`
- live API failure without usable Redis fallback -> source becomes `UNAVAILABLE`

Practical implication:

- the project now distinguishes between "live source healthy", "live source down but data still served through Redis fallback", and "no usable data path"

## WebClient Rules

WebClient was added progressively and does not replace all RestTemplate usage at once.

Primary configuration:

- `tn.iteam.config.WebClientConfig`

Standard bean:

- primary `WebClient` for regular integrations

Special ZKBio bean:

- `zkbioUnsafeTlsWebClientForInternalUseOnly`

Current timeout properties:

- `integration.webclient.connect-timeout-ms=5000`
- `integration.webclient.response-timeout-ms=10000`
- `integration.webclient.read-timeout-ms=10000`
- `integration.webclient.write-timeout-ms=10000`

### ZKBio TLS Rule

ZKBio currently needs a TLS-permissive client because its endpoint may use a certificate setup that is not validated by the default client.

Rules:

- only ZKBio may inject the unsafe TLS WebClient
- the bean name must remain explicit and alarming
- this is a transitional technical compromise, not a general security pattern
- other integrations must keep using the standard WebClient unless a separate explicit decision is made

## Redis Key Conventions

### Generic Integration Snapshot Pattern

Redis integration snapshots use the prefix:

- `integration:snapshot:{source}:{key}`

Normalization behavior in `RedisIntegrationCacheService`:

- lowercase
- spaces replaced with `-`
- `:` replaced with `-`

### Zabbix Parametrized Keys

Zabbix Redis keys must be parameter-dependent to avoid collisions.

Important rule:

- old generic legacy fallbacks must not be reintroduced
- only parametrized keys should be used for Zabbix snapshot reads

Examples of current key intent:

- hosts snapshot
- recent problems snapshot
- host by specific `hostId`
- trigger by specific `triggerId`
- items by specific host or host list
- history by `itemId`, `valueType`, `from`, `to`
- batch history by item set and time bounds
- last item value by item and value type

The exact string builders live in:

- `tn.iteam.adapter.zabbix.ZabbixClient`

The fallback reads live in:

- `tn.iteam.adapter.zabbix.ZabbixAdapter`

## Code Conventions That Matter

### Scope and Change Discipline

- avoid mass refactors
- prefer narrow and progressive changes
- modify only the classes needed for the task
- keep behavior unchanged unless the task explicitly asks for behavioral change
- when archiving obsolete classes, prefer moving only files confirmed as safe by an explicit usage audit
- archived Java sources should be neutralized as compile inputs to avoid duplicate classes or accidental Spring loading

### Placement of Responsibilities

- controllers should stay thin
- transport resilience belongs near clients
- fallback orchestration belongs in adapters or tightly related service classes
- schedulers should remain robust but not overly complex

### Serialization

- always use the Spring-injected `ObjectMapper`
- do not introduce `new ObjectMapper()` in production code

### Redis Usage

- Redis is a helper for cache and fallback
- Redis is not the source of truth
- keys must be explicit and collision-safe

### Monitoring Semantics

- do not cache degraded unified aggregation results
- do not silently hide provider failures if the aggregator must know the result is degraded
- keep unified endpoint semantics as consistent as possible across sources

## Important Known Constraints

- Zabbix unified provider currently serves persisted snapshots, while Observium and ZKBio providers are closer to live or fallback reads
- unified metrics are not fully populated by every source
- source health does not yet expose a first-class `DEGRADED` state
- fallback correctness depends on both Redis availability and provider error propagation

## Decisions Already Taken

- Redis was added for both Spring Cache and integration fallback snapshots
- WebClient was introduced without removing all RestTemplate usage
- Resilience4j was added progressively at the external-client layer
- `MonitoringCacheService` was introduced as a separate bean to make `@Cacheable` effective
- `ObserviumMonitoringProvider` was aligned with degraded-cache rules by no longer converting integration failures into empty lists
- `ObserviumAdapter` unified reads were aligned with degraded-cache rules by throwing an explicit exception when API plus Redis fallback cannot provide valid data
- `ObserviumClientX.cachedFallback()` was aligned with degraded-cache rules by returning success only for a real Redis snapshot and throwing an explicit exception when no snapshot exists
- `ZkBioAdapter` unified reads were aligned with degraded-cache rules by throwing an explicit exception when API plus Redis fallback cannot provide valid data
- `ZkBioServiceImpl` unified monitoring methods were wired directly to adapter reads so aggregation continues to receive propagated integration failures
- `MonitoringAggregationService` and `MonitoringController` were aligned to expose a minimal unified response wrapper so the API no longer loses the `degraded` flag produced by `MonitoringCacheService`
- unified monitoring responses now expose per-source freshness metadata with the minimal values `live`, `persisted` and `redis_fallback`
- `/api/monitoring/metrics` now exposes explicit coverage metadata so clients can see that current unified metric support is mainly provided by Zabbix
- `SourceAvailabilityService` now exposes `DEGRADED` when a source is down but still served via Redis fallback
- safe duplicate, obsolete and unused sources were archived under `src/main/java/depl`
- archived sources use the `.archived` suffix so they remain traceable without re-entering Maven compilation or Spring scanning
- Zabbix Redis fallback was changed to parametrized keys to avoid collisions
- the unsafe TLS WebClient was explicitly isolated to the ZKBio context

## Guidance For Future Codex Sessions

- start by checking whether a failure is live API, Redis fallback, or persistence-related
- verify whether a provider is surfacing failure or silently converting it to empty data
- keep cache logic and fallback logic separate in your reasoning
- when reviewing source health, compare returned data path with `SourceAvailabilityService` state
- before modifying monitoring semantics, verify consistency across Zabbix, Observium and ZKBio
