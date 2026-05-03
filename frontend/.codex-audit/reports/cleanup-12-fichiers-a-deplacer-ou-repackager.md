# Fichiers a deplacer ou repackager

- `src/main/java/tn/iteam/domain/ApiResponse.java`
  - Pourquoi: wrapper HTTP, pas modele metier.
  - Cible proposee: `tn.iteam.dto` ou `tn.iteam.web.dto`.

- `src/main/java/tn/iteam/MonitoringStartup.java`
  - Pourquoi: startup hook Spring place au package racine.
  - Cible proposee: `tn.iteam.config.startup` ou `tn.iteam.bootstrap`.

- `src/main/java/tn/iteam/service/ZabbixSyncService.java`
  - Pourquoi: support technique specifique Zabbix, pas service metier generique.
  - Cible proposee: `tn.iteam.adapter.zabbix.support` ou `tn.iteam.integration.zabbix`.

- `src/main/java/tn/iteam/service/ZkBioServiceImpl.java`
  - Pourquoi: implementation concrete isolee dans `service` alors que les autres implementations vivent dans `service.impl`.
  - Cible proposee: `tn.iteam.service.impl`.

- `src/main/java/tn/iteam/client/ObserviumClientX.java`
  - Pourquoi: incoherence de packaging et nommage par rapport a `ZabbixClient`.
  - Cible proposee: `tn.iteam.adapter.observium.ObserviumClient`.

- `src/main/java/tn/iteam/client/ZkBioClientX.java`
  - Pourquoi: meme derive de packaging/nommage.
  - Cible proposee: `tn.iteam.adapter.zkbio.ZkBioClient`.

- `src/main/java/tn/iteam/util/IntegrationClientSupport.java`
  - Pourquoi: helper d'integration, pas utilitaire transverse reel.
  - Cible proposee: `tn.iteam.integration.support`.

- `src/main/java/tn/iteam/util/MonitoringConstants.java`
  - Pourquoi: constantes fortement liees au domaine monitoring.
  - Cible proposee: `tn.iteam.monitoring.support`.
