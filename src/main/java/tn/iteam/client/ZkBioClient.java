package tn.iteam.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tn.iteam.cache.IntegrationCacheService;
import tn.iteam.exception.IntegrationResponseException;
import tn.iteam.exception.IntegrationTimeoutException;
import tn.iteam.exception.IntegrationUnavailableException;
import tn.iteam.service.SourceAvailabilityService;
import tn.iteam.util.IntegrationClientSupport;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Client pour l'API ZKBio Time
 * Documentation: https://zkbioserver.docs.apiary.io/
 * 
 * Endpoints typiques:
 * - /api/v1/device/list - Liste des appareils
 * - /api/v1/attendance/log - Journaux de présence
 * - /api/v1/event/push - Événements en temps réel
 */
@Slf4j
@Component
public class ZkBioClient {

    private static final String RESILIENCE_INSTANCE = "zkbioApi";
    private static final String SOURCE = "ZKBIO";
    private static final String SOURCE_LABEL = "ZKBio";
    private static final String SNAPSHOT_PREFIX = "zkbio";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final IntegrationCacheService integrationCacheService;
    private final SourceAvailabilityService availabilityService;

    @Value("${zkbio.url}")
    private String baseUrl;

    @Value("${zkbio.token:}")
    private String apiToken;

    // ==================== ENDPOINTS ZKBIO TIME ====================
    private static final String DEVICE_LIST_ENDPOINT = "/api/v1/device/list";
    private static final String ATTENDANCE_LOG_ENDPOINT = "/api/v1/attendance/log";
    private static final String EVENT_ENDPOINT = "/api/v1/event/push";
    private static final String USER_LIST_ENDPOINT = "/api/v1/user/list";
    private static final String STATUS_ENDPOINT = "/api/v1/status";

    public ZkBioClient(
            @Qualifier("zkbioUnsafeTlsWebClientForInternalUseOnly") WebClient webClient,
            ObjectMapper objectMapper,
            IntegrationCacheService integrationCacheService,
            SourceAvailabilityService availabilityService
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.integrationCacheService = integrationCacheService;
        this.availabilityService = availabilityService;
        log.warn("ZkBioClient is using the dedicated unsafe TLS WebClient reserved for ZKBio only.");
    }

    public URI getBaseUri() {
        try {
            return URI.create(baseUrl);
        } catch (Exception ex) {
            log.warn("Invalid ZKBio base URL: {}", baseUrl);
            return null;
        }
    }

