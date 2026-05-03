package tn.iteam.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import tn.iteam.enums.Permission;
import tn.iteam.enums.RoleName;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakRolePermissionService rolePermissionService;

    public KeycloakJwtAuthenticationConverter(KeycloakRolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<RoleName> roles = rolePermissionService.parseRoles(extractRealmRoles(jwt));
        Set<Permission> permissions = rolePermissionService.permissionsForRoles(roles);
        Collection<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (RoleName role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }
        for (Permission permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission.name()));
        }

        String principalName = preferredPrincipal(jwt);
        return new UsernamePasswordAuthenticationToken(jwt, "n/a", authorities) {
            @Override
            public String getName() {
                return principalName;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return List.of();
        }

        Object roles = realmMap.get("roles");
        if (!(roles instanceof List<?> rawRoles)) {
            return List.of();
        }

        return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private String preferredPrincipal(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }

        return jwt.getSubject();
    }
}
