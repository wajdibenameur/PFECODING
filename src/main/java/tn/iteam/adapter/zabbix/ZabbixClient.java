package tn.iteam.adapter.zabbix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class ZabbixClient {

    private static final Logger log = LoggerFactory.getLogger(ZabbixClient.class);
    private static final String RESILIENCE_INSTANCE = "zabbixApi";
    private static final String SOURCE = "ZABBIX";
    private static final String SOURCE_LABEL = "Zabbix";
    private static final String LOG_PREFIX = "[ZABBIX] ";
    private static final String JSON_RPC = "jsonrpc";
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String METHOD = "method";
    private static final String ID = "id";
    private static final int REQUEST_ID = 1;
    private static final String PARAMS = "params";
    private static final String OUTPUT = "output";
    private static final String SELECT_INTERFACES = "selectInterfaces";
    private static final String HOSTIDS = "hostids";
    private static final String SELECT_HOSTS = "selectHosts";
    private static final String SELECT_TAGS = "selectTags";
    private static final String SORT_FIELD = "sortfield";
    private static final String SORT_ORDER = "sortorder";
    private static final String LIMIT = "limit";
    private static final String RECENT = "recent";
    private static final String SEVERITIES = "severities";
    private static final String FILTER = "filter";
    private static final String ITEMIDS = "itemids";
    private static final String TIME_FROM = "time_from";
    private static final String TIME_TILL = "time_till";
    private static final String HISTORY = "history";
    private static final String HOST_ID = "hostid";
    private static final String HOST = "host";
    private static final String STATUS = "status";
    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String MAIN = "main";
    private static final String EXTEND = "extend";
    private static final String DESC = "DESC";
    private static final String ASC = "ASC";
    private static final String EVENT_ID = "eventid";
    private static final String CLOCK = "clock";
    private static final String ITEM_ID = "itemid";
    private static final String NAME = "name";
    private static final String KEY = "key_";
    private static final String VALUE_TYPE = "value_type";
    private static final String DESCRIPTION = "description";
    private static final String TRIGGER_ID = "triggerid";
    private static final String ERROR_DURING_PREFIX = "Zabbix API error";
    private static final String UNEXPECTED_RESPONSE_STRUCTURE_PREFIX = "Unexpected response structure";
    private static final String EMPTY_RESPONSE_TEMPLATE = "Empty response from Zabbix (%s)";
    private static final String INVALID_JSON_RESPONSE_PREFIX = "Invalid JSON response from Zabbix";
    private static final String UNABLE_TO_REACH_PREFIX = "Unable to reach Zabbix";
    private static final String UNEXPECTED_ERROR_PREFIX = "Unexpected error while calling Zabbix";
    private static final String GET_VERSION_CONTEXT = "getVersion";
    private static final String VERSION_API_TARGET = "version API";
    private static final String UNEXPECTED_VERSION_ERROR = "Unexpected error while fetching Zabbix version";
    private static final List<String> HOST_OUTPUT = List.of(HOST_ID, HOST, STATUS);
    private static final List<String> INTERFACE_OUTPUT = List.of(IP, PORT, MAIN);
    private static final List<String> PROBLEM_HOST_OUTPUT = List.of(HOST_ID, HOST);
    private static final List<Integer> HIGH_SEVERITIES = List.of(3, 4, 5);
    private static final List<String> ITEM_OUTPUT = List.of(ITEM_ID, NAME, KEY, VALUE_TYPE, HOST_ID);
    private static final Map<String, Integer> ACTIVE_STATUS_FILTER = Map.of(STATUS, 0);
    private static final String SNAPSHOT_PREFIX = "zabbix";

    private final WebClient webClient;
    private final IntegrationCacheService integrationCacheService;
    private final SourceAvailabilityService availabilityService;
    private final ObjectMapper objectMapper;

    @Value("${zabbix.url}")
    private String zabbixUrl;

    @Value("${zabbix.usertoken}")
    private String apiToken;

    public ZabbixClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            IntegrationCacheService integrationCacheService,
            SourceAvailabilityService availabilityService
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.integrationCacheService = integrationCacheService;
        this.availabilityService = availabilityService;
    }

    private Map<String, Object> createBasePayload(String method) {
        log.info(LOG_PREFIX + "Creating base payload for method={}", method);

        Map<String, Object> payload = new HashMap<>();
        payload.put(JSON_RPC, JSON_RPC_VERSION);
        payload.put(METHOD, method);
        payload.put(ID, REQUEST_ID);
        return payload;
    }

    private void logResultSummary(String context, JsonNode result) {
        if (result == null) {
            log.warn(LOG_PREFIX + "{} -> result is null", context);
            return;
        }

        if (result.isArray()) {
            log.info(LOG_PREFIX + "{} -> result array size={}", context, result.size());
        } else if (result.isObject()) {
            log.info(LOG_PREFIX + "{} -> result is object", context);
        } else {
            log.info(LOG_PREFIX + "{} -> result type={}, value={}", context, result.getNodeType(), result.asText());
        }
    }

    private JsonNode executeRequest(Map<String, Object> payload, String context, String snapshotKey) {
        try {
            String prettyPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            log.debug(LOG_PREFIX + "START context={} url={}", context, zabbixUrl);
            log.debug(LOG_PREFIX + "{} payload:\n{}", context, prettyPayload);

            String responseBody = postForString(payload, headers -> headers.addAll(createAuthenticatedHeaders()));

            if (responseBody == null) {
                throw new IntegrationResponseException(SOURCE, EMPTY_RESPONSE_TEMPLATE.formatted(context));
            }

            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has(IntegrationClientSupport.RESULT_FIELD)) {
                JsonNode result = root.get(IntegrationClientSupport.RESULT_FIELD);
                logResultSummary(context, result);
                saveSnapshot(snapshotKey, result);
                markAvailable();
                return result;
            }

            if (root.has(IntegrationClientSupport.ERROR_FIELD)) {
                markUnavailable(IntegrationClientSupport.duringMessage(ERROR_DURING_PREFIX, context));
                throw new IntegrationResponseException(
                        SOURCE,
                        ERROR_DURING_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context) + ": " + root.get(IntegrationClientSupport.ERROR_FIELD)
                );
            }

            markUnavailable(IntegrationClientSupport.duringMessage(UNEXPECTED_RESPONSE_STRUCTURE_PREFIX, context));
            throw new IntegrationResponseException(
                    SOURCE,
                    UNEXPECTED_RESPONSE_STRUCTURE_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context)
            );
        } catch (WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            log.warn(LOG_PREFIX + "{} HTTP error {}: {}", context, statusCode, ex.getStatusText());
            markUnavailable(IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, statusCode, context));
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, statusCode, context),
                    ex
            );
        } catch (WebClientRequestException ex) {
            log.warn(LOG_PREFIX + "{} timeout/unreachable: {}", context, ex.getMessage());
            markUnavailable(IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context));
            throw new IntegrationTimeoutException(
                    SOURCE,
                    IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, context),
                    ex
            );
        } catch (JsonProcessingException ex) {
            log.warn(LOG_PREFIX + "{} invalid JSON response: {}", context, ex.getOriginalMessage());
            markUnavailable(INVALID_JSON_RESPONSE_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context));
            throw new IntegrationResponseException(
                    SOURCE,
                    INVALID_JSON_RESPONSE_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context),
                    ex
            );
        } catch (IntegrationUnavailableException | IntegrationTimeoutException | IntegrationResponseException ex) {
            markUnavailable(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error(LOG_PREFIX + "Unexpected error during {}: {}", context, ex.getMessage(), ex);
            markUnavailable(UNEXPECTED_ERROR_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context));
            throw new IntegrationUnavailableException(
                    SOURCE,
                    UNEXPECTED_ERROR_PREFIX + ' ' + IntegrationClientSupport.parenthesized(context),
                    ex
            );
        }
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeFallback")
    public JsonNode getHosts() {
        log.info(LOG_PREFIX + "getHosts() called");

        Map<String, Object> payload = createBasePayload("host.get");
        Map<String, Object> params = new HashMap<>();
        params.put(OUTPUT, HOST_OUTPUT);
        params.put(SELECT_INTERFACES, INTERFACE_OUTPUT);
        payload.put(PARAMS, params);

        return executeRequest(payload, "hosts", "hosts");
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    public JsonNode getHostById(String hostId) {
        log.info(LOG_PREFIX + "getHostById() called with hostId={}", hostId);

        Map<String, Object> payload = createBasePayload("host.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HOSTIDS, List.of(hostId));
        params.put(OUTPUT, HOST_OUTPUT);
        params.put(SELECT_INTERFACES, INTERFACE_OUTPUT);
        payload.put(PARAMS, params);

        return executeRequest(payload, "host by id", buildHostByIdSnapshotKey(hostId));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeFallback")
    public JsonNode getRecentProblems() {
        log.info(LOG_PREFIX + "getRecentProblems() called");

        Map<String, Object> payload = createBasePayload("problem.get");
        Map<String, Object> params = new HashMap<>();
        params.put(OUTPUT, EXTEND);
        params.put(SELECT_HOSTS, PROBLEM_HOST_OUTPUT);
        params.put(SELECT_TAGS, EXTEND);
        params.put(SORT_FIELD, EVENT_ID);
        params.put(SORT_ORDER, DESC);
        params.put(LIMIT, 200);
        params.put(RECENT, true);
        params.put(SEVERITIES, HIGH_SEVERITIES);
        payload.put(PARAMS, params);

        return executeRequest(payload, "recent problems", "recent problems");
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    public JsonNode getRecentProblemsByHost(String hostId) {
        log.info(LOG_PREFIX + "getRecentProblemsByHost() called with hostId={}", hostId);

        Map<String, Object> payload = createBasePayload("problem.get");
        Map<String, Object> params = new HashMap<>();
        params.put(OUTPUT, EXTEND);
        params.put(HOSTIDS, List.of(hostId));
        params.put(SELECT_HOSTS, PROBLEM_HOST_OUTPUT);
        params.put(SELECT_TAGS, EXTEND);
        params.put(SORT_FIELD, EVENT_ID);
        params.put(SORT_ORDER, DESC);
        params.put(LIMIT, 200);
        params.put(RECENT, true);
        params.put(SEVERITIES, HIGH_SEVERITIES);
        payload.put(PARAMS, params);

        return executeRequest(payload, "recent problems by host", buildRecentProblemsByHostSnapshotKey(hostId));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    public JsonNode getTriggerById(String triggerId) {
        log.info(LOG_PREFIX + "getTriggerById() called with triggerId={}", triggerId);

        Map<String, Object> payload = createBasePayload("trigger.get");
        Map<String, Object> params = new HashMap<>();
        params.put("triggerids", List.of(triggerId));
        params.put(OUTPUT, List.of(TRIGGER_ID, DESCRIPTION));
        params.put(SELECT_HOSTS, PROBLEM_HOST_OUTPUT);
        payload.put(PARAMS, params);

        return executeRequest(payload, "trigger by id", buildTriggerByIdSnapshotKey(triggerId));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "stringFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "stringFallback")
    public String getVersion() {
        try {
            Map<String, Object> payload = createBasePayload("apiinfo.version");
            String responseBody = postForString(payload, headers -> headers.addAll(createJsonHeaders()));

            if (responseBody == null) {
                throw new IntegrationResponseException(SOURCE, EMPTY_RESPONSE_TEMPLATE.formatted(VERSION_API_TARGET));
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has(IntegrationClientSupport.RESULT_FIELD)) {
                String version = root.get(IntegrationClientSupport.RESULT_FIELD).asText();
                saveSnapshot(GET_VERSION_CONTEXT, version);
                markAvailable();
                return version;
            }
            if (root.has(IntegrationClientSupport.ERROR_FIELD)) {
                markUnavailable(IntegrationClientSupport.duringMessage(ERROR_DURING_PREFIX, GET_VERSION_CONTEXT));
                throw new IntegrationResponseException(
                        SOURCE,
                        ERROR_DURING_PREFIX + ": " + root.get(IntegrationClientSupport.ERROR_FIELD)
                );
            }
            markUnavailable("Unexpected response from Zabbix version API");
            throw new IntegrationResponseException(SOURCE, "Unexpected response from Zabbix version API");
        } catch (WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            log.warn(LOG_PREFIX + "getVersion HTTP error {}: {}", statusCode, ex.getStatusText());
            markUnavailable(IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, statusCode, GET_VERSION_CONTEXT));
            throw new IntegrationUnavailableException(
                    SOURCE,
                    IntegrationClientSupport.returnedHttpDuring(SOURCE_LABEL, statusCode, GET_VERSION_CONTEXT),
                    ex
            );
        } catch (WebClientRequestException ex) {
            log.warn(LOG_PREFIX + "getVersion timeout/unreachable: {}", ex.getMessage());
            markUnavailable(IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, GET_VERSION_CONTEXT));
            throw new IntegrationTimeoutException(
                    SOURCE,
                    IntegrationClientSupport.timeoutDuring(SOURCE_LABEL, GET_VERSION_CONTEXT),
                    ex
            );
        } catch (JsonProcessingException ex) {
            log.warn(LOG_PREFIX + "getVersion invalid JSON response: {}", ex.getOriginalMessage());
            markUnavailable("Invalid JSON response from Zabbix version API");
            throw new IntegrationResponseException(SOURCE, "Invalid JSON response from Zabbix version API", ex);
        } catch (IntegrationUnavailableException | IntegrationTimeoutException | IntegrationResponseException ex) {
            markUnavailable(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error(LOG_PREFIX + "Unexpected getVersion error: {}", ex.getMessage(), ex);
            markUnavailable(UNEXPECTED_VERSION_ERROR);
            throw new IntegrationUnavailableException(SOURCE, UNEXPECTED_VERSION_ERROR, ex);
        }
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithStringFallback")
    public JsonNode getItemsByHost(String hostId) {
        log.info(LOG_PREFIX + "getItemsByHost() called with hostId={}", hostId);

        Map<String, Object> payload = createBasePayload("item.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HOSTIDS, List.of(hostId));
        params.put(OUTPUT, ITEM_OUTPUT);
        params.put(FILTER, ACTIVE_STATUS_FILTER);
        payload.put(PARAMS, params);

        return executeRequest(payload, "items by host", buildItemsByHostSnapshotKey(hostId));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithListFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeWithListFallback")
    public JsonNode getItemsByHosts(List<String> hostIds) {
        log.info(LOG_PREFIX + "getItemsByHosts() called with hostIds count={}", hostIds != null ? hostIds.size() : 0);

        Map<String, Object> payload = createBasePayload("item.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HOSTIDS, hostIds);
        params.put(OUTPUT, ITEM_OUTPUT);
        params.put(FILTER, ACTIVE_STATUS_FILTER);
        payload.put(PARAMS, params);

        return executeRequest(payload, "items by hosts", buildItemsByHostsSnapshotKey(hostIds));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeHistoryFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeHistoryFallback")
    public JsonNode getItemHistory(String itemId, int valueType, long from, long to) {
        log.info(LOG_PREFIX + "getItemHistory() called with itemId={}, valueType={}, from={}, to={}",
                itemId, valueType, from, to);

        Map<String, Object> payload = createBasePayload("history.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HISTORY, valueType);
        params.put(ITEMIDS, List.of(itemId));
        params.put(TIME_FROM, from);
        params.put(TIME_TILL, to);
        params.put(OUTPUT, EXTEND);
        params.put(SORT_FIELD, CLOCK);
        params.put(SORT_ORDER, ASC);
        payload.put(PARAMS, params);

        return executeRequest(payload, "history", buildHistorySnapshotKey(itemId, valueType, from, to));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeBatchHistoryFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeBatchHistoryFallback")
    public JsonNode getHistoryBatch(List<String> itemIds, int valueType, long from, long to) {
        log.info(LOG_PREFIX + "getHistoryBatch() called with itemIds count={}, valueType={}, from={}, to={}",
                itemIds != null ? itemIds.size() : 0, valueType, from, to);

        Map<String, Object> payload = createBasePayload("history.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HISTORY, valueType);
        params.put(ITEMIDS, itemIds);
        params.put(TIME_FROM, from);
        params.put(TIME_TILL, to);
        params.put(OUTPUT, EXTEND);
        payload.put(PARAMS, params);

        return executeRequest(payload, "history batch", buildHistoryBatchSnapshotKey(itemIds, valueType, from, to));
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeLastValueFallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "jsonNodeLastValueFallback")
    public JsonNode getLastItemValue(String itemId, int valueType) {
        long now = Instant.now().getEpochSecond();
        long oneHourAgo = now - 3600;

        Map<String, Object> payload = createBasePayload("history.get");
        Map<String, Object> params = new HashMap<>();
        params.put(HISTORY, valueType);
        params.put(ITEMIDS, List.of(itemId));
        params.put(TIME_FROM, oneHourAgo);
        params.put(TIME_TILL, now);
        params.put(OUTPUT, EXTEND);
        params.put(SORT_FIELD, CLOCK);
        params.put(SORT_ORDER, DESC);
        params.put(LIMIT, 1);
        payload.put(PARAMS, params);

        return executeRequest(payload, "last item value", buildLastItemValueSnapshotKey(itemId, valueType));
    }

    private HttpHeaders createAuthenticatedHeaders() {
        HttpHeaders headers = createJsonHeaders();
        headers.setBearerAuth(apiToken);
        return headers;
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String postForString(Map<String, Object> payload, Consumer<HttpHeaders> headersConfigurer) {
        return webClient.post()
                .uri(zabbixUrl)
                .headers(headersConfigurer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private void markAvailable() {
        availabilityService.markAvailable(SOURCE);
    }

    private void markUnavailable(String message) {
        availabilityService.markUnavailable(SOURCE, message);
    }

    private void saveSnapshot(String context, Object data) {
        integrationCacheService.saveSnapshot(SOURCE, SNAPSHOT_PREFIX + ":" + context, data);
    }

    private String buildHostByIdSnapshotKey(String hostId) {
        return "host by id:" + safePart(hostId);
    }

    private String buildRecentProblemsByHostSnapshotKey(String hostId) {
        return "recent problems by host:" + safePart(hostId);
    }

    private String buildTriggerByIdSnapshotKey(String triggerId) {
        return "trigger by id:" + safePart(triggerId);
    }

    private String buildItemsByHostSnapshotKey(String hostId) {
        return "items by host:" + safePart(hostId);
    }

    private String buildItemsByHostsSnapshotKey(List<String> hostIds) {
        return "items by hosts:" + joinParts(hostIds);
    }

    private String buildHistorySnapshotKey(String itemId, int valueType, long from, long to) {
        return "history:item:" + safePart(itemId)
                + ":valueType:" + valueType
                + ":from:" + from
                + ":to:" + to;
    }

    private String buildHistoryBatchSnapshotKey(List<String> itemIds, int valueType, long from, long to) {
        return "history batch:items:" + joinParts(itemIds)
                + ":valueType:" + valueType
                + ":from:" + from
                + ":to:" + to;
    }

    private String buildLastItemValueSnapshotKey(String itemId, int valueType) {
        return "last item value:item:" + safePart(itemId) + ":valueType:" + valueType;
    }

    private String joinParts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }

        List<String> safeValues = new ArrayList<>();
        for (String value : values) {
            safeValues.add(safePart(value));
        }
        return String.join(",", safeValues);
    }

    private String safePart(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private JsonNode jsonNodeFallback(Throwable throwable) {
        log.warn(LOG_PREFIX + "Resilience fallback triggered, delegating cache lookup upstream: {}", throwable.getMessage());
        return null;
    }

    private JsonNode jsonNodeWithStringFallback(String ignored, Throwable throwable) {
        return jsonNodeFallback(throwable);
    }

    private JsonNode jsonNodeWithListFallback(List<String> ignored, Throwable throwable) {
        return jsonNodeFallback(throwable);
    }

    private JsonNode jsonNodeHistoryFallback(String ignoredItemId, int ignoredValueType, long ignoredFrom, long ignoredTo, Throwable throwable) {
        return jsonNodeFallback(throwable);
    }

    private JsonNode jsonNodeBatchHistoryFallback(List<String> ignoredItemIds, int ignoredValueType, long ignoredFrom, long ignoredTo, Throwable throwable) {
        return jsonNodeFallback(throwable);
    }

    private JsonNode jsonNodeLastValueFallback(String ignoredItemId, int ignoredValueType, Throwable throwable) {
        return jsonNodeFallback(throwable);
    }

    private String stringFallback(Throwable throwable) {
        log.warn(LOG_PREFIX + "Fallback triggered for getVersion, delegating cache lookup upstream: {}", throwable.getMessage());
        return null;
    }
}