    // ==================== HEADERS ====================
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiToken != null && !apiToken.isEmpty()) {
            headers.setBearerAuth(apiToken);
        }
        return headers;
    }

    private String postForString(String endpoint, Map<String, Object> payload) {
        return webClient.post()
                .uri(baseUrl + endpoint)
                .headers(headers -> headers.addAll(createHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String getForString(String endpoint) {
        return webClient.get()
                .uri(baseUrl + endpoint)
                .headers(headers -> headers.addAll(createHeaders()))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private JsonNode executeListRequest(String endpoint, Map<String, Object> payload, String context) {
        try {
            String responseBody = postForString(endpoint, payload);
            if (responseBody == null) {
                throw new IntegrationResponseException(SOURCE, "Empty response from ZKBio during " + context);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode normalized = extractListPayload(root);
            saveSnapshot(context, normalized);
            markAvailable();
            return normalized;
        } catch (WebClientResponseException ex) {
            markUnavailable(IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, ex.getStatusCode().value(), context));
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, ex.getStatusCode().value(), context),
                    ex
            );
        } catch (WebClientRequestException ex) {
            markUnavailable(IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context));
            throw new IntegrationTimeoutException(SOURCE, IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context), ex);
        } catch (JsonProcessingException ex) {
            markUnavailable("Invalid JSON response from ZKBio during " + context);
            throw new IntegrationResponseException(SOURCE, "Invalid JSON response from ZKBio during " + context, ex);
        } catch (IntegrationUnavailableException | IntegrationTimeoutException | IntegrationResponseException ex) {
            markUnavailable(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markUnavailable("Unexpected error while calling ZKBio during " + context);
            throw new IntegrationUnavailableException(SOURCE, "Unexpected error while calling ZKBio during " + context, ex);
        }
    }

    private JsonNode executeObjectRequest(String endpoint, String context) {
        try {
            String responseBody = getForString(endpoint);
            if (responseBody == null) {
                throw new IntegrationResponseException(SOURCE, "Empty response from ZKBio during " + context);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            saveSnapshot(context, root);
            markAvailable();
            return root;
        } catch (WebClientResponseException ex) {
            markUnavailable(IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, ex.getStatusCode().value(), context));
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, ex.getStatusCode().value(), context),
                    ex
            );
        } catch (WebClientRequestException ex) {
            markUnavailable(IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context));
            throw new IntegrationTimeoutException(SOURCE, IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context), ex);
        } catch (JsonProcessingException ex) {
            markUnavailable("Invalid JSON response from ZKBio during " + context);
            throw new IntegrationResponseException(SOURCE, "Invalid JSON response from ZKBio during " + context, ex);
        } catch (IntegrationUnavailableException | IntegrationTimeoutException | IntegrationResponseException ex) {
            markUnavailable(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markUnavailable("Unexpected error while calling ZKBio during " + context);
            throw new IntegrationUnavailableException(SOURCE, "Unexpected error while calling ZKBio during " + context, ex);
        }
    }

    private JsonNode extractListPayload(JsonNode root) {
        if (root == null || root.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (root.has("data") && root.get("data").has("list")) {
            return root.get("data").get("list");
        }
        if (root.has("data") && root.get("data").isArray()) {
            return root.get("data");
        }
        return root;
    }

    private JsonNode getCachedSnapshot(String context, JsonNode defaultValue, Throwable throwable) {
        return integrationCacheService.getSnapshot(SOURCE, SNAPSHOT_PREFIX + ":" + context, JsonNode.class)
                .map(snapshot -> {
                    log.warn("ZKBio fallback using Redis snapshot for {}: {}", context, throwable.getMessage());
                    markUnavailable("ZKBio API unavailable, serving Redis snapshot for " + context);
                    return snapshot;
                })
                .orElseGet(() -> {
                    log.warn("ZKBio fallback has no Redis snapshot for {}: {}", context, throwable.getMessage());
                    markUnavailable("ZKBio API unavailable and no usable Redis snapshot for " + context);
                    return defaultValue;
                });
    }

    private void saveSnapshot(String context, JsonNode data) {
        integrationCacheService.saveSnapshot(SOURCE, SNAPSHOT_PREFIX + ":" + context, data);
    }

    private void markAvailable() {
        availabilityService.markAvailable(SOURCE);
    }

    private void markUnavailable(String message) {
        availabilityService.markUnavailable(SOURCE, message);
    }

    // ==================== DEVICE STATUS ====================
    
    /**
     * Récupère la liste des appareils ZKBio
     * Retourne les appareils avec leur statut (en ligne/hors ligne)
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "devicesFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "devicesFallback")
    public JsonNode getDevices() {
        log.debug("Fetching devices from ZKBio: {}", baseUrl + DEVICE_LIST_ENDPOINT);
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("size", 100);
        return executeListRequest(DEVICE_LIST_ENDPOINT, payload, "devices");
    }

    /**
     * Vérifie le statut du serveur ZKBio
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "statusFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "statusFallback")
    public JsonNode getStatus() {
        log.debug("Fetching ZKBio server status");
        return executeObjectRequest(STATUS_ENDPOINT, "status");
    }

    // ==================== ATTENDANCE LOGS ====================
    
    /**
     * Récupère les journaux de présence (événements de pointage)
     * Inclut: entrées, sorties, échecs, etc.
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "attendanceFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "attendanceFallback")
    public JsonNode getAttendanceLogs() {
        log.debug("Fetching attendance logs from ZKBio");
        long endTime = Instant.now().getEpochSecond();
        long startTime = endTime - 86400;
        return getAttendanceLogs(startTime, endTime);
    }

    /**
     * Récupère les journaux de présence pour une période spécifique
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "attendanceWindowFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "attendanceWindowFallback")
    public JsonNode getAttendanceLogs(long startTime, long endTime) {
        log.debug("Fetching attendance logs from {} to {}", startTime, endTime);
        Map<String, Object> payload = new HashMap<>();
        payload.put("start_time", startTime);
        payload.put("end_time", endTime);
        payload.put("page", 1);
        payload.put("size", 500);
        return executeListRequest(ATTENDANCE_LOG_ENDPOINT, payload, "attendance-logs");
    }

    // ==================== ALERTS / EVENTS ====================
    
    /**
     * Récupère les alertes/événements (problèmes, erreurs, etc.)
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "alertsFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "alertsFallback")
    public JsonNode getAlerts() {
        log.debug("Fetching alerts from ZKBio");
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("size", 100);
        return executeListRequest(EVENT_ENDPOINT, payload, "alerts");
    }

    // ==================== USERS ====================
    
    /**
     * Récupère la liste des utilisateurs enregistrés
     */
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "usersFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "usersFallback")
    public JsonNode getUsers() {
        log.debug("Fetching users from ZKBio");
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("size", 100);
        return executeListRequest(USER_LIST_ENDPOINT, payload, "users");
    }

    private JsonNode devicesFallback(Throwable throwable) {
        return getCachedSnapshot("devices", null, throwable);
    }

    private JsonNode statusFallback(Throwable throwable) {
        return getCachedSnapshot("status", objectMapper.createObjectNode().put("status", "offline"), throwable);
    }

    private JsonNode attendanceFallback(Throwable throwable) {
        return getCachedSnapshot("attendance-logs", null, throwable);
    }

    private JsonNode attendanceWindowFallback(long ignoredStartTime, long ignoredEndTime, Throwable throwable) {
        return attendanceFallback(throwable);
    }

    private JsonNode alertsFallback(Throwable throwable) {
        return integrationCacheService.getSnapshot(SOURCE, SNAPSHOT_PREFIX + ":alerts", JsonNode.class)
                .or(() -> integrationCacheService.getSnapshot(SOURCE, SNAPSHOT_PREFIX + ":attendance-logs", JsonNode.class))
                .map(snapshot -> {
                    log.warn("ZKBio fallback using Redis snapshot for alerts: {}", throwable.getMessage());
                    markUnavailable("ZKBio API unavailable, serving Redis snapshot for alerts");
                    return snapshot;
                })
                .orElseGet(() -> {
                    log.warn("ZKBio fallback has no Redis snapshot for alerts: {}", throwable.getMessage());
                    markUnavailable("ZKBio API unavailable and no usable Redis snapshot for alerts");
                    return null;
                });
    }

    private JsonNode usersFallback(Throwable throwable) {
        return getCachedSnapshot("users", null, throwable);
    }
}
