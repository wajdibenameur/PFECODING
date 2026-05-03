# Packages et sous-packages vides

## Packages vides confirmes
- `src/main/java/tn/iteam/cache`
- `src/main/java/tn/iteam/listener`
- `src/main/java/tn/iteam/logging`
- `src/main/java/tn/iteam/monitoring/provider`

## Sous-packages a faible valeur separatrice
- `src/main/java/tn/iteam/client`: seulement `ObserviumClientX` et `ZkBioClientX`, alors que `ZabbixClient` vit ailleurs ; package a harmoniser plutot qu'a conserver tel quel.
- `src/main/java/tn/iteam/service/support`: seulement trois helpers transverses, dont certains relevent plutot de `integration/support`.
- `src/main/java/tn/iteam/ml/controller` et `src/main/java/tn/iteam/ml/service`: un seul fichier chacun ; separation acceptable mais tres fine.
