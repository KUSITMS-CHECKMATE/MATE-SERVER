package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.AbTestCreateRequest;
import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbTestQuestionCreateHandlerTest {

    @Mock
    private AbTestRepository abTestRepository;

    @Test
    @DisplayName("AB 테스트 상세 엔티티를 저장하고 두 이미지 키를 추출한다")
    void savesAbTestDetailAndExtractsBothImageKeys() {
        AbTestQuestionCreateHandler handler = new AbTestQuestionCreateHandler(abTestRepository);
        AbTestCreateRequest request = new AbTestCreateRequest(
                "AB 테스트",
                "설명",
                "image-a",
                "image-b"
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.AB_TEST)
                .title("AB 테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<AbTest> captor = ArgumentCaptor.forClass(AbTest.class);
        verify(abTestRepository).save(captor.capture());
        AbTest saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getAImageKey()).isEqualTo("image-a");
        assertThat(saved.getBImageKey()).isEqualTo("image-b");
        assertThat(handler.extractImageKeys(request)).containsExactly("image-a", "image-b");
    }
}
