package server.MATE.toss.client.http;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.exception.parser.TossErrorResponseParser;
import server.MATE.toss.response.TossApiResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class TossHttpClient {

    private final WebClient tossWebClient;
    private final ObjectMapper objectMapper;
    private final List<TossErrorResponseParser> errorResponseParsers;

    public TossHttpClient(
            @Qualifier("tossWebClient") WebClient tossWebClient,
            ObjectMapper objectMapper,
            List<TossErrorResponseParser> errorResponseParsers
    ) {
        this.tossWebClient = tossWebClient;
        this.objectMapper = objectMapper;
        this.errorResponseParsers = errorResponseParsers;
    }

    public <T> T get(String path, Class<T> responseType) {
        return get(path, headers -> {}, responseType);
    }

    public <T> T get(String path, Consumer<HttpHeaders> headersConsumer, Class<T> responseType) {
        String responseBody = tossWebClient.get()
                .uri(path)
                .headers(headersConsumer)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(toException(response.statusCode(), path, body))))
                .bodyToMono(String.class)
                .block();

        return unwrapSuccess(path, responseBody, responseType);
    }

    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        return post(path, requestBody, headers -> {}, responseType);
    }

    public <T> T post(
            String path,
            Object requestBody,
            Map<String, String> headers,
            Class<T> responseType
    ) {
        return post(path, requestBody, httpHeaders -> headers.forEach(httpHeaders::set), responseType);
    }

    public <T> T post(
            String path,
            Object requestBody,
            Consumer<HttpHeaders> headersConsumer,
            Class<T> responseType
    ) {
        String responseBody = tossWebClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headersConsumer)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(toException(response.statusCode(), path, body))))
                .bodyToMono(String.class)
                .block();

        return unwrapSuccess(path, responseBody, responseType);
    }

    private <T> T unwrapSuccess(String path, String responseBody, Class<T> responseType) {
        try {
            JavaType tossResponseType = objectMapper.getTypeFactory().constructParametricType(TossApiResponse.class, responseType);
            TossApiResponse<T> tossResponse = objectMapper.readValue(responseBody, tossResponseType);

            if (tossResponse.isSuccess()) {
                return tossResponse.success();
            }

            throw toException(HttpStatus.OK, path, responseBody);
        } catch (TossApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TossApiException(
                    TossErrorCode.TOSS_002,
                    "토스 API 응답을 해석할 수 없습니다.",
                    responseBody
            );
        }
    }

    private TossApiException toException(HttpStatusCode statusCode, String path, String responseBody) {
        TossErrorResponseParser parser = errorResponseParsers.stream()
                .filter(candidate -> candidate.supports(statusCode, path, responseBody))
                .findFirst()
                .orElseThrow();
        return TossApiException.from(statusCode, parser.parse(statusCode, path, responseBody), responseBody);
    }
}
