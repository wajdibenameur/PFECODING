# Analyse du Couplage et des Opportunites de Refactorisation

## 1. Hotspots de couplage eleve

### `ZabbixAdapter`
#### Constat
La classe concentre:
- appels client Zabbix
- logique de batch history
- mapping technique
- ponts bloquants
- orchestration partielle des chemins de metriques

#### Impact
- cout de test eleve
- comprehension difficile
- risque de regression localisee

#### Recommandation
Scission progressive en:
- collecteur hosts/problems
- collecteur metrics/history
- helper de mapping technique

### `ZkBioIntegrationService`
#### Constat
La classe melange:
- monitoring unifie
- datasets bruts ZKBio
- fallback snapshot
- publication

#### Recommandation
Extraire a terme un collaborateur dedie aux datasets bruts ou a la publication composite.

### `DashboardServiceImpl`
#### Constat
Assemble:
- overview
- features engineering
- predictions ML
- detection d'anomalies

#### Recommandation
Scission seulement si le module dashboard continue a croitre.

## 2. Duplication inter-services d'integration
### Classes concernees
- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

### Similarites confirmees
- `saveSnapshot(...)`
- `safeGetExistingSnapshot(...)`
- `safeLoadPersistedFallback(...)`
- `saveFallbackSnapshot(...)`
- `safeMessage(...)`
- sequence `live -> snapshot -> persisted -> empty`

### Evaluation
- confiance: elevee
- conclusion: une extraction de helper ou de template method est justifiee si l'equipe prevoit encore des evolutions sur cette zone

## 3. Couplage latent du module ticketing
### Constat
Le ticketing depend d'un modele `User` / `Role` / `Permission`, mais sans enforcement securite.

### Risque
Le couplage conceptuel existe sans traduction technique complete. Le module peut etre mal interprete comme securise alors qu'il ne l'est pas.

## 4. Qualite des abstractions
### Abstractions reussies
- `AsyncIntegrationService`
- `IntegrationServiceRegistry`
- `ZkBioRefreshOrchestrationService`
- `MonitoringSnapshotPublicationService`
- `SnapshotStore`

### Abstractions a clarifier
- coexistence de `IntegrationService` et `AsyncIntegrationService`
- `ZkBioIntegrationOperations.refreshAllAndPublishAsync()` vs `ZkBioIntegrationService.refreshAllAndPublishAsync()`

## 5. Packaging et lisibilite
### Constats
- `ZkBioServiceImpl` se trouve dans `service` et non `service.impl`
- `ZabbixClient` n'est pas range avec les autres clients
- packages vides encore presents

### Recommandation
Normaliser le packaging pour rendre les patterns plus visibles.
