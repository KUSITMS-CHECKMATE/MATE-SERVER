package server.MATE.domain.question.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.image.ImageService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@ActiveProfiles("test")
class QuestionServiceIntegrationTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Autowired
    private ScaleRepository scaleRepository;

    @SpyBean
    private QuestionRepository questionRepository;

    @MockBean
    private ImageService imageService;

    @Test
    @DisplayName("문항 저장 중간에 DB 제약 오류가 나면 선행 저장도 rollback 되고 cleanup 이벤트가 rollback 시점에 실행된다")
    void rollsBackPersistedQuestionsAndTriggersCleanupOnRollback() {
        server.MATE.domain.test.entity.Test savedTest = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .build());

        questionRepository.save(Question.builder()
                .testId(savedTest.getId())
                .questionType(QuestionType.SUBJECTIVE)
                .title("기존 질문")
                .description("기존 설명")
                .sequence(2L)
                .build());

        doReturn(0L).when(questionRepository).findMaxSequenceByTestId(savedTest.getId());

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ObjectiveCreateRequest(
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(
                                new ObjectiveOptionRequest("A", "image-a"),
                                new ObjectiveOptionRequest("B", null)
                        )
                ),
                new ScaleCreateRequest(
                        "척도 질문",
                        "설명",
                        "image-scale",
                        "낮음",
                        "높음",
                        5
                )
        ));

        long questionCountBefore = questionRepository.count();

        assertThatThrownBy(() -> questionService.createQuestions(savedTest.getId(), 1L, request))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(questionRepository.count()).isEqualTo(questionCountBefore);
        assertThat(questionRepository.findAll())
                .extracting(Question::getTitle, Question::getSequence)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("기존 질문", 2L));
        assertThat(objectiveRepository.count()).isZero();
        assertThat(scaleRepository.count()).isZero();

        ArgumentCaptor<List<String>> imageKeysCaptor = ArgumentCaptor.forClass(List.class);
        then(imageService).should().deleteFiles(imageKeysCaptor.capture());
        assertThat(imageKeysCaptor.getValue()).containsExactly("image-a", "image-scale");
    }
}
