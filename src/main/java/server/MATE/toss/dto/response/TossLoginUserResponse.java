package server.MATE.toss.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossLoginUserResponse(
        Long userKey,
        String scope,
        String name,
        String ci
) {
}
