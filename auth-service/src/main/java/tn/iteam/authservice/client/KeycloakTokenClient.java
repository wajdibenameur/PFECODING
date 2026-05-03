package tn.iteam.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.iteam.authservice.config.FeignFormConfig;
import tn.iteam.authservice.dto.TokenResponse;

@FeignClient(
        name = "keycloak-token-client",
        url = "${keycloak.base-url}",
        configuration = FeignFormConfig.class
)
public interface KeycloakTokenClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            headers = "Content-Type=application/x-www-form-urlencoded"
    )
    TokenResponse obtainToken(
            @PathVariable("realm") String realm,
            @RequestBody MultiValueMap<String, String> formParams
    );
}