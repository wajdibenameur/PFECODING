# RAPPORT FINAL NETTOYAGE

## Synthese
Le backend `tn.iteam` est vivant et coherent sur ses grands flux, mais il a accumule trois couches de dette nettes: 1) des endpoints legacy conserves pour compatibilite, 2) une duplication importante entre integrations source par source, 3) une derive de packaging qui melange `domain`, `dto`, `service`, `client`, `adapter`, `support`.

Le nettoyage sur le plus rentable est immediatement disponible sur les methodes mortes sans couplage Spring et sur les packages vides. Les suppressions de controllers legacy, elles, demandent une verification de trafic runtime car le frontend du depot ne les consomme plus, mais des consommateurs externes restent plausibles.

## Totaux analyses
- Classes concretes analysees: 122
- Interfaces analysees: 29
- Enums analyses: 5
- Records analyses: 6
- Methodes analysees statiquement: environ 537

## Meilleurs candidats SUPPRIMER MAINTENANT
- Helpers morts de `IntegrationClientSupport`
- Factories mortes `IntegrationDataUnavailableException.forZabbix(...)` et `forZkBio(...)`
- `ServiceStatusMapper.toDTO(...)`
- `ZabbixMonitoringMapper.toHost(...)`, `toHostFromServiceStatus(...)`, `toMetricFromDTO(...)`
- `ZabbixMetricsService*.synchronizeAndGetPersistedMetricsSnapshot(...)`
- `ZabbixProblemService*.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- Publications WebSocket directes non consommees dans `MonitoringWebSocketPublisher` et `ZkBioWebSocketPublisher`
- Packages vides `cache`, `listener`, `logging`, `monitoring/provider`

## Meilleurs candidats FUSIONNER
- Base commune des integration services source par source
- Persistence `Observium` / `ZKBio`
- Mappers de monitoring unifie
- `ZabbixSyncService` avec `MonitoredHostPersistenceServiceImpl`
- Construction du statut serveur ZKBio partagee entre `ZkBioServiceImpl` et `ZkBioAdapter`

## Packages / sous-packages vides
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`

## Fichiers a deplacer
- `domain/ApiResponse.java`
- `MonitoringStartup.java`
- `service/ZabbixSyncService.java`
- `service/ZkBioServiceImpl.java`
- `client/ObserviumClientX.java`
- `client/ZkBioClientX.java`
- `util/IntegrationClientSupport.java`
- `util/MonitoringConstants.java`

## Hotspots de couplage les plus graves
- `ZabbixAdapter`
- `ZabbixClient`
- `ZkBioIntegrationService`
- `DashboardServiceImpl`
- `TicketServiceImpl`

## Plus grandes opportunites de simplification d'architecture
- Uniformiser toutes les integrations sur le meme socle snapshot/fallback/publication.
- Uniformiser la persistence d'inventaire d'hotes entre Zabbix et les autres sources.
- Clarifier les frontieres `adapter/client/integration/service/support`.
- Faire sortir les wrappers HTTP du package `domain`.
- Supprimer les endpoints legacy apres validation de trafic externe.

## Fichiers generes
- `docs/mefied/cleanup-00-resume-executif.md`
- `docs/mefied/cleanup-01-cartographie-complete-des-usages.md`
- `docs/mefied/cleanup-02-candidats-code-mort.md`
- `docs/mefied/cleanup-03-candidats-duplication-et-fusion.md`
- `docs/mefied/cleanup-04-analyse-interfaces-implementations.md`
- `docs/mefied/cleanup-05-analyse-endpoints-et-workflows.md`
- `docs/mefied/cleanup-06-analyse-dto-wrappers-entites.md`
- `docs/mefied/cleanup-07-analyse-configuration-et-support.md`
- `docs/mefied/cleanup-08-hotspots-de-couplage.md`
- `docs/mefied/cleanup-09-plan-action-nettoyage.md`
- `docs/mefied/cleanup-10-checklist-suppression-sure.md`
- `docs/mefied/cleanup-11-packages-et-sous-packages-vides.md`
- `docs/mefied/cleanup-12-fichiers-a-deplacer-ou-repackager.md`
- `docs/mefied/RAPPORT_FINAL_NETTOYAGE.md`
