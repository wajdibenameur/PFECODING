package tn.iteam.monitoring.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.iteam.domain.MonitoredHost;
import tn.iteam.domain.ZabbixMetric;
import tn.iteam.monitoring.MonitoringSourceType;
import tn.iteam.monitoring.dto.UnifiedMonitoringHostDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringMetricDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringProblemDTO;
import tn.iteam.repository.MonitoredHostRepository;
import tn.iteam.service.ZabbixProblemService;
import tn.iteam.service.ZabbixMetricsService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ZabbixMonitoringProvider implements MonitoringProvider {

    private final ZabbixMetricsService zabbixMetricsService;
    private final ZabbixProblemService zabbixProblemService;
    private final MonitoredHostRepository monitoredHostRepository;

    @Override
    public MonitoringSourceType getSourceType() {
        return MonitoringSourceType.ZABBIX;
    }

    @Override
    public List<UnifiedMonitoringHostDTO> getHosts() {
        return monitoredHostRepository.findBySourceOrderByNameAsc("ZABBIX").stream()
                .map(this::toHost)
                .toList();
    }

    @Override
    public List<UnifiedMonitoringProblemDTO> getProblems() {
        return zabbixProblemService.getPersistedFilteredActiveProblems().stream()
                .map(this::toProblem)
                .toList();
    }

    @Override
    public List<UnifiedMonitoringMetricDTO> getMetrics() {
        return zabbixMetricsService.getPersistedMetricsSnapshot().stream()
                .map(this::toMetric)
                .toList();
    }

    private UnifiedMonitoringHostDTO toHost(MonitoredHost host) {
        return UnifiedMonitoringHostDTO.builder()
                .id(getSourceType() + ":" + host.getHostId())
                .source(getSourceType())
                .hostId(host.getHostId())
                .name(host.getName())
                .ip(host.getIp())
                .port(host.getPort())
                .status("UNKNOWN")
                .category("SERVER")
                .build();
    }

    private UnifiedMonitoringProblemDTO toProblem(tn.iteam.dto.ZabbixProblemDTO dto) {
        return UnifiedMonitoringProblemDTO.builder()
                .id(getSourceType() + ":" + dto.getProblemId())
                .source(getSourceType())
                .problemId(dto.getProblemId())
                .eventId(dto.getEventId())
                .hostId(dto.getHostId())
                .hostName(dto.getHost())
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .active(Boolean.TRUE.equals(dto.getActive()))
                .status(dto.getStatus())
                .ip(dto.getIp())
                .port(dto.getPort())
                .startedAt(dto.getStartedAt())
                .startedAtFormatted(dto.getStartedAtFormatted())
                .resolvedAt(dto.getResolvedAt())
                .resolvedAtFormatted(dto.getResolvedAtFormatted())
                .build();
    }

    private UnifiedMonitoringMetricDTO toMetric(ZabbixMetric entity) {
        return UnifiedMonitoringMetricDTO.builder()
                .id(getSourceType() + ":" + entity.getHostId() + ":" + entity.getItemId() + ":" + entity.getTimestamp())
                .source(getSourceType())
                .hostId(entity.getHostId())
                .hostName(entity.getHostName())
                .itemId(entity.getItemId())
                .metricKey(entity.getMetricKey())
                .value(entity.getValue())
                .timestamp(entity.getTimestamp())
                .ip(entity.getIp())
                .port(entity.getPort())
                .build();
    }
}
