# Suivi PFE

## 2026-04-23 - Faible couplage registre/publication confirme

### Statut

- Verification et leger nettoyage de la couche d'orchestration termines.
- Documentation re-alignee avec le code refactorise.

### Etat reel confirme

- `IntegrationServiceRegistry` sert uniquement a resoudre un `AsyncIntegrationService` par `MonitoringSourceType`.
- `MonitoringSnapshotPublicationService` sert uniquement a centraliser les appels de publication WebSocket.
- `MonitoringController`, `MonitoringStartup` et les schedulers utilisent maintenant ces abstractions au lieu d'injecter directement les integrations concretes.

### Precision importante

- `@EnableAsync(proxyTargetClass = true)` reste present, mais il ne doit plus etre documente comme la solution principale a un couplage concret dans l'orchestration monitoring.
- Le chemin critique actuel est desormais base sur interfaces et registre de resolution.

## 2026-04-22 - Alignement docs et correction du blocage startup async

### Statut

- Erreur bloquante de demarrage backend corrigee.
- Documentation `docs/` re-alignee sur le code reel.

### Correction backend appliquee

- Le demarrage etait bloque par un proxy JDK autour de `ZkBioIntegrationService` alors que plusieurs composants injectent la classe concrete.
- La correction retenue est :
  - `@EnableAsync(proxyTargetClass = true)` dans `PfeprojectApplication`
  - suppression du doublon `@EnableAsync` dans `AsyncConfig`

### Verification

1. `mvn -q -DskipTests compile`
   - Resultat : OK
2. `mvn -q "-Dtest=MonitoringRuntimeIsolationTest,MonitoringControllerWebMvcTest" test`
   - Resultat : OK

### Ecarts de documentation corriges

- Le frontend Angular courant n'utilise plus `/api/zabbix/active` ni `/api/zabbix/metrics`.
- Les endpoints `/api/zabbix/*` et `/api/observium/summary` restent documentes comme compatibilite legacy ou consommateurs externes.
- La stack backend documentee est alignee sur :
  - `integration/*`
  - `SnapshotStore`
  - `MonitoringAggregationService`
  - `MonitoringStartup`
  - schedulers par source
- Les design patterns reellement utilises sont maintenant explicitement listes dans `docs/pfe-suivi`.

## 2026-04-21 - Verrouillage Redis optionnel et preuves par tests

### Statut

- Refactor de configuration Redis optionnelle confirme.
- Redis documente comme auxiliaire technique et non comme stockage principal.
- Suite de tests ajoutee et validee.

### Etat actuel documente

- Le flux actif du monitoring backend repose sur :
  - `SnapshotStore`
  - `InMemorySnapshotStore`
  - `MonitoringCacheService`
  - `MonitoringAggregationService`
  - services d'integration
  - publishers WebSocket
- Redis n'est pas utilise dans le code metier actif.
- Le risque reel etait surtout :
  - `spring-boot-starter-data-redis`
  - l'auto-configuration Spring Redis
  - l'indicateur Actuator Redis

### Garde-fous appliques

- `InMemorySnapshotStore` reste le fallback garanti et l'implementation par defaut.
- Redis n'est activable que par propriete explicite `app.redis.enabled=true`.
- Les beans Redis sont crees par `RedisOptionalConfiguration` uniquement si Redis est explicitement active.
- L'auto-configuration Redis Spring Boot est exclue.
- `management.health.redis.enabled=false` evite qu'un Redis down fasse croire que toute l'application est down.
- `spring.cache.type=simple` evite toute ambiguite de cache Redis implicite.

### Tests ajoutes, commandes, resultats et utilite

1. `mvn -q "-Dtest=RedisOptionalConfigurationContextTest,MonitoringCacheServiceInMemoryTest,IntegrationServicesWithoutRedisTest,MonitoringRuntimeIsolationTest" test`
   - Resultat : OK
   - Utilite : validation ciblee du comportement Redis optionnel

2. `mvn -q test`
   - Resultat : OK
   - Utilite : validation complete de la suite backend actuelle apres le refactor

### Classes de test ajoutees

- `RedisOptionalConfigurationContextTest`
  - prouve le demarrage sans Redis
  - prouve le demarrage avec Redis desactive par propriete
  - prouve le demarrage avec Redis active mais inaccessible
  - prouve que `SnapshotStore` reste `InMemorySnapshotStore`
- `MonitoringCacheServiceInMemoryTest`
  - prouve que l'agregation snapshot fonctionne avec la memoire seule
- `IntegrationServicesWithoutRedisTest`
  - prouve que Zabbix, Observium, ZKBio et Camera continuent a alimenter les snapshots sans Redis
- `MonitoringRuntimeIsolationTest`
  - prouve l'absence d'echec fatal au niveau startup warmup, schedulers, publishers et controller de collecte

### Logique boot fallback + runtime fallback

- Boot fallback :
  - Redis absent ou desactive -> l'application demarre quand meme
  - aucun bean Redis n'est requis
  - `InMemorySnapshotStore` continue seul
- Runtime fallback :
  - Redis active mais inaccessible -> l'application survit
  - le monitoring continue via les snapshots memoire
  - aucun composant metier actif ne depend d'un appel Redis reussi

### Politique health / actuator

