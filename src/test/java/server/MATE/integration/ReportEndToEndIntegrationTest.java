package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.report.service.ReportAggregationService;
import server.MATE.domain.test.entity.TestStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportEndToEndIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    @Autowired
    private ReportAggregationService reportAggregationService;

    @Autowired
    private ReportRepository reportRepository;

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
        performCreateQuestion(actors.testId(), actors.makerToken(), """
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
        performCreateQuestion(actors.testId(), actors.makerToken(), """
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
    @DisplayName("pplCount가 goalPpl에 도달하면 테스트가 자동으로 COMPLETED 전환되고 백그라운드 집계가 시작된다")
    void getReport_whenPplCountReachesGoalPpl_autoCompletesAndAggregationStarts() throws Exception {
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

        JsonNode data = reportData(actors.testId(), actors.makerToken());
        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("reportStatus").asText()).isIn("IN_PROGRESS", "COMPLETED");
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
    @DisplayName("TREE_TEST에서 동일 경로를 여러 참여자가 선택하면 pathFrequency count가 누적된다")
    void getReport_withSamePathSelectedByMultipleParticipants_pathFrequencyAccumulates() throws Exception {
        TestActors actors = createActors();
        String tester2Token = createAdditionalTester();

        performCreateQuestion(actors.testId(), actors.makerToken(), """
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
