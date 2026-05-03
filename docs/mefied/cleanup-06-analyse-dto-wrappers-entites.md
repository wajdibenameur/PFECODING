# Analyse DTO, wrappers et entites

## DTO / wrappers dormants ou a clarifier

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

`tn.iteam.domain.ApiResponse`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/domain/ApiResponse.java` / `ApiResponse`
- Categorie: Derive de packaging
- Preuves: wrapper HTTP utilise par controllers et clients (`MonitoringController`, `TorchScriptPredictionController`, `ObserviumClientX`) mais place dans `domain`
- References trouvees: flux web et integration
- References non trouvees: aucun sens metier JPA/domain model
- Niveau de confiance: eleve
- Niveau de risque: faible
- Recommandation: DEPLACER DE PACKAGE
- Pourquoi: c'est un contrat d'API, pas un aggregate metier
- Action suggeree: deplacer vers `tn.iteam.dto` ou `tn.iteam.web.dto`
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

`Permission`, `RoleName`, `Role.permissions`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/enums/Permission.java`, `src/main/java/tn/iteam/enums/RoleName.java`, `src/main/java/tn/iteam/domain/Role.java`
- Categorie: Fonctionnalite dormante
- Preuves: `pom.xml` ne declare pas `spring-boot-starter-security`; aucune `SecurityFilterChain`, `@EnableWebSecurity`, `@PreAuthorize`, `PasswordEncoder`, `UserDetailsService` ou `GrantedAuthority`; seules traces runtime = bootstrap des roles/utilisateurs et relations du ticketing
- References trouvees: `TicketingBootstrapConfiguration`, `RoleRepository`, `UserRepository`, relations `Ticket -> User`
- References non trouvees: aucun enforcement d'autorisation basee sur `Permission`
- Niveau de confiance: eleve
- Niveau de risque: eleve
- Recommandation: VERIFIER A L'EXECUTION
- Pourquoi: le modele peut etre prevu pour une phase future, mais il n'est pas branche a la securite reelle aujourd'hui
- Action suggeree: documenter le statut "modele dormant" ; ne pas supprimer avant arbitrage fonctionnel
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

`ObserviumSummaryServiceImpl` retournant `Map<String, Long>`
- Fichier / Classe / Methode: `src/main/java/tn/iteam/service/impl/ObserviumSummaryServiceImpl.java` / `getSummary`
- Categorie: Wrapper a semantique qui se chevauche
- Preuves: la reponse est un `Map<String, Long>` derive des DTO unifies au lieu d'un DTO dedie ; le dashboard unifie et le monitoring unifie portent deja des vues similaires
- References trouvees: `ObserviumController`
- References non trouvees: aucune reutilisation transversale
- Niveau de confiance: eleve
- Niveau de risque: moyen
- Recommandation: REFACTORER
- Pourquoi: contrat faiblement type, endpoint de compatibilite, semantique deja presente dans le monitoring unifie
- Action suggeree: soit supprimer l'endpoint apres migration, soit introduire un DTO explicite si l'endpoint doit vivre
- Verification manuelle necessaire ? (oui/non): oui

## DTO non utilises nulle part
- Aucun DTO entierement orphelin n'a ete trouve sous `tn.iteam.dto`, `tn.iteam.ml.dto` ou `tn.iteam.monitoring.dto`.
- En revanche, plusieurs DTO sont aujourd'hui exposes seulement via des endpoints non consommes par le frontend du depot: `ZabbixProblemDTO`, `ZabbixMetricDTO`, `ZkBioProblemDTO`, `ZkBioAttendanceDTO` sur certains chemins legacy ou specifiques.

## Entites reellement connectees aux workflows actifs
- Actives: `Ticket`, `Intervention`, `User`, `Role`, `ServiceStatus`, `MonitoredHost`, `ZabbixProblem`, `ZabbixMetric`, `ObserviumProblem`, `ObserviumMetric`, `ZkBioProblem`, `ZkBioMetric`
- Partiellement dormantes: la partie `Role.permissions` / `Permission` / `RoleName` n'est pas enforcee runtime