- Redis est traite comme optionnel
- l'indicateur Redis global est desactive
- la sante globale reste centree sur les composants critiques reels

### Avant / Apres / Raison

| Avant | Apres | Raison |
|---|---|---|
| Redis pouvait encore etre percu comme une dependance transverse potentielle | Redis est explicitement optionnel | Eviter tout blocage implicite au demarrage |
| La doc melangeait parfois Redis fallback historique et flux actif | La doc distingue clairement flux actif memoire et Redis futur optionnel | Aligner la documentation sur le code reel |
| Pas de preuve automatisee complete sur la survie sans Redis | Tests de contexte, services et runtime ajoutes | Prouver noir sur blanc le comportement |
| Health Redis pouvait etre interpretee comme critique | Redis sorti de la sante globale | Redis est auxiliaire, pas vital |

## 2026-04-21 - Audit architecture monitoring

### Statut

- Etat enregistre
- Aucun refactor applique
- Aucun fichier supprime
- Aucun nettoyage execute
- Build actuel non vert

### Resume structure de l'etat actuel

- Le flux principal monitoring est bien recentre sur `integration/* + SnapshotStore + MonitoringAggregationService`.
- `MonitoringWebSocketPublisher` reste dans le chemin principal cible.
- Zabbix est globalement propre par rapport a l'architecture cible.
- Observium est globalement propre par rapport a l'architecture cible.
- ZKBio reste hybride et bloque actuellement la coherence globale.
- `ZkBioAdapter` et `ZkBioServiceImpl` referencent encore `tn.iteam.client.ZkBioClient` supprime.
- `ZkBioClientX` depend encore de `tn.iteam.cache.IntegrationCacheService`, couche deja retiree.
- Verification constatee: `mvn -q -DskipTests compile` echoue dans l'etat actuel.

### Ecarts architecture cible vs realite

- Cible: une seule pile active par source.
  Realite: ZKBio est encore partage entre une pile active incomplete et des restes de l'ancienne pile.
- Cible: aucun flux legacy compile dans le chemin principal.
  Realite: des dependances ZKBio actives pointent encore vers des concepts deja retires.
- Cible: build Maven vert.
  Realite: le build est rouge a cause de l'etat hybride ZKBio.

### Actions prioritaires

1. Corriger ZKBio avant tout autre nettoyage.
2. Reconstituer une seule pile active coherente pour ZKBio.
3. Retablir un build vert avec `mvn -q -DskipTests compile`.
4. Ensuite seulement rationaliser les archives.
5. Ensuite seulement supprimer les wrappers inutiles.
6. Ensuite seulement decider du sort des controllers de compatibilite.

### Note critique

DO NOT CLEAN EVERYTHING FIRST - FIX ZKBIO BUILD FIRST

### Portee de cette entree

- Cette note sert de base de travail.
- Elle enregistre l'etat reel du projet a date.
- Elle ne valide aucun refactor automatique.

## 2026-04-21 - Refactor frontend aligne sur le backend reel

### Statut

- Audit d'ecart documentation/backend/frontend effectue.
- Refactor frontend applique dans `frontend`.
- Build frontend valide avec `npm run build`.
- Backend revalide avec `mvn -q -DskipTests compile`.

### Ecarts confirmes avant refactor

- Le dashboard Angular restait centre sur `/api/zabbix/active` et `/api/zabbix/metrics`, alors que le backend actif expose deja les flux unifies `/api/monitoring/*`.
- Le frontend marquait Observium, ZKBio et Camera comme sans endpoint de lecture actif, ce qui etait faux pour le flux unifie.
- Le frontend attendait encore des valeurs de metadata documentaires (`supported`, `not_supported`, etc.) alors que le backend actif publie `native`, `synthetic`, `not_applicable`, `live`, `snapshot_fallback`, `snapshot_missing`.
- Le frontend ZKBio etait encore branche sur `/topic/zkbio/problems`, mais ce topic n'est pas effectivement publie par le workflow actif; les problemes ZKBio passent par `/topic/monitoring/problems`.

### Refactor applique

- `monitoring.store.ts` a ete recentre sur les snapshots et wrappers unifies:
  - hosts via `/api/monitoring/hosts`
  - problems via `/api/monitoring/problems`
  - metrics via `/api/monitoring/metrics`
  - availability via `/api/monitoring/sources/health`
- Le dashboard global calcule maintenant ses KPI multi-sources a partir des hosts, problems et metrics unifies.
- `zabbix-workspace.store.ts` utilise maintenant les donnees Zabbix filtrees depuis le flux unifie au lieu de dependre des endpoints de compatibilite.
- `monitoring-realtime.service.ts` a ete aligne sur les topics reels actifs et la dependance frontend a `/topic/zkbio/problems` a ete retiree.
- Les composants d'etat source affichent maintenant les valeurs de couverture reelles du backend (`native`, `synthetic`, `not_applicable`).

### Compatibilites temporaires conservees

- Les endpoints backend `/api/zabbix/active`, `/api/zabbix/metrics` et `/api/observium/summary` restent exposes.
- Les endpoints ZKBio metier (`status`, `devices`, `attendance`, `users`) restent consommes par le frontend quand ils correspondent a un flux metier reel.
- Le frontend garde la logique de refresh REST differe apres collecte manuelle, car les endpoints de collecte repondent avant la stabilisation complete des snapshots.
