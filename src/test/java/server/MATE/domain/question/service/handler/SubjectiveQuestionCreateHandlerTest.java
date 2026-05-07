package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.SubjectiveCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;
import server.MATE.domain.question.repository.SubjectiveRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubjectiveQuestionCreateHandlerTest {

    @Mock
    private SubjectiveRepository subjectiveRepository;

    @Test
    @DisplayName("주관식 상세 엔티티를 저장하고 이미지 키를 추출한다")
    void savesSubjectiveDetailAndExtractsImageKeys() {
        SubjectiveQuestionCreateHandler handler = new SubjectiveQuestionCreateHandler(subjectiveRepository);
        SubjectiveCreateRequest request = new SubjectiveCreateRequest(
                "주관식",
                "설명",
                "image-subjective"
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<Subjective> captor = ArgumentCaptor.forClass(Subjective.class);
        verify(subjectiveRepository).save(captor.capture());
        Subjective saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getImageKey()).isEqualTo("image-subjective");
        assertThat(handler.extractImageKeys(request)).containsExactly("image-subjective");
    }

    @Test
    @DisplayName("주관식 이미지가 없으면 빈 이미지 키 목록을 반환한다")
    void returnsEmptyImageKeysWhenSubjectiveImageIsMissing() {
        SubjectiveQuestionCreateHandler handler = new SubjectiveQuestionCreateHandler(subjectiveRepository);
        SubjectiveCreateRequest request = new SubjectiveCreateRequest(
                "주관식",
                "설명",
                null
        );

        assertThat(handler.extractImageKeys(request)).isEmpty();
    }
}
