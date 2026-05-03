# Architecture Mémoire - Projet PFE

> Date: 25 Avril 2026
> Version: 2.0.0
> Auteur: GitHub Copilot

---

## Historique des modifications

| Date | Version | Description |
|------|---------|-------------|
| 25/04/2026 | 2.0.0 | Refactoring Zabbix avec collectors |
| xx/xx/xxxx | 1.0.0 | Architecture initiale |

---

## Architecture Globale

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           COUCHE API EXTERNE                            │
├─────────────────────────────────────────────────────────────────────────┤
│  ZabbixClient    │ ObserviumClientX  │ ZkBioClientX    │ CameraAdapter  │
│  (appels JSON-RPC)│ (HTTP REST)      │ (HTTP REST)     │ (Socket)       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        COUCHE COLLECTORS (NEUF)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  ZabbixHostCollector    → hosts retrieval & mapping                    │
│  ZabbixProblemCollector → problems retrieval & trigger resolution      │
│  ZabbixMetricsCollector → metrics & history batch processing           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          COUCHE ADAPTER (FACADE)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  ZabbixAdapter (compatible) → délègue aux collectors                    │
│  ObserviumAdapter                                                        │
│  ZkBioAdapter                                                            │
│  CameraAdapter                                                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        COUCHE SERVICE (Métier)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  ZabbixMetricsServiceImpl  │ ZabbixProblemServiceImpl                  │
│  ZabbixSyncService          │ ObserviumPersistenceService               │
│  ZkBioPersistenceService    │ CameraInventoryService                    │
│  DashboardServiceImpl       │ TicketServiceImpl                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         COUCHE CONTRÔLEUR                               │
├─────────────────────────────────────────────────────────────────────────┤
│  MonitoringController  │ ZkBioController  │ TicketController           │
│  DashboardController   │ CameraController │ TorchScriptPredictionCtrl  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Composants Zabbix (Refactorisé)

### Fichiers créés

| Fichier | Responsabilité |
|---------|----------------|
| `ZabbixHostCollector.java` | Collection hosts + mapping DTO |
| `ZabbixProblemCollector.java` | Collection problems + résolution triggers |
| `ZabbixMetricsCollector.java` | Collection metrics + history batch |

### Fichier modifié

| Fichier | Changement |
|---------|------------|
| `ZabbixAdapter.java` | Délègue aux collectors, garde signature publique compatible |

### Méthodes publiques conservées

```java
// ZabbixAdapter - inchangées pour compatibilité
fetchAll()              → délègue à ZabbixHostCollector
fetchProblems()         → délègue à ZabbixProblemCollector
fetchProblems(JsonNode) → délègue à ZabbixProblemCollector
fetchMetricsAndMap()    → délègue à ZabbixMetricsCollector
fetchMetricsAndMap(JsonNode) → délègue à ZabbixMetricsCollector
```

---

## Modules du Projet

### 1. Module Monitoring Unifié
- **Responsabilité**: Vue homogenéisée de Zabbix, Observium, ZKBio, Camera
- **Classes clés**: `MonitoringController`, `MonitoringAggregationService`, `SnapshotStore`

### 2. Module Integrations Externes
- **Responsabilité**: Appels aux sources externes
- **Sous-modules**: Zabbix, Observium, ZKBio, Camera

### 3. Module Ticketing
- **Responsabilité**: Workflow de tickets
- **Classes clés**: `TicketController`, `TicketServiceImpl`

### 4. Module Dashboard/ML
- **Responsabilité**: Analytique et predictions
- **Classes clés**: `DashboardController`, `TorchScriptPredictionController`

### 5. Module Resilience
- **Responsabilité**: Fallback, timeout, retry, circuit breaker
- **Classes clés**: `IntegrationClientSupport`, `SourceAvailabilityService`

---

## Configuration

### Zabbix
- URL: `http://192.168.11.36/zabbix/api_jsonrpc.php`
- Poll: 30s (problems), 60s (metrics)
- Timeout light: 60s
- Timeout heavy: 25min

### Observium
- URL: `http://192.168.11.229`

### Database
- MySQL: `monitoring_db` (localhost:3306)

### WebSocket
- Topics: `/topic/zabbix/problems`, `/topic/zabbix/metrics`, `/topic/tickets`

---

## Patterns Utilisés

| Pattern | Application |
|---------|-------------|
| **Collector** | Extraction logique de collecte dans `Zabbix*Collector` |
| **Façade** | `ZabbixAdapter` délègue aux collectors |
| **Circuit Breaker** | Resilience4j sur `ZabbixClient` |
| **Fallback** | Snapshot `live → snapshot → persisted → empty` |
| **Batch Processing** | `ZabbixMetricsCollector` traite par lots |

---

## Tests

### Tests Zabbix
- `ZabbixAdapterTest`: 1 test
- `ZabbixClientResilienceProfileTest`: 2 tests
- `ZabbixMetricsServiceImplTest`: 1 test

### Résultat global
```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

---

## Dette Technique Identifiée

### Code mort / Deprecated
- `ZabbixMetricsService.synchronizeAndGetPersistedMetricsSnapshot()` (candidat)
- `ZabbixProblemController`, `ZabbixMetricsController` (@Deprecated)
- `ObserviumController` (temporaire)
- Packages vides: `cache`, `listener`, `logging`, `monitoring.provider`

### Duplications
- Logique de fallback snapshot dans 4 services d'intégration
- Helpers `safeMessage(...)` dupliqués
- Wrappers de réponse parallèles (`ApiResponse` vs `UnifiedMonitoringResponse`)

### Couplage élevé
- `ZabbixAdapter` (résolu avec collectors)
- `ZkBioIntegrationService` (à surveiller)
- `DashboardServiceImpl` (à scinder si croissance)

---

## Prochaines Étapes

1. ~~Refactoring Zabbix avec collectors~~ ✅
2. Appliquer même pattern aux autres adapters (Observium, ZKBio, Camera)
3. Extraire helper de fallback générique
4. Centraliser `safeMessage(...)`
5. Supprimer packages vides

---

## Contact

- Backend: Spring Boot 3.2.5 (Java 17)
- Frontend: Angular 20.3
- ML: PyTorch (PFERTC)
- Port: 8099