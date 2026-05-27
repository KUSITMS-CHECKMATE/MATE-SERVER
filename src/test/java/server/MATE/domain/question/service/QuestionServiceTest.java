package server.MATE.domain.question.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.dto.response.ObjectiveOptionDetailResponse;
import server.MATE.domain.question.dto.response.ObjectiveDetailResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.QuestionsDetailResponse;
import server.MATE.domain.question.dto.response.QuestionSummaryResponse;
import server.MATE.domain.question.dto.response.ScaleDetailResponse;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.service.fetcher.QuestionDetailFetcher;
import server.MATE.domain.question.service.handler.QuestionCreateHandler;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.event.FileCleanupEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private QuestionCreateHandler objectiveHandler;

    @Mock
    private QuestionCreateHandler scaleHandler;

    @Mock
    private QuestionCreateHandler treeTestHandler;

    @Mock
    private QuestionDetailFetcher objectiveFetcher;

    @Mock
    private QuestionDetailFetcher scaleFetcher;

    private QuestionService questionService;

    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;

    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        lenient().when(objectiveHandler.supports()).thenReturn(QuestionType.OBJECTIVE);
        lenient().when(scaleHandler.supports()).thenReturn(QuestionType.SCALE);
        lenient().when(treeTestHandler.supports()).thenReturn(QuestionType.TREE_TEST);
        lenient().when(objectiveFetcher.supports()).thenReturn(QuestionType.OBJECTIVE);
        lenient().when(scaleFetcher.supports()).thenReturn(QuestionType.SCALE);
        lenient().when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        questionService = new QuestionService(
                testRepository,
                questionRepository,
                eventPublisher,
                List.of(objectiveHandler, scaleHandler, treeTestHandler),
                List.of(objectiveFetcher, scaleFetcher)
        );
    }

    @Test
    @DisplayName("문항 목록 조회는 sequence 순서를 유지하면서 타입별 fetcher 결과를 조립한다")
    void getQuestionsAssemblesFetcherResultsInSequenceOrder() {
        Question objectiveQuestion = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식 질문")
                .description("설명")
                .sequence(2L)
                .build();
        Question scaleQuestion = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SCALE)
                .title("척도 질문")
                .description("설명")
                .sequence(1L)
                .build();
        setQuestionId(objectiveQuestion, 201L);
        setQuestionId(scaleQuestion, 202L);

        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID))
                .willReturn(List.of(scaleQuestion, objectiveQuestion));
        given(objectiveFetcher.fetch(List.of(objectiveQuestion))).willReturn(Map.of(
                201L,
                new ObjectiveDetailResponse(
                        201L,
                        201L,
                        QuestionType.OBJECTIVE,
                        2L,
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(new ObjectiveOptionDetailResponse(1001L, "A", null, 1, false))
                )
        ));
        given(scaleFetcher.fetch(List.of(scaleQuestion))).willReturn(Map.of(
                202L,
                new ScaleDetailResponse(
                        202L,
                        202L,
                        QuestionType.SCALE,
                        1L,
                        "척도 질문",
                        "설명",
                        null,
                        "낮음",
                        "높음",
                        5
                )
        ));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(TEST_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.questions()).extracting(QuestionDetailItem::questionId)
                .containsExactly(202L, 201L);
        assertThat(response.questions()).extracting(QuestionDetailItem::type)
                .containsExactly(QuestionType.SCALE, QuestionType.OBJECTIVE);
    }

    @Test
    @DisplayName("질문이 없는 테스트 조회는 빈 questions 배열을 반환한다")
    void returnsEmptyQuestionListWhenTestHasNoQuestions() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID))
                .willReturn(List.of());

        QuestionsDetailResponse response = questionService.getQuestionsDetails(TEST_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.questions()).isEmpty();
        verify(objectiveFetcher, never()).fetch(anyList());
        verify(scaleFetcher, never()).fetch(anyList());
    }

    @Test
    @DisplayName("문항 상세 조회는 타입별 fetcher 결과를 조립한다")
    void getQuestionDetailAssemblesFetcherResult() {
        Question objectiveQuestion = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식 질문")
                .description("설명")
                .sequence(2L)
                .build();
        setQuestionId(objectiveQuestion, 201L);

        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(201L, TEST_ID))
                .willReturn(Optional.of(objectiveQuestion));
        given(objectiveFetcher.fetch(List.of(objectiveQuestion))).willReturn(Map.of(
                201L,
                new ObjectiveDetailResponse(
                        201L,
                        201L,
                        QuestionType.OBJECTIVE,
                        2L,
                        "객관식 질문",
                        "설명",
                        false,
                        null,
                        null,
                        true,
                        List.of(new ObjectiveOptionDetailResponse(1001L, "A", null, 1, false))
                )
        ));

        QuestionDetailResponse response = questionService.getQuestionDetail(TEST_ID, 201L);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.question().questionId()).isEqualTo(201L);
        assertThat(response.question().type()).isEqualTo(QuestionType.OBJECTIVE);
    }

    @Test
    @DisplayName("질문 목록 조회는 질문 개수와 참여자 수를 함께 반환한다")
    void getQuestionSummaryReturnsCountsAndQuestionSummaries() {
        setTestStatus(test, TestStatus.COMPLETED);
        setTestPplCount(test, 12L);

        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(TEST_ID)).willReturn(List.of(
                new QuestionSummaryItem(202L, 1L, "척도 질문", QuestionType.SCALE),
                new QuestionSummaryItem(201L, 2L, "객관식 질문", QuestionType.OBJECTIVE)
        ));

        QuestionSummaryResponse response = questionService.getQuestionSummary(TEST_ID, MAKER_ID);

        assertThat(response.testStatus()).isEqualTo(TestStatus.COMPLETED);
        assertThat(response.questionCount()).isEqualTo(2);
        assertThat(response.participantCount()).isEqualTo(12L);
        assertThat(response.questions()).extracting(QuestionSummaryItem::questionId)
                .containsExactly(202L, 201L);
        assertThat(response.questions()).extracting(QuestionSummaryItem::type)
                .containsExactly(QuestionType.SCALE, QuestionType.OBJECTIVE);
    }

    @Test
    @DisplayName("질문 목록 조회는 테스트가 진행 중이어도 현재 testStatus와 함께 반환한다")
    void getQuestionSummaryReturnsWhenTestIsInProgress() {
        setTestStatus(test, TestStatus.IN_PROGRESS);
        setTestPplCount(test, 3L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(TEST_ID)).willReturn(List.of(
                new QuestionSummaryItem(301L, 1L, "진행 중 질문", QuestionType.SUBJECTIVE)
        ));

        QuestionSummaryResponse response = questionService.getQuestionSummary(TEST_ID, MAKER_ID);

        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(response.questionCount()).isEqualTo(1);
        assertThat(response.participantCount()).isEqualTo(3L);
        assertThat(response.questions()).extracting(QuestionSummaryItem::questionId)
                .containsExactly(301L);
    }

    @Test
    @DisplayName("질문 요약 조회 요청자가 제작자가 아니면 TEST_005 예외가 발생한다")
    void getQuestionSummaryThrowsTest005WhenMakerDoesNotMatch() {
        setTestStatus(test, TestStatus.COMPLETED);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> questionService.getQuestionSummary(TEST_ID, MAKER_ID + 1))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_005);

        verify(questionRepository, never()).findQuestionSummariesByTestId(TEST_ID);
    }

    @Test
    @DisplayName("조회 대상 테스트가 없으면 TEST_004 예외가 발생한다")
    void getQuestionsThrowsTest004WhenTestDoesNotExist() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionsDetails(TEST_ID))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);

        verify(questionRepository, never()).findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID);
        verify(objectiveFetcher, never()).fetch(anyList());
        verify(scaleFetcher, never()).fetch(anyList());
    }

    @Test
    @DisplayName("삭제된 테스트는 조회 시 TEST_004 예외가 발생한다")
    void getQuestionsThrowsTest004WhenTestIsDeleted() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionsDetails(TEST_ID))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);

        verify(questionRepository, never()).findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID);
        verify(objectiveFetcher, never()).fetch(anyList());
        verify(scaleFetcher, never()).fetch(anyList());
    }

    @Test
    @DisplayName("문항 상세 조회 대상 테스트가 없으면 TEST_004 예외가 발생한다")
    void getQuestionDetailThrowsTest004WhenTestDoesNotExist() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionDetail(TEST_ID, 201L))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);

        verify(questionRepository, never()).findByIdAndTestIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    @DisplayName("문항 상세 조회 대상 문항이 없으면 QUESTION_005 예외가 발생한다")
    void getQuestionDetailThrowsQuestion005WhenQuestionDoesNotExist() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(201L, TEST_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionDetail(TEST_ID, 201L))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_005);
    }

    @Test
    @DisplayName("문항 목록 조회는 제작자가 아니어도 조회할 수 있다")
    void getQuestionsDoesNotRequireMaker() {
        Question scaleQuestion = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SCALE)
                .title("척도 질문")
                .description("설명")
                .sequence(1L)
                .build();
        setQuestionId(scaleQuestion, 202L);

        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID))
                .willReturn(List.of(scaleQuestion));
        given(scaleFetcher.fetch(List.of(scaleQuestion))).willReturn(Map.of(
                202L,
                new ScaleDetailResponse(
                        202L,
                        202L,
                        QuestionType.SCALE,
                        1L,
                        "척도 질문",
                        "설명",
                        null,
                        "낮음",
                        "높음",
                        5
                )
        ));

        QuestionsDetailResponse response = questionService.getQuestionsDetails(TEST_ID);

        assertThat(response.questions()).hasSize(1);
        verify(questionRepository).findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(TEST_ID);
        verify(scaleFetcher).fetch(List.of(scaleQuestion));
    }

    @Test
    @DisplayName("여러 문항을 요청 순서대로 등록하고 이미지 정리 이벤트를 한 번 발행한다")
    void createQuestionsInRequestOrderAndPublishSingleCleanupEvent() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(5L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
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
        );
        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, scale));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a"));
        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of("image-scale"));

        QuestionCreateResponse response = questionService.createQuestions(TEST_ID, MAKER_ID, request);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(2)).save(questionCaptor.capture());
        List<Question> savedQuestions = questionCaptor.getAllValues();

        assertThat(savedQuestions).hasSize(2);
        assertThat(savedQuestions.get(0).getQuestionType()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(savedQuestions.get(0).getSequence()).isEqualTo(6L);
        assertThat(savedQuestions.get(1).getQuestionType()).isEqualTo(QuestionType.SCALE);
        assertThat(savedQuestions.get(1).getSequence()).isEqualTo(7L);

        verify(objectiveHandler).validate(objective);
        verify(scaleHandler).validate(scale);
        verify(objectiveHandler).createDetail(savedQuestions.get(0), objective);
        verify(scaleHandler).createDetail(savedQuestions.get(1), scale);

        ArgumentCaptor<FileCleanupEvent> eventCaptor = ArgumentCaptor.forClass(FileCleanupEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().fileKeys()).containsExactly("image-a", "image-scale");

        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).type()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(response.questions().get(0).sequence()).isEqualTo(6L);
        assertThat(response.questions().get(1).type()).isEqualTo(QuestionType.SCALE);
        assertThat(response.questions().get(1).sequence()).isEqualTo(7L);
    }

    @Test
    @DisplayName("혼합 요청은 OBJECTIVE, SCALE, TREE_TEST 순서대로 sequence가 부여된다")
    void assignsSequenceInMixedRequestOrder() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(3L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
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
        );
        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        TreeTestCreateRequest treeTest = new TreeTestCreateRequest(
                "트리 테스트",
                "설명",
                List.of()
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, scale, treeTest));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a"));
        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of("image-scale"));
        given(treeTestHandler.extractImageKeys(treeTest)).willReturn(List.of());

        QuestionCreateResponse response = questionService.createQuestions(TEST_ID, MAKER_ID, request);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(3)).save(questionCaptor.capture());
        List<Question> savedQuestions = questionCaptor.getAllValues();

        assertThat(savedQuestions).extracting(Question::getQuestionType)
                .containsExactly(QuestionType.OBJECTIVE, QuestionType.SCALE, QuestionType.TREE_TEST);
        assertThat(savedQuestions).extracting(Question::getSequence)
                .containsExactly(4L, 5L, 6L);

        verify(objectiveHandler).createDetail(savedQuestions.get(0), objective);
        verify(scaleHandler).createDetail(savedQuestions.get(1), scale);
        verify(treeTestHandler).createDetail(savedQuestions.get(2), treeTest);

        assertThat(response.questions()).extracting(result -> result.type().name())
                .containsExactly("OBJECTIVE", "SCALE", "TREE_TEST");
        assertThat(response.questions()).extracting(result -> result.sequence())
                .containsExactly(4L, 5L, 6L);
    }

    @Test
    @DisplayName("이미지가 없는 경우 정리 이벤트를 발행하지 않는다")
    void doesNotPublishCleanupEventWhenNoImagesExist() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                null,
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(scale));

        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of());

        questionService.createQuestions(TEST_ID, MAKER_ID, request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("테스트가 없으면 TEST_004 예외가 발생한다")
    void throwsTest004WhenTestDoesNotExist() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.empty());

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    @DisplayName("삭제된 테스트면 TEST_004 예외가 발생한다")
    void throwsTest004WhenTestIsDeleted() {
        test.delete(LocalDateTime.now());
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.empty());

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    @DisplayName("제작자가 아니면 TEST_005 예외가 발생한다")
    void throwsTest005WhenMakerDoesNotMatch() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID + 1, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_005);
    }

    @Test
    @DisplayName("핸들러 검증에서 실패하면 저장과 이벤트 발행이 중단된다")
    void stopsPersistenceAndEventPublishingWhenHandlerValidationFails() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                null,
                "낮음",
                "높음",
                9
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(scale));

        BaseException exception = new BaseException(BaseErrorCode.COMMON_002);
        org.mockito.Mockito.doThrow(exception).when(scaleHandler).validate(scale);

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isSameAs(exception);

        verify(scaleHandler).validate(scale);
        verify(questionRepository, never()).save(any(Question.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("중간 문항 처리에서 실패하면 이후 문항 처리와 이벤트 발행이 중단된다")
    void stopsProcessingRemainingItemsWhenIntermediateItemFails() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
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
        );
        TreeTestCreateRequest treeTest = new TreeTestCreateRequest(
                "트리 테스트",
                "설명",
                List.of()
        );
        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, treeTest, scale));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a"));

        BaseException exception = new BaseException(BaseErrorCode.QUESTION_006);
        org.mockito.Mockito.doThrow(exception).when(treeTestHandler).validate(treeTest);

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isSameAs(exception);

        verify(objectiveHandler).validate(objective);
        verify(treeTestHandler).validate(treeTest);
        verify(questionRepository, never()).save(any(Question.class));
        verify(objectiveHandler, never()).createDetail(any(), eq(objective));
        verify(treeTestHandler, never()).createDetail(any(), any());
        verify(scaleHandler, never()).validate(any());
        verify(scaleHandler, never()).createDetail(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("이미지가 포함된 문항이 모두 성공하면 cleanup 이벤트를 한 번만 발행한다")
    void publishesCleanupEventOnlyWhenRequestSucceeds() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(10L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
                "객관식 질문",
                "설명",
                false,
                null,
                null,
                true,
                List.of(
                        new ObjectiveOptionRequest("A", "image-a"),
                        new ObjectiveOptionRequest("B", "image-b")
                )
        );
        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, scale));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a", "image-b"));
        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of("image-scale"));

        questionService.createQuestions(TEST_ID, MAKER_ID, request);

        ArgumentCaptor<FileCleanupEvent> eventCaptor = ArgumentCaptor.forClass(FileCleanupEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().fileKeys())
                .containsExactly("image-a", "image-b", "image-scale");
    }

    @Test
    @DisplayName("중간 실패가 발생하면 cleanup 이벤트를 발행하지 않는다")
    void doesNotPublishCleanupEventWhenIntermediateItemFails() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
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
        );
        TreeTestCreateRequest treeTest = new TreeTestCreateRequest(
                "트리 테스트",
                "설명",
                List.of()
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, treeTest));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a"));
        BaseException exception = new BaseException(BaseErrorCode.QUESTION_006);
        org.mockito.Mockito.doThrow(exception).when(treeTestHandler).validate(treeTest);

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isSameAs(exception);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private void setQuestionId(Question question, Long id) {
        try {
            java.lang.reflect.Field field = Question.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(question, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTestStatus(server.MATE.domain.test.entity.Test test, TestStatus status) {
        try {
            java.lang.reflect.Field field = server.MATE.domain.test.entity.Test.class.getDeclaredField("testStatus");
            field.setAccessible(true);
            field.set(test, status);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTestPplCount(server.MATE.domain.test.entity.Test test, Long pplCount) {
        try {
            java.lang.reflect.Field field = server.MATE.domain.test.entity.Test.class.getDeclaredField("pplCount");
            field.setAccessible(true);
            field.set(test, pplCount);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
