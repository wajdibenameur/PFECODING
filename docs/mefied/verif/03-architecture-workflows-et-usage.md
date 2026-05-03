# Architecture, Workflows et Usage Reel

## Architecture backend confirmee
Le backend suit une architecture en couches avec noyau de monitoring unifie:

1. `controller`
2. `integration` et `service`
3. `adapter` et `client`
4. `repository`
5. `mapper` et `dto`
6. `scheduler`
7. `monitoring.snapshot`
8. `websocket`
9. `ml`

## Architecture frontend confirmee
Le frontend suit une structure Angular en domaines:

1. `core`
2. `layout`
3. `features/monitoring`
4. `features/tickets`
5. `shared`

Particularites:
- architecture standalone Angular
- etat principalement porte par `signals`
- temps reel via STOMP/SockJS
- un store transversal global et un store dedie Zabbix

## Workflow principal monitoring
### Flux nominal
1. un scheduler ou un endpoint manuel declenche la collecte
2. le service d'integration source-specific interroge l'adapter ou le client
3. les donnees sont mappees vers le modele unifie
4. un snapshot memoire est ecrit
5. une persistance base peut etre tentee
6. la disponibilite source est mise a jour
7. la publication WebSocket alimente le frontend

### Flux de degradation
1. echec live
2. tentative de repli sur snapshot memoire existant
3. pour certaines sources, tentative de fallback persiste
4. sinon snapshot vide marque degrade ou indisponible

## Workflows metier confirmes
### Monitoring unifie
- entree API: `MonitoringController`
- aggregation lecture: `MonitoringAggregationService`
- publication: `MonitoringSnapshotPublicationService`
- consommation frontend: `MonitoringApiService`, `MonitoringRealtimeService`, `MonitoringStore`

### Zabbix workspace analytique
- donnees de base: `/api/monitoring/problems`, `/api/monitoring/metrics`
- donnees dashboard: `/dashboard/overview`, `/dashboard/predictions`, `/dashboard/anomalies`
- consommation frontend: `ZabbixWorkspaceStore`

### ZKBio
- monitoring unifie via `ZkBioIntegrationService`
- attendance et devices via endpoints ZKBio specifiques
- publication complementaire via `ZkBioWebSocketPublisher`

### Ticketing
- entree: `TicketController`
- orchestration: `TicketServiceImpl`
- consommation frontend: `TicketManagerApiService`
- parcours UI:
  - liste
  - creation
  - suivi

## Endpoints non consommes par le frontend du depot
- `GET /api/observium/summary`
- `GET /api/zabbix/active`
- `GET /api/zabbix/metrics`
- `POST /predict`
- `GET /api/zkbio/problems`
- `GET /api/zkbio/attendance/range`
- `GET /api/zkbio/users`

Interpretation:
- soit compatibilite externe
- soit dette documentaire ou technique
- soit reste de transition architecturale

## Points d'architecture a retenir pour le PFE
- le coeur du projet n'est plus un ensemble d'ecrans isoles, mais une chaine de supervision multi-sources unifiee
- la resilience est un element central du design
- le frontend n'est pas un simple consommateur CRUD, il assemble snapshots, temps reel et vues analytiques
- la partie Zabbix est la plus riche techniquement, mais aussi la plus complexe et la plus couplee

## Ecarts d'architecture entre intention et realite
- la securite role/permission existe surtout comme modele et beaucoup moins comme enforcement runtime
- l'architecture cible unifiee coexiste encore avec des endpoints legacy
- le frontend est partiellement store-driven seulement; Observium et certaines pages gardent encore de la logique embarquee dans les composants
