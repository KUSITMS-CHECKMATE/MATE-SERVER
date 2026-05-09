package server.MATE.domain.question.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.ImageService;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class QuestionServiceIntegrationTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Autowired
    private ScaleRepository scaleRepository;

    @Autowired
    private TreeTestRepository treeTestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private ScaleQuestionCreateHandler scaleQuestionCreateHandler;

    @MockitoBean
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
    @DisplayName("혼합 요청이 성공하면 sequence와 세부 구조가 함께 저장되고 cleanup 삭제는 실행되지 않는다")
    void savesMixedQuestionsInOrderWithDetailsWithoutDeletingImagesOnSuccess() {
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

        Long objectiveId = objectiveRepository.findAll().get(0).getId();
        List<Object[]> objectiveOptions = entityManager.createQuery("""
                        select option.content, option.sequence
                        from ObjectiveOption option
                        where option.objective.id = :objectiveId
                        order by option.sequence
                        """, Object[].class)
                .setParameter("objectiveId", objectiveId)
                .getResultList();
        assertThat(objectiveOptions)
                .extracting(row -> row[0], row -> row[1])
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", 1),
                        org.assertj.core.groups.Tuple.tuple("B", 2)
                );

        Long treeQuestionId = savedQuestions.get(2).getId();
        List<Object[]> treeNodes = entityManager.createQuery("""
                        select node.label, node.depth, parent.label
                        from TreeTest node
                        left join node.parent parent
                        where node.question.id = :questionId
                        order by node.depth, node.sequence
                        """, Object[].class)
                .setParameter("questionId", treeQuestionId)
                .getResultList();
        assertThat(treeNodes)
                .extracting(row -> row[0], row -> row[1], row -> row[2])
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("마이페이지", 0, null),
                        org.assertj.core.groups.Tuple.tuple("설정", 1, "마이페이지")
                );

        assertThat(objectiveRepository.count()).isEqualTo(1L);
        assertThat(scaleRepository.count()).isEqualTo(1L);
        assertThat(treeTestRepository.count()).isEqualTo(2L);
        verify(imageService, never()).deleteFiles(anyList());
    }

    @Test
    @DisplayName("두 번째 문항 상세 저장에서 애플리케이션 예외가 나면 선행 저장도 rollback 되고 cleanup 이벤트가 rollback 시점에 실행된다")
    void rollsBackWhenApplicationExceptionOccursDuringSecondDetailCreation() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        doThrow(new BaseException(BaseErrorCode.COMMON_999, "detail save failed"))
                .when(scaleQuestionCreateHandler)
                .createDetail(any(Question.class), any());

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

        assertThatThrownBy(() -> questionService.createQuestions(savedTest.getId(), 1L, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_999);

        assertThat(questionRepository.count()).isZero();
        assertThat(objectiveRepository.count()).isZero();
        assertThat(scaleRepository.count()).isZero();
        verify(imageService).deleteFiles(List.of("image-a", "image-scale"));
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
