# Analyse du Modele d'Exceptions

## 1. Taxonomie des exceptions

### Famille integration
- `IntegrationException`
- `IntegrationTimeoutException`
- `IntegrationUnavailableException`
- `IntegrationResponseException`
- `IntegrationDataUnavailableException`

### Famille ticketing
- `TicketingException`

### Handler global
- `GlobalExceptionHandler`

## 2. Boundary de mapping
### Faits confirmes
- les clients HTTP remappent les erreurs techniques via `IntegrationClientSupport`
- les timeouts et indisponibilites sont convertis en exceptions du domaine integration
- `GlobalExceptionHandler` uniformise ensuite la reponse REST

## 3. Propagation typique
### Cas integration HTTP
1. timeout / erreur reseau dans le client
2. conversion en `IntegrationTimeoutException` ou `IntegrationUnavailableException`
3. eventuel fallback circuit breaker
4. capture eventuelle dans un service d'integration
5. fallback snapshot ou repropagation selon le workflow

### Cas ticketing
1. entite absente ou transition invalide
2. levee de `TicketingException`
3. conversion en `ApiErrorResponse`

## 4. Zones ou l'exception est absorbee
### Services d'integration
`Fait confirme`

Les services `ZabbixIntegrationService`, `ObserviumIntegrationService`, `ZkBioIntegrationService` et `CameraIntegrationService` absorbent une partie des erreurs en servant un fallback.

### Consequence
Le workflow global peut etre fonctionnellement reussi alors qu'une collecte live a effectivement echoue.

## 5. Logging
### Points positifs
- meilleure conservation des stacktraces aux frontieres techniques
- `MonitoringStartup` journalise completement les echecs de warmup

### Points faibles
- certains logs de fallback restent centres sur `safeMessage(...)`
- une partie du contexte est volontairement simplifiee pour reduire le bruit

## 6. Coherence generale
### Evaluation
- bonne coherence du modele d'exception
- bonne separation entre erreur transport, indisponibilite et erreur de reponse
- necessite de documenter plus explicitement le fait qu'une reponse degradee n'est pas equivalente a un succes live

## 7. Recommandation
Centraliser a moyen terme la logique commune:
- journalisation du fallback
- construction de messages degrade/unavailable
- preservation du contexte technique minimal
