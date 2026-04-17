package tn.iteam.monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.iteam.monitoring.MonitoringSourceType;
import tn.iteam.monitoring.dto.UnifiedMonitoringHostDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringMetricDTO;
import tn.iteam.monitoring.dto.UnifiedMonitoringProblemDTO;
import tn.iteam.monitoring.provider.MonitoringProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class MonitoringAggregationService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringAggregationService.class);

    private final List<MonitoringProvider> providers;

    public MonitoringAggregationService(List<MonitoringProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<UnifiedMonitoringProblemDTO> getProblems(String source) {
        return providersFor(source).stream()
                .flatMap(provider -> safeFetch(provider, "problems", provider::getProblems).stream())
                .sorted(Comparator.comparing(UnifiedMonitoringProblemDTO::getStartedAt, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    public List<UnifiedMonitoringMetricDTO> getMetrics(String source) {
        return providersFor(source).stream()
                .flatMap(provider -> safeFetch(provider, "metrics", provider::getMetrics).stream())
                .sorted(Comparator.comparing(UnifiedMonitoringMetricDTO::getTimestamp, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    public List<UnifiedMonitoringHostDTO> getHosts(String source) {
        return providersFor(source).stream()
                .flatMap(provider -> safeFetch(provider, "hosts", provider::getHosts).stream())
                .sorted(Comparator.comparing(UnifiedMonitoringHostDTO::getSource).thenComparing(UnifiedMonitoringHostDTO::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<UnifiedMonitoringProblemDTO> getProblems(MonitoringSourceType source) {
        return getProblems(source != null ? source.name() : null);
    }

    public List<UnifiedMonitoringMetricDTO> getMetrics(MonitoringSourceType source) {
        return getMetrics(source != null ? source.name() : null);
    }

    public List<UnifiedMonitoringHostDTO> getHosts(MonitoringSourceType source) {
        return getHosts(source != null ? source.name() : null);
    }

    private <T> List<T> safeFetch(MonitoringProvider provider, String operation, Supplier<List<T>> fetcher) {
        try {
            return fetcher.get();
        } catch (Exception ex) {
            log.warn("Monitoring provider {} failed while fetching {}: {}",
                    provider.getSourceType(), operation, ex.getMessage(), ex);
            return List.of();
        }
    }

    private List<MonitoringProvider> providersFor(String source) {
        if (source == null || source.isBlank() || "ALL".equalsIgnoreCase(source)) {
            return providers;
        }

        MonitoringSourceType requested = MonitoringSourceType.valueOf(source.trim().toUpperCase(Locale.ROOT));
        return providers.stream()
                .filter(provider -> provider.getSourceType() == requested)
                .toList();
    }
}
