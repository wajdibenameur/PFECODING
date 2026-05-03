# Candidats code mort

## Candidats a forte confiance

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

`IntegrationClientSupport.httpOn(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/util/IntegrationClientSupport.java` / `IntegrationClientSupport` / `httpOn`
- Categorie: Methode non utilisee
- Preuves: recherche exacte `httpOn(` dans `src/main/java`, `src/test/java`
- References trouvees: declaration uniquement dans `IntegrationClientSupport.java:42`
- References non trouvees: aucun appel applicatif ou test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: methode utilitaire pure, sans activation Spring ni reflection
- Action suggeree: supprimer la methode et relancer la compilation
- Verification manuelle necessaire ? (oui/non): non

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

`IntegrationClientSupport.timeoutOn(...)`, `invalidJsonOn(...)`, `returnedHttpDuring(...)`, `stableFallbackReason(...)`, `transportErrorOn(...)`, `unexpectedErrorOn(...)`, `duringMessage(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/util/IntegrationClientSupport.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `timeoutOn(`, `invalidJsonOn(`, `returnedHttpDuring(`, `stableFallbackReason(`, `transportErrorOn(`, `unexpectedErrorOn(`, `duringMessage(`
- References trouvees: declaration uniquement dans la classe utilitaire
- References non trouvees: aucun appel main/test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: API utilitaire trop large, ces helpers ne participent a aucun workflow actif
- Action suggeree: supprimer les methodes mortes, conserver `mapTransportException(...)` et les helpers reellement consommes
- Verification manuelle necessaire ? (oui/non): non

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

`IntegrationDataUnavailableException.forZabbix(...)` et `IntegrationDataUnavailableException.forZkBio(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/exception/IntegrationDataUnavailableException.java`
- Categorie: Methodes non utilisees
- Preuves: recherche exacte `forZabbix(` et `forZkBio(`
- References trouvees: declaration uniquement dans la classe d'exception
- References non trouvees: aucun appel applicatif ; seul `forObservium(...)` est consomme par `ObserviumAdapter`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: factories inertes sans couplage runtime
- Action suggeree: supprimer les deux factories inutilisees
- Verification manuelle necessaire ? (oui/non): non

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

`ServiceStatusMapper.toDTO(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/ServiceStatusMapper.java` / `ServiceStatusMapper` / `toDTO`
- Categorie: Methode non utilisee
- Preuves: recherche exacte `statusMapper.toDTO(` et `toDTO(` sur le mapper
- References trouvees: declaration seule `ServiceStatusMapper.java:44`
- References non trouvees: aucun appel main/test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: le flux actif est uniquement DTO -> entity (`toEntity`, `updateEntity`)
- Action suggeree: supprimer `toDTO(...)`
- Verification manuelle necessaire ? (oui/non): non

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

`ZabbixMonitoringMapper.toHost(...)`, `toHostFromServiceStatus(...)`, `toMetricFromDTO(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/mapper/ZabbixMonitoringMapper.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `toHostFromServiceStatus(`, `toMetricFromDTO(` ; recherche de `toHost(` ne remonte aucune consommation du mapper Zabbix hors declaration
- References trouvees: declaration uniquement dans `ZabbixMonitoringMapper`
- References non trouvees: aucun appel depuis `ZabbixIntegrationService`, `MonitoringAggregationService` ou tests
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: seul `toProblem(...)` et `toMetric(ZabbixMetric)` participent aux snapshots actifs
- Action suggeree: supprimer ces trois methodes et laisser le mapper sur le sous-ensemble reellement utilise
- Verification manuelle necessaire ? (oui/non): non

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

`ZabbixMetricsService.synchronizeAndGetPersistedMetricsSnapshot(...)` et `ZabbixProblemService.synchronizeAndGetPersistedFilteredActiveProblems(...)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ZabbixMetricsService.java`, `src/main/java/tn/iteam/service/impl/ZabbixMetricsServiceImpl.java`, `src/main/java/tn/iteam/service/ZabbixProblemService.java`, `src/main/java/tn/iteam/service/impl/ZabbixProblemServiceImpl.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes des deux signatures
- References trouvees: interface + implementation uniquement
- References non trouvees: aucun controller, scheduler, integration service ou test ne les appelle
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: les workflows actifs utilisent `fetchAndSaveMetrics()`, `getPersistedMetricsSnapshot()`, `synchronizeActiveProblemsFromZabbix()` et `getPersistedFilteredActiveProblems()`
- Action suggeree: supprimer les signatures de l'interface et les implementations mortes
- Verification manuelle necessaire ? (oui/non): non

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

`MonitoringWebSocketPublisher.publishProblems(List<...>)` et `publishMetrics(List<...>)`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/websocket/MonitoringWebSocketPublisher.java`
- Categorie: Methodes non utilisees
- Preuves: recherche exacte `publishProblems(` et `publishMetrics(` hors declaration
- References trouvees: declaration seule ; les appels actifs visent `publishProblemsFromSnapshot(...)` et `publishMetricsFromSnapshot(...)`
- References non trouvees: aucun appel applicatif ni test
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: le projet a bascule vers publication depuis `SnapshotStore`
- Action suggeree: supprimer les methodes de publication directe et conserver les variantes snapshot
- Verification manuelle necessaire ? (oui/non): non

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

`ZkBioWebSocketPublisher.publishProblems(List<...>)`, `publishAttendance(List<...>)`, `publishDevices(List<?>)`, `publishStatus(Object)`, `publishProblemsFromSnapshot()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/websocket/ZkBioWebSocketPublisher.java`
- Categorie: Methodes non utilisees
- Preuves: recherches exactes `publishAttendance(`, `publishDevices(`, `publishStatus(`, `publishProblemsFromSnapshot()`
- References trouvees: declaration seule ; les appels actifs passent par `publishAttendanceFromSnapshot()`, `publishDevicesFromSnapshot()`, `publishStatusFromSnapshot()`
- References non trouvees: aucun appel applicatif ni test pour les variantes directes ni pour `publishProblemsFromSnapshot()`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: SUPPRIMER MAINTENANT
- Pourquoi: les snapshots ZKBio publies aujourd'hui sont attendance/device/status ; la publication directe des problems est orpheline
- Action suggeree: supprimer ces cinq methodes apres compilation locale
- Verification manuelle necessaire ? (oui/non): non
