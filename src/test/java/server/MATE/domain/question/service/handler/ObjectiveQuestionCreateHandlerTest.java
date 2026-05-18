package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ObjectiveQuestionCreateHandlerTest {

    @Mock
    private ObjectiveRepository objectiveRepository;

    @Test
    @DisplayName("객관식 중복 선택에서 min이 1보다 작으면 QUESTION_001 예외가 발생한다")
    void throwsQuestion001WhenMinSelectIsLessThanOne() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                true,
                2,
                0,
                false,
                List.of(
                        new ObjectiveOptionRequest("A", null),
                        new ObjectiveOptionRequest("B", null)
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_001);
    }

    @Test
    @DisplayName("객관식 중복 선택에서 max가 min보다 작으면 QUESTION_002 예외가 발생한다")
    void throwsQuestion002WhenMaxSelectIsLessThanMinSelect() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                true,
                1,
                2,
                false,
                List.of(
                        new ObjectiveOptionRequest("A", null),
                        new ObjectiveOptionRequest("B", null)
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_002);
    }

    @Test
    @DisplayName("객관식 중복 선택에서 min 또는 max가 선택지 개수를 초과하면 QUESTION_003 예외가 발생한다")
    void throwsQuestion003WhenSelectRangeExceedsOptionCount() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                true,
                3,
                1,
                false,
                List.of(
                        new ObjectiveOptionRequest("A", null),
                        new ObjectiveOptionRequest("B", null)
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_003);
    }

    @Test
    @DisplayName("기타 포함 총 선택지가 10개를 초과하면 COMMON_002 예외가 발생한다")
    void throwsCommon002WhenEffectiveOptionCountExceedsTen() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                false,
                null,
                null,
                true,
                List.of(
                        new ObjectiveOptionRequest("A", null),
                        new ObjectiveOptionRequest("B", null),
                        new ObjectiveOptionRequest("C", null),
                        new ObjectiveOptionRequest("D", null),
                        new ObjectiveOptionRequest("E", null),
                        new ObjectiveOptionRequest("F", null),
                        new ObjectiveOptionRequest("G", null),
                        new ObjectiveOptionRequest("H", null),
                        new ObjectiveOptionRequest("I", null),
                        new ObjectiveOptionRequest("J", null)
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
    }

    @Test
    @DisplayName("단일 선택 객관식에 min/max가 들어오면 QUESTION_009 예외가 발생한다")
    void throwsQuestion009WhenSingleSelectObjectiveContainsMinMax() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                false,
                2,
                1,
                false,
                List.of(
                        new ObjectiveOptionRequest("A", null),
                        new ObjectiveOptionRequest("B", null)
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_009);
    }

    @Test
    @DisplayName("객관식 상세 엔티티를 저장하고 이미지 키를 추출한다")
    void savesObjectiveDetailAndExtractsImageKeys() {
        ObjectiveQuestionCreateHandler handler = new ObjectiveQuestionCreateHandler(objectiveRepository);
        ObjectiveCreateRequest request = new ObjectiveCreateRequest(
                "객관식",
                "설명",
                false,
                null,
                null,
                true,
                List.of(
                        new ObjectiveOptionRequest("A", "image-a"),
                        new ObjectiveOptionRequest("B", null)
                )
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<Objective> captor = ArgumentCaptor.forClass(Objective.class);
        verify(objectiveRepository).save(captor.capture());
        Objective saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getOptions()).hasSize(3);
        assertThat(saved.getOptions().get(0).getSequence()).isEqualTo(1);
        ObjectiveOption otherOption = saved.getOptions().get(2);
        assertThat(otherOption.getContent()).isEqualTo("기타 (직접 입력)");
        assertThat(otherOption.getImageKey()).isNull();
        assertThat(otherOption.getSequence()).isEqualTo(3);
        assertThat(otherOption.getIsOtherOption()).isTrue();
        assertThat(handler.extractImageKeys(request)).containsExactly("image-a");
    }
}
