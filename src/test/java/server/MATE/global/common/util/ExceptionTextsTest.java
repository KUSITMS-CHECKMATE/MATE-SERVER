package server.MATE.global.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTextsTest {

    @Test
    @DisplayName("패키지를 뺀 클래스명과 메시지를 잇는다")
    void describe_joinsClassAndMessage() {
        assertThat(ExceptionTexts.describe(new IllegalStateException("boom")))
                .isEqualTo("IllegalStateException: boom");
    }

    @Test
    @DisplayName("메시지가 없으면 (no message)")
    void describe_nullMessage() {
        assertThat(ExceptionTexts.describe(new NullPointerException()))
                .isEqualTo("NullPointerException: (no message)");
    }

    @Test
    @DisplayName("중첩 클래스는 $ 이름을 유지한다")
    void describe_keepsNestedName() {
        WebClientResponseException e = WebClientResponseException.create(401, "Unauthorized", null, null, null);

        assertThat(ExceptionTexts.describe(e)).startsWith("WebClientResponseException$Unauthorized: ");
    }

    @Test
    @DisplayName("위치는 server.MATE로 시작하는 첫 스택 프레임, 없거나 null이면 N/A")
    void location() {
        assertThat(ExceptionTexts.location(new RuntimeException("x"))).startsWith("server.MATE.global.common.util.ExceptionTextsTest.location:");
        assertThat(ExceptionTexts.location(null)).isEqualTo("N/A");
    }
}
