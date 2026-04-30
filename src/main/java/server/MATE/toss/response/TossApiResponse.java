package server.MATE.toss.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossApiResponse<T>(
        TossResultType resultType,
        T success,
        TossErrorResponse error
) {
    public boolean isSuccess() {
        return resultType == TossResultType.SUCCESS;
    }
}
