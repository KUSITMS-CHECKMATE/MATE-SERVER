package server.MATE.toss;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnBean(name = "tossRestClient")
@RequiredArgsConstructor
public class TossApiClient {

    private final RestClient tossRestClient;

    public String get(String path) {
        return tossRestClient.get()
                .uri(path)
                .retrieve()
                .body(String.class);
    }

    public String post(String path, Object body) {
        return tossRestClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    public String post(String path, Object body, Map<String, String> headers) {
        return tossRestClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                .body(body)
                .retrieve()
                .body(String.class);
    }
}
