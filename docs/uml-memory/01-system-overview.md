# System Overview

## Project Summary

The project is a monitoring and operations platform built around a Spring Boot backend and an Angular frontend.
It aggregates operational data from Zabbix, Observium, ZKBio, and a camera subnet into unified monitoring endpoints and realtime streams.
On top of that monitoring core, the backend also exposes:

- dashboard analytics and ML-assisted predictions based on persisted Zabbix data
- ticket management APIs
- ZKBio business endpoints for server status, devices, attendance, and users

## Functional Scope

The observable functional perimeter from the code includes:

- consultation of a unified monitoring dashboard
- consultation of dedicated Zabbix, Observium, and ZKBio workspaces
- manual and scheduled data collection from monitoring sources
- realtime publication of unified monitoring problems, metrics, and source availability
- realtime publication of ZKBio attendance, devices, and status
- analytics endpoints for overview, predictions, and anomalies
- ticket lifecycle management

## Main Modules

### Frontend

- Angular shell and monitoring pages
- `MonitoringApiService` for REST consumption
- `MonitoringRealtimeService` for STOMP/SockJS consumption
- `MonitoringStore` for the global dashboard
- `ZabbixWorkspaceStore` for the Zabbix workspace

### Backend Monitoring Core

- `MonitoringController`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `SnapshotStore` with active `InMemorySnapshotStore`
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`
- `MonitoringStartup`
- source-specific schedulers

### Backend Integration Layer

- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`
- source adapters and external clients

### Persistence and Analytics

- JPA repositories and entities for monitored hosts, Zabbix metrics/problems, tickets, users, and source statuses
- `DashboardServiceImpl`
- `TorchScriptPredictionService`

## Observed Architecture

The architecture observed in code is a snapshot-centric unified monitoring architecture:

1. source-specific integration services fetch live data from external platforms
2. each integration service stores unified snapshots in `SnapshotStore`
3. `MonitoringCacheService` reads snapshots and computes freshness/degraded state
4. `MonitoringAggregationService` exposes unified REST responses
5. websocket publishers emit unified monitoring updates and ZKBio-specific business updates
6. the Angular frontend consumes REST snapshots first and then updates parts of the UI from websocket streams

## Internal Components

- Spring Boot application with async and scheduled execution enabled
- REST controllers for monitoring, ZKBio, dashboard, tickets, and compatibility endpoints
- websocket/STOMP broker configured through `/ws` with `/topic`
- source availability state machine with websocket publication
- optional Redis bean configuration guarded by property, without an active Redis-backed snapshot store

## External Components

- Zabbix API
- Observium API
- ZKBio API
- camera subnet scanning
- MySQL database
- optional Redis runtime beans when `app.redis.enabled=true`
- optional TorchScript model and metadata files for predictions

## Major Flows

- unified dashboard load through `/api/monitoring/hosts`, `/api/monitoring/problems`, `/api/monitoring/metrics`, and `/api/monitoring/sources/health`
- unified realtime updates through `/topic/monitoring/problems`, `/topic/monitoring/metrics`, and `/topic/monitoring/sources`
- ZKBio business flow through `/api/zkbio/*` and `/topic/zkbio/*`
- dashboard analytics through `/dashboard/overview`, `/dashboard/predictions`, and `/dashboard/anomalies`
- manual collection through `/api/monitoring/collect*` and `/api/zkbio/collect`

## Main Actors

- Monitoring Operator
- Support Agent
- Administrator
- External Monitoring Sources: Zabbix, Observium, ZKBio, Camera network
- Optional ML API consumer for `/predict`
