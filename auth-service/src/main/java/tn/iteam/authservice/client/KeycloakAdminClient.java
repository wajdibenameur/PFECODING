package tn.iteam.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tn.iteam.authservice.dto.KeycloakCredentialRepresentation;
import tn.iteam.authservice.dto.KeycloakUserRepresentation;

import java.util.List;

/**
 * Feign client for Keycloak Admin REST API.
 * Handles user creation, lookup, and password management.
 */
@FeignClient(
        name = "keycloak-admin-client",
        url = "${keycloak.base-url}"
)
public interface KeycloakAdminClient {

    /**
     * Create a new user in the realm.
     * Returns 201 Created with Location header containing the new user's ID.
     */
    @PostMapping(
            value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    feign.Response createUser(
            @PathVariable("realm") String realm,
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody KeycloakUserRepresentation user
    );

    /**
     * Find users by username (exact match).
     */
    @GetMapping("/admin/realms/{realm}/users")
    List<KeycloakUserRepresentation> findUserByUsername(
            @PathVariable("realm") String realm,
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam("username") String username,
            @RequestParam("exact") boolean exact
    );

    /**
     * Set (reset) a user's password.
     */
    @PutMapping(
            value = "/admin/realms/{realm}/users/{userId}/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void resetPassword(
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody KeycloakCredentialRepresentation credential
    );
}
