package server.MATE.global.discord.bot.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import server.MATE.global.discord.config.DiscordProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordBotRestClientTest {

    private final List<ClientRequest> requests = new ArrayList<>();

    private DiscordBotRestClient client(String token, String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseJson)
                    .build());
        });
        DiscordProperties properties = new DiscordProperties("", "", new DiscordProperties.Bot(false, token, "", "777"), null);
        return new DiscordBotRestClient(builder, properties);
    }

    @Test
    @DisplayName("메시지 생성은 봇 토큰으로 POST하고 메시지 ID를 돌려준다")
    void createMessage() {
        DiscordBotRestClient client = client("token-abc", "{\"id\":\"1234567890\"}");

        String id = client.createMessage("777", Map.of("content", "hi"));

        assertThat(id).isEqualTo("1234567890");
        ClientRequest request = requests.getFirst();
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.url().toString()).isEqualTo("https://discord.com/api/v10/channels/777/messages");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bot token-abc");
    }

    @Test
    @DisplayName("메시지 수정은 PATCH, 스레드 생성은 메시지 하위 threads로 POST")
    void editAndStartThread() {
        DiscordBotRestClient client = client("token-abc", "{}");

        client.editMessage("777", "555", Map.of("content", "x"));
        client.startThread("777", "555", "처리 기록");

        assertThat(requests.get(0).method()).isEqualTo(HttpMethod.PATCH);
        assertThat(requests.get(0).url().toString()).isEqualTo("https://discord.com/api/v10/channels/777/messages/555");
        assertThat(requests.get(1).method()).isEqualTo(HttpMethod.POST);
        assertThat(requests.get(1).url().toString()).isEqualTo("https://discord.com/api/v10/channels/777/messages/555/threads");
    }

    @Test
    @DisplayName("메시지 조회는 GET으로 JSON을 맵으로 돌려준다")
    void getMessage() {
        DiscordBotRestClient client = client("token-abc", "{\"id\":\"555\",\"embeds\":[{\"description\":\"d\"}]}");

        Map<String, Object> message = client.getMessage("777", "555");

        assertThat(requests.getFirst().method()).isEqualTo(HttpMethod.GET);
        assertThat(message).containsEntry("id", "555");
    }

    @Test
    @DisplayName("토큰이 비어 있으면 미설정")
    void isConfigured() {
        assertThat(client("", "{}").isConfigured()).isFalse();
        assertThat(client("t", "{}").isConfigured()).isTrue();
    }
}
