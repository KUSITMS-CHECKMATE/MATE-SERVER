package server.MATE.toss.dto.response;

public record TossDecryptedUserInfo(
        Long userKey,
        String scope,
        String ci,
        String name
) {
}
