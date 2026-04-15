package tn.iteam.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iteam.adapter.zabbix.ZabbixAdapter;
import tn.iteam.domain.ZabbixProblem;
import tn.iteam.dto.ZabbixProblemDTO;
import tn.iteam.exception.IntegrationException;
import tn.iteam.mapper.ZabbixProblemMapper;
import tn.iteam.repository.ZabbixProblemRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ZabbixProblemService {

    private static final Logger log = LoggerFactory.getLogger(ZabbixProblemService.class);
    private static final List<String> EXPOSED_SEVERITIES = List.of("4", "5");

    private final ZabbixAdapter zabbixAdapter;
    private final ZabbixProblemMapper mapper;
    private final ZabbixProblemRepository repository;
    private final SourceAvailabilityService availabilityService;
    private final ZabbixDataQualityService dataQualityService;

    public List<ZabbixProblemDTO> getPersistedFilteredActiveProblems() {
        return repository.findByActiveTrueAndSeverityIn(EXPOSED_SEVERITIES).stream()
                .map(mapper::toDTO)
                .toList();
    }

    public List<ZabbixProblemDTO> synchronizeAndGetPersistedFilteredActiveProblems() {
        synchronizeActiveProblemsFromZabbix();
        return getPersistedFilteredActiveProblems();
    }

    @Transactional
    public List<ZabbixProblemDTO> synchronizeActiveProblemsFromZabbix() {
        return synchronizeActiveProblemsFromZabbix(null);
    }

    @Transactional
    public List<ZabbixProblemDTO> synchronizeActiveProblemsFromZabbix(JsonNode hosts) {
        log.info("Fetching active Zabbix problems");

        try {
            List<ZabbixProblemDTO> dtos = hosts == null
                    ? zabbixAdapter.fetchProblems()
                    : zabbixAdapter.fetchProblems(hosts);
            List<ZabbixProblem> entitiesToSave = new ArrayList<>();
            Set<String> liveProblemIds = new HashSet<>();

            for (ZabbixProblemDTO dto : dtos) {
                ZabbixProblemDTO sanitized = sanitize(dto);
                if (sanitized == null) {
                    continue;
                }

                liveProblemIds.add(sanitized.getProblemId());

                ZabbixProblem entity = mapper.toEntity(sanitized);
                List<ZabbixProblem> existingList = repository.findAllByProblemId(entity.getProblemId()).stream()
                        .sorted(Comparator.comparing(ZabbixProblem::getId))
                        .toList();

                if (!existingList.isEmpty()) {
                    ZabbixProblem existing = existingList.get(existingList.size() - 1);

                    existing.setHostId(entity.getHostId());
                    existing.setHost(entity.getHost());
                    existing.setDescription(entity.getDescription());
                    existing.setSeverity(entity.getSeverity());
                    existing.setActive(entity.getActive());
                    existing.setSource(entity.getSource());
                    existing.setEventId(entity.getEventId());
                    existing.setIp(entity.getIp());
                    existing.setPort(entity.getPort());
                    existing.setStartedAt(entity.getStartedAt());
                    existing.setResolvedAt(entity.getResolvedAt());
                    existing.setStatus(entity.getStatus());

                    if (existingList.size() > 1) {
                        log.warn("Duplicate problemId={} found in DB: {} rows", entity.getProblemId(), existingList.size());
                    }
                    entitiesToSave.add(existing);
                } else {
                    entitiesToSave.add(entity);
                }
            }

            long resolvedAt = Instant.now().getEpochSecond();
            for (ZabbixProblem persistedActive : repository.findByActiveTrue()) {
                if (persistedActive.getProblemId() == null || liveProblemIds.contains(persistedActive.getProblemId())) {
                    continue;
                }

                persistedActive.setActive(false);
                if (persistedActive.getResolvedAt() == null || persistedActive.getResolvedAt() == 0) {
                    persistedActive.setResolvedAt(resolvedAt);
                }
                persistedActive.setStatus("RESOLVED");
                entitiesToSave.add(persistedActive);
            }

            List<ZabbixProblem> saved = repository.saveAll(entitiesToSave);
            availabilityService.markAvailable("ZABBIX");
            dataQualityService.logProblemQualitySummary(saved);
            log.info("Saved {} Zabbix problems, active in live feed={}", saved.size(), liveProblemIds.size());

            return dtos;
        } catch (IntegrationException e) {
            availabilityService.markUnavailable("ZABBIX", e.getMessage());
            log.warn("Zabbix problems synchronization failed: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            availabilityService.markUnavailable("ZABBIX", e.getMessage());
            log.error("Unexpected error fetching Zabbix problems", e);
            return List.of();
        }
    }

    private ZabbixProblemDTO sanitize(ZabbixProblemDTO dto) {
        if (dto == null) {
            return null;
        }

        if (dto.getProblemId() == null || dto.getProblemId().isBlank()) {
            log.warn("Skipping problem with empty problemId: {}", dto);
            return null;
        }

        if (dto.getHostId() == null || dto.getHostId().isBlank()) {
            log.warn("Skipping problem {} because hostId is missing", dto.getProblemId());
            return null;
        }

        long fallbackNow = Instant.now().getEpochSecond();
        Long startedAt = dto.getStartedAt();
        if (startedAt == null || startedAt <= 0) {
            log.warn("Problem {} missing startedAt from Zabbix, applying fallback current time", dto.getProblemId());
            startedAt = fallbackNow;
        }

        boolean active = dto.getActive() == null || dto.getActive();
        String status = dto.getStatus();
        if (status == null || status.isBlank()) {
            status = active ? "ACTIVE" : "RESOLVED";
        }

        Long resolvedAt = dto.getResolvedAt();
        if ("RESOLVED".equalsIgnoreCase(status) && (resolvedAt == null || resolvedAt <= 0)) {
            resolvedAt = fallbackNow;
            log.warn("Problem {} marked RESOLVED without resolvedAt, applying fallback current time", dto.getProblemId());
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            resolvedAt = null;
        }

        String severity = dto.getSeverity();
        if (severity == null || severity.isBlank()) {
            severity = "0";
            log.warn("Problem {} missing severity, applying fallback severity=0", dto.getProblemId());
        }

        Long eventId = dto.getEventId();
        if (eventId == null || eventId <= 0) {
            try {
                eventId = Long.parseLong(dto.getProblemId());
            } catch (NumberFormatException ex) {
                eventId = fallbackNow;
            }
            log.warn("Problem {} missing eventId, applying fallback value={}", dto.getProblemId(), eventId);
        }

        return ZabbixProblemDTO.builder()
                .problemId(dto.getProblemId())
                .host(dto.getHost() == null || dto.getHost().isBlank() ? "UNKNOWN" : dto.getHost())
                .port(dto.getPort())
                .hostId(dto.getHostId())
                .description(dto.getDescription() == null || dto.getDescription().isBlank() ? "NO_DESCRIPTION" : dto.getDescription())
                .severity(severity)
                .active(active)
                .source(dto.getSource() == null || dto.getSource().isBlank() ? "Zabbix" : dto.getSource())
                .eventId(eventId)
                .ip(dto.getIp())
                .startedAt(startedAt)
                .startedAtFormatted(dto.getStartedAtFormatted())
                .resolvedAt(resolvedAt)
                .resolvedAtFormatted(dto.getResolvedAtFormatted())
                .status(status.toUpperCase())
                .build();
    }
}
