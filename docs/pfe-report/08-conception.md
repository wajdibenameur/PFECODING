# 08 - Conception

La documentation UML du système a été reconstruite dans le dossier `docs/uml-memory` à partir du code source réel du projet.

La modélisation a été réalisée conformément à la norme UML 2.5.1 définie par l’OMG, en s’appuyant sur une approche de rétro-ingénierie du code. Cette approche consiste à analyser les différentes couches applicatives (`controllers`, `services`, `repositories`), ainsi que les mécanismes de communication (`WebSocket`), d’intégration externe et de monitoring.

Les diagrammes générés couvrent les vues essentielles du système :

- un diagramme de cas d’utilisation ;
- un diagramme de classes ;
- trois diagrammes de séquence représentant les principaux scénarios d’exécution ;
- un diagramme d’architecture basé sur un `Component Diagram` UML 2.5.1.

## Hypothèses et constats issus de l’analyse

- `InMemorySnapshotStore` constitue le mécanisme principal de stockage des snapshots en mémoire dans le système actuel ;
- Redis est préparé comme composant optionnel, mais ne joue pas de rôle actif dans le flux métier courant ;
- les endpoints `/api/zabbix/*` et `/api/observium/*` sont maintenus principalement à des fins de compatibilité avec des systèmes existants ;
- le flux principal du système repose sur les endpoints `/api/monitoring/*` et la diffusion temps réel via `/topic/monitoring/*` ;
- le composant `ZkBioWebSocketPublisher` est utilisé pour la diffusion d’informations liées à l’`attendance`, aux `devices` et au `status`, mais n’intervient pas comme flux principal de monitoring côté frontend.

## Limites de la reconstruction

- la topologie de déploiement physique du système n’est pas entièrement déductible à partir du dépôt de code ;
- certains éléments (`DTO`, `mappers`, utilitaires) ont été volontairement omis dans les diagrammes afin de préserver leur lisibilité ;
- certaines parties du frontend, notamment liées aux tickets, restent partiellement implémentées ;
- certains tests contiennent des artefacts ou conventions historiques qui ne reflètent pas exactement le comportement actuel du système.

## Validation

- l’ensemble des fichiers UML demandés a été généré ;
- une cohérence a été assurée entre les descriptions textuelles (`.md`) et les diagrammes (`.puml`) ;
- les diagrammes produits restent fidèles au code observé ;
- aucune relation ou dépendance n’a été introduite sans justification dans le code.
