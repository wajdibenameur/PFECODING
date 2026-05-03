# Analyse interfaces / implementations

## Interfaces a conserver
- `AsyncIntegrationService`: vraie valeur polymorphique. C'est l'API branchee dans `IntegrationServiceRegistry` et consommee par `MonitoringStartup`, `MonitoringController`, `ZabbixScheduler`, `ObserviumScheduler`, `ObserviumHostsScheduler`.
- `ZkBioIntegrationOperations`: utile car `ZkBioScheduler`, `ZkBioController` et `ZkBioRefreshOrchestrationService` exploitent a la fois les variantes sync et async, y compris `refreshAttendanceAsync()` et `refreshAllAndPublishAsync()`.
- Les interfaces Spring Data `*Repository`: a conserver ; elles sont activees par JPA meme avec peu de references directes.

## Interfaces a valeur faible ou triviale

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

`IntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/IntegrationService.java` / `IntegrationService`
- Categorie: Interface non utilisee ; Opportunite de simplification
- Preuves: seule reference statique hors declaration = `AsyncIntegrationService extends IntegrationService`; aucune injection, aucun consommateur direct, aucune recherche par type `IntegrationService`
- References trouvees: `AsyncIntegrationService`
- References non trouvees: aucun controller, scheduler, service ou test ne depend de `IntegrationService`
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: FUSIONNER
- Pourquoi: l'abstraction utile du projet est `AsyncIntegrationService`, pas le parent synchrone
- Action suggeree: soit supprimer `IntegrationService` et remonter les defaults utiles dans `AsyncIntegrationService`, soit documenter clairement pourquoi garder deux niveaux
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

`ObserviumSummaryService` / `ObserviumSummaryServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/ObserviumSummaryService.java`, `src/main/java/tn/iteam/service/impl/ObserviumSummaryServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; Classe utilisee une seule fois ; Opportunite de fusion
- Preuves: un seul consommateur `ObserviumController`; implementation unique ; logique simple basee sur `MonitoringAggregationService`
- References trouvees: `ObserviumController.getSummary()`
- References non trouvees: aucun autre module, aucun test, aucun wiring alternatif
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: FUSIONNER
- Pourquoi: abstraction mince sans variation d'implementation ; le vrai service coeur est deja `MonitoringAggregationService`
- Action suggeree: soit deplacer la logique dans le controller de compatibilite, soit l'integrer a `MonitoringAggregationService` comme vue derivee Observium
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

`CameraInventoryService` / `CameraInventoryServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/CameraInventoryService.java`, `src/main/java/tn/iteam/service/impl/CameraInventoryServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; Classe utilisee une seule fois
- Preuves: un seul consommateur `CameraController`; implementation unique ; logique = lecture repository + projection DTO
- References trouvees: `CameraController.getRegisteredCameras()`
- References non trouvees: aucun autre service, aucun test dedie, aucune autre implementation
- Niveau de confiance: eleve
- Niveau de risque: moyen-faible
- Recommandation: FUSIONNER
- Pourquoi: service tres fin ; peut vivre dans un composant plus proche du module camera
- Action suggeree: deplacer pres du module camera ou absorber dans un query service plus local au controller
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

`DashboardService` / `DashboardServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/DashboardService.java`, `src/main/java/tn/iteam/service/impl/DashboardServiceImpl.java`
- Categorie: Interface a valeur polymorphique faible ; God class / service trop gros
- Preuves: un seul consommateur `DashboardController`; implementation unique ; concentre overview + predictions + anomalies + data quality + ML
- References trouvees: `DashboardController`
- References non trouvees: aucun autre consommateur par interface
- Niveau de confiance: moyen
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: l'interface seule n'apporte pas beaucoup ; le vrai sujet est de decouper `DashboardServiceImpl`
- Action suggeree: avant de supprimer l'interface, extraire `DashboardPredictionService` et `DashboardAnomalyService`
- Verification manuelle necessaire ? (oui/non): oui
