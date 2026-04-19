package tn.iteam.adapter.zkbio;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.iteam.cache.IntegrationCacheService;
import tn.iteam.client.ZkBioClient;
import tn.iteam.domain.ZkBioProblem;
import tn.iteam.dto.ZkBioProblemDTO;
import tn.iteam.dto.ServiceStatusDTO;
import tn.iteam.exception.IntegrationUnavailableException;
import tn.iteam.mapper.ZkBioMapper;
import tn.iteam.repository.ZkBioProblemRepository;
import tn.iteam.service.SourceAvailabilityService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter pour l'intégration ZKBio Time
 * Gère la récupération des appareils, problèmes et journaux de présence
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZkBioAdapter {

    private static final String SOURCE = "ZKBIO";
    private static final String SNAPSHOT_PREFIX = "zkbio:";

    private final ZkBioClient zkBioClient;
    private final ZkBioMapper zkBioMapper;
    private final ZkBioProblemRepository problemRepository;
    private final IntegrationCacheService integrationCacheService;
    private final SourceAvailabilityService availabilityService;

    /**
     * Vérifie l'état du serveur ZKBio
     */
    public List<ServiceStatusDTO> fetchAll() {
        log.info(" Checking ZKBio server");

        ServiceStatusDTO dto = new ServiceStatusDTO();
        dto.setSource("ZKBIO");
        dto.setName("ZKBio Server");
        URI baseUri = zkBioClient.getBaseUri();
        dto.setIp(baseUri != null ? baseUri.getHost() : null);
        dto.setPort(resolvePort(baseUri));
        dto.setProtocol(baseUri != null ? normalizeProtocol(baseUri.getScheme()) : null);
        dto.setCategory("ACCESS_CONTROL");
        dto.setStatus("DOWN");
        JsonNode devices = resolveCachedNode(zkBioClient.getDevices(), "devices");
        if (devices == null) {
            log.warn(" No ZKBio device data available from API or Redis");
            throw new IntegrationUnavailableException(
                    SOURCE,
                    "ZKBio devices unavailable and no usable Redis snapshot"
            );
        }

        if (availabilityService.isAvailable(SOURCE)) {
            dto.setStatus("UP");
            log.info(" ZKBio Server is UP via live API response");
        } else {
            dto.setStatus("UP");
            log.warn(" ZKBio Server is UP via Redis fallback snapshot");
        }

        return List.of(dto);
    }

    private Integer resolvePort(URI baseUri) {
        if (baseUri == null) {
            return null;
        }
        if (baseUri.getPort() > 0) {
            return baseUri.getPort();
        }
        return "https".equalsIgnoreCase(baseUri.getScheme()) ? 443 : 80;
    }

    private String normalizeProtocol(String scheme) {
        return scheme == null ? null : scheme.toUpperCase();
    }

    /**
     * Récupère les alertes/problèmes depuis ZKBio et les transforme en DTO
     */
    public List<ZkBioProblemDTO> fetchProblems() {
        log.info(" Fetching problems from ZKBio");

        JsonNode alerts = resolveCachedNode(zkBioClient.getAlerts(), "alerts");
        List<ZkBioProblemDTO> dtos = new ArrayList<>();

        if (alerts == null || !alerts.isArray()) {
            log.warn("No alerts received from ZKBio");
            throw new IntegrationUnavailableException(
                    SOURCE,
                    "ZKBio alerts unavailable and no usable Redis snapshot"
            );
        }

        for (JsonNode alertNode : alerts) {
            dtos.add(zkBioMapper.mapAlertToDTO(alertNode));
        }

        log.info(" {} problems fetched from ZKBio", dtos.size());
        return dtos;
    }

    /**
     * Récupère les alertes et les enregistre dans la base (uniquement les actifs)
     */
    public List<ZkBioProblemDTO> fetchProblemsAndSave() {
        log.info(" Fetching problems from ZKBio and saving to DB");

        JsonNode alerts = resolveCachedNode(zkBioClient.getAlerts(), "alerts");
        List<ZkBioProblemDTO> dtos = new ArrayList<>();
        List<ZkBioProblem> entities = new ArrayList<>();

        if (alerts == null || !alerts.isArray()) {
            log.warn("No alerts received from ZKBio");
            return dtos;
        }

        for (JsonNode alertNode : alerts) {
            ZkBioProblemDTO dto = zkBioMapper.mapAlertToDTO(alertNode);
            dtos.add(dto);

            // On ne garde que les problèmes actifs
            if(dto.isActive()) {
                ZkBioProblem entity = ZkBioProblem.builder()
                        .problemId(dto.getProblemId())
                        .device(dto.getHost())
                        .description(dto.getDescription())
                        .active(dto.isActive())
                        .source(dto.getSource())
                        .eventId(dto.getEventId())
                        .build();

                entities.add(entity);
            }
        }

        if(!entities.isEmpty()) {
            problemRepository.saveAll(entities);
            log.info(" {} problems saved to ZKBio database", entities.size());
        }

        return dtos;
    }

    private JsonNode resolveCachedNode(JsonNode liveNode, String snapshotKey) {
        if (liveNode != null) {
            return liveNode;
        }

        Optional<JsonNode> cachedSnapshot = integrationCacheService.getSnapshot(
                SOURCE,
                SNAPSHOT_PREFIX + snapshotKey,
                JsonNode.class
        );

        if (cachedSnapshot.isPresent()) {
            log.warn("Using Redis fallback snapshot for ZKBio {}", snapshotKey);
            availabilityService.markUnavailable(SOURCE, "ZKBio API unavailable, serving Redis snapshot for " + snapshotKey);
            return cachedSnapshot.get();
        }

        log.warn("No usable Redis snapshot for ZKBio {}, keeping current fallback behavior", snapshotKey);
        availabilityService.markUnavailable(SOURCE, "ZKBio API unavailable and no usable Redis snapshot for " + snapshotKey);
        return null;
    }
}
