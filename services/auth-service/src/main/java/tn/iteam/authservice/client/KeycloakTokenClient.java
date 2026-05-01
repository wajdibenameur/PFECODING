package tn.iteam.authservice.client;

import feign.form.FormData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tn.iteam.authservice.config.FeignFormConfig;
import tn.iteam.authservice.dto.TokenResponse;

import java.util.Map;

/**
 * Feign client for Keycloak token endpoint.
 * Handles login (password grant) and refresh (refresh_token grant).
 */
@FeignClient(
        name = "keycloak-token-client",
        url = "${keycloak.base-url}",
        configuration = FeignFormConfig.class
)
public interface KeycloakTokenClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenResponse obtainToken(
            @PathVariable("realm") String realm,
            @RequestParam Map<String, ?> formParams
    );
}
