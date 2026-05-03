# Idees Codex

## Statut du document
Ce fichier recense des pistes d'evolution compatibles avec l'architecture actuelle. Il ne decrit pas l'etat courant du code; il propose des travaux futurs juges coherents avec le design reel.

## 1. Evolutions prioritaires realistes

### 1. Mutualiser le fallback snapshot
Objectif:
- reduire la duplication entre `ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService` et `CameraIntegrationService`

Forme recommandee:
- petit helper de support ou abstraction de coordination
- conserver les decisions metier dans chaque service d'integration

### 2. Reduire le hotspot `ZabbixAdapter`
Objectif:
- separer la collecte hosts/problems de la collecte metrics/history
- reduire le melange entre orchestration technique, mapping et ponts bloquants

### 3. Clarifier la strate legacy
Objectif:
- isoler clairement les endpoints de compatibilite
- preparer le retrait de:
  - `ObserviumController`
  - `ZabbixMetricsController`
  - `ZabbixProblemController`

Condition:
- confirmer d'abord les consommateurs externes

## 2. Securite et gouvernance

### 1. Activer une vraie securite backend
Constat actuel:
- le modele `User` / `Role` / `Permission` existe
- aucune autorisation runtime n'est enforcee

Piste:
- ajouter une securite Spring explicite
- ou documenter durablement le backend comme non securise si c'est un choix transitoire

### 2. Clarifier le role SuperAdmin
Constat:
- `MANAGE_USERS` et `MANAGE_ROLES` existent dans le modele
- aucun module de gestion n'est expose

Piste:
- soit implementer le module correspondant
- soit simplifier le modele pour qu'il colle au perimetre reel

## 3. Resilience et exploitation

### 1. Verrou distribue en multi-instance
Si le backend doit tourner en plusieurs instances:
- ajouter un verrou distribue pour les refresh sensibles
- cibler en priorite les collectes lourdes Zabbix

### 2. Visibilite du mode degrade
Piste:
- rendre plus visible le fait qu'une reponse provient d'un `snapshot_fallback`
- sans casser les contrats REST existants
- par logs, metadonnees ou observabilite interne

### 3. Metriques techniques
Pistes utiles:
- latence des integrations
- nombre de retries
- transitions circuit breaker
- nombre de refresh sautes par cooldown
- nombre de refresh lourds Zabbix sautes pour cause de recouvrement

## 4. ZKBio

### 1. Clarifier les DTOs metier
Constat:
- `getUsers()` retourne encore `ZkBioAttendanceDTO`

Piste:
- introduire un DTO plus semantique pour les utilisateurs ZKBio si le contrat doit durer

### 2. Clarifier le workflow composite ZKBio
Piste:
- mieux distinguer dans le code et la documentation:
  - orchestration globale monitoring
  - workflow source-specific `/api/zkbio/collect`

## 5. Dashboard et ML

### 1. Segmenter `DashboardServiceImpl` si le module grandit
Pistes de decoupage:
- service overview
- service anomalies
- service prediction

### 2. Elargir la couverture multi-sources
Piste:
- aligner plus fortement le dashboard frontend sur la couverture reelle multi-sources
- ne pas rester implicitement centre sur Zabbix si d'autres sources montent en maturite

## 6. Redis

### Regle a conserver
Redis ne doit pas redevenir obligatoire pour le chemin metier principal.

Si Redis revient plus fortement:
- garder `SnapshotStore` comme contrat metier unique
- conserver `InMemorySnapshotStore` comme base sure
- faire du Redis un complement optionnel et survivable

## 7. Documentation

### Piste importante
Maintenir en coherence:
- `docs/codex-memory.md`
- `docs/codex-ideas.md`
- `docs/uml-memory/2/*`

Regle:
- `codex-memory` = etat de reference du code reel
- `codex-ideas` = evolutions possibles
- `uml-memory/2` = analyse architecturale exhaustive et argumentee
