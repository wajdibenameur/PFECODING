package tn.iteam.authservice.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.iteam.authservice.client.KeycloakTokenClient;
import tn.iteam.authservice.config.KeycloakProperties;
import tn.iteam.authservice.dto.TokenResponse;
import tn.iteam.authservice.exception.KeycloakIntegrationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the admin access token for Keycloak Admin API calls.
 * Uses client-credentials grant and caches the token until near-expiry.
 */
@Service
public class AdminTokenService {

    private static final Logger log = LoggerFactory.getLogger(AdminTokenService.class);
    private static final int EXPIRY_BUFFER_SECONDS = 30;

    private final KeycloakTokenClient tokenClient;
    private final KeycloakProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public AdminTokenService(KeycloakTokenClient tokenClient, KeycloakProperties properties) {
        this.tokenClient = tokenClient;
        this.properties = properties;
    }

    /**
     * Returns a valid admin access token. Refreshes if expired or near-expiry.
     */
    public String getAdminToken() {
        if (isTokenValid()) {
            return cachedToken;
        }

        lock.lock();
        try {
            if (isTokenValid()) {
                return cachedToken;
            }
            return fetchNewAdminToken();
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null && Instant.now().isBefore(tokenExpiry);
    }

    private String fetchNewAdminToken() {
        log.debug("Fetching new admin token for realm '{}'", properties.getRealm());

        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "client_credentials");
        params.put("client_id", properties.getAdminClientId());
        params.put("client_secret", properties.getAdminClientSecret());

        try {
            TokenResponse response = tokenClient.obtainToken(properties.getRealm(), params);
            cachedToken = response.getAccessToken();
            int expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 60;
            tokenExpiry = Instant.now().plusSeconds(expiresIn - EXPIRY_BUFFER_SECONDS);
            log.debug("Admin token refreshed, expires in {}s", expiresIn);
            return cachedToken;
        } catch (FeignException e) {
            throw new KeycloakIntegrationException("Failed to obtain admin token: " + e.getMessage(), e);
        }
    }
}
