package tn.iteam.service.impl;

import org.springframework.stereotype.Service;
import tn.iteam.dto.SourceAvailabilityDTO;
import tn.iteam.service.SourceAvailabilityService;
import tn.iteam.websocket.MonitoringWebSocketPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SourceAvailabilityServiceImpl implements SourceAvailabilityService {

    private static final String ZABBIX = "ZABBIX";
    private static final String OBSERVIUM = "OBSERVIUM";
    private static final String ZKBIO = "ZKBIO";
    private static final String CAMERA = "CAMERA";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String UNKNOWN_INTEGRATION_ERROR = "Unknown integration error";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String UNAVAILABLE = "UNAVAILABLE";

    private static final List<String> KNOWN_SOURCES = List.of(ZABBIX, OBSERVIUM, ZKBIO, CAMERA);

    private final Map<String, AvailabilityState> states = new ConcurrentHashMap<>();
    private final MonitoringWebSocketPublisher publisher;

    public SourceAvailabilityServiceImpl(MonitoringWebSocketPublisher publisher) {
        this.publisher = publisher;
        for (String source : KNOWN_SOURCES) {
            states.put(source, new AvailabilityState(true, null, null));
        }
    }

    @Override
    public void markAvailable(String source) {
        String normalized = normalize(source);
        AvailabilityState previous = getState(normalized);
        AvailabilityState next = new AvailabilityState(true, null, null);
        states.put(normalized, next);

        if (!previous.available()) {
            publisher.publishSourceAvailability(toDto(normalized, next, Instant.now()));
        }
    }

    @Override
    public void markUnavailable(String source, String errorMessage) {
        String normalized = normalize(source);
        String safeMessage = errorMessage == null || errorMessage.isBlank()
                ? UNKNOWN_INTEGRATION_ERROR
                : errorMessage;
        Instant failureAt = Instant.now();
        AvailabilityState previous = getState(normalized);
        AvailabilityState next = new AvailabilityState(false, safeMessage, failureAt);
        states.put(normalized, next);

        if (previous.available()) {
            publisher.publishSourceAvailability(toDto(normalized, next, failureAt));
        }
    }

    @Override
    public boolean isAvailable(String source) {
        return getState(source).available();
    }

    @Override
    public String getLastError(String source) {
        return getState(source).lastError();
    }

    @Override
    public Instant getLastFailureAt(String source) {
        return getState(source).lastFailureAt();
    }

    @Override
    public boolean shouldAttempt(String source, long retryBackoffMs) {
        return !isRetryCooldownActive(source, retryBackoffMs);
    }

    @Override
    public boolean isRetryCooldownActive(String source, long retryBackoffMs) {
        if (retryBackoffMs <= 0 || isAvailable(source)) {
            return false;
        }

        Instant lastFailureAt = getLastFailureAt(source);
        if (lastFailureAt == null) {
            return false;
        }

        long elapsedMs = Instant.now().toEpochMilli() - lastFailureAt.toEpochMilli();
        return elapsedMs < retryBackoffMs;
    }

    @Override
    public List<SourceAvailabilityDTO> getAll() {
        return KNOWN_SOURCES.stream().map(this::get).toList();
    }

    @Override
    public SourceAvailabilityDTO get(String source) {
        AvailabilityState state = getState(source);
        return toDto(normalize(source), state, state.lastFailureAt());
    }

    private AvailabilityState getState(String source) {
        return states.getOrDefault(normalize(source), new AvailabilityState(true, null, null));
    }

    private String normalize(String source) {
        return source == null ? UNKNOWN : source.trim().toUpperCase();
    }

    private SourceAvailabilityDTO toDto(String source, AvailabilityState state, Instant timestamp) {
        return SourceAvailabilityDTO.builder()
                .source(source)
                .available(state.available())
                .status(state.available() ? AVAILABLE : UNAVAILABLE)
                .message(state.available() ? null : state.lastError())
                .lastError(state.lastError())
                .lastFailureAt(state.lastFailureAt())
                .timestamp(timestamp)
                .build();
    }

    private record AvailabilityState(boolean available, String lastError, Instant lastFailureAt) {
    }
}
