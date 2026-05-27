package server.MATE.domain.answer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.dto.request.AnswerCreateRequest;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerBatchCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.answer.service.handler.AnswerCreateHandler;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.promotion.event.PromotionRewardRequestEvent;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.question.dto.response.AnswerQuestionTypeView;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    private static final Long TEST_ID = 10L;
    private static final Long TESTER_ID = 20L;
    private static final Long QUESTION_ID = 201L;
    private static final Long PARTICIPATION_ID = 301L;

    @Mock
    private TestRepository testRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ObjectiveRepository objectiveRepository;

    @Mock
    private FiveSecondRepository fiveSecondRepository;

    @Mock
    private ScaleRepository scaleRepository;

    @Mock
    private CardSortingRepository cardSortingRepository;

    @Mock
    private TreeTestRepository treeTestRepository;

    @Mock
    private PromotionRewardRepository promotionRewardRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AnswerCreateHandler subjectiveHandler;

    private AnswerService answerService;

    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        given(subjectiveHandler.supports()).willReturn(QuestionType.SUBJECTIVE);

        answerService = new AnswerService(
                testRepository,
                participationRepository,
                questionRepository,
                answerRepository,
                objectiveRepository,
                fiveSecondRepository,
                scaleRepository,
                cardSortingRepository,
                treeTestRepository,
                promotionRewardRepository,
                eventPublisher,
                List.of(subjectiveHandler)
        );

        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .goalPpl(2)
                .reward(300)
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
    }

    @Test
    @DisplayName("createAnswers는 질문 유형 projection을 조회해 검증 후 응답을 저장한다")
    void createAnswers_usesQuestionTypeProjectionAndPersistsAnswers() {
        AnswerCreateRequest request = new AnswerCreateRequest(List.of(
                new SubjectiveAnswerCreateRequest(QUESTION_ID, "응답 내용")
        ));

        Answer builtAnswer = Answer.builder()
                .participationId(PARTICIPATION_ID)
                .questionId(QUESTION_ID)
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", "응답 내용"))
                .build();

        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(participationRepository.existsByTestIdAndTesterIdAndDeletedAtIsNull(TEST_ID, TESTER_ID)).willReturn(false);
        given(questionRepository.findAnswerQuestionTypesInTest(TEST_ID)).willReturn(List.of(
                new AnswerQuestionTypeView(QUESTION_ID, QuestionType.SUBJECTIVE)
        ));
        given(participationRepository.save(any(Participation.class))).willAnswer(invocation -> {
            Participation participation = invocation.getArgument(0);
            ReflectionTestUtils.setField(participation, "id", PARTICIPATION_ID);
            return participation;
        });
        given(subjectiveHandler.build(anyLong(), any(), any())).willReturn(builtAnswer);
        given(answerRepository.saveAll(anyList())).willReturn(List.of(builtAnswer));

        AnswerBatchCreateResponse response = answerService.createAnswers(TEST_ID, TESTER_ID, request);

        assertThat(response.participationId()).isEqualTo(PARTICIPATION_ID);
        verify(questionRepository).findAnswerQuestionTypesInTest(TEST_ID);
        verify(subjectiveHandler).validate(any(), any());
        verify(subjectiveHandler).build(eq(PARTICIPATION_ID), eq(request.answers().getFirst()), any());
        verify(answerRepository).saveAll(List.of(builtAnswer));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PromotionRewardRequestEvent.class);
        verify(eventPublisher, never()).publishEvent(any(server.MATE.domain.test.event.TestCompleteEvent.class));
    }
}
