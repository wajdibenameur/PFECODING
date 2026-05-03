# Hotspots de couplage

## Hotspots les plus graves

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

`ZabbixAdapter`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/adapter/zabbix/ZabbixAdapter.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 769 lignes ; depend de `ZabbixSyncService` + `ZabbixClient`; gere hosts, problems, metrics, batching, fallback history, normalisation et mapping DTO
- References trouvees: appele par `ZabbixIntegrationService`, `ZabbixMetricsServiceImpl`, `ZabbixProblemServiceImpl`
- References non trouvees: aucune vraie decomposition interne par responsabilite
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: cette classe bloque la simplification de la pile Zabbix
- Action suggeree: decouper en `ZabbixHostGateway`, `ZabbixProblemGateway`, `ZabbixMetricGateway` ou equivalent ; conserver `ZabbixClient` comme transport pur
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

`ZabbixClient`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/adapter/zabbix/ZabbixClient.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 643 lignes ; melange construction JSON-RPC, transport WebClient, mapping d'erreurs, timeouts light/heavy, fallback Resilience4j, endpoints multiples Zabbix
- References trouvees: appele par `ZabbixAdapter`, `ZabbixSyncService`, tests resiliency
- References non trouvees: aucune couche de request builder separee
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: forte volatilite technique dans une seule classe
- Action suggeree: extraire un `ZabbixRequestFactory` et un `ZabbixResponseDecoder`, garder le client sur la couche transport
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

`ZkBioIntegrationService`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/integration/ZkBioIntegrationService.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 333 lignes ; 13 dependances finales ; gere refresh hosts/problems/metrics/attendance/status/devices + publication websocket + fallback snapshots + availability
- References trouvees: `ZkBioController`, `ZkBioScheduler`, `ZkBioRefreshOrchestrationService`
- References non trouvees: aucune separation nette entre orchestration, persistence et publication
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: cas le plus couple hors pile Zabbix
- Action suggeree: extraire au minimum `ZkBioSnapshotRefreshService` et `ZkBioRawDatasetRefreshService`
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

`DashboardServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/DashboardServiceImpl.java`
- Categorie: Hotspot de couplage fort ; God class / service trop gros
- Preuves: 260 lignes ; 5 dependances ; combine overview, data quality, prediction ML, anomaly detection, queries repository
- References trouvees: `DashboardController`
- References non trouvees: aucune decomposition prediction/anomaly/data-quality
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: logique metier analytique et logique ML melangees
- Action suggeree: extraire `DashboardPredictionService` et `DashboardAnomalyService` avant tout gros changement fonctionnel
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

`TicketServiceImpl`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/TicketServiceImpl.java`
- Categorie: Hotspot de couplage fort
- Preuves: 336 lignes ; gere workflow metier, transitions, persistence, creation d'intervention et emission WebSocket dans la meme classe
- References trouvees: `TicketController`
- References non trouvees: aucune couche dediee pour transitions ou notifications
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: le service reste actif et central, mais il melange orchestration et details bas niveau
- Action suggeree: extraire `TicketWorkflowService` + `TicketNotificationPublisher` ou equivalent minimal
- Verification manuelle necessaire ? (oui/non): oui
