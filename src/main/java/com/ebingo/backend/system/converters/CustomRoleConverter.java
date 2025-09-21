package com.ebingo.backend.system.converters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Converts claims from Supabase JWT into Spring authorities. Adjust to your claims structure.
 * Typical places to look for roles in Supabase tokens:
 * - "role" (string)
 * - custom claims like "permissions" or app_metadata fields if you wrote custom functions
 */
@Component
@Slf4j
public class CustomRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {


    private static final String ROLE_PREFIX = "ROLE_";


    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        log.info("===============================================================>>> Converting roles for user: " + jwt.getClaim("email"));
        Set<String> roles = new HashSet<>();


        // 1) top-level "role" claim (single role string)
        Object roleClaim = jwt.getClaim("role");
        if (roleClaim instanceof String) {
            roles.add((String) roleClaim);
        }


        // 2) a custom array claim named "roles" or "app_roles"
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof Collection) {
            ((Collection<?>) rolesClaim).forEach(r -> { if (r != null) roles.add(r.toString()); });
        }


        // 3) permissions claim (example: map of feature->true)
        Object permissions = jwt.getClaim("permissions");
        if (permissions instanceof Map) {
            // convert keys that are truthy to authorities, e.g. permissions.{feature}: true -> PERM_feature
            ((Map<?, ?>) permissions).forEach((k, v) -> {
                if (v instanceof Boolean && ((Boolean) v)) roles.add("PERM_" + k.toString().toUpperCase(Locale.ROOT));
            });
        }


        // 4) fallback: use 'email' as a minimal authority so we don't produce empty authorities if needed
        if (roles.isEmpty() && jwt.getClaim("email") != null) {
            roles.add("PLAYER");
        }


        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(ROLE_PREFIX + r.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }
}