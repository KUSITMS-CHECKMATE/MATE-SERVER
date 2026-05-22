package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.test.entity.TestStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportEndToEndIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    @Test
    @DisplayName("IN_PROGRESS 테스트의 리포트는 results가 빈 리스트다")
    void getReport_whenInProgress_returnsEmptyResults() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());

        JsonNode data = reportData(actors.testId(), actors.makerToken());

        assertThat(data.path("testStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(data.path("questionCount").asInt()).isEqualTo(1);
        assertThat(data.path("results").isArray()).isTrue();
        assertThat(data.path("results")).isEmpty();
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
    void getReport_withSubjectiveAnswers_returnsResponses() throws Exception {
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

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("results").get(0);
        assertThat(result.path("type").asText()).isEqualTo("SUBJECTIVE");
        JsonNode responses = result.path("report").path("responses");
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).asText()).isEqualTo("주관식 응답");
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
                .path("results").get(0).path("report").path("options");
        assertThat(options.isArray()).isTrue();
        int totalCount = 0;
        for (JsonNode option : options) {
            totalCount += option.path("count").asInt();
        }
        assertThat(totalCount).isEqualTo(1);
        assertThat(options.get(0).path("count").asInt()).isEqualTo(1); // 가장 많이 선택된 옵션이 첫 번째
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

        JsonNode report = reportData(actors.testId(), actors.makerToken()).path("results").get(0).path("report");
        assertThat(report.path("average").asDouble()).isEqualTo(4.0);
        JsonNode distribution = report.path("distribution");
        assertThat(distribution).hasSize(5);
        assertThat(distribution.get(3).path("score").asInt()).isEqualTo(4);
        assertThat(distribution.get(3).path("count").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("FIVE_SECOND 주관식 응답 제출 후 리포트에 텍스트 목록이 반환된다")
    void getReport_withFiveSecondSubjectiveAnswers_returnsResponses() throws Exception {
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

        JsonNode result = reportData(actors.testId(), actors.makerToken()).path("results").get(0);
        assertThat(result.path("type").asText()).isEqualTo("FIVE_SECOND");
        JsonNode responses = result.path("report").path("responses");
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).asText()).isEqualTo("5초 주관 응답");
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
                .path("results").get(0).path("report").path("options");
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
    void getReport_withAbTestAnswers_returnsABCountsAndPercentages() throws Exception {
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

        JsonNode report = reportData(actors.testId(), actors.makerToken()).path("results").get(0).path("report");
        assertThat(report.path("aCount").asInt()).isEqualTo(1);
        assertThat(report.path("aPercentage").asDouble()).isEqualTo(100.0);
        assertThat(report.path("bCount").asInt()).isEqualTo(0);
        assertThat(report.path("bPercentage").asDouble()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CARD_SORTING 응답 제출 후 리포트에 카테고리별 카드 랭킹이 반환된다")
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

        JsonNode groups = reportData(actors.testId(), actors.makerToken())
                .path("results").get(0).path("report").path("groups");
        assertThat(groups).hasSize(2);
        assertThat(groups).anySatisfy(g -> {
            assertThat(g.path("groupName").asText()).isEqualTo("쇼핑");
            assertThat(g.path("cards")).hasSize(4); // 전체 카드 수 포함 (선택 안 된 카드도 0건으로 포함)
        });
        assertThat(groups).anySatisfy(g -> assertThat(g.path("groupName").asText()).isEqualTo("정보"));
    }

    @Test
    @DisplayName("TREE_TEST 응답 제출 후 리포트에 리프 노드별 카운트가 반환된다")
    void getReport_withTreeTestAnswers_returnsNodeCounts() throws Exception {
        TestActors actors = createActors();
        // 단일 루트 노드 트리 — 루트 자체가 리프이므로 path = [nodeId] 가 유효
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

        JsonNode nodes = reportData(actors.testId(), actors.makerToken())
                .path("results").get(0).path("report").path("nodes");
        assertThat(nodes.isArray()).isTrue();
        assertThat(nodes).anySatisfy(node -> assertThat(node.path("count").asInt()).isEqualTo(1));
    }

    @Test
    @DisplayName("COMPLETED 테스트에 질문이 없으면 questions와 results가 모두 빈 리스트다")
    void getReport_whenCompletedWithNoQuestions_returnsEmptyLists() throws Exception {
        TestActors actors = createActors();
        completeTest(actors.testId());

        JsonNode data = reportData(actors.testId(), actors.makerToken());

        assertThat(data.path("testStatus").asText()).isEqualTo("COMPLETED");
        assertThat(data.path("questionCount").asInt()).isEqualTo(0);
        assertThat(data.path("questions")).isEmpty();
        assertThat(data.path("results")).isEmpty();
    }

    @Test
    @DisplayName("응답이 없는 SCALE 질문의 리포트는 average 0.0, distribution 전체 count 0이다")
    void getReport_withNoScaleAnswers_returnsZeroAverageAndDistribution() throws Exception {
        TestActors actors = createActors();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        completeTest(actors.testId());

        JsonNode report = reportData(actors.testId(), actors.makerToken()).path("results").get(0).path("report");
        assertThat(report.path("average").asDouble()).isEqualTo(0.0);
        JsonNode distribution = report.path("distribution");
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
                .path("results").get(0).path("report").path("options");
        assertThat(options.isArray()).isTrue();
        for (JsonNode option : options) {
            assertThat(option.path("count").asInt()).isEqualTo(0);
            assertThat(option.path("percentage").asDouble()).isEqualTo(0.0);
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
    @DisplayName("여러 타입 질문이 혼재할 때 results가 sequence 순서로 조립되고 각 항목의 필드가 올바르다")
    void getReport_withMultipleQuestionTypes_resultsAssembledInSequenceOrder() throws Exception {
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

        JsonNode results = reportData(actors.testId(), actors.makerToken()).path("results");
        assertThat(results).hasSize(3);

        JsonNode r0 = results.get(0);
        assertThat(r0.path("questionId").asLong()).isEqualTo(subjectiveId);
        assertThat(r0.path("sequence").asLong()).isEqualTo(1L);
        assertThat(r0.path("title").asText()).isEqualTo("주관식 질문");
        assertThat(r0.path("type").asText()).isEqualTo("SUBJECTIVE");
        assertThat(r0.path("report").path("responses").get(0).asText()).isEqualTo("응답");

        JsonNode r1 = results.get(1);
        assertThat(r1.path("questionId").asLong()).isEqualTo(scaleId);
        assertThat(r1.path("sequence").asLong()).isEqualTo(2L);
        assertThat(r1.path("type").asText()).isEqualTo("SCALE");
        assertThat(r1.path("report").path("average").asDouble()).isEqualTo(3.0);

        JsonNode r2 = results.get(2);
        assertThat(r2.path("questionId").asLong()).isEqualTo(abTestId);
        assertThat(r2.path("sequence").asLong()).isEqualTo(3L);
        assertThat(r2.path("type").asText()).isEqualTo("AB_TEST");
        assertThat(r2.path("report").path("bCount").asInt()).isEqualTo(1);
        assertThat(r2.path("report").path("bPercentage").asDouble()).isEqualTo(100.0);
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
        try {
            java.lang.reflect.Field field = server.MATE.domain.test.entity.Test.class.getDeclaredField("testStatus");
            field.setAccessible(true);
            field.set(test, TestStatus.COMPLETED);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        testRepository.save(test);
    }

}
