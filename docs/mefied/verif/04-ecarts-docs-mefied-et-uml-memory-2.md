# Ecarts Documentaires Corriges

## Ecarts confirmes dans `docs/mefied`
### Compteurs
Les documents existants annoncaient:
- `122` classes concretes
- `29` interfaces
- `environ 537` methodes

Les chiffres verifies au `2026-04-28` sont:
- `130` classes
- `30` interfaces
- `5` enums
- `6` records
- `557` methodes

### Portee
- `docs/mefied` etait majoritairement backend-centric
- le frontend etait mentionne surtout comme consommateur d'endpoints
- la verification presente ajoute une lecture explicite des stores, pages, routes et services Angular

### Execution
- les rapports historiques disaient que compilation et tests Maven etaient OK pour une passe precedente
- l'etat courant du depot n'est plus totalement vert: `2` erreurs de test Mockito sont presentes

## Ecarts confirmes dans `docs/uml-memory/2`
### Portee du dossier
- le dossier reste principalement une memoire backend
- il ne couvrait pas assez le frontend actuel, pourtant central pour les workflows reels

### Etat d'execution
- le frontend build correctement
- le backend lance ses tests mais la suite n'est pas totalement verte

### Redondance
- la memoire architecturale identifiait bien la duplication backend
- elle sous-decrivait la duplication frontend entre `MonitoringStore`, `ZabbixWorkspaceStore` et `MonitoringObserviumPageComponent`

## Corrections de fond appliquees
Les syntheses ont ete alignees sur:
- les compteurs reels
- l'existence explicite du frontend Angular dans l'architecture globale
- l'etat d'execution actuel backend et frontend
- les duplications les plus visibles cote Angular

## Recommandation
Pour la suite du PFE, utiliser ce dossier `verif` comme reference de verite rapide et conserver `docs/uml-memory/2` comme memoire architecturale backend detaillee.
