package server.MATE.toss.dto;

public record TossDecryptedUserInfo(
        Long userKey,
        String scope,
        String ci,
        String name
) {
}
