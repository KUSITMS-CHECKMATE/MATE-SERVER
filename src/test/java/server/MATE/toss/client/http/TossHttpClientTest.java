package server.MATE.toss.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossTokenResponse;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.exception.parser.TossLoginErrorResponseParser;
import server.MATE.toss.exception.parser.TossPromotionErrorResponseParser;

class TossHttpClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("success wrapper를 정상 unwrap 한다")
    void unwrapsSuccessResponse() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.just(jsonResponse(
                HttpStatus.OK,
                """
                {
                  "resultType": "SUCCESS",
                  "success": {
                    "tokenType": "Bearer",
                    "accessToken": "access-token",
                    "refreshToken": "refresh-token",
                    "expiresIn": 3600,
                    "scope": "user_ci"
                  }
                }
                """
        )));

        TossTokenResponse response = tossHttpClient.post("/api-partner/v1/apps-in-toss/user/oauth2/generate-token", java.util.Map.of(), TossTokenResponse.class);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("FAIL wrapper는 TossApiException으로 변환한다")
    void convertsFailWrapperToTossApiException() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.just(jsonResponse(
                HttpStatus.OK,
                """
                {
                  "resultType": "FAIL",
                  "error": {
                    "errorCode": "INVALID_ACCESS_TOKEN",
                    "reason": "access token이 유효하지 않습니다."
                  }
                }
                """
        )));

        assertThatThrownBy(() -> tossHttpClient.get("/api-partner/v1/apps-in-toss/user/oauth2/login-me", TossTokenResponse.class))
                .isInstanceOf(TossApiException.class)
                .extracting(exception -> ((TossApiException) exception).getErrorCode())
                .isEqualTo(TossErrorCode.TOSS_005);
    }

    @Test
    @DisplayName("malformed success body는 TOSS_002로 변환한다")
    void convertsMalformedSuccessBodyToToss002() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.just(jsonResponse(HttpStatus.OK, "not-json")));

        assertThatThrownBy(() -> tossHttpClient.get("/api-partner/v1/apps-in-toss/user/oauth2/login-me", TossTokenResponse.class))
                .isInstanceOf(TossApiException.class)
                .extracting(exception -> ((TossApiException) exception).getErrorCode())
                .isEqualTo(TossErrorCode.TOSS_002);
    }

    @Test
    @DisplayName("4xx error body는 parser를 통해 TossApiException으로 변환한다")
    void converts4xxErrorResponseToTossApiException() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.just(jsonResponse(
                HttpStatus.BAD_REQUEST,
                """
                {
                  "error": "invalid_grant"
                }
                """
        )));

        assertThatThrownBy(() -> tossHttpClient.post("/api-partner/v1/apps-in-toss/user/oauth2/refresh-token", java.util.Map.of(), TossTokenResponse.class))
                .isInstanceOf(TossApiException.class)
                .extracting(exception -> ((TossApiException) exception).getErrorCode())
                .isEqualTo(TossErrorCode.TOSS_004);
    }

    @Test
    @DisplayName("5xx error body는 parser를 통해 TossApiException으로 변환한다")
    void converts5xxErrorResponseToTossApiException() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.just(jsonResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                """
                {
                  "error": {
                    "errorCode": "INTERNAL_ERROR",
                    "reason": "토스 내부 오류"
                  }
                }
                """
        )));

        assertThatThrownBy(() -> tossHttpClient.get("/api-partner/v1/apps-in-toss/user/oauth2/login-me", TossTokenResponse.class))
                .isInstanceOf(TossApiException.class)
                .extracting(exception -> ((TossApiException) exception).getErrorCode())
                .isEqualTo(TossErrorCode.TOSS_006);
    }

    @Test
    @DisplayName("네트워크 예외는 TOSS_001 TossApiException으로 변환한다")
    void convertsNetworkExceptionToToss001() {
        TossHttpClient tossHttpClient = createClient(request -> Mono.error(new IllegalStateException("network failure")));

        assertThatThrownBy(() -> tossHttpClient.get("/api-partner/v1/apps-in-toss/user/oauth2/login-me", TossTokenResponse.class))
                .isInstanceOf(TossApiException.class)
                .satisfies(exception -> {
                    TossApiException tossApiException = (TossApiException) exception;
                    assertThat(tossApiException.getErrorCode()).isEqualTo(TossErrorCode.TOSS_001);
                    assertThat(tossApiException.getCause()).isInstanceOf(IllegalStateException.class);
                    assertThat(tossApiException.getCause()).hasMessage("network failure");
                });
    }

    @Test
    @DisplayName("promotion 경로의 success wrapper를 정상 unwrap 한다")
    void unwrapsPromotionSuccessResponse() {
        TossHttpClient tossHttpClient = createPromotionAwareClient(request -> Mono.just(jsonResponse(
                HttpStatus.OK,
                """
                {
                  "resultType": "SUCCESS",
                  "success": {
                    "key": "3oBpxjUgl5r66edcVi7ynHGIjhzr9KOka6FfEKikev0="
                  }
                }
                """
        )));

        TossPromotionKeyResponse response = tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key",
                java.util.Map.of(),
                TossPromotionKeyResponse.class
        );

        assertThat(response.key()).isEqualTo("3oBpxjUgl5r66edcVi7ynHGIjhzr9KOka6FfEKikev0=");
    }

    @Test
    @DisplayName("promotion 경로의 FAIL wrapper는 TossPromotionErrorResponseParser를 통해 구체적인 코드로 변환한다")
    void convertsPromotionFailWrapperToTossApiException() {
        TossHttpClient tossHttpClient = createPromotionAwareClient(request -> Mono.just(jsonResponse(
                HttpStatus.OK,
                """
                {
                  "resultType": "FAIL",
                  "error": {
                    "errorCode": "4112",
                    "reason": "프로모션 머니가 부족해요"
                  }
                }
                """
        )));

        assertThatThrownBy(() -> tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/promotion/execute-promotion",
                java.util.Map.of(),
                TossPromotionKeyResponse.class
        ))
                .isInstanceOf(TossApiException.class)
                .extracting(exception -> ((TossApiException) exception).getErrorCode())
                .isEqualTo(TossErrorCode.TOSS_014);
    }

    private TossHttpClient createClient(ExchangeFunction exchangeFunction) {
        return createClientWithParsers(exchangeFunction, List.of(new TossLoginErrorResponseParser(objectMapper)));
    }

    private TossHttpClient createPromotionAwareClient(ExchangeFunction exchangeFunction) {
        return createClientWithParsers(exchangeFunction, List.of(
                new TossLoginErrorResponseParser(objectMapper),
                new TossPromotionErrorResponseParser(objectMapper)
        ));
    }

    private TossHttpClient createClientWithParsers(ExchangeFunction exchangeFunction, List<server.MATE.toss.exception.parser.TossErrorResponseParser> parsers) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        return new TossHttpClient(webClient, objectMapper, parsers);
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
