package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.payment.entity.PaymentRefund;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.mock.MockPaymentGateway;
import server.MATE.domain.payment.repository.PaymentRefundRepository;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.repository.AbTestRepository;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.SubjectiveRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.testdraft.entity.TestDraftStatus;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.storage.FileStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DraftPaymentPublishEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TestDraftRepository testDraftRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRefundRepository paymentRefundRepository;

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

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TestLikeRepository testLikeRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoSpyBean
    private QuestionService questionService;

    @MockitoSpyBean
    private MockPaymentGateway mockPaymentGateway;

    @AfterEach
    void tearDown() {
        answerRepository.deleteAll();
        participationRepository.deleteAll();
        testLikeRepository.deleteAll();
        treeTestRepository.deleteAll();
        cardSortingRepository.deleteAll();
        abTestRepository.deleteAll();
        fiveSecondRepository.deleteAll();
        subjectiveRepository.deleteAll();
        objectiveRepository.deleteAll();
        scaleRepository.deleteAll();
        questionRepository.deleteAll();
        paymentRefundRepository.deleteAll();
        paymentRepository.deleteAll();
        testDraftRepository.deleteAll();
        testRepository.deleteAll();
        usersRepository.deleteAll();
    }

    @Test
    @DisplayName("draft 수정 후 mock 결제 실행을 통해 실제 test와 question이 게시된다")
    void publishesDraftThroughMockPaymentFlow() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());

        Long paymentId = createPayment(draftId, makerToken);
        JsonNode executeData = executePayment(paymentId, makerToken);
        Long testId = executeData.path("testId").asLong();

        assertThat(testId).isPositive();

        var savedDraft = testDraftRepository.findById(draftId).orElseThrow();
        assertThat(savedDraft.getStatus()).isEqualTo(TestDraftStatus.PUBLISHED);
        assertThat(savedDraft.getPublishedTestId()).isEqualTo(testId);

        var savedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(savedPayment.getPayStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
        assertThat(savedPayment.getTestId()).isEqualTo(testId);
        assertThat(savedPayment.getPaidAmount()).isEqualTo(2750);

        var savedTest = testRepository.findById(testId).orElseThrow();
        assertThat(savedTest.getTitle()).isEqualTo("신규 테스트");
        assertThat(savedTest.getGoalPpl()).isEqualTo(5);
        assertThat(savedTest.getReward()).isEqualTo(300);
        assertThat(savedTest.getTestStatus()).isEqualTo(TestStatus.WAITING);
        assertThat(questionRepository.countByTestIdAndDeletedAtIsNull(testId)).isEqualTo(1);

        JsonNode questions = getQuestions(testId, makerToken);
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).path("type").asText()).isEqualTo("SUBJECTIVE");
        assertThat(questions.get(0).path("title").asText()).isEqualTo("어떤 점이 불편했나요?");
    }

    @Test
    @DisplayName("이미 게시가 끝난 결제의 execute 재호출은 기존 testId를 반환한다")
    void executeIsIdempotentAfterPublish() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());
        Long paymentId = createPayment(draftId, makerToken);

        JsonNode firstExecute = executePayment(paymentId, makerToken);
        long firstTestId = firstExecute.path("testId").asLong();

        JsonNode secondExecute = executePayment(paymentId, makerToken);
        long secondTestId = secondExecute.path("testId").asLong();

        assertThat(secondTestId).isEqualTo(firstTestId);
        assertThat(testRepository.count()).isEqualTo(1);
        assertThat(questionRepository.countByTestIdAndDeletedAtIsNull(firstTestId)).isEqualTo(1);
    }

    @Test
    @DisplayName("게시 중 예외가 나면 결제 성공 상태를 보존하고 execute 재호출로 publish를 재시도한다")
    void retriesPublishAfterPublishFailure() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());
        Long paymentId = createPayment(draftId, makerToken);

        doThrow(new RuntimeException("publish failed"))
                .doCallRealMethod()
                .when(questionService)
                .createQuestions(anyLong(), eq(maker.getId()), any(QuestionCreateRequest.class));

        JsonNode firstError = executePaymentExpectingError(paymentId, makerToken, 500, "COMMON_999");
        assertThat(firstError.path("message").asText()).isEqualTo("서버 내부 오류가 발생했습니다.");

        var failedDraft = testDraftRepository.findById(draftId).orElseThrow();
        var succeededPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(failedDraft.getStatus()).isEqualTo(TestDraftStatus.PUBLISH_FAILED);
        assertThat(failedDraft.getPublishedTestId()).isNull();
        assertThat(succeededPayment.getPayStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
        assertThat(succeededPayment.getTestId()).isNull();
        assertThat(testRepository.count()).isZero();
        assertThat(questionRepository.count()).isZero();

        JsonNode secondExecute = executePayment(paymentId, makerToken);
        long publishedTestId = secondExecute.path("testId").asLong();

        var recoveredDraft = testDraftRepository.findById(draftId).orElseThrow();
        var linkedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(publishedTestId).isPositive();
        assertThat(recoveredDraft.getStatus()).isEqualTo(TestDraftStatus.PUBLISHED);
        assertThat(recoveredDraft.getPublishedTestId()).isEqualTo(publishedTestId);
        assertThat(linkedPayment.getTestId()).isEqualTo(publishedTestId);
        assertThat(testRepository.count()).isEqualTo(1);
        assertThat(questionRepository.countByTestIdAndDeletedAtIsNull(publishedTestId)).isEqualTo(1);
    }

    @Test
    @DisplayName("goalPpl과 reward가 없는 draft는 결제 생성이 차단된다")
    void blocksPaymentCreationForIncompleteDraft() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);

        JsonNode error = createPaymentExpectingError(draftId, makerToken, 400, "PAYMENT_005");

        assertThat(error.path("message").asText()).isEqualTo("결제 금액 계산에 필요한 값이 누락되었습니다.");
        assertThat(paymentRepository.findByDraftId(draftId)).isEmpty();
        assertThat(testDraftRepository.findById(draftId).orElseThrow().getStatus()).isEqualTo(TestDraftStatus.DRAFT);
    }

    @Test
    @DisplayName("게시 필수 정보가 없는 draft는 결제 성공 후에도 publish가 차단되고 draft는 PUBLISH_FAILED로 남는다")
    void marksDraftAsPublishFailedWhenPublishValidationFails() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, """
                {
                  "goalPpl": 5,
                  "reward": 300
                }
                """);

        Long paymentId = createPayment(draftId, makerToken);
        JsonNode error = executePaymentExpectingError(paymentId, makerToken, 400, "DRAFT_004");

        assertThat(error.path("message").asText()).isEqualTo("게시할 수 없는 테스트 초안입니다.");
        var draft = testDraftRepository.findById(draftId).orElseThrow();
        var payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(draft.getStatus()).isEqualTo(TestDraftStatus.PUBLISH_FAILED);
        assertThat(draft.getPublishedTestId()).isNull();
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
        assertThat(payment.getTestId()).isNull();
        assertThat(testRepository.count()).isZero();
        assertThat(questionRepository.count()).isZero();
    }

    @Test
    @DisplayName("결제 생성은 PAY_CREATED를 재사용하고 PAY_SUCCEEDED 이후에는 차단되며 PAY_FAILED는 같은 row로 재시도한다")
    void enforcesPaymentCreateReuseRetryPolicy() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());

        JsonNode firstCreate = createPaymentData(draftId, makerToken);
        JsonNode reusedCreate = createPaymentData(draftId, makerToken);
        assertThat(reusedCreate.path("paymentId").asLong()).isEqualTo(firstCreate.path("paymentId").asLong());
        assertThat(reusedCreate.path("orderNo").asText()).isEqualTo(firstCreate.path("orderNo").asText());
        assertThat(reusedCreate.path("payToken").asText()).isEqualTo(firstCreate.path("payToken").asText());

        Long paymentId = firstCreate.path("paymentId").asLong();
        executePayment(paymentId, makerToken);

        JsonNode publishedError = createPaymentExpectingError(draftId, makerToken, 400, "DRAFT_003");
        assertThat(publishedError.path("message").asText()).isEqualTo("결제를 생성할 수 없는 테스트 초안입니다.");

        Users retryMaker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("retry-maker")
                .build());
        String retryMakerToken = bearerToken(retryMaker);

        Long retryDraftId = createDraft(retryMakerToken);
        updateDraft(retryDraftId, retryMakerToken, draftPayload());

        doThrow(new RuntimeException("create failed"))
                .doCallRealMethod()
                .when(mockPaymentGateway)
                .createPayment(any());

        JsonNode failedCreate = createPaymentExpectingError(retryDraftId, retryMakerToken, 500, "COMMON_999");
        assertThat(failedCreate.path("message").asText()).isEqualTo("서버 내부 오류가 발생했습니다.");

        var failedPayment = paymentRepository.findByDraftId(retryDraftId).orElseThrow();
        String failedOrderNo = failedPayment.getOrderNo();
        assertThat(failedPayment.getPayStatus()).isEqualTo(PayStatus.PAY_FAILED);
        assertThat(testDraftRepository.findById(retryDraftId).orElseThrow().getStatus()).isEqualTo(TestDraftStatus.PAYMENT_FAILED);

        JsonNode executeBeforeCreateError = executePaymentExpectingError(failedPayment.getId(), retryMakerToken, 400, "PAYMENT_003");
        assertThat(executeBeforeCreateError.path("message").asText()).isEqualTo("결제를 실행할 수 없는 상태입니다.");

        JsonNode retriedCreate = createPaymentData(retryDraftId, retryMakerToken);
        var retriedPayment = paymentRepository.findByDraftId(retryDraftId).orElseThrow();
        assertThat(retriedCreate.path("paymentId").asLong()).isEqualTo(failedPayment.getId());
        assertThat(retriedPayment.getId()).isEqualTo(failedPayment.getId());
        assertThat(retriedPayment.getPayStatus()).isEqualTo(PayStatus.PAY_CREATED);
        assertThat(retriedPayment.getOrderNo()).isNotEqualTo(failedOrderNo);
        assertThat(retriedPayment.getPayToken()).isEqualTo(retriedCreate.path("payToken").asText());
        assertThat(testDraftRepository.findById(retryDraftId).orElseThrow().getStatus()).isEqualTo(TestDraftStatus.PAYMENT_CREATED);
    }

    @Test
    @DisplayName("publish된 테스트는 목록 상세 질문 조회와 응답 제출까지 정상 동작한다")
    void publishedTestIsVisibleAndAnswerable() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        Users tester = usersRepository.save(Users.builder()
                .ci("tester-" + System.nanoTime())
                .name("tester")
                .build());
        String makerToken = bearerToken(maker);
        String testerToken = bearerToken(tester);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());
        Long paymentId = createPayment(draftId, makerToken);
        long testId = executePayment(paymentId, makerToken).path("testId").asLong();
        var publishedTest = testRepository.findById(testId).orElseThrow();
        publishedTest.start();
        testRepository.saveAndFlush(publishedTest);

        JsonNode listBody = parseBody(mockMvc.perform(get("/api/v1/tests")
                        .header("Authorization", testerToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(listBody.path("data").isArray()).isTrue();
        boolean existsInList = false;
        for (JsonNode node : listBody.path("data")) {
            if (node.path("id").asLong() == testId) {
                assertThat(node.path("title").asText()).isEqualTo("신규 테스트");
                existsInList = true;
                break;
            }
        }
        assertThat(existsInList).isTrue();

        JsonNode detailBody = parseBody(mockMvc.perform(get("/api/v1/tests/{testId}", testId)
                        .header("Authorization", testerToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detailBody.path("data").path("id").asLong()).isEqualTo(testId);
        assertThat(detailBody.path("data").path("title").asText()).isEqualTo("신규 테스트");
        assertThat(detailBody.path("data").path("reward").asInt()).isEqualTo(300);

        JsonNode questions = getQuestions(testId, testerToken);
        long questionId = questions.get(0).path("questionId").asLong();
        JsonNode answerBody = parseBody(mockMvc.perform(post("/api/v1/tests/{testId}/answers", testId)
                        .header("Authorization", testerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "type": "SUBJECTIVE",
                                      "questionId": %d,
                                      "text": "가입 흐름이 헷갈렸어요."
                                    }
                                  ]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(answerBody.path("message").asText()).isEqualTo("응답이 등록되었습니다.");
        var answeredTest = testRepository.findById(testId).orElseThrow();
        assertThat(answeredTest.getPplCount()).isEqualTo(1L);
        assertThat(participationRepository.count()).isEqualTo(1L);
        assertThat(answerRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("환불은 REFUNDED 상태와 refund 이력을 남기고 재환불을 차단한다")
    void refundsPaymentAndBlocksDuplicateRefund() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        String makerToken = bearerToken(maker);

        Long draftId = createDraft(makerToken);
        updateDraft(draftId, makerToken, draftPayload());
        Long paymentId = createPayment(draftId, makerToken);
        executePayment(paymentId, makerToken);

        JsonNode refundData = refundPayment(paymentId, makerToken, "테스트 환불");
        var payment = paymentRepository.findById(paymentId).orElseThrow();
        var refunds = paymentRefundRepository.findAllByPaymentIdOrderByApprovalTimeDesc(paymentId);

        assertThat(refundData.path("payStatus").asText()).isEqualTo("REFUNDED");
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.REFUNDED);
        assertThat(refunds).hasSize(1);
        PaymentRefund refund = refunds.getFirst();
        assertThat(refund.getReason()).isEqualTo("테스트 환불");
        assertThat(refund.getPaymentId()).isEqualTo(paymentId);

        JsonNode duplicateRefundError = refundPaymentExpectingError(paymentId, makerToken, "중복 환불", 400, "PAYMENT_004");
        assertThat(duplicateRefundError.path("message").asText()).isEqualTo("환불할 수 없는 상태입니다.");
    }

    private String bearerToken(Users user) {
        return "Bearer " + jwtProvider.generateToken(user.getId(), user.getRole().name(), TokenType.ACCESS);
    }

    private Long createDraft(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/test-drafts")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn();
        return parseBody(result).path("data").path("draftId").asLong();
    }

    private void updateDraft(Long draftId, String token, String payload) throws Exception {
        mockMvc.perform(patch("/api/v1/test-drafts/{draftId}", draftId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private Long createPayment(Long draftId, String token) throws Exception {
        return createPaymentData(draftId, token).path("paymentId").asLong();
    }

    private JsonNode createPaymentData(Long draftId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %d,
                                  "isTestPayment": true
                                }
                                """.formatted(draftId)))
                .andExpect(status().isCreated())
                .andReturn();
        return parseBody(result).path("data");
    }

    private JsonNode executePayment(Long paymentId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments/{paymentId}/execute", paymentId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result).path("data");
    }

    private JsonNode createPaymentExpectingError(Long draftId, String token, int statusCode, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %d,
                                  "isTestPayment": true
                                }
                                """.formatted(draftId)))
                .andExpect(status().is(statusCode))
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(code);
        return body;
    }

    private JsonNode executePaymentExpectingError(Long paymentId, String token, int statusCode, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments/{paymentId}/execute", paymentId)
                        .header("Authorization", token))
                .andExpect(status().is(statusCode))
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(code);
        return body;
    }

    private JsonNode refundPayment(Long paymentId, String token, String reason) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments/{paymentId}/refund", paymentId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "%s"
                                }
                                """.formatted(reason)))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result).path("data");
    }

    private JsonNode refundPaymentExpectingError(Long paymentId, String token, String reason, int statusCode, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mock/payments/{paymentId}/refund", paymentId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "%s"
                                }
                                """.formatted(reason)))
                .andExpect(status().is(statusCode))
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(code);
        return body;
    }

    private JsonNode getQuestions(Long testId, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tests/{testId}/questions", testId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result).path("data").path("questions");
    }

    private JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String draftPayload() {
        return """
                {
                  "title": "신규 테스트",
                  "description": "테스트 소개",
                  "serviceName": "서비스명",
                  "serviceDescription": "서비스 설명",
                  "imageKeys": [],
                  "categories": ["FOOD"],
                  "goalPpl": 5,
                  "reward": 300,
                  "questionsPayload": {
                    "questions": [
                      {
                        "type": "SUBJECTIVE",
                        "title": "어떤 점이 불편했나요?",
                        "description": "자유롭게 작성해주세요.",
                        "imageKey": null
                      }
                    ]
                  }
                }
                """;
    }
}
