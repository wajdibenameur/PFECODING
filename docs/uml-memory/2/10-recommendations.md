# Recommandations de Refactorisation

## 1. Priorite critique

### 1. Formaliser la posture securite
- pourquoi: le modele `User` / `Role` / `Permission` peut laisser croire a une securite effective alors qu'aucun enforcement serveur n'est actif
- action:
  - soit introduire une vraie securite Spring
  - soit documenter explicitement que le backend est actuellement non securise

### 2. Centraliser la logique de fallback snapshot
- pourquoi: duplication elevee et risque de divergence entre integrations
- action:
  - extraire un helper/support service specialise
  - conserver les decisions metier dans chaque integration

## 2. Priorite importante

### 1. Reduire la taille de `ZabbixAdapter`
- pourquoi: principal hotspot de couplage et de complexite technique
- action:
  - separer les collectes hosts/problems des collectes metrics/history

### 2. Clarifier l'architecture ZKBio
- pourquoi: superposition partielle entre orchestration, publication et refresh composite
- action:
  - documenter les cas d'usage respectifs
  - extraire si le module continue a croitre

### 3. Planifier le retrait des endpoints de compatibilite
- pourquoi: maintien d'une dette de contrat et de mapping
- action:
  - recenser les consommateurs
  - isoler puis retirer progressivement

### 4. Normaliser le packaging
- pourquoi: rendre les patterns plus lisibles pour l'equipe et pour la documentation PFE
- action:
  - replacer `ZkBioServiceImpl`
  - homogénéiser les packages `client` / `adapter`
  - supprimer les packages vides

## 3. Priorite utile mais non urgente

### 1. Clarifier les DTOs ZKBio
- pourquoi: `getUsers()` et `ZkBioAttendanceDTO` suggerent une confusion de contrat

### 2. Segmenter `DashboardServiceImpl` si le module s'etend
- pourquoi: risque de classe analytique trop large

### 3. Elaguer les methodes faiblement consommees
- pourquoi: reduire les API internes mortes ou ambiguës
- cible initiale:
  - `synchronizeAndGetPersistedMetricsSnapshot()`

## 4. Recommandation documentaire
Chaque refactoring futur devrait maintenir la distinction suivante:

- `fait confirme`
- `inference statique`
- `hypothese a verifier en execution`

Cette discipline est particulierement utile pour:
- les endpoints de compatibilite
- les roles non enforce
- les candidats dead code utilises potentiellement par des consommateurs externes
