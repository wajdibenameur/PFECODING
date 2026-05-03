# Analyse par Role

## Statut general
`Fait confirme`

Le code contient un modele `User` / `Role` / `Permission` ainsi qu'un bootstrap de comptes et de roles. En revanche, aucun mecanisme d'authentification ou d'autorisation Spring n'est actif dans le code analyse:

- pas de `SecurityFilterChain`
- pas de `@PreAuthorize`
- pas de `UserDetailsService`
- pas de controleur d'authentification

Ainsi, l'analyse suivante distingue:

- `capacites metier modeleees`
- `contraintes runtime effectivement appliquees`

## 1. Administrator
### Capacites metier inferees
- consulter le monitoring unifie
- declencher une collecte manuelle
- creer, affecter, valider ou rejeter des tickets
- consulter le dashboard

### Contraintes runtime confirmees
- aucune contrainte de role enforcee par le backend

### Conclusion
Le role Administrator existe conceptuellement, mais son perimetre d'autorisation est actuellement une convention de modele ou d'IHM.

## 2. Viewer Support
### Capacites metier inferees
- consulter le monitoring
- consulter le dashboard
- creer ou commenter des tickets
- suivre les interventions

### Indices
- enum `RoleName`
- bootstrap ticketing
- structure du workflow ticketing

### Contraintes runtime
- non appliquees

## 3. SuperAdmin
### Capacites metier inferees
- toutes les actions Administrator
- potentiellement gestion des utilisateurs et des roles

### Limite
`Fait confirme`: les permissions `MANAGE_USERS` et `MANAGE_ROLES` ne correspondent a aucun module expose ni a aucun controle de securite actif.

### Interpretation
Le role SuperAdmin parait preparatoire ou anticipe, non pleinement implemente.

## 4. Systeme externe / integrations
Les sources suivantes jouent le role d'acteurs techniques externes:

- Zabbix
- Observium
- ZKBio
- reseau Camera

Elles ne consomment pas l'API comme des utilisateurs humains; elles sont sollicitees par les adapters/clients.

## Conclusion analytique
- `Fait confirme`: le backend ne met pas en oeuvre de securite applicative runtime.
- `Inference statique`: les roles documentes relèvent davantage d'un modele cible ou d'une couche frontend/backoffice que d'un mecanisme effectivement enforce cote serveur.
