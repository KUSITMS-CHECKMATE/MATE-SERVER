package server.MATE.domain.test.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;
import server.MATE.domain.question.repository.*;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.dto.request.TestDeleteMode;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.repository.TestCategoryRepository;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.security.config.AdminProperties;
import server.MATE.global.storage.event.FileDeleteEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDeleteServiceTest {

    @Mock
    private TestRepository testRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PromotionRewardRepository promotionRewardRepository;
    @Mock
    private TestCategoryRepository testCategoryRepository;
    @Mock
    private TestLikeRepository testLikeRepository;
    @Mock
    private ObjectiveRepository objectiveRepository;
    @Mock
    private ObjectiveOptionRepository objectiveOptionRepository;
    @Mock
    private SubjectiveRepository subjectiveRepository;
    @Mock
    private AbTestRepository abTestRepository;
    @Mock
    private ScaleRepository scaleRepository;
    @Mock
    private CardSortingRepository cardSortingRepository;
    @Mock
    private FiveSecondRepository fiveSecondRepository;
    @Mock
    private FiveSecondOptionRepository fiveSecondOptionRepository;
    @Mock
    private TreeTestRepository treeTestRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private AdminProperties adminProperties;
    @Mock
    private Clock clock;

    @InjectMocks
    private TestDeleteService testDeleteService;

    private server.MATE.domain.test.entity.Test test;
    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("기존 제목")
                .description("기존 소개")
                .serviceName("기존 서비스")
                .serviceDescription("기존 서비스 소개")
                .imageKeys(new ArrayList<>(List.of("old-key-1", "old-key-2")))
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        test.addCategories(List.of(Category.FOOD));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-05-30T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(adminProperties.hardDeleteKey()).thenReturn("hard-delete-key");
    }

    @Test
    void 관리자가_soft_delete를_수행하면_연관_엔티티를_논리삭제한다() {
        Question question = createQuestion(101L, QuestionType.SUBJECTIVE);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionsInTest(TEST_ID)).willReturn(List.of(question));

        testDeleteService.deleteTest(TEST_ID, Role.ADMIN, TestDeleteMode.SOFT, null);

        verify(reportRepository).softDeleteAllByTestId(eq(TEST_ID), any(LocalDateTime.class));
        verify(answerRepository).softDeleteAllByQuestionIds(eq(List.of(101L)), any(LocalDateTime.class));
        verify(participationRepository).softDeleteByTestId(eq(TEST_ID), any(LocalDateTime.class));
        verify(questionRepository).softDeleteByTestId(eq(TEST_ID), any(LocalDateTime.class));
        verify(testCategoryRepository, never()).softDeleteAllByTestId(anyLong(), any(LocalDateTime.class));
        verify(testRepository).softDeleteById(eq(TEST_ID), any(LocalDateTime.class));
    }

    @Test
    void 관리자가_hard_delete를_수행하면_연관_데이터_삭제와_파일_삭제_이벤트를_발행한다() {
        Question subjectiveQuestion = createQuestion(101L, QuestionType.SUBJECTIVE);
        given(testRepository.findByIdIncludingDeleted(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionsInTestIncludingDeleted(TEST_ID)).willReturn(List.of(subjectiveQuestion));

        Subjective subjective = Subjective.builder()
                .question(subjectiveQuestion)
                .imageKey("question-image-key")
                .build();
        given(subjectiveRepository.findAllByQuestion_IdIn(List.of(101L))).willReturn(List.of(subjective));

        testDeleteService.deleteTest(TEST_ID, Role.ADMIN, TestDeleteMode.HARD, "hard-delete-key");

        verify(promotionRewardRepository).deleteAllByTestId(TEST_ID);
        verify(answerRepository).deleteAllByQuestionIds(List.of(101L));
        verify(reportRepository).deleteAllByTestId(TEST_ID);
        verify(testLikeRepository).deleteAllByTestId(TEST_ID);
        verify(subjectiveRepository).deleteAllInBatch(anyList());
        verify(questionRepository).deleteAllInBatch(anyList());
        verify(testCategoryRepository).deleteAllByTestId(TEST_ID);
        verify(participationRepository).deleteByTestId(TEST_ID);
        verify(paymentRepository).deleteAllByTestId(TEST_ID);
        verify(testRepository).delete(test);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(FileDeleteEvent.class);
        FileDeleteEvent fileDeleteEvent = (FileDeleteEvent) eventCaptor.getValue();
        assertThat(fileDeleteEvent.fileKeys())
                .contains("old-key-1", "old-key-2", "question-image-key");
    }

    @Test
    void hard_delete_키가_없으면_예외가_발생한다() {
        given(testRepository.findByIdIncludingDeleted(TEST_ID)).willReturn(Optional.of(test));

        BaseException exception = Assertions.assertThrows(BaseException.class,
                () -> testDeleteService.deleteTest(TEST_ID, Role.ADMIN, TestDeleteMode.HARD, null));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_009);
    }

    @Test
    void 관리자가_아니면_삭제할_수_없다() {
        BaseException exception = Assertions.assertThrows(BaseException.class,
                () -> testDeleteService.deleteTest(TEST_ID, Role.USER, TestDeleteMode.SOFT, null));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.COMMON_009);
        verifyNoInteractions(testRepository);
    }

    private Question createQuestion(Long id, QuestionType questionType) {
        Question question = Question.builder()
                .testId(TEST_ID)
                .questionType(questionType)
                .title("질문")
                .description("설명")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
