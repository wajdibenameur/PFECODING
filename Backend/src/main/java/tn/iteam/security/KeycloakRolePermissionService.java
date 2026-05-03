package tn.iteam.security;

import org.springframework.stereotype.Service;
import tn.iteam.enums.Permission;
import tn.iteam.enums.RoleName;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KeycloakRolePermissionService {

    private final Map<RoleName, Set<Permission>> permissionsByRole = new EnumMap<>(RoleName.class);

    public KeycloakRolePermissionService() {
        permissionsByRole.put(RoleName.SUPERADMIN, EnumSet.allOf(Permission.class));
        permissionsByRole.put(RoleName.ADMIN, EnumSet.of(
                Permission.VIEW_DASHBOARD,
                Permission.VIEW_METRICS,
                Permission.VIEW_ALERTS,
                Permission.VIEW_LOGS,
                Permission.REFRESH_DASHBOARD,
                Permission.EXPORT_DASHBOARD,
                Permission.VIEW_HOSTS,
                Permission.MANAGE_HOSTS,
                Permission.EDIT_HOST,
                Permission.VIEW_TICKETS,
                Permission.VIEW_ALL_TICKETS,
                Permission.VIEW_ASSIGNED_TICKETS,
                Permission.CREATE_TICKET,
                Permission.EDIT_TICKET,
                Permission.DELETE_TICKET,
                Permission.ASSIGN_TICKET,
                Permission.VALIDATE_TICKET,
                Permission.ADD_COMMENT,
                Permission.EDIT_COMMENT,
                Permission.VIEW_USERS
        ));
        permissionsByRole.put(RoleName.SUPPORT, EnumSet.of(
                Permission.VIEW_DASHBOARD,
                Permission.VIEW_METRICS,
                Permission.VIEW_ALERTS,
                Permission.VIEW_LOGS,
                Permission.REFRESH_DASHBOARD,
                Permission.VIEW_HOSTS,
                Permission.VIEW_TICKETS,
                Permission.VIEW_ASSIGNED_TICKETS,
                Permission.CREATE_TICKET,
                Permission.EDIT_TICKET,
                Permission.ASSIGN_TICKET,
                Permission.ADD_COMMENT,
                Permission.EDIT_COMMENT
        ));
        permissionsByRole.put(RoleName.VIEWER, EnumSet.of(
                Permission.VIEW_DASHBOARD,
                Permission.VIEW_METRICS,
                Permission.VIEW_ALERTS,
                Permission.VIEW_LOGS,
                Permission.VIEW_HOSTS,
                Permission.VIEW_TICKETS,
                Permission.VIEW_ASSIGNED_TICKETS
        ));
    }

    public Set<Permission> permissionsFor(RoleName roleName) {
        if (roleName == null) {
            return Set.of();
        }
        return permissionsByRole.getOrDefault(roleName, Set.of());
    }

    public Set<Permission> permissionsForRoles(Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        EnumSet<Permission> merged = EnumSet.noneOf(Permission.class);
        for (RoleName roleName : roleNames) {
            merged.addAll(permissionsFor(roleName));
        }
        return Set.copyOf(merged);
    }

    public Set<RoleName> parseRoles(Iterable<String> roles) {
        if (roles == null) {
            return Set.of();
        }

        EnumSet<RoleName> resolved = EnumSet.noneOf(RoleName.class);
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            try {
                resolved.add(RoleName.valueOf(role.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown realm roles that are irrelevant for this application.
            }
        }
        return Set.copyOf(resolved);
    }

    public RoleName highestPrivilegeRole(Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return null;
        }

        return Arrays.stream(RoleName.values())
                .filter(roleNames::contains)
                .findFirst()
                .orElse(null);
    }
}
