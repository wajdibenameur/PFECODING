# Codex Ideas

## Ameliorations cache

- Ajouter une distinction explicite entre resultat sain, resultat degrade et resultat vide valide.
- Ajouter une invalidation ciblee du cache d'agregation apres certains cycles de collecte critiques.
- Ajouter des metadonnees de fraicheur dans les reponses unifiees pour exposer l'age du snapshot.
- Eviter de cacher des resultats partiels si un provider masque une erreur en retournant une liste vide.
- Ajouter des tests d'integration Redis plus proches des cas reels de panne et reprise.

## Securite (TLS ZKBio)

- Remplacer le WebClient TLS permissif ZKBio par une truststore dediee.
- Documenter la procedure d'installation du certificat ZKBio pour les environnements cible.
- Ajouter un flag de configuration explicite pour interdire le mode TLS permissif hors environnement interne.
- Ajouter une alerte visible au demarrage si le client ZKBio unsafe est actif.
- Prevoir a terme une rotation propre des certificats et une verification d'empreinte si necessaire.

## Monitoring avance (metrics, Grafana)

- Etendre les metrics unifiees au-dela de Zabbix quand Observium ou ZKBio peuvent fournir des donnees utiles.
- Ajouter des indicateurs techniques sur les appels externes: latence, erreurs, retries, circuit breaker open.
- Exposer plus proprement les metriques Resilience4j et Redis via Actuator / Prometheus.
- Connecter ces metriques a Grafana avec des dashboards dedies par source.
- Ajouter des vues Grafana pour distinguer live API, fallback Redis et indisponibilite complete.

## Multi-instance (lock distribue Redis)

- Ajouter un verrou distribue Redis pour eviter l'execution simultanee des schedulers en multi-instance.
- Limiter d'abord ce verrou aux collectes les plus sensibles ou couteuses.
- Definir des TTL de lock courts avec mecanisme de renouvellement prudent.
- Journaliser clairement l'acquisition, la perte et l'expiration des locks.
- Prevoir une strategie de degradation si Redis lock est indisponible mais que le service doit rester operationnel.

## Optimisations futures

- Harmoniser la semantique des endpoints monitoring unifies entre sources live, snapshots persistants et fallback Redis.
- Introduire un etat source `DEGRADED` si la lecture continue via Redis alors que la source live est KO.
- Revoir la coherence de fraicheur entre Zabbix, Observium et ZKBio dans l'agregation unifiee.
- Ajouter plus de tests cibles pour Zabbix et ZKBio en cas de panne API, panne Redis et reprise.
- Reduire les logs verbeux de payload en production tout en gardant des logs utiles pour la resilience.
