package server.MATE.toss.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossLoginUserResponse(
        Long userKey,
        String scope,
        String name,
        String ci
) {
}
