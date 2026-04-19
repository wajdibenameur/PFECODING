package tn.iteam.adapter.observium;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.iteam.client.ObserviumClientX;
import tn.iteam.domain.ApiResponse;
import tn.iteam.domain.ObserviumMetric;
import tn.iteam.domain.ObserviumProblem;
import tn.iteam.dto.ObserviumMetricDTO;
import tn.iteam.dto.ObserviumProblemDTO;
import tn.iteam.dto.ServiceStatusDTO;
import tn.iteam.exception.IntegrationDataUnavailableException;
import tn.iteam.mapper.ObserviumMetricMapper;
import tn.iteam.mapper.ObserviumMonitoringMapper;
import tn.iteam.mapper.ObserviumMapper;
import tn.iteam.monitoring.dto.UnifiedMonitoringProblemDTO;
import tn.iteam.repository.ObserviumMetricRepository;
import tn.iteam.repository.ObserviumProblemRepository;
import tn.iteam.websocket.MonitoringWebSocketPublisher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObserviumAdapter {

    private final ObserviumClientX observiumClient;
    private final ObserviumMapper observiumMapper;
    private final ObserviumMetricMapper observiumMetricMapper;
    private final ObserviumMonitoringMapper observiumMonitoringMapper;
    private final ObserviumMetricRepository metricRepository;
    private final ObserviumProblemRepository problemRepository;
    private final MonitoringWebSocketPublisher monitoringWebSocketPublisher;

    private boolean isValid(ApiResponse<JsonNode> response) {
        return response != null
                && response.isSuccess()
                && response.getData() != null
                && response.getData().isArray();
    }

    public List<ServiceStatusDTO> fetchAll() {
        log.info("Fetching devices from Observium");
        ApiResponse<JsonNode> response = observiumClient.getDevices();
        List<ServiceStatusDTO> dtos = new ArrayList<>();
        if (!isValid(response)) {
            String errorMsg = response != null ? response.getMessage() : "null response";
            log.warn("Devices API failed: {}", errorMsg);
            throw IntegrationDataUnavailableException.forObservium("Devices unavailable: " + errorMsg);
        }
        for (JsonNode node : response.getData()) {
            dtos.add(observiumMapper.mapDeviceToDTO(node));
        }
        return dtos;
    }

    public List<ObserviumProblemDTO> fetchProblems() {
        log.info("Fetching alerts from Observium");
        ApiResponse<JsonNode> response = observiumClient.getAlerts();
        List<ObserviumProblemDTO> dtos = new ArrayList<>();
        if (!isValid(response)) {
            String errorMsg = response != null ? response.getMessage() : "null response";
            log.warn("Alerts API failed: {}", errorMsg);
            throw IntegrationDataUnavailableException.forObservium("Alerts unavailable: " + errorMsg);
        }
        for (JsonNode node : response.getData()) {
            dtos.add(observiumMapper.mapAlertToDTO(node));
        }
        return dtos;
    }

    public List<ObserviumProblemDTO> fetchProblemsAndSave() {
        log.info("Fetching + saving Observium problems");
        ApiResponse<JsonNode> response = observiumClient.getAlerts();

        List<ObserviumProblemDTO> dtos = new ArrayList<>();
        List<UnifiedMonitoringProblemDTO> monitoringProblems = new ArrayList<>();

        if (!isValid(response)) {
            log.warn("Alerts API failed: {}", response.getMessage());
            return dtos;
        }

        for (JsonNode node : response.getData()) {
            ObserviumProblemDTO dto = observiumMapper.mapAlertToDTO(node);
            dtos.add(dto);
            monitoringProblems.add(observiumMonitoringMapper.toProblem(dto));
        }

        List<ObserviumProblem> entities = buildEntitiesToSave(dtos);
        entities.addAll(buildResolvedEntitiesToSave(dtos));

        if (!entities.isEmpty()) {
            problemRepository.saveAll(entities);
            log.info("{} problems saved", entities.size());
        }

        monitoringWebSocketPublisher.publishProblems(monitoringProblems);

        return dtos;
    }

    public List<ObserviumMetricDTO> fetchMetrics() {
        List<ServiceStatusDTO> statuses = fetchAll();
        long now = Instant.now().getEpochSecond();
        List<ObserviumMetricDTO> metrics = new ArrayList<>();

        for (ServiceStatusDTO status : statuses) {
            String hostName = status.getName() != null && !status.getName().isBlank() ? status.getName() : "UNKNOWN";
            String hostId = status.getIp() != null && !status.getIp().isBlank() && !"IP_UNKNOWN".equalsIgnoreCase(status.getIp())
                    ? status.getIp()
                    : hostName;

            metrics.add(ObserviumMetricDTO.builder()
                    .hostId(hostId)
                    .hostName(hostName)
                    .itemId("device-status")
                    .metricKey("observium.device.status")
                    .value("UP".equalsIgnoreCase(status.getStatus()) ? 1.0 : 0.0)
                    .timestamp(now)
                    .ip(status.getIp())
                    .port(status.getPort())
                    .build());
        }

        return metrics;
    }

    public List<ObserviumMetric> fetchMetricsAndSave() {
        List<ObserviumMetricDTO> dtos = fetchMetrics();
        List<ObserviumMetric> entitiesToSave = new ArrayList<>();

        for (ObserviumMetricDTO dto : dtos) {
            if (dto.getHostId() == null || dto.getHostId().isBlank() || dto.getTimestamp() == null) {
                continue;
            }

            ObserviumMetric entity = observiumMetricMapper.toEntity(dto);
            ObserviumMetric finalEntity = metricRepository.findByHostIdAndItemIdAndTimestamp(
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

    private List<ObserviumProblem> buildEntitiesToSave(List<ObserviumProblemDTO> dtos) {
        Map<String, ObserviumProblemDTO> activeByProblemId = new LinkedHashMap<>();

        for (ObserviumProblemDTO dto : dtos) {
            if (dto.isActive() && dto.getProblemId() != null && !dto.getProblemId().isBlank()) {
                activeByProblemId.put(dto.getProblemId(), dto);
            }
        }

        if (activeByProblemId.isEmpty()) {
            return List.of();
        }

        Map<String, ObserviumProblem> existingByProblemId = new LinkedHashMap<>();
        for (ObserviumProblem existing : problemRepository.findByProblemIdIn(activeByProblemId.keySet())) {
            existingByProblemId.put(existing.getProblemId(), existing);
        }

        List<ObserviumProblem> entities = new ArrayList<>();
        for (ObserviumProblemDTO dto : activeByProblemId.values()) {
            ObserviumProblem entity = existingByProblemId.getOrDefault(dto.getProblemId(), new ObserviumProblem());
            entity.setProblemId(dto.getProblemId());
            entity.setHostId(dto.getHostId());
            entity.setDevice(dto.getHost());
            entity.setDescription(dto.getDescription());
            entity.setSeverity(dto.getSeverity());
            entity.setActive(true);
            entity.setSource(dto.getSource());
            entity.setEventId(dto.getEventId());
            entity.setStartedAt(dto.getStartedAt());
            entity.setResolvedAt(dto.getResolvedAt());
            entities.add(entity);
        }

        return entities;
    }

    private List<ObserviumProblem> buildResolvedEntitiesToSave(List<ObserviumProblemDTO> dtos) {
        Map<String, ObserviumProblemDTO> byProblemId = new LinkedHashMap<>();

        for (ObserviumProblemDTO dto : dtos) {
            if (dto.getProblemId() != null && !dto.getProblemId().isBlank()) {
                byProblemId.put(dto.getProblemId(), dto);
            }
        }

        List<ObserviumProblem> resolvedEntities = new ArrayList<>();
        for (ObserviumProblem existing : problemRepository.findBySourceAndActiveTrue("OBSERVIUM")) {
            ObserviumProblemDTO dto = byProblemId.get(existing.getProblemId());
            if (dto != null) {
                existing.setHostId(dto.getHostId());
                existing.setDevice(dto.getHost());
                existing.setDescription(dto.getDescription());
                existing.setSeverity(dto.getSeverity());
                existing.setActive(dto.isActive());
                existing.setSource(dto.getSource());
                existing.setEventId(dto.getEventId());
                existing.setStartedAt(dto.getStartedAt());
                existing.setResolvedAt(dto.getResolvedAt());
            } else {
                existing.setActive(false);
            }
            resolvedEntities.add(existing);
        }

        return resolvedEntities;
    }
}
