package tn.iteam.adapter.zkbio;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.iteam.client.ZkBioClientX;
import tn.iteam.domain.ZkBioMetric;
import tn.iteam.domain.ZkBioProblem;
import tn.iteam.dto.ServiceStatusDTO;
import tn.iteam.dto.ZkBioMetricDTO;
import tn.iteam.dto.ZkBioProblemDTO;
import tn.iteam.exception.IntegrationException;
import tn.iteam.mapper.ZkBioMetricMapper;
import tn.iteam.mapper.ZkBioMapper;
import tn.iteam.repository.ZkBioMetricRepository;
import tn.iteam.repository.ZkBioProblemRepository;
import tn.iteam.util.MonitoringConstants;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ZkBioAdapter {

    private static final Logger log = LoggerFactory.getLogger(ZkBioAdapter.class);
    private static final String SERVER_NAME = "ZKBio Server";
    private static final String SERVER_IP = "192.168.11.8";
    private static final int SERVER_PORT = 8098;
    private static final String CHECKING_SERVER_MESSAGE = "Checking ZKBio server";
    private static final String SERVER_UP_MESSAGE = "ZKBio Server is UP";
    private static final String DEVICES_API_NULL_MESSAGE = "ZKBio Server devices API returned null";
    private static final String FETCHING_PROBLEMS_MESSAGE = "Fetching problems from ZKBio";
    private static final String NO_ALERTS_MESSAGE = "No alerts received from ZKBio";
    private static final String FETCHING_AND_SAVING_PROBLEMS_MESSAGE = "Fetching problems from ZKBio and saving to DB";
    private static final String PROBLEMS_FETCHED_MESSAGE = "{} problems fetched from ZKBio";
    private static final String PROBLEMS_SAVED_MESSAGE = "{} problems saved to ZKBio database";

    private final ZkBioClientX zkBioClient;
    private final ZkBioMapper zkBioMapper;
    private final ZkBioMetricMapper zkBioMetricMapper;
    private final ZkBioMetricRepository metricRepository;
    private final ZkBioProblemRepository problemRepository;

    public List<ServiceStatusDTO> fetchAll() {
        log.info(CHECKING_SERVER_MESSAGE);

        List<ServiceStatusDTO> dtos = new ArrayList<>();
        ServiceStatusDTO dto = new ServiceStatusDTO();
        dto.setSource(MonitoringConstants.SOURCE_ZKBIO);
        dto.setName(SERVER_NAME);
        dto.setIp(SERVER_IP);
        dto.setPort(SERVER_PORT);
        dto.setProtocol(MonitoringConstants.PROTOCOL_HTTPS);
        dto.setCategory(MonitoringConstants.CATEGORY_ACCESS);
        dto.setStatus(MonitoringConstants.STATUS_DOWN);

        try {
            if (zkBioClient.getDevices() != null) {
                dto.setStatus(MonitoringConstants.STATUS_UP);
                log.info(SERVER_UP_MESSAGE);
            } else {
                log.warn(DEVICES_API_NULL_MESSAGE);
            }
        } catch (IntegrationException e) {
            log.warn("ZKBio unavailable: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error connecting to ZKBio Server", e);
        }

        dtos.add(dto);
        return dtos;
    }

    public List<ZkBioProblemDTO> fetchProblems() {
        log.info(FETCHING_PROBLEMS_MESSAGE);

        List<ZkBioProblemDTO> dtos = new ArrayList<>();
        JsonNode alerts = fetchAlerts("ZKBio problems unavailable: {}");

        if (alerts == null || !alerts.isArray()) {
            log.warn(NO_ALERTS_MESSAGE);
            return dtos;
        }

        for (JsonNode alertNode : alerts) {
            dtos.add(zkBioMapper.mapAlertToDTO(alertNode));
        }

        log.info(PROBLEMS_FETCHED_MESSAGE, dtos.size());
        return dtos;
    }

    public List<ZkBioProblemDTO> fetchProblemsAndSave() {
        log.info(FETCHING_AND_SAVING_PROBLEMS_MESSAGE);

        List<ZkBioProblemDTO> dtos = new ArrayList<>();
        List<ZkBioProblem> entities = new ArrayList<>();
        JsonNode alerts = fetchAlerts("ZKBio problems unavailable during persistence: {}");

        if (alerts == null || !alerts.isArray()) {
            log.warn(NO_ALERTS_MESSAGE);
            return dtos;
        }

        for (JsonNode alertNode : alerts) {
            ZkBioProblemDTO dto = zkBioMapper.mapAlertToDTO(alertNode);
            dtos.add(dto);

            if (dto.isActive()) {
                ZkBioProblem entity = ZkBioProblem.builder()
                        .problemId(dto.getProblemId())
                        .device(dto.getHost())
                        .description(dto.getDescription())
                        .active(dto.isActive())
                        .status(dto.getStatus())
                        .startedAt(dto.getStartedAt())
                        .resolvedAt(dto.getResolvedAt())
                        .source(dto.getSource())
                        .eventId(dto.getEventId())
                        .build();
                entities.add(entity);
            }
        }

        if (!entities.isEmpty()) {
            problemRepository.saveAll(entities);
            log.info(PROBLEMS_SAVED_MESSAGE, entities.size());
        }

        return dtos;
    }

    private JsonNode fetchAlerts(String unavailableLogTemplate) {
        try {
            return zkBioClient.getAlerts();
        } catch (IntegrationException e) {
            log.warn(unavailableLogTemplate, e.getMessage());
            return null;
        }
    }

    public List<ZkBioMetricDTO> fetchMetrics() {
        long now = Instant.now().getEpochSecond();
        List<ZkBioMetricDTO> metrics = new ArrayList<>();
        JsonNode devices = zkBioClient.getDevices();

        int deviceCount = devices != null && devices.isArray() ? devices.size() : 0;
        metrics.add(ZkBioMetricDTO.builder()
                .hostId("ZKBIO_SERVER")
                .hostName(SERVER_NAME)
                .itemId("devices-count")
                .metricKey("zkbio.devices.count")
                .value((double) deviceCount)
                .timestamp(now)
                .ip(SERVER_IP)
                .port(SERVER_PORT)
                .build());

        metrics.add(ZkBioMetricDTO.builder()
                .hostId("ZKBIO_SERVER")
                .hostName(SERVER_NAME)
                .itemId("server-status")
                .metricKey("zkbio.server.status")
                .value(deviceCount > 0 ? 1.0 : 0.0)
                .timestamp(now)
                .ip(SERVER_IP)
                .port(SERVER_PORT)
                .build());

        return metrics;
    }

    public List<ZkBioMetric> fetchMetricsAndSave() {
        List<ZkBioMetricDTO> dtos = fetchMetrics();
        List<ZkBioMetric> entitiesToSave = new ArrayList<>();

        for (ZkBioMetricDTO dto : dtos) {
            if (dto.getHostId() == null || dto.getHostId().isBlank() || dto.getTimestamp() == null) {
                continue;
            }

            ZkBioMetric entity = zkBioMetricMapper.toEntity(dto);
            ZkBioMetric finalEntity = metricRepository.findByHostIdAndItemIdAndTimestamp(
                            dto.getHostId(),
                            dto.getItemId(),
                            dto.getTimestamp()
                    )
                    .map(existing -> {
                        existing.setHostName(entity.getHostName());
                        existing.setMetricKey(entity.getMetricKey());
                        existing.setValue(entity.getValue());
                        existing.setIp(entity.getIp());
                        existing.setPort(entity.getPort());
                        return existing;
                    })
                    .orElse(entity);

            entitiesToSave.add(finalEntity);
        }

        return metricRepository.saveAll(entitiesToSave);
    }
}
