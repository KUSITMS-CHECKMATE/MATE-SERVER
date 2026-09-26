package server.MATE.global.discord.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

class DiscordWebhookClientTest {

    private static final DiscordEmbed EMBED = new DiscordEmbed("제목", "본문", EmbedColor.INFO);

    private DiscordWebhookClient clientRespondingWith(HttpStatus status, AtomicInteger calls) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(status).build());
        });
        return new DiscordWebhookClient(builder);
    }

    @Test
    @DisplayName("sendAndWait: 2xx 응답이면 예외 없이 끝난다")
    void sendAndWait_success() {
        AtomicInteger calls = new AtomicInteger();
        DiscordWebhookClient client = clientRespondingWith(HttpStatus.NO_CONTENT, calls);

        assertThatCode(() -> client.sendAndWait("test-alert", "https://discord.test/hook", EMBED))
                .doesNotThrowAnyException();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("sendAndWait: 5xx 응답이면 예외를 던진다")
    void sendAndWait_serverError() {
        DiscordWebhookClient client = clientRespondingWith(HttpStatus.INTERNAL_SERVER_ERROR, new AtomicInteger());

        assertThatThrownBy(() -> client.sendAndWait("test-alert", "https://discord.test/hook", EMBED))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    @DisplayName("sendAndWait: 웹훅 URL이 비어 있으면 요청 없이 예외를 던진다")
    void sendAndWait_blankUrl() {
        AtomicInteger calls = new AtomicInteger();
        DiscordWebhookClient client = clientRespondingWith(HttpStatus.NO_CONTENT, calls);

        assertThatThrownBy(() -> client.sendAndWait("test-alert", " ", EMBED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-alert");
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("sendAndWait: 5xx 응답은 재시도하지 않는다")
    void sendAndWait_serverError_doesNotRetry() {
        AtomicInteger calls = new AtomicInteger();
        DiscordWebhookClient client = clientRespondingWith(HttpStatus.INTERNAL_SERVER_ERROR, calls);

        assertThatThrownBy(() -> client.sendAndWait("test-alert", "https://discord.test/hook", EMBED))
                .isInstanceOf(WebClientResponseException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("sendAndWait: 타임아웃 등 네트워크 실패는 재시도 후 성공하면 예외 없이 끝난다")
    void sendAndWait_retriesOnTimeout_thenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            if (calls.incrementAndGet() < 2) {
                return Mono.error(timeoutException());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
        });
        DiscordWebhookClient client = new DiscordWebhookClient(builder);

        assertThatCode(() -> client.sendAndWait("test-alert", "https://discord.test/hook", EMBED))
                .doesNotThrowAnyException();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("sendAndWait: 재시도 횟수를 모두 소진하면 마지막 실패를 던진다")
    void sendAndWait_retryExhausted_throwsLastFailure() {
        AtomicInteger calls = new AtomicInteger();
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.error(timeoutException());
                });
        DiscordWebhookClient client = new DiscordWebhookClient(builder);

        assertThatThrownBy(() -> client.sendAndWait("test-alert", "https://discord.test/hook", EMBED))
                .isInstanceOf(WebClientRequestException.class);
        assertThat(calls.get()).isEqualTo(3);
    }

    private static WebClientRequestException timeoutException() {
        return new WebClientRequestException(
                new IOException("Operation timed out"),
                HttpMethod.POST,
                URI.create("https://discord.test/hook"),
                new HttpHeaders());
    }
}
