# Candidats duplication et fusion

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService`, `CameraIntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/*IntegrationService.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion ; Hotspot de couplage fort
- Preuves: memes blocs `refresh*`, `saveSnapshot`, `handleRefreshFailure`, `safeGetExistingSnapshot`, `safeLoadPersistedFallback`, `saveFallbackSnapshot`, `runStepAsync`, `subscribeSafely`, `safeMessage`, `toException`
- References trouvees: services actifs via `IntegrationServiceRegistry`, schedulers, `MonitoringStartup`, `MonitoringController`, `ZkBioController`
- References non trouvees: aucune abstraction commune pour le socle snapshot/fallback
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: duplication large mais critique ; une extraction partielle est sure si elle laisse chaque source maitriser uniquement `loadPersistedFallback` et ses datasets
- Action suggeree: introduire un support commun `AbstractSnapshotBackedIntegrationService` ou un helper de fallback/snapshot ; garder les specifics source par source en overrides
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumPersistenceServiceImpl` et `ZkBioPersistenceServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/ObserviumPersistenceServiceImpl.java`, `src/main/java/tn/iteam/service/impl/ZkBioPersistenceServiceImpl.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion
- Preuves: meme structure pour `saveProblems(...)`, meme cloture des problems actifs absents du flux live, meme strategy `find existing -> merge -> saveAll`, meme logic pour `saveMetrics(...)`
- References trouvees: services actifs appeles depuis `ObserviumIntegrationService` et `ZkBioIntegrationService`
- References non trouvees: aucune factorisation actuelle
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: la partie metrics est quasiment identique ; la partie problem differe legerement sur les champs (`severity`, `status`, `hostId`)
- Action suggeree: factoriser d'abord la persistence metrics, puis extraire un template pour la cloture des problems et le merge conditionnel
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ObserviumMonitoringMapper`, `ZkBioMonitoringMapper`, parties de `ZabbixMonitoringMapper`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/*MonitoringMapper.java`
- Categorie: Implementation quasi dupliquee ; Opportunite de fusion
- Preuves: meme pattern `source + hostId + itemId + timestamp -> UnifiedMonitoringMetricDTO`, meme pattern de construction `UnifiedMonitoringProblemDTO`, normalisation `UNKNOWN` / timestamps de fallback, duplication de `formatTimestamp(...)`
- References trouvees: mappers actifs consommes par les services d'integration
- References non trouvees: aucune base commune de mapping monitoring unifie
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: extraction partielle sure sur les builders communs `metric` et utilitaires `timestamp/normalize`
- Action suggeree: creer un support de mapping monitoring unifie par source, sans toucher tout de suite aux regles Zabbix specifiques
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`MonitoredHostPersistenceServiceImpl` et `ZabbixSyncService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/MonitoredHostPersistenceServiceImpl.java`, `src/main/java/tn/iteam/service/ZabbixSyncService.java`
- Categorie: Services/helpers qui se chevauchent ; Opportunite de fusion
- Preuves: les deux mettent a jour `MonitoredHostRepository` avec des regles proches de merge nom/ip/port ; `ZabbixSyncService.loadHostMap(JsonNode hosts)` reproduit un parcours source-specifique la ou Observium/ZKBio utilisent `MonitoredHostPersistenceService`
- References trouvees: `ZabbixIntegrationService.refreshHosts()` appelle `zabbixSyncService.loadHostMap()`, `ObserviumIntegrationService` et `ZkBioIntegrationService` appellent `MonitoredHostPersistenceService.saveAll(...)`
- References non trouvees: aucune convergence d'API entre le chemin Zabbix et les autres sources
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: FUSIONNER
- Pourquoi: c'est la divergence technique la plus nette entre Zabbix et les autres sources pour un besoin identique
- Action suggeree: extraire la persistence d'hotes Zabbix vers `MonitoredHostPersistenceService`, garder `ZabbixSyncService` uniquement pour le cache transitoire si vraiment necessaire
- Verification manuelle necessaire ? (oui/non): oui

- Candidat
- Fichier / Classe / Methode
- Categorie
- Preuves
- References trouvees
- References non trouvees
- Niveau de confiance
- Niveau de risque
- Recommandation
- Pourquoi
- Action suggeree
- Verification manuelle necessaire ? (oui/non)

`ZkBioServiceImpl.getServerStatus()` et `ZkBioAdapter.baseServerStatus()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ZkBioServiceImpl.java`, `src/main/java/tn/iteam/adapter/zkbio/ZkBioAdapter.java`
- Categorie: Implementation quasi dupliquee
- Preuves: les deux reconstruisent `ServiceStatusDTO` a partir de `ZkBioClientX.getBaseUri()` et de regles `scheme -> port/protocol`
- References trouvees: `ZkBioServiceImpl.getServerStatus()` alimente le controller ; `ZkBioAdapter.baseServerStatus()` alimente `fetchAll()` et `fetchMetrics()`
- References non trouvees: aucune methode commune partagee
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: EXTRAIRE LOGIQUE COMMUNE
- Pourquoi: petite duplication mais repetee sur un point sensible (URL, port, protocol, nom serveur)
- Action suggeree: extraire un `ZkBioStatusFactory` ou une methode commune dans l'adapter/service apres arbitrage d'ownership
- Verification manuelle necessaire ? (oui/non): non
