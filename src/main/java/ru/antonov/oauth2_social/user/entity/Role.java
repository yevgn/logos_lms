package ru.antonov.oauth2_social.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public enum Role {
    STUDENT(
            1,
            List.of(
                    Permission.READ,
                    Permission.CREATE,
                    Permission.UPDATE,
                    Permission.DELETE
            )
    ),

    TUTOR(
            2,
            List.of(
                    Permission.READ,
                    Permission.CREATE,
                    Permission.UPDATE,
                    Permission.DELETE
            )
    ),

    ADMIN(
            3,
            List.of(
                    Permission.READ,
                    Permission.CREATE,
                    Permission.UPDATE,
                    Permission.DELETE
            )
    )

    ;

    // Чем больше, тем выше приоритет
    private final int priorityValue;
    private final List<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
