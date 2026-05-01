package tn.iteam.authservice.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.authservice.client.KeycloakAdminClient;
import tn.iteam.authservice.client.KeycloakTokenClient;
import tn.iteam.authservice.config.KeycloakProperties;
import tn.iteam.authservice.dto.*;
import tn.iteam.authservice.exception.AuthenticationException;
import tn.iteam.authservice.exception.UserAlreadyExistsException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakTokenClient tokenClient;

    @Mock
    private KeycloakAdminClient adminClient;

    @Mock
    private AdminTokenService adminTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        KeycloakProperties props = new KeycloakProperties();
        props.setBaseUrl("http://localhost:8080");
        props.setRealm("my-realm");
        props.setClientId("auth-service");
        props.setClientSecret("secret");
        props.setAdminClientId("auth-service-admin");
        props.setAdminClientSecret("admin-secret");

        authService = new AuthService(tokenClient, adminClient, props, adminTokenService);
    }

    @Test
    void loginReturnsTokenResponseOnSuccess() {
        TokenResponse expected = new TokenResponse();
        expected.setAccessToken("access-token");
        expected.setRefreshToken("refresh-token");

        when(tokenClient.obtainToken(eq("my-realm"), anyMap())).thenReturn(expected);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("Password123!");

        TokenResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginThrowsAuthenticationExceptionOnUnauthorized() {
        when(tokenClient.obtainToken(eq("my-realm"), anyMap()))
                .thenThrow(mock(FeignException.Unauthorized.class));

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void refreshReturnsNewTokenOnSuccess() {
        TokenResponse expected = new TokenResponse();
        expected.setAccessToken("new-access-token");

        when(tokenClient.obtainToken(eq("my-realm"), anyMap())).thenReturn(expected);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh-token");

        TokenResponse result = authService.refresh(request);
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    void registerThrowsUserAlreadyExistsWhenUsernameExists() {
        when(adminTokenService.getAdminToken()).thenReturn("admin-token");
        when(adminClient.findUserByUsername(
                eq("my-realm"), anyString(), eq("alice"), eq(true)))
                .thenReturn(List.of(KeycloakUserRepresentation.builder()
                        .id("existing-id")
                        .username("alice")
                        .build()));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("Password123!");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice");
    }
}
