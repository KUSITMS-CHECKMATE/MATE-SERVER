package server.MATE.domain.question.service;

import org.junit.jupiter.api.AfterEach;
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
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.question.service.handler.ScaleQuestionCreateHandler;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.image.ImageService;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

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

    @Autowired
    private TreeTestRepository treeTestRepository;

    @SpyBean
    private QuestionRepository questionRepository;

    @SpyBean
    private ScaleQuestionCreateHandler scaleQuestionCreateHandler;

    @MockBean
    private ImageService imageService;

    @AfterEach
    void tearDown() {
        treeTestRepository.deleteAll();
        objectiveRepository.deleteAll();
        scaleRepository.deleteAll();
        questionRepository.deleteAll();
        testRepository.deleteAll();
    }

    @Test
    @DisplayName("혼합 요청이 성공하면 sequence 순서와 세부 엔티티가 함께 저장되고 cleanup 삭제는 실행되지 않는다")
    void savesMixedQuestionsInOrderWithoutDeletingImagesOnSuccess() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

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
                ),
                new TreeTestCreateRequest(
                        "트리 테스트",
                        "설명",
                        List.of(
                                new TreeTestCreateRequest.Feature(
                                        "마이페이지",
                                        List.of(
                                                new TreeTestCreateRequest.TreeNode("설정", List.of())
                                        )
                                )
                        )
                )
        ));

        QuestionCreateResponse response = questionService.createQuestions(savedTest.getId(), 1L, request);

        List<Question> savedQuestions = questionRepository.findAll().stream()
                .sorted(Comparator.comparing(Question::getSequence))
                .toList();

        assertThat(savedQuestions).extracting(Question::getQuestionType)
                .containsExactly(QuestionType.OBJECTIVE, QuestionType.SCALE, QuestionType.TREE_TEST);
        assertThat(savedQuestions).extracting(Question::getSequence)
                .containsExactly(1L, 2L, 3L);
        assertThat(response.questions()).extracting(result -> result.sequence())
                .containsExactly(1L, 2L, 3L);

        assertThat(objectiveRepository.count()).isEqualTo(1L);
        assertThat(scaleRepository.count()).isEqualTo(1L);
        assertThat(treeTestRepository.count()).isEqualTo(2L);
        then(imageService).should(never()).deleteFiles(any());
    }

    @Test
    @DisplayName("문항 저장 중간에 DB 제약 오류가 나면 선행 저장도 rollback 되고 cleanup 이벤트가 rollback 시점에 실행된다")
    void rollsBackPersistedQuestionsAndTriggersCleanupOnRollback() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

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

    @Test
    @DisplayName("두 번째 문항 상세 저장에서 예외가 나면 선행 저장도 rollback 되고 cleanup 이벤트가 rollback 시점에 실행된다")
    void rollsBackWhenDetailCreationFailsAfterFirstQuestionIsSaved() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

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

        doThrow(new IllegalStateException("detail save failed"))
                .when(scaleQuestionCreateHandler)
                .createDetail(any(Question.class), any());

        assertThatThrownBy(() -> questionService.createQuestions(savedTest.getId(), 1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("detail save failed");

        assertThat(questionRepository.count()).isZero();
        assertThat(objectiveRepository.count()).isZero();
        assertThat(scaleRepository.count()).isZero();

        ArgumentCaptor<List<String>> imageKeysCaptor = ArgumentCaptor.forClass(List.class);
        then(imageService).should().deleteFiles(imageKeysCaptor.capture());
        assertThat(imageKeysCaptor.getValue()).containsExactly("image-a", "image-scale");
    }

    private server.MATE.domain.test.entity.Test createTest() {
        return testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .build());
    }
}
