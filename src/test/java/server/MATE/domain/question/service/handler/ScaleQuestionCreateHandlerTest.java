package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScaleQuestionCreateHandlerTest {

    @Mock
    private ScaleRepository scaleRepository;

    @Test
    @DisplayName("척도 문항 range가 5나 7이 아니면 COMMON_002 예외가 발생한다")
    void throwsCommon002WhenRangeIsNotFiveOrSeven() {
        ScaleQuestionCreateHandler handler = new ScaleQuestionCreateHandler(scaleRepository);
        ScaleCreateRequest request = new ScaleCreateRequest(
                "척도",
                "설명",
                null,
                "낮음",
                "높음",
                9
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
    }

    @Test
    @DisplayName("척도 상세 엔티티를 저장하고 이미지 키를 추출한다")
    void savesScaleDetailAndExtractsImageKeys() {
        ScaleQuestionCreateHandler handler = new ScaleQuestionCreateHandler(scaleRepository);
        ScaleCreateRequest request = new ScaleCreateRequest(
                "척도",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.SCALE)
                .title("척도")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<Scale> captor = ArgumentCaptor.forClass(Scale.class);
        verify(scaleRepository).save(captor.capture());
        Scale saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getRange()).isEqualTo(5);
        assertThat(handler.extractImageKeys(request)).containsExactly("image-scale");
    }

    @Test
    @DisplayName("척도 라벨이 비어 있으면 엔티티 기본 라벨을 저장한다")
    void savesDefaultLabelsWhenScaleLabelsAreMissing() {
        ScaleQuestionCreateHandler handler = new ScaleQuestionCreateHandler(scaleRepository);
        ScaleCreateRequest request = new ScaleCreateRequest(
                "척도",
                "설명",
                null,
                null,
                null,
                5
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.SCALE)
                .title("척도")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<Scale> captor = ArgumentCaptor.forClass(Scale.class);
        verify(scaleRepository).save(captor.capture());
        Scale saved = captor.getValue();

        assertThat(saved.getMinLabel()).isEqualTo("전혀 아니다");
        assertThat(saved.getMaxLabel()).isEqualTo("매우 그렇다");
    }
}
