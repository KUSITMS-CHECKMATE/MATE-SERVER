package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.report.service.ReportAggregationService;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.support.time.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ReportEndToEndIntegrationTest.TestClockConfig.class)
class ReportEndToEndIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    private static final Instant DEFAULT_TEST_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private ReportAggregationService reportAggregationService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private MutableClock mutableClock;

    @Test
    @DisplayName("IN_PROGRESS 테스트의 리포트는 reports가 빈 리스트다")
    void getReport_whenInProgress_returnsEmptyResults() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());

        JsonNode data = reportData(actors.testId(), actors.makerToken());

        assertThat(data.path("testStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(data.path("questionCount").asInt()).isEqualTo(1);
        assertThat(data.path("reports").isArray()).isTrue();
        assertThat(data.path("reports")).isEmpty();
    }

    @Test
    @DisplayName("메이커가 아닌 사용자가 리포트 조회 시 403을 반환한다")
    void getReport_byNonMaker_returns403() throws Exception {
        TestActors actors = createActors();
        completeTest(actors.testId());

        performExpectingError(
                get("/api/v1/tests/{testId}/report", actors.testId())
                        .header("Authorization", actors.testerToken()),
                403, "TEST_005"
        );
    }

    @Test
    @DisplayName("존재하지 않는 테스트 리포트 조회 시 404를 반환한다")
    void getReport_withNonExistentTest_returns404() throws Exception {
        TestActors actors = createActors();

        performExpectingError(
                get("/api/v1/tests/{testId}/report", 999999L)
                        .header("Authorization", actors.makerToken()),
                404, "TEST_004"
        );
    }

    @Test
    @DisplayName("SUBJECTIVE 응답 제출 후 리포트에 텍스트 목록이 반환된다")
    void getReport_withSubjectiveAnswers_returnsTexts() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "주관식 응답" }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0);
        assertThat(result.path("type").asText()).isEqualTo("SUBJECTIVE");
        JsonNode texts = result.path("result").path("texts");
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0).asText()).isEqualTo("주관식 응답");
    }

    @Test
    @DisplayName("OBJECTIVE 응답 제출 후 리포트에 선택지별 카운트가 반환된다")
    void getReport_withObjectiveAnswers_returnsOptionCounts() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, false);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long firstOptionId = questionNode.path("options").get(0).path("objectiveOptionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "OBJECTIVE", "questionId": %d, "optionIds": [%d] }
                  ]
                }
                """.formatted(questionId, firstOptionId));
        completeTest(actors.testId());

        JsonNode options = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("options");
        assertThat(options.isArray()).isTrue();
        int totalCount = 0;
        for (JsonNode option : options) {
            totalCount += option.path("count").asInt();
        }
        assertThat(totalCount).isEqualTo(1);
        assertThat(options.get(0).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("SCALE 응답 제출 후 리포트에 평균과 점수별 분포가 반환된다")
    void getReport_withScaleAnswers_returnsAverageAndDistribution() throws Exception {
        TestActors actors = createActors();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SCALE", "questionId": %d, "value": 4 }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        assertThat(result.path("average").asDouble()).isEqualTo(4.0);
        JsonNode distribution = result.path("distribution");
        assertThat(distribution).hasSize(5);
        assertThat(distribution.get(3).path("score").asInt()).isEqualTo(4);
        assertThat(distribution.get(3).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("FIVE_SECOND 주관식 응답 제출 후 리포트에 텍스트 목록이 반환된다")
    void getReport_withFiveSecondSubjectiveAnswers_returnsTexts() throws Exception {
        TestActors actors = createActors();
        createFiveSecondSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "FIVE_SECOND", "questionId": %d, "text": "5초 주관 응답" }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0);
        assertThat(result.path("type").asText()).isEqualTo("FIVE_SECOND");
        JsonNode texts = result.path("result").path("texts");
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0).asText()).isEqualTo("5초 주관 응답");
    }

    @Test
    @DisplayName("FIVE_SECOND 객관식 응답 제출 후 리포트에 선택지별 카운트가 반환된다")
    void getReport_withFiveSecondObjectiveAnswers_returnsOptionCounts() throws Exception {
        TestActors actors = createActors();
        createFiveSecondObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, false);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long firstOptionId = questionNode.path("options").get(0).path("fiveSecondOptionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "FIVE_SECOND", "questionId": %d, "optionIds": [%d] }
                  ]
                }
                """.formatted(questionId, firstOptionId));
        completeTest(actors.testId());

        JsonNode options = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("options");
        assertThat(options.isArray()).isTrue();
        int totalCount = 0;
        for (JsonNode option : options) {
            totalCount += option.path("count").asInt();
        }
        assertThat(totalCount).isEqualTo(1);
        assertThat(options.get(0).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("AB_TEST 응답 제출 후 리포트에 A/B 카운트와 비율이 반환된다")
    void getReport_withAbTestAnswers_returnsABCountsAndRatios() throws Exception {
        TestActors actors = createActors();
        createAbTestQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "AB_TEST", "questionId": %d, "selected": "A" }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        assertThat(result.path("A").path("count").asInt()).isEqualTo(1);
        assertThat(result.path("A").path("ratio").asDouble()).isEqualTo(1.0);
        assertThat(result.path("B").path("count").asInt()).isEqualTo(0);
        assertThat(result.path("B").path("ratio").asDouble()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CARD_SORTING 응답 제출 후 리포트에 byCategory와 byCard가 반환된다")
    void getReport_withCardSortingAnswers_returnsGroupResults() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
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
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result");

        JsonNode byCategory = result.path("byCategory");
        assertThat(byCategory).hasSize(2);
        assertThat(byCategory).anySatisfy(g ->
                assertThat(g.path("category").asText()).isEqualTo("쇼핑"));
        assertThat(byCategory).anySatisfy(g ->
                assertThat(g.path("category").asText()).isEqualTo("정보"));

        JsonNode byCard = result.path("byCard");
        assertThat(byCard).hasSize(4);
    }

    @Test
    @DisplayName("TREE_TEST 응답 제출 후 리포트에 nodeFrequency와 pathFrequency가 반환된다")
    void getReport_withTreeTestAnswers_returnsNodeFrequencyAndPathFrequency() throws Exception {
        TestActors actors = createActors();
        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "TREE_TEST",
                      "title": "트리 질문",
                      "description": "설명",
                      "features": [
                        { "label": "홈", "children": [] }
                      ]
                    }
                  ]
                }
                """);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long rootNodeId = questionNode.path("features").get(0).path("treeTestId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "TREE_TEST", "questionId": %d, "nodeId": %d, "path": [%d] }
                  ]
                }
                """.formatted(questionId, rootNodeId, rootNodeId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result");

        JsonNode nodeFrequency = result.path("nodeFrequency");
        assertThat(nodeFrequency.isArray()).isTrue();
        assertThat(nodeFrequency).anySatisfy(node -> assertThat(node.path("count").asInt()).isEqualTo(1));

        JsonNode pathFrequency = result.path("pathFrequency");
        assertThat(pathFrequency.isArray()).isTrue();
        assertThat(pathFrequency).hasSize(1);
        assertThat(pathFrequency.get(0).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("COMPLETED 테스트에 질문이 없으면 questions와 reports가 모두 빈 리스트다")
    void getReport_whenCompletedWithNoQuestions_returnsEmptyLists() throws Exception {
        TestActors actors = createActors();
        completeTest(actors.testId());

        JsonNode data = reportData(actors.testId(), actors.makerToken());

        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("questionCount").asInt()).isEqualTo(0);
        assertThat(data.path("questions")).isEmpty();
        assertThat(data.path("reports")).isEmpty();
    }

    @Test
    @DisplayName("응답이 없는 SCALE 질문의 리포트는 average 0.0, distribution 전체 count 0이다")
    void getReport_withNoScaleAnswers_returnsZeroAverageAndDistribution() throws Exception {
        TestActors actors = createActors();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        assertThat(result.path("average").asDouble()).isEqualTo(0.0);
        JsonNode distribution = result.path("distribution");
        assertThat(distribution).hasSize(5);
        for (JsonNode item : distribution) {
            assertThat(item.path("count").asInt()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("응답이 없는 OBJECTIVE 질문의 리포트는 모든 선택지 count가 0이다")
    void getReport_withNoObjectiveAnswers_returnsZeroCounts() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, false);
        completeTest(actors.testId());

        JsonNode options = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("options");
        assertThat(options.isArray()).isTrue();
        for (JsonNode option : options) {
            assertThat(option.path("count").asInt()).isEqualTo(0);
            assertThat(option.path("ratio").asDouble()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("응답이 없는 SUBJECTIVE 질문의 리포트는 texts가 빈 리스트다")
    void getReport_withNoSubjectiveAnswers_returnsEmptyTexts() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode texts = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("texts");
        assertThat(texts.isArray()).isTrue();
        assertThat(texts).isEmpty();
    }

    @Test
    @DisplayName("응답이 없는 FIVE_SECOND 주관식 질문의 리포트는 texts가 빈 리스트다")
    void getReport_withNoFiveSecondSubjectiveAnswers_returnsEmptyTexts() throws Exception {
        TestActors actors = createActors();
        createFiveSecondSubjectiveQuestion(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode texts = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("texts");
        assertThat(texts.isArray()).isTrue();
        assertThat(texts).isEmpty();
    }

    @Test
    @DisplayName("응답이 없는 FIVE_SECOND 객관식 질문의 리포트는 모든 선택지 count와 ratio가 0이다")
    void getReport_withNoFiveSecondObjectiveAnswers_returnsZeroCounts() throws Exception {
        TestActors actors = createActors();
        createFiveSecondObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, false);
        completeTest(actors.testId());

        JsonNode options = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("options");
        assertThat(options.isArray()).isTrue();
        for (JsonNode option : options) {
            assertThat(option.path("count").asInt()).isEqualTo(0);
            assertThat(option.path("ratio").asDouble()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("응답이 없는 AB_TEST 질문의 리포트는 A/B count와 ratio가 모두 0이다")
    void getReport_withNoAbTestAnswers_returnsZeroCountsAndRatios() throws Exception {
        TestActors actors = createActors();
        createAbTestQuestion(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result");
        assertThat(result.path("A").path("count").asInt()).isEqualTo(0);
        assertThat(result.path("A").path("ratio").asDouble()).isEqualTo(0.0);
        assertThat(result.path("B").path("count").asInt()).isEqualTo(0);
        assertThat(result.path("B").path("ratio").asDouble()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("응답이 없는 CARD_SORTING 질문의 리포트는 초기 구조를 유지하고 모든 count와 ratio가 0이다")
    void getReport_withNoCardSortingAnswers_returnsInitialStructureWithZeroCounts() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result");

        JsonNode byCategory = result.path("byCategory");
        assertThat(byCategory).hasSize(2);
        for (JsonNode category : byCategory) {
            JsonNode cards = category.path("cards");
            assertThat(cards).hasSize(4);
            for (JsonNode card : cards) {
                assertThat(card.path("count").asInt()).isEqualTo(0);
                assertThat(card.path("ratio").asDouble()).isEqualTo(0.0);
            }
        }

        JsonNode byCard = result.path("byCard");
        assertThat(byCard).hasSize(4);
        for (JsonNode card : byCard) {
            JsonNode categories = card.path("categories");
            assertThat(categories.path("쇼핑").asInt()).isEqualTo(0);
            assertThat(categories.path("정보").asInt()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("응답이 없는 TREE_TEST 질문의 리포트는 pathFrequency가 빈 리스트고 leaf node count와 ratio가 0이다")
    void getReport_withNoTreeTestAnswers_returnsEmptyPathFrequencyAndZeroLeafCounts() throws Exception {
        TestActors actors = createActors();
        createTreeTestQuestion(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result");

        JsonNode nodeFrequency = result.path("nodeFrequency");
        assertThat(nodeFrequency.isArray()).isTrue();
        assertThat(nodeFrequency).hasSize(1);
        assertThat(nodeFrequency.get(0).path("count").asInt()).isEqualTo(0);
        assertThat(nodeFrequency.get(0).path("ratio").asDouble()).isEqualTo(0.0);

        JsonNode pathFrequency = result.path("pathFrequency");
        assertThat(pathFrequency.isArray()).isTrue();
        assertThat(pathFrequency).isEmpty();
    }

    @Test
    @DisplayName("리포트의 questions 필드는 sequence 오름차순으로 질문 목록을 반환한다")
    void getReport_questions_returnedInSequenceOrder() throws Exception {
        TestActors actors = createActors();
        createTwoSubjectiveQuestions(actors.testId(), actors.makerToken());
        completeTest(actors.testId());

        JsonNode questions = reportData(actors.testId(), actors.makerToken()).path("questions");
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).path("sequence").asLong()).isEqualTo(1L);
        assertThat(questions.get(1).path("sequence").asLong()).isEqualTo(2L);
    }

    @Test
    @DisplayName("응답 제출 후 리포트의 participantCount가 증가한다")
    void getReport_afterAnswerSubmission_participantCountIncremented() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "응답" }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode data = reportData(actors.testId(), actors.makerToken());
        assertThat(data.path("participantCount").asLong()).isEqualTo(1L);
    }

    @Test
    @DisplayName("여러 타입 질문이 혼재할 때 reports가 sequence 순서로 조립되고 각 항목의 필드가 올바르다")
    void getReport_withMultipleQuestionTypes_reportsAssembledInSequenceOrder() throws Exception {
        TestActors actors = createActors();
        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    { "type": "SUBJECTIVE", "title": "주관식 질문", "description": "설명", "imageKey": null },
                    { "type": "SCALE", "title": "척도 질문", "description": "설명", "imageKey": null, "minLabel": "낮음", "maxLabel": "높음", "range": 5 },
                    { "type": "AB_TEST", "title": "AB 질문", "description": "설명", "aImageKey": "a", "bImageKey": "b", "imageRatio": "9:16" }
                  ]
                }
                """);
        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());
        Long subjectiveId = questions.get(0).path("questionId").asLong();
        Long scaleId = questions.get(1).path("questionId").asLong();
        Long abTestId = questions.get(2).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "응답" },
                    { "type": "SCALE", "questionId": %d, "value": 3 },
                    { "type": "AB_TEST", "questionId": %d, "selected": "B" }
                  ]
                }
                """.formatted(subjectiveId, scaleId, abTestId));
        completeTest(actors.testId());

        JsonNode reports = reportData(actors.testId(), actors.makerToken()).path("reports");
        assertThat(reports).hasSize(3);

        JsonNode r0 = reports.get(0);
        assertThat(r0.path("questionId").asLong()).isEqualTo(subjectiveId);
        assertThat(r0.path("sequence").asLong()).isEqualTo(1L);
        assertThat(r0.path("title").asText()).isEqualTo("주관식 질문");
        assertThat(r0.path("type").asText()).isEqualTo("SUBJECTIVE");
        assertThat(r0.path("result").path("texts").get(0).asText()).isEqualTo("응답");

        JsonNode r1 = reports.get(1);
        assertThat(r1.path("questionId").asLong()).isEqualTo(scaleId);
        assertThat(r1.path("sequence").asLong()).isEqualTo(2L);
        assertThat(r1.path("type").asText()).isEqualTo("SCALE");
        assertThat(r1.path("result").path("average").asDouble()).isEqualTo(3.0);

        JsonNode r2 = reports.get(2);
        assertThat(r2.path("questionId").asLong()).isEqualTo(abTestId);
        assertThat(r2.path("sequence").asLong()).isEqualTo(3L);
        assertThat(r2.path("type").asText()).isEqualTo("AB_TEST");
        assertThat(r2.path("result").path("B").path("count").asInt()).isEqualTo(1);
        assertThat(r2.path("result").path("B").path("ratio").asDouble()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("전체 질문 유형이 혼재할 때 reports가 sequence 순서로 조립되고 각 유형별 핵심 집계 필드를 포함한다")
    void getReport_withAllQuestionTypes_reportsAssembledInSequenceOrder() throws Exception {
        TestActors actors = createActors();
        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    { "type": "SUBJECTIVE", "title": "주관식 질문", "description": "설명", "imageKey": null },
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": false,
                      "maxSelect": null,
                      "minSelect": null,
                      "isOther": false,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null }
                      ]
                    },
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
                    },
                    {
                      "type": "FIVE_SECOND",
                      "title": "5초 객관식",
                      "description": "설명",
                      "imageKey": "five-second-image",
                      "imageRatio": "9:16",
                      "isObjective": true,
                      "isDuplicate": false,
                      "minSelect": null,
                      "maxSelect": null,
                      "isOther": false,
                      "options": [
                        { "content": "검색창" },
                        { "content": "배너" }
                      ]
                    },
                    {
                      "type": "SCALE",
                      "title": "척도 질문",
                      "description": "설명",
                      "imageKey": null,
                      "minLabel": "낮음",
                      "maxLabel": "높음",
                      "range": 5
                    },
                    {
                      "type": "AB_TEST",
                      "title": "AB 질문",
                      "description": "설명",
                      "aImageKey": "a-image",
                      "bImageKey": "b-image",
                      "imageRatio": "9:16"
                    },
                    {
                      "type": "CARD_SORTING",
                      "title": "카드 소팅",
                      "description": "설명",
                      "cards": ["홈", "검색", "장바구니", "공지사항"],
                      "categories": ["쇼핑", "정보"]
                    },
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

        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());
        assertThat(questions).hasSize(8);

        JsonNode objectiveQuestion = questions.get(1);
        Long objectiveOptionId = objectiveQuestion.path("options").get(0).path("objectiveOptionId").asLong();

        JsonNode fiveSecondObjectiveQuestion = questions.get(3);
        Long fiveSecondOptionId = fiveSecondObjectiveQuestion.path("options").get(0).path("fiveSecondOptionId").asLong();

        JsonNode treeQuestion = questions.get(7);
        Long treeQuestionId = treeQuestion.path("questionId").asLong();
        Long rootNodeId = treeQuestion.path("features").get(0).path("treeTestId").asLong();
        Long childNodeId = treeQuestion.path("features").get(0).path("children").get(0).path("treeTestId").asLong();
        Long leafNodeId = treeQuestion.path("features").get(0).path("children").get(0)
                .path("children").get(0).path("treeTestId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "주관식 응답" },
                    { "type": "OBJECTIVE", "questionId": %d, "optionIds": [%d] },
                    { "type": "FIVE_SECOND", "questionId": %d, "text": "5초 주관 응답" },
                    { "type": "FIVE_SECOND", "questionId": %d, "optionIds": [%d] },
                    { "type": "SCALE", "questionId": %d, "value": 4 },
                    { "type": "AB_TEST", "questionId": %d, "selected": "A" },
                    {
                      "type": "CARD_SORTING",
                      "questionId": %d,
                      "groups": [
                        { "category": "쇼핑", "cardNames": ["홈", "장바구니"] },
                        { "category": "정보", "cardNames": ["검색", "공지사항"] }
                      ]
                    },
                    { "type": "TREE_TEST", "questionId": %d, "nodeId": %d, "path": [%d, %d, %d] }
                  ]
                }
                """.formatted(
                questions.get(0).path("questionId").asLong(),
                objectiveQuestion.path("questionId").asLong(),
                objectiveOptionId,
                questions.get(2).path("questionId").asLong(),
                fiveSecondObjectiveQuestion.path("questionId").asLong(),
                fiveSecondOptionId,
                questions.get(4).path("questionId").asLong(),
                questions.get(5).path("questionId").asLong(),
                questions.get(6).path("questionId").asLong(),
                treeQuestionId,
                leafNodeId,
                rootNodeId,
                childNodeId,
                leafNodeId
        ));
        completeTest(actors.testId());

        JsonNode data = reportData(actors.testId(), actors.makerToken());
        JsonNode reports = data.path("reports");

        assertThat(data.path("questionCount").asInt()).isEqualTo(8);
        assertThat(reports).hasSize(8);

        for (int i = 0; i < reports.size(); i++) {
            assertThat(reports.get(i).path("sequence").asInt()).isEqualTo(i + 1);
        }

        JsonNode subjectiveReport = reports.get(0);
        assertThat(subjectiveReport.path("type").asText()).isEqualTo("SUBJECTIVE");
        assertThat(subjectiveReport.path("result").path("texts").get(0).asText()).isEqualTo("주관식 응답");

        JsonNode objectiveReport = reports.get(1);
        assertThat(objectiveReport.path("type").asText()).isEqualTo("OBJECTIVE");
        assertThat(objectiveReport.path("result").path("options").isArray()).isTrue();
        assertThat(objectiveReport.path("result").path("options").get(0).path("count").asInt()).isEqualTo(1);

        JsonNode fiveSecondSubjectiveReport = reports.get(2);
        assertThat(fiveSecondSubjectiveReport.path("type").asText()).isEqualTo("FIVE_SECOND");
        assertThat(fiveSecondSubjectiveReport.path("result").path("texts").get(0).asText()).isEqualTo("5초 주관 응답");

        JsonNode fiveSecondObjectiveReport = reports.get(3);
        assertThat(fiveSecondObjectiveReport.path("type").asText()).isEqualTo("FIVE_SECOND");
        assertThat(fiveSecondObjectiveReport.path("result").path("options").isArray()).isTrue();
        assertThat(fiveSecondObjectiveReport.path("result").path("options").get(0).path("count").asInt()).isEqualTo(1);

        JsonNode scaleReport = reports.get(4);
        assertThat(scaleReport.path("type").asText()).isEqualTo("SCALE");
        assertThat(scaleReport.path("result").path("average").asDouble()).isEqualTo(4.0);
        assertThat(scaleReport.path("result").path("distribution").isArray()).isTrue();

        JsonNode abTestReport = reports.get(5);
        assertThat(abTestReport.path("type").asText()).isEqualTo("AB_TEST");
        assertThat(abTestReport.path("result").path("A").path("count").asInt()).isEqualTo(1);
        assertThat(abTestReport.path("result").path("A").path("ratio").asDouble()).isEqualTo(1.0);

        JsonNode cardSortingReport = reports.get(6);
        assertThat(cardSortingReport.path("type").asText()).isEqualTo("CARD_SORTING");
        assertThat(cardSortingReport.path("result").path("byCategory").isArray()).isTrue();
        assertThat(cardSortingReport.path("result").path("byCard").isArray()).isTrue();

        JsonNode treeTestReport = reports.get(7);
        assertThat(treeTestReport.path("type").asText()).isEqualTo("TREE_TEST");
        assertThat(treeTestReport.path("result").path("nodeFrequency").isArray()).isTrue();
        assertThat(treeTestReport.path("result").path("pathFrequency").isArray()).isTrue();
        assertThat(treeTestReport.path("result").path("pathFrequency").get(0).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("goalPpl=1에서 응답 제출만으로 비동기 이벤트 경로를 통해 리포트가 완료된다")
    void getReport_whenGoalPplIsOne_reportCompletesViaAsyncEventFlow() throws Exception {
        TestActors actors = createActors();
        setGoalPpl(actors.testId(), 1);
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "자동완료 응답" }
                  ]
                }
                """.formatted(questionId));

        JsonNode data = awaitCompletedReportData(actors.testId(), actors.makerToken());
        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("reportStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("reports")).hasSize(1);
        assertThat(data.path("reports").get(0).path("type").asText()).isEqualTo("SUBJECTIVE");
        assertThat(data.path("reports").get(0).path("result").path("texts")).hasSize(1);
        assertThat(data.path("reports").get(0).path("result").path("texts").get(0).asText())
                .isEqualTo("자동완료 응답");
    }

    @Test
    @DisplayName("OBJECTIVE isOther=true 응답 시 리포트에 otherTexts가 포함된다")
    void getReport_withObjectiveOtherText_returnsOtherTexts() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long otherOptionId = findOtherOptionId(questionNode);

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "OBJECTIVE", "questionId": %d, "optionIds": [%d], "otherText": "직접 입력 응답" }
                  ]
                }
                """.formatted(questionId, otherOptionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        JsonNode otherTexts = result.path("otherTexts");
        assertThat(otherTexts).hasSize(1);
        assertThat(otherTexts.get(0).asText()).isEqualTo("직접 입력 응답");
    }

    @Test
    @DisplayName("여러 참여자의 AB_TEST 응답이 ratio로 정확히 집계된다")
    void getReport_withMultipleParticipants_ratioCalculatedCorrectly() throws Exception {
        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();
        createAbTestQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                { "answers": [{ "type": "AB_TEST", "questionId": %d, "selected": "A" }] }
                """.formatted(questionId));
        submitAnswer(actors.testId(), tester2Token, """
                { "answers": [{ "type": "AB_TEST", "questionId": %d, "selected": "B" }] }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        assertThat(result.path("A").path("count").asInt()).isEqualTo(1);
        assertThat(result.path("A").path("ratio").asDouble()).isEqualTo(0.5);
        assertThat(result.path("B").path("count").asInt()).isEqualTo(1);
        assertThat(result.path("B").path("ratio").asDouble()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("CARD_SORTING byCard에 카드별 카테고리 카운트가 정확히 집계된다")
    void getReport_withCardSortingAnswers_byCardCountsCorrect() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [{
                    "type": "CARD_SORTING", "questionId": %d,
                    "groups": [
                      { "category": "쇼핑", "cardNames": ["홈", "장바구니"] },
                      { "category": "정보", "cardNames": ["검색", "공지사항"] }
                    ]
                  }]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode byCard = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("byCard");
        assertThat(byCard).hasSize(4);

        JsonNode homeCard = null;
        for (JsonNode card : byCard) {
            if ("홈".equals(card.path("cardName").asText())) {
                homeCard = card;
                break;
            }
        }
        assertThat(homeCard).isNotNull();
        assertThat(homeCard.path("categories").path("쇼핑").asInt()).isEqualTo(1);
        assertThat(homeCard.path("categories").path("정보").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("CARD_SORTING byCategory.cards는 count 내림차순으로 정렬되고 동점 rank와 ratio를 유지한다")
    void getReport_withCardSortingAnswers_byCategoryCardsSortedWithTieRankAndRatio() throws Exception {
        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();
        String tester3Token = createAdditionalTester();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [{
                    "type": "CARD_SORTING", "questionId": %d,
                    "groups": [
                      { "category": "쇼핑", "cardNames": ["홈", "검색", "장바구니"] },
                      { "category": "정보", "cardNames": ["공지사항"] }
                    ]
                  }]
                }
                """.formatted(questionId));
        submitAnswer(actors.testId(), tester2Token, """
                {
                  "answers": [{
                    "type": "CARD_SORTING", "questionId": %d,
                    "groups": [
                      { "category": "쇼핑", "cardNames": ["홈", "장바구니", "공지사항"] },
                      { "category": "정보", "cardNames": ["검색"] }
                    ]
                  }]
                }
                """.formatted(questionId));
        submitAnswer(actors.testId(), tester3Token, """
                {
                  "answers": [{
                    "type": "CARD_SORTING", "questionId": %d,
                    "groups": [
                      { "category": "쇼핑", "cardNames": ["홈", "검색", "공지사항"] },
                      { "category": "정보", "cardNames": ["장바구니"] }
                    ]
                  }]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode byCategory = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("byCategory");

        JsonNode shopping = null;
        for (JsonNode category : byCategory) {
            if ("쇼핑".equals(category.path("category").asText())) {
                shopping = category;
                break;
            }
        }

        assertThat(shopping).isNotNull();
        JsonNode cards = shopping.path("cards");
        assertThat(cards).hasSize(4);

        assertThat(cards.get(0).path("cardName").asText()).isEqualTo("홈");
        assertThat(cards.get(0).path("rank").asInt()).isEqualTo(1);
        assertThat(cards.get(0).path("count").asInt()).isEqualTo(3);
        assertThat(cards.get(0).path("ratio").asDouble()).isEqualTo(1.0);

        assertThat(cards.get(1).path("cardName").asText()).isEqualTo("검색");
        assertThat(cards.get(1).path("rank").asInt()).isEqualTo(2);
        assertThat(cards.get(1).path("count").asInt()).isEqualTo(2);
        assertThat(cards.get(1).path("ratio").asDouble()).isEqualTo(0.667);

        assertThat(cards.get(2).path("cardName").asText()).isEqualTo("장바구니");
        assertThat(cards.get(2).path("rank").asInt()).isEqualTo(2);
        assertThat(cards.get(2).path("count").asInt()).isEqualTo(2);
        assertThat(cards.get(2).path("ratio").asDouble()).isEqualTo(0.667);

        assertThat(cards.get(3).path("cardName").asText()).isEqualTo("공지사항");
        assertThat(cards.get(3).path("rank").asInt()).isEqualTo(2);
        assertThat(cards.get(3).path("count").asInt()).isEqualTo(2);
        assertThat(cards.get(3).path("ratio").asDouble()).isEqualTo(0.667);
    }

    @Test
    @DisplayName("TREE_TEST에서 동일 경로를 여러 참여자가 선택하면 pathFrequency count가 누적된다")
    void getReport_withSamePathSelectedByMultipleParticipants_pathFrequencyAccumulates() throws Exception {
        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();

        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [{
                    "type": "TREE_TEST", "title": "트리 질문", "description": "설명",
                    "features": [{ "label": "홈", "children": [] }]
                  }]
                }
                """);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long rootNodeId = questionNode.path("features").get(0).path("treeTestId").asLong();

        String answerPayload = """
                { "answers": [{ "type": "TREE_TEST", "questionId": %d, "nodeId": %d, "path": [%d] }] }
                """.formatted(questionId, rootNodeId, rootNodeId);
        submitAnswer(actors.testId(), actors.testerToken(), answerPayload);
        submitAnswer(actors.testId(), tester2Token, answerPayload);
        completeTest(actors.testId());

        JsonNode pathFrequency = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("pathFrequency");
        assertThat(pathFrequency).hasSize(1);
        assertThat(pathFrequency.get(0).path("count").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("SUBJECTIVE와 FIVE_SECOND 주관식 리포트의 texts는 createdAt 오름차순으로 정렬된다")
    void getReport_withSubjectiveTexts_returnsCreatedAtAscendingOrder() throws Exception {
        mutableClock.setInstant(DEFAULT_TEST_INSTANT);

        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();
        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    { "type": "SUBJECTIVE", "title": "주관식 질문", "description": "설명", "imageKey": null },
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
        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());
        Long subjectiveQuestionId = questions.get(0).path("questionId").asLong();
        Long fiveSecondQuestionId = questions.get(1).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "빠른 주관식 응답" },
                    { "type": "FIVE_SECOND", "questionId": %d, "text": "빠른 5초 응답" }
                  ]
                }
                """.formatted(subjectiveQuestionId, fiveSecondQuestionId));
        mutableClock.advance(Duration.ofSeconds(1));
        submitAnswer(actors.testId(), tester2Token, """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "늦은 주관식 응답" },
                    { "type": "FIVE_SECOND", "questionId": %d, "text": "늦은 5초 응답" }
                  ]
                }
                """.formatted(subjectiveQuestionId, fiveSecondQuestionId));

        completeTest(actors.testId());

        JsonNode reports = reportData(actors.testId(), actors.makerToken()).path("reports");
        JsonNode subjectiveTexts = reports.get(0).path("result").path("texts");
        JsonNode fiveSecondTexts = reports.get(1).path("result").path("texts");

        assertThat(subjectiveTexts).hasSize(2);
        assertThat(subjectiveTexts.get(0).asText()).isEqualTo("빠른 주관식 응답");
        assertThat(subjectiveTexts.get(1).asText()).isEqualTo("늦은 주관식 응답");

        assertThat(fiveSecondTexts).hasSize(2);
        assertThat(fiveSecondTexts.get(0).asText()).isEqualTo("빠른 5초 응답");
        assertThat(fiveSecondTexts.get(1).asText()).isEqualTo("늦은 5초 응답");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(DEFAULT_TEST_INSTANT, ZoneId.systemDefault());
        }
    }

    @Test
    @DisplayName("TREE_TEST 리포트는 path와 pathLabels를 함께 조립한다")
    void getReport_withTreeTestAnswers_returnsPathAndPathLabels() throws Exception {
        TestActors actors = createActors();
        createTreeTestQuestion(actors.testId(), actors.makerToken());
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long rootNodeId = questionNode.path("features").get(0).path("treeTestId").asLong();
        Long childNodeId = questionNode.path("features").get(0).path("children").get(0).path("treeTestId").asLong();
        Long leafNodeId = questionNode.path("features").get(0).path("children").get(0)
                .path("children").get(0).path("treeTestId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "TREE_TEST", "questionId": %d, "nodeId": %d, "path": [%d, %d, %d] }
                  ]
                }
                """.formatted(questionId, leafNodeId, rootNodeId, childNodeId, leafNodeId));
        completeTest(actors.testId());

        JsonNode pathFrequency = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("pathFrequency");

        assertThat(pathFrequency).hasSize(1);
        assertThat(pathFrequency.get(0).path("path")).hasSize(3);
        assertThat(pathFrequency.get(0).path("path").get(0).asLong()).isEqualTo(rootNodeId);
        assertThat(pathFrequency.get(0).path("path").get(1).asLong()).isEqualTo(childNodeId);
        assertThat(pathFrequency.get(0).path("path").get(2).asLong()).isEqualTo(leafNodeId);
        assertThat(pathFrequency.get(0).path("pathLabels")).hasSize(3);
        assertThat(pathFrequency.get(0).path("pathLabels").get(0).asText()).isEqualTo("마이페이지");
        assertThat(pathFrequency.get(0).path("pathLabels").get(1).asText()).isEqualTo("설정");
        assertThat(pathFrequency.get(0).path("pathLabels").get(2).asText()).isEqualTo("알림 설정");
    }

    @Test
    @DisplayName("완전한 리포트가 이미 있으면 aggregate 재호출 시 상태와 결과를 그대로 유지한다")
    void aggregate_whenReportAlreadyCompleted_keepsStatusAndExistingReports() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "기존 응답" }
                  ]
                }
                """.formatted(questionId));
        completeTest(actors.testId());

        List<Report> existingReports = reportRepository.findAllByTestId(actors.testId());
        JsonNode before = reportData(actors.testId(), actors.makerToken());

        List<Report> reusedReports = reportAggregationService.aggregate(actors.testId());

        server.MATE.domain.test.entity.Test updated = testRepository.findById(actors.testId()).orElseThrow();
        JsonNode after = reportData(actors.testId(), actors.makerToken());

        assertThat(updated.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(reusedReports).hasSize(1);
        assertThat(existingReports).hasSize(1);
        assertThat(reusedReports.get(0).getId()).isEqualTo(existingReports.get(0).getId());
        assertThat(reusedReports.get(0).getResult()).isEqualTo(existingReports.get(0).getResult());
        assertThat(reportRepository.findAllByTestId(actors.testId())).hasSize(1);
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("OBJECTIVE 복수 선택 시 선택된 옵션 각각 count가 증가한다")
    void getReport_withObjectiveMultipleOptionIds_eachOptionCounted() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), true, 1, 2, false);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long opt1 = questionNode.path("options").get(0).path("objectiveOptionId").asLong();
        Long opt2 = questionNode.path("options").get(1).path("objectiveOptionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "OBJECTIVE", "questionId": %d, "optionIds": [%d, %d] }
                  ]
                }
                """.formatted(questionId, opt1, opt2));
        completeTest(actors.testId());

        JsonNode options = reportData(actors.testId(), actors.makerToken())
                .path("reports").get(0).path("result").path("options");
        int opt1Count = 0, opt2Count = 0;
        for (JsonNode opt : options) {
            long id = opt.path("optionId").asLong();
            if (id == opt1) opt1Count = opt.path("count").asInt();
            else if (id == opt2) opt2Count = opt.path("count").asInt();
        }
        assertThat(opt1Count).isEqualTo(1);
        assertThat(opt2Count).isEqualTo(1);
    }

    @Test
    @DisplayName("FIVE_SECOND isOther=true 응답 시 리포트에 otherTexts가 포함된다")
    void getReport_withFiveSecondOtherText_returnsOtherTexts() throws Exception {
        TestActors actors = createActors();
        createFiveSecondObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        JsonNode questionNode = getSingleQuestion(actors.testId(), actors.makerToken());
        Long questionId = questionNode.path("questionId").asLong();
        Long otherOptionId = null;
        for (JsonNode o : questionNode.path("options")) {
            if (o.path("isOtherOption").asBoolean()) {
                otherOptionId = o.path("fiveSecondOptionId").asLong();
                break;
            }
        }
        assertThat(otherOptionId).isNotNull();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "FIVE_SECOND", "questionId": %d, "optionIds": [%d], "otherText": "5초 기타 응답" }
                  ]
                }
                """.formatted(questionId, otherOptionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        JsonNode otherTexts = result.path("otherTexts");
        assertThat(otherTexts).hasSize(1);
        assertThat(otherTexts.get(0).asText()).isEqualTo("5초 기타 응답");
    }

    @Test
    @DisplayName("SCALE 최솟값과 최댓값 응답이 distribution 양 끝에 정확히 집계된다")
    void getReport_withScaleBoundaryValues_distributionCorrect() throws Exception {
        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                { "answers": [{ "type": "SCALE", "questionId": %d, "value": 1 }] }
                """.formatted(questionId));
        submitAnswer(actors.testId(), tester2Token, """
                { "answers": [{ "type": "SCALE", "questionId": %d, "value": 5 }] }
                """.formatted(questionId));
        completeTest(actors.testId());

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("reports").get(0).path("result");
        assertThat(result.path("average").asDouble()).isEqualTo(3.0);
        JsonNode distribution = result.path("distribution");
        assertThat(distribution.get(0).path("score").asInt()).isEqualTo(1);
        assertThat(distribution.get(0).path("count").asInt()).isEqualTo(1);
        assertThat(distribution.get(4).path("score").asInt()).isEqualTo(5);
        assertThat(distribution.get(4).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("리포트 집계 중이면 reportStatus가 IN_PROGRESS이고 reports는 빈 리스트다")
    void getReport_whenReportAggregating_returnsInProgressStatus() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        Long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswer(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    { "type": "SUBJECTIVE", "questionId": %d, "text": "응답" }
                  ]
                }
                """.formatted(questionId));
        completeTestStatusOnly(actors.testId());

        JsonNode data = reportData(actors.testId(), actors.makerToken());
        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("reportStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(data.path("reports")).isEmpty();
    }

    @Test
    @DisplayName("부분 생성된 리포트가 있으면 집계는 FAILED로 처리된다")
    void aggregate_whenReportsPartiallyExist_marksFailed() throws Exception {
        TestActors actors = createActors();
        createTwoSubjectiveQuestions(actors.testId(), actors.makerToken());
        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());

        reportRepository.save(Report.builder()
                .testId(actors.testId())
                .questionId(questions.get(0).path("questionId").asLong())
                .questionType(QuestionType.SUBJECTIVE)
                .result(Map.of("texts", java.util.List.of("partial")))
                .build());

        completeTestStatusOnly(actors.testId());
        reportAggregationService.aggregate(actors.testId());

        server.MATE.domain.test.entity.Test updated = testRepository.findById(actors.testId()).orElseThrow();
        assertThat(updated.getReportStatus()).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    @DisplayName("reportStatus가 COMPLETED여도 리포트 수가 질문 수와 다르면 조회 시 FAILED와 빈 reports를 반환한다")
    void getReport_whenCompletedButReportsAreIncomplete_returnsFailedAndEmptyReports() throws Exception {
        TestActors actors = createActors();
        createTwoSubjectiveQuestions(actors.testId(), actors.makerToken());
        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());

        reportRepository.save(Report.builder()
                .testId(actors.testId())
                .questionId(questions.get(0).path("questionId").asLong())
                .questionType(QuestionType.SUBJECTIVE)
                .result(Map.of("texts", java.util.List.of("partial")))
                .build());

        server.MATE.domain.test.entity.Test test = testRepository.findById(actors.testId()).orElseThrow();
        test.complete();
        test.completeReportAggregation();
        testRepository.save(test);

        JsonNode data = reportData(actors.testId(), actors.makerToken());
        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("reportStatus").asText()).isEqualTo("FAILED");
        assertThat(data.path("reports")).isEmpty();
    }

    private JsonNode reportData(Long testId, String token) throws Exception {
        var result = mockMvc.perform(
                        get("/api/v1/tests/{testId}/report", testId)
                                .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = parseBody(result);
        assertThat(body.path("success").asBoolean()).isTrue();
        return body.path("data");
    }

    private JsonNode awaitCompletedReportData(Long testId, String token) {
        return await("report should complete via async event flow")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .ignoreExceptions()
                .until(() -> reportDataUnchecked(testId, token),
                        data -> "COMPLETED".equals(data.path("reportStatus").asText()));
    }

    private JsonNode reportDataUnchecked(Long testId, String token) {
        try {
            return reportData(testId, token);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void completeTest(Long testId) {
        server.MATE.domain.test.entity.Test test = testRepository.findById(testId).orElseThrow();
        test.complete();
        testRepository.save(test);
        reportAggregationService.aggregate(testId);
    }

    private void completeTestStatusOnly(Long testId) {
        server.MATE.domain.test.entity.Test test = testRepository.findById(testId).orElseThrow();
        test.complete();
        test.startReportAggregation();
        testRepository.save(test);
    }

    private void setGoalPpl(Long testId, int goalPpl) {
        server.MATE.domain.test.entity.Test test = testRepository.findById(testId).orElseThrow();
        try {
            java.lang.reflect.Field field = server.MATE.domain.test.entity.Test.class.getDeclaredField("goalPpl");
            field.setAccessible(true);
            field.set(test, goalPpl);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        testRepository.save(test);
    }

    private String createAdditionalTester() {
        server.MATE.domain.users.entity.Users tester = usersRepository.save(
                server.MATE.domain.users.entity.Users.builder()
                        .ci("tester-extra-" + System.nanoTime())
                        .name("tester2")
                        .build());
        return bearerToken(tester);
    }

    private Long findOtherOptionId(JsonNode questionNode) {
        for (JsonNode option : questionNode.path("options")) {
            if (option.path("isOtherOption").asBoolean()) {
                return option.path("objectiveOptionId").asLong();
            }
        }
        throw new IllegalStateException("기타 선택지를 찾을 수 없습니다");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDownReport() {
        reportRepository.deleteAll();
    }
}
