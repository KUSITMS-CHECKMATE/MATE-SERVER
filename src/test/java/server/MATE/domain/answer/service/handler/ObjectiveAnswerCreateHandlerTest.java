package server.MATE.domain.answer.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectiveAnswerCreateHandlerTest {

    private final ObjectiveAnswerCreateHandler handler = new ObjectiveAnswerCreateHandler();

    @Test
    @DisplayName("기타 선택지와 otherText를 함께 보내면 객관식 응답을 저장한다")
    void buildsObjectiveAnswerWithOtherOption() {
        Objective objective = objective(true, true);
        ObjectiveOption firstOption = option(objective, 1001L, "검색", 1, false);
        ObjectiveOption otherOption = option(objective, 1999L, "기타 (직접 입력)", 2, true);
        objective.addOption(firstOption);
        objective.addOption(otherOption);

        ObjectiveAnswerCreateRequest request = new ObjectiveAnswerCreateRequest(1L, List.of(1001L, 1999L), "직접 입력");

        Answer answer = handler.build(10L, request, context(objective));

        assertThat(answer.getQuestionType()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(answer.getAnswer()).containsEntry("optionIds", List.of(1001L, 1999L));
        assertThat(answer.getAnswer()).containsEntry("otherText", "직접 입력");
    }

    @Test
    @DisplayName("기타 선택지 없이 otherText만 보내면 ANSWER_004 예외가 발생한다")
    void throwsAnswer004WhenOtherTextProvidedWithoutOtherOption() {
        Objective objective = objective(false, true);
        objective.addOption(option(objective, 1001L, "검색", 1, false));
        objective.addOption(option(objective, 1999L, "기타 (직접 입력)", 2, true));

        ObjectiveAnswerCreateRequest request = new ObjectiveAnswerCreateRequest(1L, List.of(1001L), "직접 입력");

        assertThatThrownBy(() -> handler.build(10L, request, context(objective)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.ANSWER_004);
    }

    @Test
    @DisplayName("기타 선택지인데 otherText가 없으면 ANSWER_004 예외가 발생한다")
    void throwsAnswer004WhenOtherOptionSelectedWithoutOtherText() {
        Objective objective = objective(false, true);
        objective.addOption(option(objective, 1999L, "기타 (직접 입력)", 1, true));

        ObjectiveAnswerCreateRequest request = new ObjectiveAnswerCreateRequest(1L, List.of(1999L), null);

        assertThatThrownBy(() -> handler.build(10L, request, context(objective)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.ANSWER_004);
    }

    private AnswerCreateContext context(Objective objective) {
        return new AnswerCreateContext(
                Map.of(1L, objective),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    private Objective objective(boolean isDuplicate, boolean isOther) {
        return Objective.builder()
                .question(null)
                .isDuplicate(isDuplicate)
                .maxSelect(null)
                .minSelect(null)
                .isOther(isOther)
                .build();
    }

    private ObjectiveOption option(Objective objective, Long id, String content, int sequence, boolean isOtherOption) {
        ObjectiveOption option = ObjectiveOption.builder()
                .objective(objective)
                .content(content)
                .imageKey(null)
                .sequence(sequence)
                .isOtherOption(isOtherOption)
                .build();
        setOptionId(option, id);
        return option;
    }

    private void setOptionId(ObjectiveOption option, Long id) {
        try {
            java.lang.reflect.Field field = ObjectiveOption.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(option, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
