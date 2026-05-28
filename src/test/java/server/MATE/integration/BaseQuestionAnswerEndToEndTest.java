package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
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
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.storage.FileStorageService;
import server.MATE.support.TestEntityFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class BaseQuestionAnswerEndToEndTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired
    protected QuestionService questionService;

    @Autowired
    protected UsersRepository usersRepository;

    @Autowired
    protected TestRepository testRepository;

    @Autowired
    protected QuestionRepository questionRepository;

    @Autowired
    protected ObjectiveRepository objectiveRepository;

    @Autowired
    protected ScaleRepository scaleRepository;

    @Autowired
    protected TreeTestRepository treeTestRepository;

    @Autowired
    protected SubjectiveRepository subjectiveRepository;

    @Autowired
    protected FiveSecondRepository fiveSecondRepository;

    @Autowired
    protected AbTestRepository abTestRepository;

    @Autowired
    protected CardSortingRepository cardSortingRepository;

    @Autowired
    protected ParticipationRepository participationRepository;

    @Autowired
    protected AnswerRepository answerRepository;

    @Autowired
    protected PromotionRewardRepository promotionRewardRepository;

    @Autowired
    protected TossAccountRepository tossAccountRepository;

    @PersistenceContext
    protected EntityManager entityManager;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDownBase() {
        promotionRewardRepository.deleteAll();
        tossAccountRepository.deleteAll();
        answerRepository.deleteAll();
        participationRepository.deleteAll();
        treeTestRepository.deleteAll();
        cardSortingRepository.deleteAll();
        abTestRepository.deleteAll();
        fiveSecondRepository.deleteAll();
        subjectiveRepository.deleteAll();
        objectiveRepository.deleteAll();
        scaleRepository.deleteAll();
        questionRepository.deleteAll();
        testRepository.deleteAll();
        usersRepository.deleteAll();
    }

    protected TestActors createActors() {
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
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(java.time.LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build());
        return new TestActors(
                maker.getId(),
                tester.getId(),
                test.getId(),
                bearerToken(maker),
                bearerToken(tester)
        );
    }

    protected String bearerToken(Users user) {
        return "Bearer " + jwtProvider.generateToken(user.getId(), user.getRole().name(), TokenType.ACCESS);
    }

    protected TossAccount linkTossAccount(Users user, Long tossUserKey) {
        return tossAccountRepository.save(TossAccount.builder()
                .user(user)
                .tossUserKey(tossUserKey)
                .isLinked(true)
                .lastLoginAt(java.time.LocalDateTime.now())
                .lastTokenRefreshedAt(java.time.LocalDateTime.now())
                .build());
    }

    protected JsonNode seedQuestion(Long testId, String token, String payload) throws Exception {
        Long makerId = extractUserId(token);
        QuestionCreateRequest request = objectMapper.readValue(payload, QuestionCreateRequest.class);
        QuestionCreateResponse response = questionService.createQuestions(testId, makerId, request);
        JsonNode body = objectMapper.valueToTree(java.util.Map.of("data", java.util.Map.of("questions", response.questions())));
        JsonNode createdQuestion = body.path("data").path("questions").get(0);
        assertThat(createdQuestion.path("questionId").isNumber()).isTrue();
        assertThat(createdQuestion.path("sequence").asLong()).isEqualTo(1L);

        return body;
    }

    protected void seedQuestions(Long testId, String token, String payload) throws Exception {
        seedQuestion(testId, token, payload);
    }

    protected JsonNode getSingleQuestion(Long testId, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tests/{testId}/questions", testId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("code").asText()).isEqualTo("200");
        assertThat(body.path("message").asText()).isEqualTo("문항을 조회했습니다.");
        assertThat(body.path("data").path("testId").asLong()).isEqualTo(testId);
        assertThat(body.path("data").path("questions")).hasSize(1);
        return body.path("data").path("questions").get(0);
    }

    protected JsonNode getQuestionsArray(Long testId, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tests/{testId}/questions", testId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = parseBody(result);
        return body.path("data").path("questions");
    }

    protected JsonNode submitAnswer(Long testId, String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tests/{testId}/answers", testId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("code").asText()).isEqualTo("201");
        assertThat(body.path("message").asText()).isEqualTo("응답이 등록되었습니다.");
        assertThat(body.path("data").path("participationId").isNumber()).isTrue();
        return body;
    }

    protected JsonNode submitAnswerExpectSuccess(Long testId, String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tests/{testId}/answers", testId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isTrue();
        return body;
    }

    protected JsonNode submitAnswerExpectingError(Long testId, String token, String payload, int statusCode, String code) throws Exception {
        return performExpectingError(post("/api/v1/tests/{testId}/answers", testId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(payload),
                statusCode,
                code);
    }

    protected JsonNode performExpectingError(MockHttpServletRequestBuilder builder, int statusCode, String code) throws Exception {
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().is(statusCode))
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(code);
        return body;
    }

    protected JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected Long extractUserId(String bearerToken) {
        return jwtProvider.extractUserId(bearerToken.replace("Bearer ", ""));
    }

    protected Long extractCreatedQuestionId(JsonNode createResponse) {
        return createResponse.path("data").path("questions").get(0).path("questionId").asLong();
    }

    protected void createSingleSubjectiveQuestion(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, singleSubjectiveQuestionPayload());
    }

    protected void createTwoSubjectiveQuestions(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "SUBJECTIVE",
                      "title": "질문 1",
                      "description": "설명 1",
                      "imageKey": null
                    },
                    {
                      "type": "SUBJECTIVE",
                      "title": "질문 2",
                      "description": "설명 2",
                      "imageKey": null
                    }
                  ]
                }
                """);
    }

    protected void createObjectiveQuestion(Long testId, String makerToken, boolean isDuplicate, Integer minSelect, Integer maxSelect, boolean isOther) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": %s,
                      "maxSelect": %s,
                      "minSelect": %s,
                      "isOther": %s,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null }
                      ]
                    }
                  ]
                }
                """.formatted(isDuplicate, jsonValue(maxSelect), jsonValue(minSelect), isOther));
    }

    protected void createFiveSecondSubjectiveQuestion(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "FIVE_SECOND",
                      "title": "5초 주관식",
                      "description": "설명",
                      "imageKey": "five-second-image",
                      "imageRatio": "9:16",
                      "isObjective": false,
                      "isDuplicate": null,
                      "minSelect": null,
                      "maxSelect": null,
                      "isOther": null,
                      "options": []
                    }
                  ]
                }
                """);
    }

    protected void createFiveSecondObjectiveQuestion(Long testId, String makerToken, boolean isDuplicate, Integer minSelect, Integer maxSelect, boolean isOther) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "FIVE_SECOND",
                      "title": "5초 객관식",
                      "description": "설명",
                      "imageKey": "five-second-image",
                      "imageRatio": "9:16",
                      "isObjective": true,
                      "isDuplicate": %s,
                      "minSelect": %s,
                      "maxSelect": %s,
                      "isOther": %s,
                      "options": [
                        { "content": "검색창" },
                        { "content": "배너" }
                      ]
                    }
                  ]
                }
                """.formatted(isDuplicate, jsonValue(minSelect), jsonValue(maxSelect), isOther));
    }

    protected void createScaleQuestion(Long testId, String makerToken, int range) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "SCALE",
                      "title": "척도 질문",
                      "description": "설명",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": %d
                    }
                  ]
                }
                """.formatted(range));
    }

    protected void createAbTestQuestion(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "AB_TEST",
                      "title": "AB 질문",
                      "description": "설명",
                      "aImageKey": "a-image",
                      "bImageKey": "b-image",
                      "imageRatio": "9:16"
                    }
                  ]
                }
                """);
    }

    protected void createCardSortingQuestion(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "CARD_SORTING",
                      "title": "카드 소팅",
                      "description": "설명",
                      "cards": ["홈", "검색", "장바구니", "공지사항"],
                      "categories": ["쇼핑", "정보"]
                    }
                  ]
                }
                """);
    }

    protected void createTreeTestQuestion(Long testId, String makerToken) throws Exception {
        seedQuestions(testId, makerToken, """
                {
                  "questions": [
                    {
                      "type": "TREE_TEST",
                      "title": "트리 질문",
                      "description": "설명",
                      "features": [
                        {
                          "label": "마이페이지",
                          "children": [
                            {
                              "label": "설정",
                              "children": [
                                {
                                  "label": "알림 설정",
                                  "children": []
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);
    }

    protected String singleSubjectiveQuestionPayload() {
        return """
                {
                  "questions": [
                    {
                      "type": "SUBJECTIVE",
                      "title": "질문",
                      "description": "설명",
                      "imageKey": null
                    }
                  ]
                }
                """;
    }

    protected String jsonValue(Integer value) {
        return value == null ? "null" : value.toString();
    }

    protected record TestActors(
            Long makerId,
            Long testerId,
            Long testId,
            String makerToken,
            String testerToken
    ) {
    }
}
