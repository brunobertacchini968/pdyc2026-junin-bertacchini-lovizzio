package ar.edu.unnoba.pdyc2026.events.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(extractRoles(jwt.getClaim("realm_access")));
        authorities.addAll(extractResourceRoles(jwt.getClaim("resource_access")));
        return authorities;
    }

    private Collection<GrantedAuthority> extractRoles(Object claim) {
        if (!(claim instanceof Map<?, ?> claimMap)) {
            return List.of();
        }

        Object roles = claimMap.get("roles");
        if (!(roles instanceof Iterable<?> iterableRoles)) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object role : iterableRoles) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }
        return authorities;
    }

    private Collection<GrantedAuthority> extractResourceRoles(Object claim) {
        if (!(claim instanceof Map<?, ?> claimMap)) {
            return List.of();
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Object clientClaim : claimMap.values()) {
            if (!(clientClaim instanceof Map<?, ?> clientMap)) {
                continue;
            }
            authorities.addAll(extractRoles(clientMap));
        }
        return authorities;
    }
}