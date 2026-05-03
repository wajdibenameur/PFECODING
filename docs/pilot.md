# Analyse croisée backend / frontend / auth-service / ML

## Backend
- Le backend principal expose aujourd’hui une API unifiée dans `tn.iteam.controller.MonitoringController` :
  - `GET /api/monitoring/hosts`
  - `GET /api/monitoring/problems`
  - `GET /api/monitoring/metrics`
  - `GET /api/monitoring/sources/health`
  - `POST /api/monitoring/collect`
  - `POST /api/monitoring/collect/zabbix`
  - `POST /api/monitoring/collect/observium`
  - `POST /api/monitoring/collect/camera`
- Les topics WebSocket publiés par `tn.iteam.websocket.MonitoringWebSocketPublisher` sont :
  - `/topic/monitoring/problems`
  - `/topic/monitoring/metrics`
  - `/topic/monitoring/sources`
- Il existe des contrôleurs source-spécifiques complémentaires :
  - `/api/zkbio/*` dans `ZkBioController`
  - `/api/cameras` dans `CameraController`
  - `/dashboard/*` dans `DashboardController`
- Le backend n’a plus de mapping `/api/zabbix` dans le code source : recherche globale ne retourne aucun endpoint ou contrôleur restant.

## Frontend
- Le frontend consomme les endpoints unifiés via `frontend/src/app/features/monitoring/data/monitoring-api.service.ts` :
  - `/api/monitoring/hosts`
  - `/api/monitoring/problems`
  - `/api/monitoring/metrics`
  - `/api/monitoring/sources/health`
  - `/api/zkbio/status`
  - `/api/zkbio/devices`
  - `/api/zkbio/attendance`
  - `/dashboard/overview`
  - `/dashboard/predictions`
  - `/dashboard/anomalies`
- Les flux temps réel utilisent `frontend/src/app/features/monitoring/data/monitoring-realtime.service.ts` :
  - `/topic/monitoring/problems`
  - `/topic/monitoring/metrics`
  - `/topic/monitoring/sources`
  - `/topic/zkbio/attendance`
  - `/topic/zkbio/devices`
  - `/topic/zkbio/status`
- Le frontend ne contient aucune référence à `/api/zabbix/*` dans `frontend/src/**/*.{ts,html}`.

## Auth-service
- `auth-service/` est un microservice séparé avec Keycloak wrapper :
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/register`
- Il utilise OpenFeign et Keycloak token/admin clients dans `auth-service/src/main/java/tn/iteam/authservice/service/AuthService.java`.
- Mais le frontend actuel ne l’utilise pas :
  - `frontend/src/app/core/auth/noop-auth-context.service.ts` retourne toujours `null`
  - Pas de logique de login/register active dans le frontend
  - `frontend/src/app/core/http/auth-header.interceptor.ts` ajoute l’en-tête `Authorization` seulement si un token existe, mais aucun token n’est fourni par défaut

## ML / PFERTC
- Le module `PFERTC` produit :
  - `artifacts/models/model.pt`
  - `artifacts/models/feature_metadata.json`
- Le backend charge ce modèle via `TorchScriptPredictionService` et `MlTorchScriptProperties`.
- `DashboardServiceImpl` utilise ce service pour construire les prédictions et anomalies affichées sur le tableau de bord.
- Le frontend ne consomme pas directement `/predict`, il consomme plutôt les agrégats de `DashboardController` :
  - `/dashboard/predictions`
  - `/dashboard/anomalies`
  - `/dashboard/overview`

## Principal écart trouvé
- **Intégration d’authentification manquante** :
  - Le backend auth-service existe, mais le frontend reste en mode `NoopAuthContextService`.
  - En pratique, il n’y a pas de flow `login/register` frontend connecté à `auth-service`.
- **Pas de consommation de `/predict` par le frontend** :
  - ML est intégré indirectement via le dashboard.
  - Si l’objectif était une API ML explicite côté client, elle n’est pas utilisée aujourd’hui.

## Conclusion
- La suppression des anciens endpoints `/api/zabbix/*` est cohérente : le frontend n’en dépend plus.
- Les endpoints unifiés `/api/monitoring/*` et les topics `/topic/monitoring/*` couvrent actuellement les besoins applicatifs du frontend.
- Le vrai gap fonctionnel est l’authentification : le frontend est prêt pour l’authentification tokenisée, mais le lien avec `auth-service` n’est pas encore effectif.