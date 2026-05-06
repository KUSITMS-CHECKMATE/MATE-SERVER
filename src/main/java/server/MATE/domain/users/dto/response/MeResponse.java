package server.MATE.domain.users.dto.response;

import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;

public record MeResponse(
        Long id,
        String name,
        Role role
) {
    public static MeResponse from(Users user) {
        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }

    public static MeResponse of(Long id, String name, Role role) {
        return new MeResponse(id, name, role);
    }
}
