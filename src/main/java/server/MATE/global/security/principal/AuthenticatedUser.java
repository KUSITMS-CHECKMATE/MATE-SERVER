package server.MATE.global.security.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser {

    private final Long id;
    private final Role role;
    private final TokenType tokenType;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, Role role, TokenType tokenType) {
        this.id = id;
        this.role = role;
        this.tokenType = tokenType;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static AuthenticatedUser from(Users user, TokenType tokenType) {
        return new AuthenticatedUser(user.getId(), user.getRole(), tokenType);
    }
}
