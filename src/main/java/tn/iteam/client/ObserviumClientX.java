package tn.iteam.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.iteam.domain.ApiResponse;
import tn.iteam.exception.IntegrationResponseException;
import tn.iteam.exception.IntegrationTimeoutException;
import tn.iteam.exception.IntegrationUnavailableException;
import tn.iteam.service.SourceAvailabilityService;
import tn.iteam.util.IntegrationClientSupport;

@Slf4j
@Component
public class ObserviumClientX {

    private static final String SOURCE = "OBSERVIUM";
    private static final String SOURCE_LABEL = "Observium";
    private static final String DEVICES_ENDPOINT = "/api/v0/devices";
    private static final String ALERTS_ENDPOINT = "/api/v0/alerts";
    private static final String X_AUTH_TOKEN = "X-Auth-Token";
    private static final String DEVICES_SUCCESS_MESSAGE = "Devices fetched successfully";
    private static final String ALERTS_SUCCESS_MESSAGE = "Alerts fetched successfully";
    private static final String EMPTY_RESPONSE_PREFIX = "Empty response from Observium: ";
    private static final String NULL_JSON_ROOT_TEMPLATE = "Observium returned a null JSON root for %s";
    private static final String MISSING_FIELD_TEMPLATE = "Observium response missing '%s' field for %s (status=%s)";
    private static final String NOT_ARRAY_TEMPLATE = "Observium response field '%s' is not an array for %s";
    private static final String MISSING_FIELD_LOG_TEMPLATE =
            "Observium response for {} is missing expected field '{}' (status={})";
    private static final String NOT_ARRAY_LOG_TEMPLATE =
            "Observium response field '{}' for {} is not an array (type={})";
    private static final String HTTP_ERROR_LOG_TEMPLATE = "Observium HTTP error on {}: {}";
    private static final String TIMEOUT_LOG_TEMPLATE = "Observium timeout/unreachable on {}: {}";
    private static final String INVALID_JSON_LOG_TEMPLATE = "Observium invalid JSON on {}: {}";
    private static final String TRANSPORT_ERROR_LOG_TEMPLATE = "Observium transport error on {}: {}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SourceAvailabilityService availabilityService;
    private final String baseUrl;
    private final String token;

    public ObserviumClientX(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            SourceAvailabilityService availabilityService,
            @Value("${observium.url}") String baseUrl,
            @Value("${observium.token}") String token
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.availabilityService = availabilityService;
        this.baseUrl = baseUrl;
        this.token = token;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(X_AUTH_TOKEN, token);
        return headers;
    }

    public ApiResponse<JsonNode> getDevices() {
        try {
            JsonNode data = callApi(DEVICES_ENDPOINT, IntegrationClientSupport.DEVICES_FIELD, true);
            return ApiResponse.<JsonNode>builder()
                    .success(true)
                    .source(SOURCE)
                    .message(DEVICES_SUCCESS_MESSAGE)
                    .data(data)
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Observium devices request failed, returning empty dataset: {}", ex.getMessage());
            return ApiResponse.<JsonNode>builder()
                    .success(false)
                    .source(SOURCE)
                    .message(ex.getMessage())
                    .data(objectMapper.createArrayNode())
                    .build();
        }
    }

    public ApiResponse<JsonNode> getAlerts() {
        try {
            JsonNode data = callApi(ALERTS_ENDPOINT, IntegrationClientSupport.ALERTS_FIELD, false);
            return ApiResponse.<JsonNode>builder()
                    .success(true)
                    .source(SOURCE)
                    .message(ALERTS_SUCCESS_MESSAGE)
                    .data(data)
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Observium alerts request failed, returning empty dataset: {}", ex.getMessage());
            return ApiResponse.<JsonNode>builder()
                    .success(false)
                    .source(SOURCE)
                    .message(ex.getMessage())
                    .data(objectMapper.createArrayNode())
                    .build();
        }
    }

    private JsonNode callApi(String endpoint, String responseField, boolean allowObjectCollection) {
        String url = baseUrl + endpoint;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class
            );

            if (response.getBody() == null) {
                throw new IntegrationResponseException(SOURCE, EMPTY_RESPONSE_PREFIX + endpoint);
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root == null || root.isNull()) {
                throw new IntegrationResponseException(SOURCE, NULL_JSON_ROOT_TEMPLATE.formatted(endpoint));
            }

            JsonNode data = root.get(responseField);
            if (data == null || data.isNull()) {
                String status = extractStatus(root);
                log.warn(MISSING_FIELD_LOG_TEMPLATE, endpoint, responseField, status);
                throw new IntegrationResponseException(SOURCE, MISSING_FIELD_TEMPLATE.formatted(responseField, endpoint, status));
            }

            if (data.isArray()) {
                markAvailable();
                return data;
            }

            if ((allowObjectCollection || IntegrationClientSupport.ALERTS_FIELD.equals(responseField)) && data.isObject()) {
                markAvailable();
                return normalizeObjectCollection(data);
            }

            log.warn(NOT_ARRAY_LOG_TEMPLATE, responseField, endpoint, data.getNodeType());
            throw new IntegrationResponseException(SOURCE, NOT_ARRAY_TEMPLATE.formatted(responseField, endpoint));

        } catch (HttpStatusCodeException ex) {
            int statusCode = ex.getStatusCode().value();
            markUnavailable(IntegrationClientSupport.httpOn(endpoint, statusCode));
            log.warn(HTTP_ERROR_LOG_TEMPLATE, endpoint, statusCode);
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.returnedHttp(SOURCE_LABEL, statusCode, endpoint),
                    ex
            );
        } catch (ResourceAccessException ex) {
            markUnavailable(IntegrationClientSupport.timeoutOn(endpoint));
            log.warn(TIMEOUT_LOG_TEMPLATE, endpoint, ex.getMessage());
            throw new IntegrationTimeoutException(SOURCE, IntegrationClientSupport.timeout(SOURCE_LABEL, endpoint), ex);
        } catch (JsonProcessingException ex) {
            markUnavailable(IntegrationClientSupport.invalidJsonOn(endpoint));
            log.warn(INVALID_JSON_LOG_TEMPLATE, endpoint, ex.getOriginalMessage());
            throw new IntegrationResponseException(
                    SOURCE,
                    IntegrationClientSupport.invalidJsonResponse(SOURCE_LABEL, endpoint),
                    ex
            );
        } catch (RestClientException ex) {
            markUnavailable(IntegrationClientSupport.transportErrorOn(endpoint));
            log.warn(TRANSPORT_ERROR_LOG_TEMPLATE, endpoint, ex.getMessage());
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.unreachable(SOURCE_LABEL, endpoint),
                    ex
            );
        }
    }

    private void markAvailable() {
        availabilityService.markAvailable(SOURCE);
    }

    private void markUnavailable(String reason) {
        availabilityService.markUnavailable(SOURCE, reason);
    }

    private String extractStatus(JsonNode root) {
        JsonNode statusNode = root.get(IntegrationClientSupport.STATUS_FIELD);
        return statusNode == null || statusNode.isNull()
                ? IntegrationClientSupport.UNKNOWN
                : statusNode.asText();
    }

    private JsonNode normalizeObjectCollection(JsonNode data) {
        ArrayNode normalized = objectMapper.createArrayNode();
        data.elements().forEachRemaining(normalized::add);
        return normalized;
    }
}
