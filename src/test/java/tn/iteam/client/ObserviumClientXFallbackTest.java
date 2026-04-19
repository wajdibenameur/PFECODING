package tn.iteam.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tn.iteam.cache.IntegrationCacheService;
import tn.iteam.domain.ApiResponse;
import tn.iteam.exception.IntegrationUnavailableException;
import tn.iteam.service.SourceAvailabilityService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ObserviumClientXFallbackTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private IntegrationCacheService integrationCacheService;
    private SourceAvailabilityService availabilityService;
    private ObserviumClientX observiumClientX;

    @BeforeEach
    void setUp() {
        integrationCacheService = mock(IntegrationCacheService.class);
        availabilityService = mock(SourceAvailabilityService.class);
        observiumClientX = new ObserviumClientX(
                mock(WebClient.class),
                OBJECT_MAPPER,
                integrationCacheService,
                availabilityService,
                "http://observium.local",
                "token"
        );
    }

    @Test
    void apiDownRedisUpReturnsSnapshotAndMarksObserviumUnavailable() {
        JsonNode snapshot = OBJECT_MAPPER.createArrayNode().add(OBJECT_MAPPER.createObjectNode().put("device", "sw1"));
        when(integrationCacheService.getSnapshot("OBSERVIUM", "observium:devices", JsonNode.class))
                .thenReturn(Optional.of(snapshot));

        @SuppressWarnings("unchecked")
        ApiResponse<JsonNode> response = (ApiResponse<JsonNode>) ReflectionTestUtils.invokeMethod(
                observiumClientX,
                "cachedFallback",
                "devices",
                "Devices fetched successfully",
                new IntegrationUnavailableException("OBSERVIUM", "Observium API down")
        );

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(snapshot);
        assertThat(response.getMessage()).contains("Redis fallback");
        verify(availabilityService).markUnavailable(
                eq("OBSERVIUM"),
                eq("Observium API unavailable, serving Redis snapshot for devices")
        );
    }

    @Test
    void apiDownRedisDownReturnsFailureAndMarksObserviumUnavailable() {
        when(integrationCacheService.getSnapshot("OBSERVIUM", "observium:alerts", JsonNode.class))
                .thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        ApiResponse<JsonNode> response = (ApiResponse<JsonNode>) ReflectionTestUtils.invokeMethod(
                observiumClientX,
                "cachedFallback",
                "alerts",
                "Alerts fetched successfully",
                new IntegrationUnavailableException("OBSERVIUM", "Observium API down")
        );

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().isArray()).isTrue();
        assertThat(response.getData()).isEmpty();
        verify(availabilityService).markUnavailable(
                eq("OBSERVIUM"),
                eq("Observium API unavailable and no usable Redis snapshot for alerts")
        );
    }

    @Test
    void circuitBreakerOpenUsesRedisFallbackAndMarksObserviumUnavailable() {
        JsonNode snapshot = OBJECT_MAPPER.createArrayNode().add(OBJECT_MAPPER.createObjectNode().put("device", "fw1"));
        when(integrationCacheService.getSnapshot("OBSERVIUM", "observium:devices", JsonNode.class))
                .thenReturn(Optional.of(snapshot));

        CallNotPermittedException callNotPermittedException =
                CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("observiumApi"));

        @SuppressWarnings("unchecked")
        ApiResponse<JsonNode> response = (ApiResponse<JsonNode>) ReflectionTestUtils.invokeMethod(
                observiumClientX,
                "devicesFallback",
                callNotPermittedException
        );

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(snapshot);
        verify(availabilityService).markUnavailable(
                eq("OBSERVIUM"),
                eq("Observium API unavailable, serving Redis snapshot for devices")
        );
    }
}
