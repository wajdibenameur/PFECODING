# Analyse endpoints et workflows

## Endpoints actifs cotes frontend du depot
- `GET /api/monitoring/hosts`, `GET /api/monitoring/problems`, `GET /api/monitoring/metrics`, `GET /api/monitoring/sources/health`, `POST /api/monitoring/collect*`
- `GET /api/cameras`
- `GET /api/zkbio/status`, `GET /api/zkbio/devices`, `GET /api/zkbio/attendance`, `POST /api/zkbio/collect`
- `GET /dashboard/overview`, `GET /dashboard/predictions`, `GET /dashboard/anomalies`
- `GET/POST/PUT/DELETE /api/tickets/*`

## Endpoints non consommes par le frontend du depot

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

`ObserviumController` / `GET /api/observium/summary`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ObserviumController.java` / `ObserviumController` / `getSummary`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Classe utilisee une seule fois
- Preuves: commentaire de classe parlant explicitement de compatibilite temporaire ; absence d'appel dans `frontend/src`; resume derive du flux unifie
- References trouvees: bean `@RestController`, injection `ObserviumSummaryService`
- References non trouvees: aucune consommation frontend du depot de `/api/observium/summary`
- Niveau de confiance: eleve
- Niveau de risque: moyen-eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: probablement obsolete dans le depot, mais possiblement encore consomme par un client externe
- Action suggeree: journaliser les hits ou mettre l'endpoint en deprecation fonctionnelle avant suppression
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

`ZabbixMetricsController` / `GET /api/zabbix/metrics`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZabbixMetricsController.java` / `ZabbixMetricsController` / `getMetrics`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Endpoint supplanter par un autre
- Preuves: `@Deprecated(since = "2026-04-22")`, commentaire de compatibilite, absence d'usage dans `frontend/src`, donnees deja derivees de `/api/monitoring/metrics`
- References trouvees: bean `@RestController`
- References non trouvees: aucun appel frontend du depot vers `/api/zabbix/metrics`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: supplanté dans le depot, mais endpoint historique potentiellement expose a l'exterieur
- Action suggeree: mesurer les hits HTTP puis supprimer apres migration si zero trafic
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

`ZabbixProblemController` / `GET /api/zabbix/active`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZabbixProblemController.java` / `ZabbixProblemController` / `allActive`
- Categorie: Fonctionnalite dormante ; Endpoint de compatibilite ; Endpoint supplanter par un autre
- Preuves: `@Deprecated(since = "2026-04-22")`, commentaire de compatibilite, absence d'usage dans `frontend/src`, remplacement evident par `/api/monitoring/problems`
- References trouvees: bean `@RestController`
- References non trouvees: aucun appel frontend du depot vers `/api/zabbix/active`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: meme situation que le controller metrics
- Action suggeree: deprecier activement puis supprimer apres validation trafic
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

`TorchScriptPredictionController` / `POST /predict`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/ml/controller/TorchScriptPredictionController.java` / `TorchScriptPredictionController` / `predict`
- Categorie: Fonctionnalite dormante
- Preuves: aucun appel detecte a `/predict` dans `frontend/src`; la fonctionnalite ML consommee par le frontend passe par `DashboardServiceImpl -> TorchScriptPredictionService`
- References trouvees: bean `@RestController`, wiring vers `TorchScriptPredictionService`
- References non trouvees: aucun consommateur interne du depot
- Niveau de confiance: moyen
- Niveau de risque: moyen-eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: endpoint potentiellement reserve a des appels manuels ou externes hors repo
- Action suggeree: mesurer le trafic ; si zero, retirer le controller et exposer seulement le dashboard agrege
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

`ZkBioController.getProblems()`, `getAttendanceLogsByRange()`, `getUsers()`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/controller/ZkBioController.java`
- Categorie: Fonctionnalite dormante ; Methodes possiblement non utilisees dans une classe active
- Preuves: absence d'appels frontend du depot vers `/api/zkbio/problems`, `/api/zkbio/attendance/range`, `/api/zkbio/users`; les problemes ZKBio visibles du frontend passent deja par le monitoring unifie
- References trouvees: exposition HTTP Spring
- References non trouvees: aucune consommation frontend locale
- Niveau de confiance: moyen
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: ces routes peuvent encore etre appelees par des outils externes ou des usages backoffice
- Action suggeree: tracer les hits puis archiver les routes sans trafic
- Verification manuelle necessaire ? (oui/non): oui

## Chevauchements clairs d'API
- `GET /api/zabbix/metrics` est supplanté par `GET /api/monitoring/metrics` + filtrage source `ZABBIX`.
- `GET /api/zabbix/active` est supplanté par `GET /api/monitoring/problems` + filtrage source `ZABBIX`.
- `GET /api/observium/summary` est un derive simplifie des flux `GET /api/monitoring/hosts` et `GET /api/monitoring/problems` pour `OBSERVIUM`.
- `POST /api/zkbio/collect` chevauche partiellement `POST /api/monitoring/collect` et `POST /api/monitoring/collect/zkbio`, avec un enrichissement attendance/device/status propre a ZKBio.
