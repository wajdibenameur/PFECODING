package tn.iteam.monitoring.provider;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.iteam.adapter.observium.ObserviumAdapter;
import tn.iteam.dto.ObserviumProblemDTO;
import tn.iteam.dto.ServiceStatusDTO;
import tn.iteam.monitoring.MonitoringSourceType;
import tn.iteam.monitoring.dto.UnifiedMonitoringHostDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringMetricDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringProblemDTO;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ObserviumMonitoringProvider implements MonitoringProvider {

    private static final Logger log = LoggerFactory.getLogger(ObserviumMonitoringProvider.class);

    private final ObserviumAdapter observiumAdapter;

    @Override
    public MonitoringSourceType getSourceType() {
        return MonitoringSourceType.OBSERVIUM;
    }

    @Override
    public List<UnifiedMonitoringHostDTO> getHosts() {
        try {
            return observiumAdapter.fetchAll().stream()
                    .map(this::toHost)
                    .toList();
        } catch (Exception ex) {
            log.warn("Observium provider failed while fetching hosts: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    @Override
    public List<UnifiedMonitoringProblemDTO> getProblems() {
        try {
            return observiumAdapter.fetchProblems().stream()
                    .map(this::toProblem)
                    .toList();
        } catch (Exception ex) {
            log.warn("Observium provider failed while fetching problems: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    @Override
    public List<UnifiedMonitoringMetricDTO> getMetrics() {
        return List.of();
    }

    private UnifiedMonitoringHostDTO toHost(ServiceStatusDTO dto) {
        String hostName = normalizeText(dto.getName());
        String ip = normalizeIp(dto.getIp());
        String hostKey = hostName != null ? hostName : (ip != null ? ip : "UNKNOWN");

        return UnifiedMonitoringHostDTO.builder()
                .id(getSourceType() + ":" + hostKey)
                .source(getSourceType())
                .hostId(hostKey)
                .name(hostName != null ? hostName : hostKey)
                .ip(ip)
                .port(null)
                .protocol(null)
                .status(normalizeText(dto.getStatus()))
                .category(normalizeText(dto.getCategory()))
                .build();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank() || "IP_UNKNOWN".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private UnifiedMonitoringProblemDTO toProblem(ObserviumProblemDTO dto) {
        String hostName = dto.getHost() != null && !dto.getHost().isBlank() ? dto.getHost() : "UNKNOWN";
        String hostKey = dto.getHostId() != null && !dto.getHostId().isBlank() ? dto.getHostId() : hostName;

        return UnifiedMonitoringProblemDTO.builder()
                .id(getSourceType() + ":" + dto.getProblemId())
                .source(getSourceType())
                .problemId(dto.getProblemId())
                .eventId(dto.getEventId())
                .hostId(hostKey)
                .hostName(hostName)
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .active(dto.isActive())
                .status(dto.isActive() ? "ACTIVE" : "RESOLVED")
                .build();
    }
}
