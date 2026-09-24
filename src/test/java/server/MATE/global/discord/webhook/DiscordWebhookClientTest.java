package server.MATE.global.discord.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
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
}
