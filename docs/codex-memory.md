# Memoire Codex

## Statut du document
Ce fichier conserve la memoire technique durable du projet. Il doit refleter l'etat courant du code reel et non les anciennes hypotheses d'architecture.

## Regle de lecture prioritaire
- les sections ci-dessous sont l'interpretation de reference
- si un ancien document ou une ancienne note mentionne des classes ou flux absents du code actuel, il faut considerer cette information comme historique et non normative
- le dossier `docs/uml-memory/2` constitue maintenant la synthese architecturale detaillee la plus a jour

## Architecture backend de reference

### Noyau monitoring
Le backend est centre sur une architecture de supervision unifiee composee de:

- `integration/*`
- `SnapshotStore`
- `InMemorySnapshotStore`
- `MonitoringAggregationService`
- `MonitoringCacheService`
- `MonitoringStartup`
- les schedulers par source
- les publishers WebSocket

Le flux principal de lecture unifiee est:

1. `MonitoringController`
2. `MonitoringAggregationService`
3. `MonitoringCacheService`
4. `SnapshotStore`

Le flux principal de collecte est:

1. service d'integration par source
2. persistence / reconstruction eventuelle
3. ecriture du snapshot
4. marquage de disponibilite source
5. publication WebSocket apres completion

### Integrations actives
Les integrations actives et coherentes avec le code courant sont:

- `ZabbixIntegrationService`
- `ObserviumIntegrationService`
- `ZkBioIntegrationService`
- `CameraIntegrationService`

Leur resolution se fait par:

- `AsyncIntegrationService`
- `IntegrationServiceRegistry`

### Orchestration ZKBio
La regle de sequencing ZKBio est explicite:

1. refresh monitoring ZKBio
2. refresh attendance

Cette regle est portee par:

- `ZkBioRefreshOrchestrationService`

Elle est utilisee dans:

- `MonitoringStartup`
- `MonitoringController.collectAll()`

### Publication
La publication temps reel est centralisee dans:

- `MonitoringSnapshotPublicationService`
- `MonitoringWebSocketPublisher`
- `ZkBioWebSocketPublisher`

`MonitoringSnapshotPublicationService` doit rester un delegateur de publication uniquement:

- pas d'acces repository
- pas de logique de fallback
- pas de routage metier complexe

## Regles de decouplage de reference
- eviter l'injection de classes concretes proxifiees dans les controleurs, schedulers et orchestrateurs
- privilegier les interfaces ou abstractions source-specific
- garder `IntegrationServiceRegistry` comme resolver simple
- garder les services de support legers

## Regles Spring / proxy
- `@EnableAsync(proxyTargetClass = true)` est actif dans `PfeprojectApplication`
- cette option reste un garde-fou utile
- le chemin critique d'orchestration ne doit toutefois plus dependre d'injections par classe concrete

## Resilience de reference

### Snapshots et fallback
La logique metier attendue est:

- tentative live
- fallback snapshot memoire si disponible
- fallback persiste si disponible
- resultat vide en dernier recours

### Redis
Le code metier courant n'utilise pas Redis comme socle principal de monitoring.

Points de reference:
- `InMemorySnapshotStore` est la base sure
- Redis est optionnel
- `app.redis.enabled=true` est necessaire pour activer la voie Redis
- Redis ne doit pas redevenir le store principal obligatoire

### Resilience4j
Des configurations explicites existent maintenant pour:

- `zabbixApiLight`
- `zabbixApiHeavy`
- `observiumApi`
- `zkbioApi`

Intentions a respecter:
- retry borne pour erreurs transitoires uniquement
- circuit breaker au boundary client
- cooldown scheduler via `SourceAvailabilityService.shouldAttempt(...)`

### Specificite Zabbix
Zabbix distingue deux classes d'appels:

- appels legers
- appels lourds de type items/history/metrics

Les appels lourds:
- utilisent le profil `zabbixApiHeavy`
- ont un timeout long dedie
- ne doivent pas etre fortement retried
- ne doivent pas se recouvrir pendant une collecte lourde deja en cours

## Frontend reel a prendre comme reference
Le frontend Angular courant lit principalement:

- `GET /api/monitoring/hosts`
- `GET /api/monitoring/problems`
- `GET /api/monitoring/metrics`
- `GET /api/monitoring/sources/health`
- `GET /api/zkbio/*` pour les vues metier ZKBio
- `GET /dashboard/*` pour les vues analytiques

Les endpoints suivants existent encore mais sont a considerer comme compatibilite ou support externe:

- `/api/zabbix/active`
- `/api/zabbix/metrics`
- `/api/observium/summary`

## Ticketing et securite
Le module ticketing est actif et coherent.

Le modele suivant existe:

- `User`
- `Role`
- `Permission`
- `RoleName`

Mais aucune securite Spring effective n'est actuellement appliquee:

- pas de `SecurityFilterChain`
- pas de `@PreAuthorize`
- pas de `UserDetailsService`

Conclusion:
- les roles sont modeles et bootstrapes
- ils ne constituent pas une autorisation runtime enforcee

## Zones de dette connues
- duplication de logique de fallback entre services d'integration
- `ZabbixAdapter` reste le principal hotspot de couplage
- `ZkBioIntegrationService` reste plus large que les autres integrations
- persistance de controleurs de compatibilite legacy
- derives de packaging (`ZkBioServiceImpl`, `ZabbixClient`)

## Informations explicitement obsoletes a ne plus prendre comme reference
Toute mention centrale des elements suivants doit etre consideree comme historique si elle reapparait dans d'anciens textes:

- `MonitoringProvider`
- `MonitoringServiceImpl`
- `ZabbixMonitoringService`
- `ObserviumMonitoringService`
- `ZkBioMonitoringService`
- `ZabbixLiveSynchronizationServiceImpl`
- `tn.iteam.client.ZkBioClient`
- `tn.iteam.cache.IntegrationCacheService`
- `RedisIntegrationCacheService` comme pilier principal du monitoring
- `monitoring.provider.*` comme chemin actif de lecture unifiee

## Source detaillee associee
Pour l'analyse exhaustive:

- `docs/uml-memory/2/00-overview.md`
- `docs/uml-memory/2/02-class-method-inventory.md`
- `docs/uml-memory/2/09-dead-code-redundancy-analysis.md`
- `docs/uml-memory/2/FINAL_ARCHITECTURE_REPORT.md`
