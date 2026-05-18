package server.MATE.domain.answer.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.domain.answer.dto.request.FiveSecondAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiveSecondAnswerCreateHandlerTest {

    private final FiveSecondAnswerCreateHandler handler = new FiveSecondAnswerCreateHandler();

    @Test
    @DisplayName("기타 선택지와 otherText를 함께 보내면 5초 테스트 객관식 응답을 저장한다")
    void buildsFiveSecondObjectiveAnswerWithOtherOption() {
        FiveSecond fiveSecond = objectiveFiveSecond(true, true, 1, 2);
        FiveSecondOption firstOption = option(fiveSecond, 2001L, "검색창", 1, false);
        FiveSecondOption otherOption = option(fiveSecond, 2099L, "기타 (직접 입력)", 2, true);
        fiveSecond.addOption(firstOption);
        fiveSecond.addOption(otherOption);

        FiveSecondAnswerCreateRequest request =
                new FiveSecondAnswerCreateRequest(1L, null, List.of(2001L, 2099L), "하단 CTA 버튼");

        Answer answer = handler.build(10L, request, context(fiveSecond));

        assertThat(answer.getQuestionType()).isEqualTo(QuestionType.FIVE_SECOND);
        assertThat(answer.getAnswer()).containsEntry("optionIds", List.of(2001L, 2099L));
        assertThat(answer.getAnswer()).containsEntry("otherText", "하단 CTA 버튼");
    }

    @Test
    @DisplayName("기타 선택지 없이 otherText만 보내면 ANSWER_004 예외가 발생한다")
    void throwsAnswer004WhenOtherTextProvidedWithoutOtherOption() {
        FiveSecond fiveSecond = objectiveFiveSecond(true, true, 1, 2);
        fiveSecond.addOption(option(fiveSecond, 2001L, "검색창", 1, false));
        fiveSecond.addOption(option(fiveSecond, 2099L, "기타 (직접 입력)", 2, true));

        FiveSecondAnswerCreateRequest request =
                new FiveSecondAnswerCreateRequest(1L, null, List.of(2001L), "하단 CTA 버튼");

        assertThatThrownBy(() -> handler.build(10L, request, context(fiveSecond)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.ANSWER_004);
    }

    @Test
    @DisplayName("기타 선택지인데 otherText가 없으면 ANSWER_004 예외가 발생한다")
    void throwsAnswer004WhenOtherOptionSelectedWithoutOtherText() {
        FiveSecond fiveSecond = objectiveFiveSecond(false, true, null, null);
        fiveSecond.addOption(option(fiveSecond, 2099L, "기타 (직접 입력)", 1, true));

        FiveSecondAnswerCreateRequest request =
                new FiveSecondAnswerCreateRequest(1L, null, List.of(2099L), null);

        assertThatThrownBy(() -> handler.build(10L, request, context(fiveSecond)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.ANSWER_004);
    }

    @Test
    @DisplayName("주관식 5초 테스트에 otherText가 오면 ANSWER_004 예외가 발생한다")
    void throwsAnswer004WhenSubjectiveFiveSecondContainsOtherText() {
        FiveSecond fiveSecond = subjectiveFiveSecond();

        FiveSecondAnswerCreateRequest request =
                new FiveSecondAnswerCreateRequest(1L, "첫 인상", null, "불필요한 기타 텍스트");

        assertThatThrownBy(() -> handler.build(10L, request, context(fiveSecond)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.ANSWER_004);
    }

    private AnswerCreateContext context(FiveSecond fiveSecond) {
        return new AnswerCreateContext(
                Map.of(),
                Map.of(1L, fiveSecond),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    private FiveSecond objectiveFiveSecond(boolean isDuplicate, boolean isOther, Integer minSelect, Integer maxSelect) {
        return FiveSecond.builder()
                .question(null)
                .imageKey("image")
                .imageRatio(ImageRatio.RATIO_9_16)
                .isObjective(true)
                .isDuplicate(isDuplicate)
                .minSelect(minSelect)
                .maxSelect(maxSelect)
                .isOther(isOther)
                .build();
    }

    private FiveSecond subjectiveFiveSecond() {
        return FiveSecond.builder()
                .question(null)
                .imageKey("image")
                .imageRatio(ImageRatio.RATIO_9_16)
                .isObjective(false)
                .isDuplicate(null)
                .minSelect(null)
                .maxSelect(null)
                .isOther(null)
                .build();
    }

    private FiveSecondOption option(FiveSecond fiveSecond, Long id, String content, int sequence, boolean isOtherOption) {
        FiveSecondOption option = FiveSecondOption.builder()
                .fiveSecond(fiveSecond)
                .content(content)
                .sequence(sequence)
                .isOtherOption(isOtherOption)
                .build();
        setOptionId(option, id);
        return option;
    }

    private void setOptionId(FiveSecondOption option, Long id) {
        try {
            java.lang.reflect.Field field = FiveSecondOption.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(option, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
