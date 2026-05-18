package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.SubjectiveRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.storage.FileStorageService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestionAnswerEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
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

    @Test
    @DisplayName("OBJECTIVE 질문 생성 후 기타 option id로 응답 등록하고 JSON shape를 저장한다")
    void createsObjectiveQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "자주 쓰는 기능은?",
                      "description": "하나를 선택해주세요.",
                      "isDuplicate": false,
                      "maxSelect": null,
                      "minSelect": null,
                      "isOther": true,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null }
                      ]
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("objectiveId").isNumber()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));
        Long otherOptionId = extractOtherOptionId(questionNode, "options", "objectiveOptionId");

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [%d],
                      "otherText": "직접 입력"
                    }
                  ]
                }
                """.formatted(questionId, otherOptionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(savedAnswer.getQuestionId()).isEqualTo(questionId);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "optionIds", "otherText");
        assertThat(toLongList(savedAnswer.getAnswer().get("optionIds"))).containsExactly(otherOptionId);
        assertThat(savedAnswer.getAnswer()).containsEntry("otherText", "직접 입력");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("SUBJECTIVE 질문 생성 후 응답 등록하고 JSON shape를 저장한다")
    void createsSubjectiveQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "SUBJECTIVE",
                      "title": "불편한 점은?",
                      "description": "자유롭게 작성해주세요.",
                      "imageKey": null
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("subjectiveId").isNumber()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "주관식 응답"
                    }
                  ]
                }
                """.formatted(questionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.SUBJECTIVE);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "text");
        assertThat(savedAnswer.getAnswer()).containsEntry("text", "주관식 응답");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("FIVE_SECOND 주관식 질문 생성 후 응답 등록하고 JSON shape를 저장한다")
    void createsFiveSecondSubjectiveQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "FIVE_SECOND",
                      "title": "가장 먼저 보인 것은?",
                      "description": "자유롭게 작성해주세요.",
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

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("fiveSecondId").isNumber()).isTrue();
        assertThat(questionNode.path("isObjective").asBoolean()).isFalse();
        assertThat(questionNode.path("options").isArray()).isTrue();
        assertThat(questionNode.path("options").size()).isZero();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "FIVE_SECOND",
                      "questionId": %d,
                      "text": "5초 주관식 응답"
                    }
                  ]
                }
                """.formatted(questionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.FIVE_SECOND);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "text");
        assertThat(savedAnswer.getAnswer()).containsEntry("text", "5초 주관식 응답");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("FIVE_SECOND 객관식 질문 생성 후 기타 option id로 응답 등록하고 JSON shape를 저장한다")
    void createsFiveSecondObjectiveQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "FIVE_SECOND",
                      "title": "눈에 띈 요소는?",
                      "description": "객관식으로 선택해주세요.",
                      "imageKey": "five-second-image",
                      "imageRatio": "9:16",
                      "isObjective": true,
                      "isDuplicate": false,
                      "minSelect": null,
                      "maxSelect": null,
                      "isOther": true,
                      "options": [
                        { "content": "검색창" },
                        { "content": "메인 배너" }
                      ]
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("fiveSecondId").isNumber()).isTrue();
        assertThat(questionNode.path("isObjective").asBoolean()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));
        Long otherOptionId = extractOtherOptionId(questionNode, "options", "fiveSecondOptionId");

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "FIVE_SECOND",
                      "questionId": %d,
                      "optionIds": [%d],
                      "otherText": "기타 응답"
                    }
                  ]
                }
                """.formatted(questionId, otherOptionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.FIVE_SECOND);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "optionIds", "otherText");
        assertThat(toLongList(savedAnswer.getAnswer().get("optionIds"))).containsExactly(otherOptionId);
        assertThat(savedAnswer.getAnswer()).containsEntry("otherText", "기타 응답");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("SCALE 질문 생성 후 응답 등록하고 JSON shape를 저장한다")
    void createsScaleQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "SCALE",
                      "title": "만족도는?",
                      "description": "5점 척도로 선택해주세요.",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": 5
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("scaleId").isNumber()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SCALE",
                      "questionId": %d,
                      "value": 4
                    }
                  ]
                }
                """.formatted(questionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.SCALE);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "value");
        assertThat(((Number) savedAnswer.getAnswer().get("value")).intValue()).isEqualTo(4);

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("AB_TEST 질문 생성 후 응답 등록하고 JSON shape를 저장한다")
    void createsAbTestQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "AB_TEST",
                      "title": "어느 시안이 더 좋은가요?",
                      "description": "A와 B 중 하나를 골라주세요.",
                      "aImageKey": "a-image",
                      "bImageKey": "b-image",
                      "imageRatio": "9:16"
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("abTestId").isNumber()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "AB_TEST",
                      "questionId": %d,
                      "selected": "A"
                    }
                  ]
                }
                """.formatted(questionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.AB_TEST);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "selected");
        assertThat(savedAnswer.getAnswer()).containsEntry("selected", "A");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("CARD_SORTING 질문 생성 후 응답 등록하고 JSON shape를 저장한다")
    void createsCardSortingQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "CARD_SORTING",
                      "title": "카드를 그룹으로 묶어주세요.",
                      "description": "비슷한 항목끼리 분류해주세요.",
                      "cards": ["홈", "검색", "장바구니", "공지사항"],
                      "categories": ["쇼핑", "정보"]
                    }
                  ]
                }
                """);

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        assertThat(questionNode.path("cardSortingId").isNumber()).isTrue();
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "CARD_SORTING",
                      "questionId": %d,
                      "groups": [
                        { "category": "쇼핑", "cardNames": ["홈", "장바구니"] },
                        { "category": "정보", "cardNames": ["검색", "공지사항"] }
                      ]
                    }
                  ]
                }
                """.formatted(questionId));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.CARD_SORTING);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "groups");
        JsonNode groupsNode = objectMapper.valueToTree(savedAnswer.getAnswer().get("groups"));
        assertThat(groupsNode.isArray()).isTrue();
        assertThat(groupsNode).hasSize(2);
        assertThat(groupsNode.get(0).path("category").asText()).isEqualTo("쇼핑");
        assertThat(groupsNode.get(1).path("category").asText()).isEqualTo("정보");
        assertThat(toTextSet(groupsNode.get(0).path("cardNames"))).containsExactlyInAnyOrder("홈", "장바구니");
        assertThat(toTextSet(groupsNode.get(1).path("cardNames"))).containsExactlyInAnyOrder("검색", "공지사항");
        assertThat(toTextSet(groupsNode.get(0).path("cardNames"), groupsNode.get(1).path("cardNames")))
                .containsExactlyInAnyOrder("홈", "검색", "장바구니", "공지사항");

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    @Test
    @DisplayName("TREE_TEST 질문 생성 후 leaf node path로 응답 등록하고 JSON shape를 저장한다")
    void createsTreeTestQuestionThenSubmitsAnswerAndPersistsJson() throws Exception {
        TestActors actors = createActors();

        JsonNode createResponse = createQuestion(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "TREE_TEST",
                      "title": "알림 설정은 어디에 있나요?",
                      "description": "예상 경로를 따라 선택해주세요.",
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

        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        assertThat(questionId).isEqualTo(extractCreatedQuestionId(createResponse));
        TreeSelection leaf = extractLeafSelection(questionNode.path("features"));
        assertThat(leaf.path().getLast()).isEqualTo(leaf.nodeId());

        JsonNode answerResponse = submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "TREE_TEST",
                      "questionId": %d,
                      "nodeId": %d,
                      "path": %s
                    }
                  ]
                }
                """.formatted(questionId, leaf.nodeId(), objectMapper.writeValueAsString(leaf.path())));

        Answer savedAnswer = loadSingleAnswer();
        assertThat(savedAnswer.getQuestionType()).isEqualTo(QuestionType.TREE_TEST);
        assertNoUnexpectedKeys(savedAnswer.getAnswer(), "nodeId", "path");
        assertThat(((Number) savedAnswer.getAnswer().get("nodeId")).longValue()).isEqualTo(leaf.nodeId());
        List<Long> savedPath = toLongList(savedAnswer.getAnswer().get("path"));
        assertThat(savedPath).containsExactlyElementsOf(leaf.path());
        assertThat(savedPath.getLast()).isEqualTo(leaf.nodeId());

        assertParticipationAndPplCount(actors.testId(), actors.testerId(), answerResponse.path("data").path("participationId").asLong());
    }

    private TestActors createActors() {
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
                .build());
        return new TestActors(
                maker.getId(),
                tester.getId(),
                test.getId(),
                bearerToken(maker),
                bearerToken(tester)
        );
    }

    private String bearerToken(Users user) {
        return "Bearer " + jwtProvider.generateToken(user.getId(), user.getRole().name(), TokenType.ACCESS);
    }

    private JsonNode createQuestion(Long testId, String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tests/{testId}/questions", testId)
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("code").asText()).isEqualTo("201");
        assertThat(body.path("message").asText()).isEqualTo("문항이 등록되었습니다.");
        assertThat(body.path("data").path("questions")).hasSize(1);

        JsonNode createdQuestion = body.path("data").path("questions").get(0);
        assertThat(createdQuestion.path("questionId").isNumber()).isTrue();
        assertThat(createdQuestion.path("sequence").asLong()).isEqualTo(1L);

        return body;
    }

    private JsonNode getSingleQuestion(Long testId, String token) throws Exception {
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

    private JsonNode submitAnswer(Long testId, String token, String payload) throws Exception {
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

    private JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Long extractCreatedQuestionId(JsonNode createResponse) {
        return createResponse.path("data").path("questions").get(0).path("questionId").asLong();
    }

    private Long extractOtherOptionId(JsonNode questionNode, String optionsField, String idField) {
        JsonNode options = questionNode.path(optionsField);
        assertThat(options.isArray()).isTrue();
        for (JsonNode option : options) {
            if (option.path("isOtherOption").asBoolean(false)) {
                assertThat(option.path(idField).isNumber()).isTrue();
                return option.path(idField).asLong();
            }
        }
        throw new IllegalStateException("other option not found");
    }

    private TreeSelection extractLeafSelection(JsonNode features) {
        assertThat(features.isArray()).isTrue();
        assertThat(features.isEmpty()).isFalse();

        for (JsonNode feature : features) {
            TreeSelection selection = findLeaf(feature, new ArrayList<>());
            if (selection != null) {
                return selection;
            }
        }
        throw new IllegalStateException("leaf node not found");
    }

    private TreeSelection findLeaf(JsonNode node, List<Long> path) {
        assertThat(node.path("treeTestId").isNumber()).isTrue();
        List<Long> nextPath = new ArrayList<>(path);
        nextPath.add(node.path("treeTestId").asLong());

        JsonNode children = node.path("children");
        if (!children.isArray() || children.isEmpty()) {
            return new TreeSelection(node.path("treeTestId").asLong(), nextPath);
        }

        for (JsonNode child : children) {
            TreeSelection selection = findLeaf(child, nextPath);
            if (selection != null) {
                return selection;
            }
        }
        return null;
    }

    private Answer loadSingleAnswer() {
        entityManager.clear();
        List<Answer> answers = answerRepository.findAll();
        assertThat(answers).hasSize(1);
        return answers.getFirst();
    }

    private void assertParticipationAndPplCount(Long testId, Long testerId, Long responseParticipationId) {
        entityManager.clear();

        List<Participation> participations = participationRepository.findAll();
        assertThat(participations).hasSize(1);

        Participation participation = participations.getFirst();
        assertThat(participation.getId()).isEqualTo(responseParticipationId);
        assertThat(participation.getTestId()).isEqualTo(testId);
        assertThat(participation.getTesterId()).isEqualTo(testerId);

        server.MATE.domain.test.entity.Test reloadedTest = testRepository.findById(testId).orElseThrow();
        assertThat(reloadedTest.getPplCount()).isEqualTo(1L);
    }

    private void assertNoUnexpectedKeys(Map<String, Object> answer, String... expectedKeys) {
        assertThat(answer).containsOnlyKeys(expectedKeys);
    }

    private List<Long> toLongList(Object rawValue) {
        assertThat(rawValue).isInstanceOf(Collection.class);
        List<Long> values = new ArrayList<>();
        for (Object value : (Collection<?>) rawValue) {
            assertThat(value).isInstanceOf(Number.class);
            values.add(((Number) value).longValue());
        }
        return values;
    }

    private Set<String> toTextSet(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private Set<String> toTextSet(JsonNode first, JsonNode second) {
        Set<String> values = new LinkedHashSet<>(toTextSet(first));
        values.addAll(toTextSet(second));
        return values;
    }

    private record TestActors(
            Long makerId,
            Long testerId,
            Long testId,
            String makerToken,
            String testerToken
    ) {
    }

    private record TreeSelection(Long nodeId, List<Long> path) {
    }
}
