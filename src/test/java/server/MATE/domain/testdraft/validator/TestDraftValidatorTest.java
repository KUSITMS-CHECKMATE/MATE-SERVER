package server.MATE.domain.testdraft.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestDraftValidatorTest {

    private TestDraftValidator testDraftValidator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        testDraftValidator = new TestDraftValidator(objectMapper, validator);
    }

    @Test
    @DisplayName("게시 검증에서 questions 값이 List가 아니면 DRAFT_006")
    void validateForPublish_questionsNotList_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .status(server.MATE.domain.testdraft.entity.TestDraftStatus.PUBLISH_FAILED)
                .title("제목")
                .description("설명")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", "invalid_string_not_a_list"))
                .build();

        BaseException ex = assertThrows(BaseException.class,
                () -> testDraftValidator.validateForPublish(draft));
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("게시 검증에서 상태가 PUBLISHING이면 DRAFT_004")
    void validateForPublish_invalidState_throwsDraft004() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .status(server.MATE.domain.testdraft.entity.TestDraftStatus.PUBLISHING)
                .title("제목")
                .description("설명")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", List.of(
                        Map.of("type", "SUBJECTIVE", "title", "질문 제목", "description", "질문 설명")
                )))
                .build();

        BaseException ex = assertThrows(BaseException.class,
                () -> testDraftValidator.validateForPublish(draft));
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_004);
    }
}
