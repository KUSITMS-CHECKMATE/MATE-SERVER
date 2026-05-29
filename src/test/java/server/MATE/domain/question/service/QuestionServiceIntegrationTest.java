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
import server.MATE.domain.question.dto.request.AbTestCreateRequest;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.dto.request.FiveSecondCreateRequest;
import server.MATE.domain.question.dto.request.FiveSecondOptionRequest;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.dto.request.SubjectiveCreateRequest;
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.dto.response.AbTestDetailResponse;
import server.MATE.domain.question.dto.response.CardSortingDetailResponse;
import server.MATE.domain.question.dto.response.FiveSecondDetailResponse;
import server.MATE.domain.question.dto.response.ObjectiveDetailResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.QuestionsDetailResponse;
import server.MATE.domain.question.dto.response.ScaleDetailResponse;
import server.MATE.domain.question.dto.response.SubjectiveDetailResponse;
import server.MATE.domain.question.dto.response.TreeTestDetailResponse;
import server.MATE.domain.question.dto.response.TreeTestNodeDetailResponse;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.SubjectiveRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.question.service.handler.ScaleQuestionCreateHandler;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.FileStorageService;
import server.MATE.support.TestEntityFixtures;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
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

    @Autowired
    private SubjectiveRepository subjectiveRepository;

    @Autowired
    private FiveSecondRepository fiveSecondRepository;

    @Autowired
    private AbTestRepository abTestRepository;

    @Autowired
    private CardSortingRepository cardSortingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private ScaleQuestionCreateHandler scaleQuestionCreateHandler;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
        treeTestRepository.deleteAll();
        cardSortingRepository.deleteAll();
        abTestRepository.deleteAll();
        fiveSecondRepository.deleteAll();
        subjectiveRepository.deleteAll();
        objectiveRepository.deleteAll();
        scaleRepository.deleteAll();
        questionRepository.deleteAll();
        testRepository.deleteAll();
    }

    @Test
    @DisplayName("삭제된 테스트는 문항 목록 조회 시 TEST_004 예외가 발생한다")
    void throwsTest004WhenGettingQuestionsForDeletedTest() {
        server.MATE.domain.test.entity.Test savedTest = createTest();
        savedTest.delete(LocalDateTime.now());
        testRepository.saveAndFlush(savedTest);

        assertThatThrownBy(() -> questionService.getQuestionsDetails(savedTest.getId()))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    @DisplayName("객관식 조회는 선택지 sequence 순서를 유지한다")
    void getsObjectiveOptionsInSequenceOrder() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new ObjectiveCreateRequest(
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(
                                new ObjectiveOptionRequest("첫 번째", null),
                                new ObjectiveOptionRequest("두 번째", null),
                                new ObjectiveOptionRequest("세 번째", null)
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        ObjectiveDetailResponse objectiveResponse = (ObjectiveDetailResponse) response.questions().getFirst();
        assertThat(objectiveResponse.options()).extracting(option -> option.content(), option -> option.sequence())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("첫 번째", 1),
                        org.assertj.core.groups.Tuple.tuple("두 번째", 2),
                        org.assertj.core.groups.Tuple.tuple("세 번째", 3),
                        org.assertj.core.groups.Tuple.tuple("기타 (직접 입력)", 4)
                );
    }

    @Test
    @DisplayName("문항 상세 조회는 특정 문항 하나만 반환한다")
    void getsSingleQuestionDetail() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        QuestionCreateResponse createResponse = questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new ObjectiveCreateRequest(
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(
                                new ObjectiveOptionRequest("첫 번째", null),
                                new ObjectiveOptionRequest("두 번째", null)
                        )
                ),
                new SubjectiveCreateRequest(
                        "주관식 질문",
                        "설명",
                        null
                )
        )));

        long questionId = createResponse.questions().getFirst().questionId();
        QuestionDetailResponse response = questionService.getQuestionDetail(savedTest.getId(), questionId);

        assertThat(response.testId()).isEqualTo(savedTest.getId());
        assertThat(response.question()).isInstanceOf(ObjectiveDetailResponse.class);

        ObjectiveDetailResponse objectiveResponse = (ObjectiveDetailResponse) response.question();
        assertThat(objectiveResponse.questionId()).isEqualTo(questionId);
        assertThat(objectiveResponse.options()).hasSize(3);
    }

    @Test
    @DisplayName("문항 상세 조회에서 다른 테스트 문항이면 QUESTION_005 예외가 발생한다")
    void throwsQuestion005WhenGettingQuestionDetailFromAnotherTest() {
        server.MATE.domain.test.entity.Test firstTest = createTest();
        server.MATE.domain.test.entity.Test secondTest = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(2L)
                .title("다른 테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build());

        QuestionCreateResponse createResponse = questionService.createQuestions(secondTest.getId(), 2L, new QuestionCreateRequest(List.of(
                new SubjectiveCreateRequest("주관식 질문", "설명", null)
        )));

        long foreignQuestionId = createResponse.questions().getFirst().questionId();

        assertThatThrownBy(() -> questionService.getQuestionDetail(firstTest.getId(), foreignQuestionId))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_005);
    }

    @Test
    @DisplayName("5초 테스트 객관식 조회는 선택지 sequence 순서를 유지한다")
    void getsFiveSecondOptionsInSequenceOrder() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new FiveSecondCreateRequest(
                        "5초 질문",
                        "설명",
                        "five-second-image",
                        ImageRatio.RATIO_9_16,
                        true,
                        true,
                        1,
                        3,
                        true,
                        List.of(
                                new FiveSecondOptionRequest("첫 번째"),
                                new FiveSecondOptionRequest("두 번째"),
                                new FiveSecondOptionRequest("세 번째")
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        FiveSecondDetailResponse fiveSecondResponse = (FiveSecondDetailResponse) response.questions().getFirst();
        assertThat(fiveSecondResponse.options()).extracting(option -> option.content(), option -> option.sequence())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("첫 번째", 1),
                        org.assertj.core.groups.Tuple.tuple("두 번째", 2),
                        org.assertj.core.groups.Tuple.tuple("세 번째", 3),
                        org.assertj.core.groups.Tuple.tuple("기타 (직접 입력)", 4)
                );
    }

    @Test
    @DisplayName("트리 테스트 조회는 같은 depth의 children 순서를 유지한다")
    void getsTreeChildrenInSequenceOrder() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new TreeTestCreateRequest(
                        "트리 질문",
                        "설명",
                        List.of(
                                new TreeTestCreateRequest.Feature(
                                        "마이페이지",
                                        List.of(
                                                new TreeTestCreateRequest.TreeNode("설정", List.of()),
                                                new TreeTestCreateRequest.TreeNode("프로필", List.of()),
                                                new TreeTestCreateRequest.TreeNode("보안", List.of())
                                        )
                                )
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        TreeTestDetailResponse treeResponse = (TreeTestDetailResponse) response.questions().getFirst();
        assertThat(treeResponse.features()).singleElement();
        assertThat(treeResponse.features().getFirst().children())
                .extracting(TreeTestNodeDetailResponse::label)
                .containsExactly("설정", "프로필", "보안");
    }

    @Test
    @DisplayName("5초 테스트 주관식 조회는 객관식 전용 필드를 비워서 반환한다")
    void getsSubjectiveFiveSecondDetailWithoutObjectiveFields() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new FiveSecondCreateRequest(
                        "5초 주관식 질문",
                        "설명",
                        "five-second-image",
                        ImageRatio.RATIO_9_16,
                        false,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                )
        )));

        given(fileStorageService.generateDownloadUrl("five-second-image"))
                .willReturn("https://example.com/five-second-image");

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        FiveSecondDetailResponse fiveSecondResponse = (FiveSecondDetailResponse) response.questions().getFirst();
        assertThat(fiveSecondResponse.fiveSecondId()).isNotNull();
        assertThat(fiveSecondResponse.imageRatio()).isEqualTo(ImageRatio.RATIO_9_16);
        assertThat(fiveSecondResponse.isObjective()).isFalse();
        assertThat(fiveSecondResponse.isDuplicate()).isNull();
        assertThat(fiveSecondResponse.minSelect()).isNull();
        assertThat(fiveSecondResponse.maxSelect()).isNull();
        assertThat(fiveSecondResponse.isOther()).isNull();
        assertThat(fiveSecondResponse.options()).isEmpty();
        assertThat(fiveSecondResponse.imageUrl()).isEqualTo("https://example.com/five-second-image");
    }

    @Test
    @DisplayName("5초 테스트 객관식 조회는 선택지 구조와 선택 조건을 유지한다")
    void getsObjectiveFiveSecondDetailWithOptionStructure() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new FiveSecondCreateRequest(
                        "5초 객관식 질문",
                        "설명",
                        "five-second-image",
                        ImageRatio.RATIO_9_16,
                        true,
                        true,
                        1,
                        2,
                        true,
                        List.of(
                                new FiveSecondOptionRequest("검색창"),
                                new FiveSecondOptionRequest("메인 배너")
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        FiveSecondDetailResponse fiveSecondResponse = (FiveSecondDetailResponse) response.questions().getFirst();
        assertThat(fiveSecondResponse.fiveSecondId()).isNotNull();
        assertThat(fiveSecondResponse.imageRatio()).isEqualTo(ImageRatio.RATIO_9_16);
        assertThat(fiveSecondResponse.isObjective()).isTrue();
        assertThat(fiveSecondResponse.isDuplicate()).isTrue();
        assertThat(fiveSecondResponse.minSelect()).isEqualTo(1);
        assertThat(fiveSecondResponse.maxSelect()).isEqualTo(2);
        assertThat(fiveSecondResponse.isOther()).isTrue();
        assertThat(fiveSecondResponse.options()).extracting(option -> option.content(), option -> option.sequence())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("검색창", 1),
                        org.assertj.core.groups.Tuple.tuple("메인 배너", 2),
                        org.assertj.core.groups.Tuple.tuple("기타 (직접 입력)", 3)
                );
        assertThat(fiveSecondResponse.options()).anySatisfy(option -> {
            assertThat(option.content()).isEqualTo("기타 (직접 입력)");
            assertThat(option.isOtherOption()).isTrue();
        });
        assertThat(fiveSecondResponse.options()).allSatisfy(option -> assertThat(option.fiveSecondOptionId()).isNotNull());
    }

    @Test
    @DisplayName("트리 테스트 조회는 다중 루트 feature 구조를 유지한다")
    void getsTreeTestDetailWithMultipleRootFeatures() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new TreeTestCreateRequest(
                        "트리 질문",
                        "설명",
                        List.of(
                                new TreeTestCreateRequest.Feature(
                                        "마이페이지",
                                        List.of(new TreeTestCreateRequest.TreeNode("설정", List.of()))
                                ),
                                new TreeTestCreateRequest.Feature(
                                        "고객센터",
                                        List.of(new TreeTestCreateRequest.TreeNode("문의하기", List.of()))
                                )
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        TreeTestDetailResponse treeResponse = (TreeTestDetailResponse) response.questions().getFirst();
        assertThat(treeResponse.features()).hasSize(2);
        assertThat(treeResponse.features()).extracting(TreeTestNodeDetailResponse::label)
                .containsExactly("마이페이지", "고객센터");
        assertThat(treeResponse.features()).allSatisfy(node -> assertThat(node.treeTestId()).isNotNull());
        assertThat(treeResponse.features().get(0).children()).singleElement()
                .extracting(TreeTestNodeDetailResponse::label)
                .isEqualTo("설정");
        assertThat(treeResponse.features().get(1).children()).singleElement()
                .extracting(TreeTestNodeDetailResponse::label)
                .isEqualTo("문의하기");
    }

    @Test
    @DisplayName("트리 테스트 조회는 다단계 children 구조를 재귀적으로 복원한다")
    void getsTreeTestDetailWithNestedChildren() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new TreeTestCreateRequest(
                        "트리 질문",
                        "설명",
                        List.of(
                                new TreeTestCreateRequest.Feature(
                                        "마이페이지",
                                        List.of(
                                                new TreeTestCreateRequest.TreeNode(
                                                        "설정",
                                                        List.of(
                                                                new TreeTestCreateRequest.TreeNode(
                                                                        "알림 설정",
                                                                        List.of(new TreeTestCreateRequest.TreeNode("푸시 알림", List.of()))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        TreeTestDetailResponse treeResponse = (TreeTestDetailResponse) response.questions().getFirst();
        TreeTestNodeDetailResponse root = treeResponse.features().getFirst();
        TreeTestNodeDetailResponse child = root.children().getFirst();
        TreeTestNodeDetailResponse grandChild = child.children().getFirst();
        TreeTestNodeDetailResponse leaf = grandChild.children().getFirst();

        assertThat(root.treeTestId()).isNotNull();
        assertThat(root.label()).isEqualTo("마이페이지");
        assertThat(child.treeTestId()).isNotNull();
        assertThat(child.label()).isEqualTo("설정");
        assertThat(grandChild.treeTestId()).isNotNull();
        assertThat(grandChild.label()).isEqualTo("알림 설정");
        assertThat(leaf.treeTestId()).isNotNull();
        assertThat(leaf.label()).isEqualTo("푸시 알림");
        assertThat(leaf.children()).isEmpty();
    }

    @Test
    @DisplayName("문항 목록 조회는 각 타입별 상세 id 필드를 모두 반환한다")
    void getsAllDetailIdsForEachQuestionType() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new ObjectiveCreateRequest(
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(
                                new ObjectiveOptionRequest("A", null),
                                new ObjectiveOptionRequest("B", "image-b")
                        )
                ),
                new SubjectiveCreateRequest("주관식 질문", "설명", "subjective-image"),
                new FiveSecondCreateRequest(
                        "5초 질문",
                        "설명",
                        "five-second-image",
                        ImageRatio.RATIO_9_16,
                        true,
                        true,
                        1,
                        2,
                        true,
                        List.of(
                                new FiveSecondOptionRequest("검색창"),
                                new FiveSecondOptionRequest("메인 배너")
                        )
                ),
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5),
                new AbTestCreateRequest("AB 질문", "설명", "a-image", "b-image", ImageRatio.RATIO_9_16),
                new CardSortingCreateRequest("카드 질문", "설명", List.of("A", "B", "C", "D"), List.of("cat1", "cat2")),
                new TreeTestCreateRequest(
                        "트리 질문",
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
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        assertThat(response.questions()).hasSize(7);

        ObjectiveDetailResponse objectiveResponse = (ObjectiveDetailResponse) response.questions().get(0);
        assertThat(objectiveResponse.objectiveId()).isNotNull();
        assertThat(objectiveResponse.options()).hasSize(3);
        assertThat(objectiveResponse.options()).anySatisfy(option -> {
            assertThat(option.content()).isEqualTo("기타 (직접 입력)");
            assertThat(option.isOtherOption()).isTrue();
        });
        assertThat(objectiveResponse.options()).allSatisfy(option -> assertThat(option.objectiveOptionId()).isNotNull());

        SubjectiveDetailResponse subjectiveResponse = (SubjectiveDetailResponse) response.questions().get(1);
        assertThat(subjectiveResponse.subjectiveId()).isNotNull();

        FiveSecondDetailResponse fiveSecondResponse = (FiveSecondDetailResponse) response.questions().get(2);
        assertThat(fiveSecondResponse.fiveSecondId()).isNotNull();
        assertThat(fiveSecondResponse.options()).hasSize(3);
        assertThat(fiveSecondResponse.options()).anySatisfy(option -> {
            assertThat(option.content()).isEqualTo("기타 (직접 입력)");
            assertThat(option.isOtherOption()).isTrue();
        });
        assertThat(fiveSecondResponse.options()).allSatisfy(option -> assertThat(option.fiveSecondOptionId()).isNotNull());

        ScaleDetailResponse scaleResponse = (ScaleDetailResponse) response.questions().get(3);
        assertThat(scaleResponse.scaleId()).isNotNull();

        AbTestDetailResponse abTestResponse = (AbTestDetailResponse) response.questions().get(4);
        assertThat(abTestResponse.abTestId()).isNotNull();
        assertThat(abTestResponse.imageRatio()).isEqualTo(ImageRatio.RATIO_9_16);

        CardSortingDetailResponse cardSortingResponse = (CardSortingDetailResponse) response.questions().get(5);
        assertThat(cardSortingResponse.cardSortingId()).isNotNull();

        TreeTestDetailResponse treeResponse = (TreeTestDetailResponse) response.questions().get(6);
        assertThat(treeResponse.features()).isNotEmpty();
        assertThat(treeResponse.features()).allSatisfy(node -> assertThat(node.treeTestId()).isNotNull());
        assertThat(treeResponse.features().getFirst().children()).allSatisfy(node -> assertThat(node.treeTestId()).isNotNull());
    }

    @Test
    @DisplayName("문항 목록 조회는 루트 testId와 타입별 상세 sequence를 함께 반환한다")
    void getsQuestionDetailsWithRootTestIdAndNestedSequences() {
        server.MATE.domain.test.entity.Test savedTest = createTest();

        questionService.createQuestions(savedTest.getId(), 1L, new QuestionCreateRequest(List.of(
                new ScaleCreateRequest(
                        "척도 질문",
                        "설명",
                        null,
                        "낮음",
                        "높음",
                        5
                ),
                new ObjectiveCreateRequest(
                        "객관식 질문",
                        "설명",
                        true,
                        2,
                        1,
                        true,
                        List.of(
                                new ObjectiveOptionRequest("A", null),
                                new ObjectiveOptionRequest("B", "image-b")
                        )
                ),
                new TreeTestCreateRequest(
                        "트리 테스트",
                        "설명",
                        List.of(
                                new TreeTestCreateRequest.Feature(
                                        "마이페이지",
                                        List.of(
                                                new TreeTestCreateRequest.TreeNode(
                                                        "설정",
                                                        List.of(new TreeTestCreateRequest.TreeNode("알림 설정", List.of()))
                                                )
                                        )
                                )
                        )
                )
        )));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(savedTest.getId());

        assertThat(response.testId()).isEqualTo(savedTest.getId());
        assertThat(response.questions()).extracting(QuestionDetailItem::type)
                .containsExactly(QuestionType.SCALE, QuestionType.OBJECTIVE, QuestionType.TREE_TEST);
        assertThat(response.questions()).extracting(QuestionDetailItem::sequence)
                .containsExactly(1L, 2L, 3L);

        ObjectiveDetailResponse objectiveResponse = (ObjectiveDetailResponse) response.questions().get(1);
        assertThat(objectiveResponse.objectiveId()).isNotNull();
        assertThat(objectiveResponse.options()).extracting(option -> option.sequence())
                .containsExactly(1, 2, 3);
        assertThat(objectiveResponse.options()).anySatisfy(option -> {
            assertThat(option.content()).isEqualTo("기타 (직접 입력)");
            assertThat(option.isOtherOption()).isTrue();
        });
        assertThat(objectiveResponse.options()).allSatisfy(option -> assertThat(option.objectiveOptionId()).isNotNull());

        TreeTestDetailResponse treeResponse = (TreeTestDetailResponse) response.questions().get(2);
        assertThat(treeResponse.features()).allSatisfy(node -> assertThat(node.treeTestId()).isNotNull());
        assertThat(treeResponse.features().getFirst().label()).isEqualTo("마이페이지");
        assertThat(treeResponse.features().getFirst().children().getFirst().label()).isEqualTo("설정");
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
                        org.assertj.core.groups.Tuple.tuple("B", 2),
                        org.assertj.core.groups.Tuple.tuple("기타 (직접 입력)", 3)
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
        verify(fileStorageService, never()).deleteFiles(anyList());
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
        verify(fileStorageService).deleteFiles(List.of("image-a", "image-scale"));
    }

    private server.MATE.domain.test.entity.Test createTest() {
        return testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
<<<<<<< HEAD
<<<<<<< HEAD
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
=======
<<<<<<< HEAD
                .closedAt(TestEntityFixtures.DEFAULT_CLOSED_AT)
=======
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
>>>>>>> origin/dev
>>>>>>> origin/dev
=======
                .closedAt(TestEntityFixtures.DEFAULT_CLOSED_AT)
>>>>>>> origin/feat/ci
                .build());
    }
}
