package server.MATE.toss;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import server.MATE.global.common.response.ApiResponse;

@RestController
@RequestMapping("/test/toss")
@ConditionalOnBean(TossApiClient.class)
@RequiredArgsConstructor
public class TossTestController {

    private final TossApiClient tossApiClient;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(
            @RequestParam(required = false) String userKey
    ) {
        String path = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";

        try {
            String responseBody = tossApiClient.post(path, Map.of(), buildHeaders(userKey));
            return ResponseEntity.ok(ApiResponse.ok("Toss API call succeeded.", Map.of(
                    "path", path,
                    "responseBody", responseBody
            )));
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.of(HttpStatus.OK, "Toss API responded with an error response.", Map.of(
                            "path", path,
                            "status", e.getRawStatusCode(),
                            "responseBody", e.getResponseBodyAsString()
                    )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Toss API call failed before getting an HTTP response.", Map.of(
                            "path", path,
                            "errorType", e.getClass().getSimpleName(),
                            "message", e.getMessage()
                    )));
        }
    }

    private Map<String, String> buildHeaders(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return Map.of();
        }

        return Map.of("x-toss-user-key", userKey);
    }
}
