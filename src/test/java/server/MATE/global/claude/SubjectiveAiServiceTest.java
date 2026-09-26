package server.MATE.global.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectiveAiServiceTest {

    @Test
    @DisplayName("AI 호출이 401이면 실패 결과에 예외 종류와 메시지를 담는다")
    void analyze_failureCarriesReason() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()))
                .build();
        SubjectiveAiService service = new SubjectiveAiService(
                webClient, new ClaudeProperties("test-key", null, 0, 0), new ObjectMapper());

        AiAnalysisOutcome outcome = service.analyze(List.of("응답1", "응답2"));

        assertThat(outcome.isSuccess()).isFalse();
        assertThat(outcome.failureReason()).startsWith("WebClientResponseException$Unauthorized: ");
    }
}
