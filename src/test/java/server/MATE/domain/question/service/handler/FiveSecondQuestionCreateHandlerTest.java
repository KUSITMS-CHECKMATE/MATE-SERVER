package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.FiveSecondCreateRequest;
import server.MATE.domain.question.dto.request.FiveSecondOptionRequest;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FiveSecondQuestionCreateHandlerTest {

    @Mock
    private FiveSecondRepository fiveSecondRepository;

    @Test
    @DisplayName("5초 테스트가 객관식인데 선택지가 부족하면 QUESTION_004 예외가 발생한다")
    void throwsQuestion004WhenObjectiveOptionsAreInsufficient() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                false,
                null,
                null,
                true,
                List.of(new FiveSecondOptionRequest("A"))
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_004);
    }

    @Test
    @DisplayName("5초 테스트가 주관식인데 선택지가 있으면 QUESTION_008 예외가 발생한다")
    void throwsQuestion008WhenSubjectiveFiveSecondContainsOptions() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                false,
                null,
                null,
                null,
                null,
                List.of(new FiveSecondOptionRequest("A"))
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_008);
    }

    @Test
    @DisplayName("5초 테스트가 주관식인데 객관식 설정값이 있으면 QUESTION_008 예외가 발생한다")
    void throwsQuestion008WhenSubjectiveFiveSecondContainsObjectiveSettings() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                false,
                true,
                1,
                2,
                null,
                List.of()
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_008);
    }

    @Test
    @DisplayName("5초 테스트가 주관식인데 isOther가 있으면 QUESTION_008 예외가 발생한다")
    void throwsQuestion008WhenSubjectiveFiveSecondContainsIsOther() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                false,
                null,
                null,
                null,
                true,
                List.of()
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_008);
    }

    @Test
    @DisplayName("5초 테스트가 객관식인데 isOther가 없으면 COMMON_002 예외가 발생한다")
    void throwsCommon002WhenObjectiveFiveSecondDoesNotContainIsOther() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                false,
                null,
                null,
                null,
                List.of(
                        new FiveSecondOptionRequest("A"),
                        new FiveSecondOptionRequest("B")
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
    }

    @Test
    @DisplayName("5초 테스트 중복 선택에서 min이 1보다 작으면 QUESTION_001 예외가 발생한다")
    void throwsQuestion001WhenMinSelectIsLessThanOne() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                true,
                0,
                2,
                false,
                List.of(
                        new FiveSecondOptionRequest("A"),
                        new FiveSecondOptionRequest("B")
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_001);
    }

    @Test
    @DisplayName("5초 테스트 중복 선택에서 max가 min보다 작으면 QUESTION_002 예외가 발생한다")
    void throwsQuestion002WhenMaxSelectIsLessThanMinSelect() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                true,
                2,
                1,
                false,
                List.of(
                        new FiveSecondOptionRequest("A"),
                        new FiveSecondOptionRequest("B")
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_002);
    }

    @Test
    @DisplayName("5초 테스트 중복 선택에서 min 또는 max가 선택지 개수를 초과하면 QUESTION_003 예외가 발생한다")
    void throwsQuestion003WhenSelectRangeExceedsOptionCount() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                true,
                1,
                3,
                false,
                List.of(
                        new FiveSecondOptionRequest("A"),
                        new FiveSecondOptionRequest("B")
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_003);
    }

    @Test
    @DisplayName("5초 테스트 상세 엔티티를 저장하고 이미지 키를 추출한다")
    void savesFiveSecondDetailAndExtractsImageKeys() {
        FiveSecondQuestionCreateHandler handler = new FiveSecondQuestionCreateHandler(fiveSecondRepository);
        FiveSecondCreateRequest request = new FiveSecondCreateRequest(
                "5초",
                "설명",
                "image",
                true,
                false,
                null,
                null,
                true,
                List.of(
                        new FiveSecondOptionRequest("A"),
                        new FiveSecondOptionRequest("B")
                )
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.FIVE_SECOND)
                .title("5초")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<FiveSecond> captor = ArgumentCaptor.forClass(FiveSecond.class);
        verify(fiveSecondRepository).save(captor.capture());
        FiveSecond saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getIsOther()).isTrue();
        assertThat(saved.getOptions()).hasSize(2);
        assertThat(saved.getOptions().get(0).getSequence()).isEqualTo(1);
        assertThat(handler.extractImageKeys(request)).containsExactly("image");
    }
}
