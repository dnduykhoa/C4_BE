package j2ee_backend.nhom05.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import j2ee_backend.nhom05.model.Role;
import j2ee_backend.nhom05.model.User;

public final class RoleAccess {

    private RoleAccess() {
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String value = role.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("ROLE_")) {
            return value.substring(5);
        }
        return value;
    }

    public static Set<String> getRoles(UserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities() == null) {
            return Set.of();
        }
        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(RoleAccess::normalizeRole)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toSet());
    }

    public static Set<String> getRoles(User user) {
        if (user == null || user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
            .map(Role::getName)
            .map(RoleAccess::normalizeRole)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toSet());
    }

    public static boolean hasAnyRole(UserDetails userDetails, String... roles) {
        Set<String> currentRoles = getRoles(userDetails);
        return Arrays.stream(roles)
            .map(RoleAccess::normalizeRole)
            .anyMatch(currentRoles::contains);
    }

    public static boolean hasAnyRole(User user, String... roles) {
        Set<String> currentRoles = getRoles(user);
        return Arrays.stream(roles)
            .map(RoleAccess::normalizeRole)
            .anyMatch(currentRoles::contains);
    }

    public static boolean isBackoffice(UserDetails userDetails) {
        return hasAnyRole(userDetails, "ADMIN", "MANAGER", "STAFF");
    }
}