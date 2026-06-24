package server.MATE.domain.testdraft.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestDraftValidationTest {

   @Test
    @DisplayName("goalPpl 없으면 DRAFT_005")
    void validateAmountFields_missingGoalPpl_throwsDraft005() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .reward(300)
                .closedAt(LocalDateTime.now().plusDays(10))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validateAmountFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_005);
    }

    @Test
    @DisplayName("reward 없으면 DRAFT_005")
    void validateAmountFields_missingReward_throwsDraft005() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5)
                .closedAt(LocalDateTime.now().plusDays(10))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validateAmountFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_005);
    }

    @Test
    @DisplayName("closedAt 없으면 DRAFT_005")
    void validateAmountFields_missingClosedAt_throwsDraft005() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5)
                .reward(300)
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validateAmountFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_005);
    }

    @Test
    @DisplayName("goalPpl이 0 이하이면 DRAFT_005")
    void validateAmountFields_nonPositiveGoalPpl_throwsDraft005() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(0)
                .reward(300)
                .closedAt(LocalDateTime.now().plusDays(10))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validateAmountFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_005);
    }

    @Test
    @DisplayName("reward가 음수이면 DRAFT_005")
    void validateAmountFields_negativeReward_throwsDraft005() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5)
                .reward(-1)
                .closedAt(LocalDateTime.now().plusDays(10))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validateAmountFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_005);
    }

    @Test
    @DisplayName("title이 blank이면 DRAFT_006")
    void validatePublishableFields_blankTitle_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("  ")
                .description("설명")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("description이 blank이면 DRAFT_006")
    void validatePublishableFields_blankDescription_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목")
                .description("")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("categories가 비어있으면 DRAFT_006")
    void validatePublishableFields_emptyCategories_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(List.of())
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("categories에 유효하지 않은 enum 값이 있으면 DRAFT_006")
    void validatePublishableFields_invalidCategory_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(List.of("INVALID_CATEGORY"))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("categories에 null 요소가 있으면 DRAFT_006")
    void validatePublishableFields_nullCategory_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(Arrays.asList("FOOD", null))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("questionsPayload가 null이면 DRAFT_006")
    void validatePublishableFields_nullQuestionsPayload_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(List.of("FOOD"))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("questionsPayload의 questions 리스트가 비어있으면 DRAFT_006")
    void validatePublishableFields_emptyQuestions_throwsDraft006() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", List.of()))
                .build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishableFields);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_006);
    }

    @Test
    @DisplayName("게시 필수 필드를 모두 충족하면 예외 없음")
    void validatePublishableFields_allFieldsValid_doesNotThrow() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .status(TestDraftStatus.DRAFT)
                .goalPpl(5).reward(300).closedAt(LocalDateTime.now().plusDays(10))
                .title("제목").description("설명")
                .categories(List.of("FOOD"))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE"))))
                .build();

        assertThatCode(draft::validatePublishableFields).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DRAFT 상태이면 예외 없음")
    void validatePublishState_draftStatus_doesNotThrow() {
        TestDraft draft = TestDraft.builder().makerId(1L).status(TestDraftStatus.DRAFT).build();
        assertThatCode(draft::validatePublishState).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PUBLISH_FAILED 상태이면 예외 없음")
    void validatePublishState_publishFailed_doesNotThrow() {
        TestDraft draft = TestDraft.builder().makerId(1L).status(TestDraftStatus.PUBLISH_FAILED).build();
        assertThatCode(draft::validatePublishState).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PUBLISHING 상태이면 DRAFT_004")
    void validatePublishState_publishing_throwsDraft004() {
        TestDraft draft = TestDraft.builder().makerId(1L).status(TestDraftStatus.PUBLISHING).build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishState);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_004);
    }

    @Test
    @DisplayName("EXPIRED 상태이면 DRAFT_004")
    void validatePublishState_expired_throwsDraft004() {
        TestDraft draft = TestDraft.builder().makerId(1L).status(TestDraftStatus.EXPIRED).build();

        BaseException ex = assertThrows(BaseException.class, draft::validatePublishState);
        assertThat(ex.getErrorCode()).isEqualTo(BaseErrorCode.DRAFT_004);
    }
}
