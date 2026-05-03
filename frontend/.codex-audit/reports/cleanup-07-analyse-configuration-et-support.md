# Analyse configuration et support

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

`AsyncConfig`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/config/AsyncConfig.java` / `AsyncConfig`
- Categorie: A conserver tel quel
- Preuves: `@Bean(name = "taskExecutor")`, usage indirect par `@Async` sur `MonitoringStartup.warmupInitialSnapshots()` et `ZkBioIntegrationService.refreshAllAndPublish()`
- References trouvees: bean Spring + `@Async`
- References non trouvees: aucune reference statique supplementaire n'est necessaire pour un executor Spring
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: CONSERVER
- Pourquoi: configuration active par framework
- Action suggeree: aucune suppression ; seulement documenter le lien avec `@Async`
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

`RedisOptionalConfiguration` + `AppRedisProperties`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/config/RedisOptionalConfiguration.java`, `src/main/java/tn/iteam/config/AppRedisProperties.java`
- Categorie: A conserver tel quel
- Preuves: `@EnableConfigurationProperties(AppRedisProperties.class)`, beans conditionnels, tests dedies `RedisOptionalConfigurationContextTest`, properties explicites dans `application.properties`
- References trouvees: configuration Spring et tests
- References non trouvees: aucune consommation applicative directe requise
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: CONSERVER
- Pourquoi: infrastructure defensive volontaire, utile meme si Redis est desactive par defaut
- Action suggeree: aucune suppression ; seulement eviter de la classer a tort comme code mort
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

`MonitoringSnapshotPublicationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/support/MonitoringSnapshotPublicationService.java`
- Categorie: Service helper qui se chevauche
- Preuves: wrapper mince autour de `MonitoringWebSocketPublisher` et `ZkBioWebSocketPublisher`, utilise par `MonitoringStartup`, `MonitoringController`, `ZabbixScheduler`, `ObserviumScheduler`, `ZkBioScheduler`
- References trouvees: plusieurs points d'entree actifs
- References non trouvees: logique metier propre faible
- Niveau de confiance: moyen
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: utile aujourd'hui comme facade, mais pourrait etre absorbe dans un service de publication plus coherent si la couche integration est simplifiee
- Action suggeree: conserver dans la premiere phase ; revisiter apres factorisation des services d'integration
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

`MonitoringStartup`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/MonitoringStartup.java` / `MonitoringStartup`
- Categorie: Derive de packaging
- Preuves: `@EventListener(ApplicationReadyEvent.class)`, orchestration de warmup et publication initiale ; place dans le package racine `tn.iteam` au lieu d'un package bootstrap/startup/config
- References trouvees: activation Spring, dependances `IntegrationServiceRegistry`, `ZkBioRefreshOrchestrationService`, `MonitoringSnapshotPublicationService`
- References non trouvees: aucun motif fonctionnel pour rester au package racine
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: DEPLACER DE PACKAGE
- Pourquoi: bon code, mauvais emplacement
- Action suggeree: deplacer vers `tn.iteam.config.startup` ou `tn.iteam.bootstrap`
- Verification manuelle necessaire ? (oui/non): non
