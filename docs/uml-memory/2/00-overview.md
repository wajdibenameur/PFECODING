# Vue d'ensemble Architecturale

## Objet du document
Le present dossier constitue une memoire technique principalement centree sur l'architecture backend implementee sous le package `tn.iteam`. L'analyse couvre les classes metier, les couches techniques, les dependances inter-modules, les flux d'execution, les mecanismes de resilience et les zones de dette technique detectables statiquement. Une verification complementaire incluant le frontend Angular a ete ajoutee dans `docs/mefied/verif`.

## Perimetre observe
- code principal: `src/main/java/tn/iteam`
- tests utiles a l'inference des comportements: `src/test/java/tn/iteam`
- verification croisee frontend et usages reels: `frontend/src/app`
- sorties documentaires: `docs/uml-memory/2`

## Convention de lecture
Les constats sont classes selon trois niveaux:

- `Fait confirme`: etabli directement a partir du code source.
- `Inference statique`: deduction solide a partir des appels, annotations, tests et conventions de nommage.
- `Hypothese a verifier en execution`: point plausible mais non prouvable sans traces runtime, usage externe ou instrumentation.

## Vision generale du backend
Le backend est structure autour d'un noyau de supervision unifiee. Quatre integrations principales alimentent ce noyau:

- Zabbix
- Observium
- ZKBio
- Camera

Ces integrations produisent des jeux de donnees homogenises:

- hosts
- problems
- metrics

Le systeme combine ensuite:

- une couche de snapshots memoire
- une couche de persistance JPA
- une API REST unifiee
- une publication WebSocket
- des schedulers de rafraichissement
- une logique de fallback `live -> snapshot -> persisted -> empty`

Deux modules adjacents s'appuient sur ce noyau:

- un module de ticketing
- un module dashboard / ML centré sur les donnees Zabbix persistees

## Hypothese architecturale centrale
`Fait confirme`

Le systeme suit une architecture en couches avec specialisations metier:

- `controller`: exposition HTTP
- `integration` / `service`: orchestration et logique d'application
- `adapter` / `client`: acces aux systemes externes
- `repository`: acces a la persistence
- `mapper` / `dto`: transformation de donnees
- `scheduler`: declenchement periodique
- `monitoring.snapshot`: cache resilient de donnees unifiees

## Conclusions principales
### Points forts
- couplage reduit dans les points d'entree monitoring recents grâce a l'injection par interfaces
- orchestration de ZKBio centralisee et rendue explicite
- publication snapshot seulement apres completion des pipelines critiques
- resilience explicite via timeout, retry, circuit breaker et cooldown scheduler
- strategie de degradation claire dans les services d'integration

### Faiblesses architecturales
- forte duplication de logique de fallback entre services d'integration
- `ZabbixAdapter` reste une classe a responsabilites multiples
- couche securite metier modelee, mais non appliquee a l'execution
- coexistence de endpoints de compatibilite et d'API unifiee
- quelques derives de packaging et de nommage

### Resultats saillants de l'analyse de redondance
- packages vides encore presents: `cache`, `listener`, `logging`, `monitoring.provider`
- controleurs de compatibilite potentiellement retirables a moyen terme
- duplication elevee des helpers `safeMessage(...)` et du patron de fallback snapshot
- une methode de service Zabbix parait non consommee statiquement

## Lecture conseillee
Pour une lecture progressive:

1. `01-package-inventory.md`
2. `03-module-architecture.md`
3. `04-workflows.md`
4. `09-dead-code-redundancy-analysis.md`
5. `08-coupling-and-refactoring.md`
6. `FINAL_ARCHITECTURE_REPORT.md`
