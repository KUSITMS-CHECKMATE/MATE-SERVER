package server.MATE.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.question.entity.*;
import server.MATE.domain.question.repository.*;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.*;
import server.MATE.domain.test.repository.TestCategoryRepository;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.storage.FileStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ObjectiveRepository objectiveRepository;
    @Autowired
    private FiveSecondRepository fiveSecondRepository;
    @Autowired
    private SubjectiveRepository subjectiveRepository;
    @Autowired
    private TreeTestRepository treeTestRepository;
    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PromotionRewardRepository promotionRewardRepository;
    @Autowired
    private TestLikeRepository testLikeRepository;
    @Autowired
    private TestCategoryRepository testCategoryRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
        answerRepository.deleteAll();
        promotionRewardRepository.deleteAll();
        participationRepository.deleteAll();
        treeTestRepository.deleteAll();
        fiveSecondRepository.deleteAll();
        objectiveRepository.deleteAll();
        subjectiveRepository.deleteAll();
        questionRepository.deleteAll();
        reportRepository.deleteAll();
        testLikeRepository.deleteAll();
        testCategoryRepository.deleteAll();
        paymentRepository.deleteAll();
        testRepository.deleteAll();
        usersRepository.deleteAll();
    }

    @Test
    @DisplayName("soft delete는 조회 대상만 논리 삭제하고 like, payment, promotion_reward는 유지한다")
    void softDeleteMarksActiveDataOnly() throws Exception {
        DeleteFixture fixture = createDeleteFixture();

        mockMvc.perform(delete("/api/v1/tests/{testId}", fixture.test.getId())
                        .header("Authorization", fixture.adminToken)
                        .queryParam("mode", "SOFT"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tests/{testId}", fixture.test.getId())
                        .header("Authorization", fixture.adminToken))
                .andExpect(status().isNotFound());

        server.MATE.domain.test.entity.Test savedTest = testRepository.findById(fixture.test.getId()).orElseThrow();
        Question savedObjectiveQuestion = questionRepository.findById(fixture.objectiveQuestion.getId()).orElseThrow();
        Question savedTreeQuestion = questionRepository.findById(fixture.treeQuestion.getId()).orElseThrow();
        Participation savedParticipation = participationRepository.findById(fixture.participation.getId()).orElseThrow();
        Answer savedAnswer = answerRepository.findById(fixture.answer.getId()).orElseThrow();
        Report savedReport = reportRepository.findById(fixture.report.getId()).orElseThrow();

        assertThat(savedTest.getDeletedAt()).isNotNull();
        assertThat(savedObjectiveQuestion.getDeletedAt()).isNotNull();
        assertThat(savedTreeQuestion.getDeletedAt()).isNotNull();
        assertThat(savedParticipation.getDeletedAt()).isNotNull();
        assertThat(savedAnswer.getDeletedAt()).isNotNull();
        assertThat(savedReport.getDeletedAt()).isNotNull();

        assertThat(testCategoryRepository.findAll()).hasSize(1);
        assertThat(testLikeRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(promotionRewardRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("soft deleted test도 hard delete로 purge 가능하고 연관 데이터를 모두 제거한다")
    void hardDeletePurgesSoftDeletedTest() throws Exception {
        DeleteFixture fixture = createDeleteFixture();

        mockMvc.perform(delete("/api/v1/tests/{testId}", fixture.test.getId())
                        .header("Authorization", fixture.adminToken)
                        .queryParam("mode", "SOFT"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/tests/{testId}", fixture.test.getId())
                        .header("Authorization", fixture.adminToken)
                        .header("X-MATE-Hard-Delete-Key", "test-hard-delete-key")
                        .queryParam("mode", "HARD"))
                .andExpect(status().isOk());

        assertThat(testRepository.findById(fixture.test.getId())).isEmpty();
        assertThat(questionRepository.findById(fixture.objectiveQuestion.getId())).isEmpty();
        assertThat(questionRepository.findById(fixture.fiveSecondQuestion.getId())).isEmpty();
        assertThat(questionRepository.findById(fixture.treeQuestion.getId())).isEmpty();
        assertThat(objectiveRepository.findById(fixture.objectiveQuestion.getId())).isEmpty();
        assertThat(fiveSecondRepository.findById(fixture.fiveSecondQuestion.getId())).isEmpty();
        assertThat(treeTestRepository.findAll()).isEmpty();
        assertThat(answerRepository.findAll()).isEmpty();
        assertThat(participationRepository.findAll()).isEmpty();
        assertThat(reportRepository.findAll()).isEmpty();
        assertThat(testLikeRepository.findAll()).isEmpty();
        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(promotionRewardRepository.findAll()).isEmpty();

        verify(fileStorageService).deleteFiles(argThat(keys ->
                keys.contains("test-image-key")
                        && keys.contains("objective-image-key")
                        && keys.contains("five-image-key")
        ));
    }

    private DeleteFixture createDeleteFixture() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Users admin = usersRepository.save(Users.builder()
                    .ci("admin-" + System.nanoTime())
                    .name("admin")
                    .role(Role.ADMIN)
                    .build());
            Users maker = usersRepository.save(Users.builder()
                    .ci("maker-" + System.nanoTime())
                    .name("maker")
                    .build());
            Users tester = usersRepository.save(Users.builder()
                    .ci("tester-" + System.nanoTime())
                    .name("tester")
                    .build());

            server.MATE.domain.test.entity.Test test = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                    .makerId(maker.getId())
                    .title("삭제 대상 테스트")
                    .description("설명")
                    .serviceName("서비스")
                    .serviceDescription("서비스 설명")
                    .imageKeys(List.of("test-image-key"))
                    .testStatus(TestStatus.IN_PROGRESS)
                    .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                    .build());
            test.addCategories(List.of(Category.FOOD));
            testRepository.save(test);

            Question objectiveQuestion = questionRepository.save(Question.builder()
                    .testId(test.getId())
                    .questionType(QuestionType.OBJECTIVE)
                    .title("객관식 질문")
                    .description("설명")
                    .sequence(1L)
                    .build());
            Objective objective = Objective.builder()
                    .question(questionRepository.getReferenceById(objectiveQuestion.getId()))
                    .isDuplicate(false)
                    .minSelect(1)
                    .maxSelect(1)
                    .isOther(false)
                    .build();
            objective.addOption(ObjectiveOption.builder()
                    .objective(objective)
                    .content("옵션 1")
                    .imageKey("objective-image-key")
                    .sequence(1)
                    .isOtherOption(false)
                    .build());
            entityManager.persist(objective);

            Question fiveSecondQuestion = questionRepository.save(Question.builder()
                    .testId(test.getId())
                    .questionType(QuestionType.FIVE_SECOND)
                    .title("5초 질문")
                    .description("설명")
                    .sequence(2L)
                    .build());
            FiveSecond fiveSecond = FiveSecond.builder()
                    .question(questionRepository.getReferenceById(fiveSecondQuestion.getId()))
                    .imageKey("five-image-key")
                    .imageRatio(ImageRatio.RATIO_9_16)
                    .isObjective(true)
                    .isDuplicate(false)
                    .minSelect(1)
                    .maxSelect(1)
                    .isOther(false)
                    .build();
            fiveSecond.addOption(FiveSecondOption.builder()
                    .fiveSecond(fiveSecond)
                    .content("옵션 A")
                    .sequence(1)
                    .isOtherOption(false)
                    .build());
            entityManager.persist(fiveSecond);

            Question treeQuestion = questionRepository.save(Question.builder()
                    .testId(test.getId())
                    .questionType(QuestionType.TREE_TEST)
                    .title("트리 질문")
                    .description("설명")
                    .sequence(3L)
                    .build());
            Question treeQuestionRef = questionRepository.getReferenceById(treeQuestion.getId());
            TreeTest root = TreeTest.create(treeQuestionRef, null, "root", 1);
            entityManager.persist(root);
            entityManager.persist(TreeTest.create(treeQuestionRef, root, "child", 1));
            entityManager.flush();

            Participation participation = participationRepository.save(Participation.builder()
                    .testId(test.getId())
                    .testerId(tester.getId())
                    .build());

            Answer answer = answerRepository.save(Answer.builder()
                    .participationId(participation.getId())
                    .questionId(objectiveQuestion.getId())
                    .questionType(QuestionType.OBJECTIVE)
                    .answer(Map.of("selectedOptionIds", List.of(1)))
                    .build());

            Report report = reportRepository.save(Report.builder()
                    .testId(test.getId())
                    .questionId(objectiveQuestion.getId())
                    .questionType(QuestionType.OBJECTIVE)
                    .result(Map.of("count", 1))
                    .build());

            paymentRepository.save(Payment.builder()
                    .draftId(999L)
                    .testId(test.getId())
                    .makerId(maker.getId())
                    .orderId("order-" + System.nanoTime())
                    .payStatus(PayStatus.PAY_SUCCEEDED)
                    .goalPpl(10)
                    .reward(300)
                    .amount(3000)
                    .build());

            promotionRewardRepository.save(PromotionReward.builder()
                    .participationId(participation.getId())
                    .testId(test.getId())
                    .testerId(tester.getId())
                    .rewardAmount(300)
                    .build());

            testLikeRepository.save(TestLike.builder()
                    .userId(tester.getId())
                    .testId(test.getId())
                    .build());

            String adminToken = "Bearer " + jwtProvider.generateToken(admin.getId(), admin.getRole().name(), TokenType.ACCESS);
            return new DeleteFixture(test, objectiveQuestion, fiveSecondQuestion, treeQuestion, participation, answer, report, adminToken);
        });
    }

    private record DeleteFixture(
            server.MATE.domain.test.entity.Test test,
            Question objectiveQuestion,
            Question fiveSecondQuestion,
            Question treeQuestion,
            Participation participation,
            Answer answer,
            Report report,
            String adminToken
    ) {
    }
}
