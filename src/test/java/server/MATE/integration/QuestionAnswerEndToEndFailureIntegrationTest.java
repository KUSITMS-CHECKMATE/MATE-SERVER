package server.MATE.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.test.entity.ApprovalStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestionAnswerEndToEndFailureIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    @Test
    @DisplayName("질문 수보다 적은 응답을 보내면 ANSWER_008을 반환한다")
    void returnsAnswer008WhenFewerAnswersThanQuestions() throws Exception {
        TestActors actors = createActors();
        createTwoSubjectiveQuestions(actors.testId(), actors.makerToken());
        JsonNode questions = getQuestionsArray(actors.testId(), actors.makerToken());
        long firstQuestionId = questions.get(0).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "응답 하나만"
                    }
                  ]
                }
                """.formatted(firstQuestionId), 400, "ANSWER_008");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("질문 수보다 많은 응답을 보내면 ANSWER_008을 반환한다")
    void returnsAnswer008WhenMoreAnswersThanQuestions() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "응답 1"
                    },
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "응답 2"
                    }
                  ]
                }
                """.formatted(questionId, questionId), 400, "ANSWER_008");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("같은 tester가 같은 테스트에 두 번 제출하면 PARTICIPATION_003을 반환한다")
    void returnsParticipation003WhenSubmittingTwice() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectSuccess(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "첫 제출"
                    }
                  ]
                }
                """.formatted(questionId));

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "두 번째 제출"
                    }
                  ]
                }
                """.formatted(questionId), 400, "PARTICIPATION_003");

        assertSideEffects(actors.testId(), 1L, 1L);
    }

    @Test
    @DisplayName("질문 타입과 다른 answer type을 보내면 ANSWER_001을 반환한다")
    void returnsAnswer001WhenAnswerTypeDoesNotMatchQuestionType() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "잘못된 타입"
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_001");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("다른 테스트의 questionId를 보내면 ANSWER_002를 반환한다")
    void returnsAnswer002WhenQuestionDoesNotBelongToTest() throws Exception {
        TestActors first = createActors();
        TestActors second = createActors();
        createSingleSubjectiveQuestion(first.testId(), first.makerToken());
        createSingleSubjectiveQuestion(second.testId(), second.makerToken());
        long foreignQuestionId = getSingleQuestion(second.testId(), second.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(first.testId(), first.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "다른 테스트 질문"
                    }
                  ]
                }
                """.formatted(foreignQuestionId), 400, "ANSWER_002");

        assertNoSideEffects(first.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("같은 questionId를 answers 배열에 중복으로 넣으면 ANSWER_003을 반환한다")
    void returnsAnswer003WhenQuestionIdIsDuplicatedInAnswers() throws Exception {
        TestActors actors = createActors();
        createTwoSubjectiveQuestions(actors.testId(), actors.makerToken());
        long questionId = getQuestionsArray(actors.testId(), actors.makerToken()).get(0).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "첫 응답"
                    },
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "중복 응답"
                    }
                  ]
                }
                """.formatted(questionId, questionId), 400, "ANSWER_003");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE에서 기타 option 없이 otherText만 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenObjectiveOtherTextProvidedWithoutOtherOption() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        long questionId = question.path("questionId").asLong();
        long firstOptionId = question.path("options").get(0).path("objectiveOptionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
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
                """.formatted(questionId, firstOptionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE에서 기타 option 선택 후 otherText가 없으면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenObjectiveOtherOptionSelectedWithoutOtherText() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        long questionId = question.path("questionId").asLong();
        long otherOptionId = extractOtherOptionId(question, "objectiveOptionId");

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [%d]
                    }
                  ]
                }
                """.formatted(questionId, otherOptionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE에서 존재하지 않는 optionId를 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenObjectiveOptionIdIsInvalid() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [999999]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE에서 중복 optionId를 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenObjectiveOptionIdIsDuplicated() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), true, 1, 2, true);
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        long questionId = question.path("questionId").asLong();
        long firstOptionId = question.path("options").get(0).path("objectiveOptionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [%d, %d]
                    }
                  ]
                }
                """.formatted(questionId, firstOptionId, firstOptionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE에서 optionIds를 비우면 ANSWER_005를 반환한다")
    void returnsAnswer005WhenObjectiveOptionIdsAreEmpty() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": []
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_005");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE 단일 선택 문항에 복수 선택을 보내면 ANSWER_006을 반환한다")
    void returnsAnswer006WhenObjectiveSingleSelectReceivesMultipleOptions() throws Exception {
        TestActors actors = createActors();
        createObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, false);
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        long questionId = question.path("questionId").asLong();
        long firstOptionId = question.path("options").get(0).path("objectiveOptionId").asLong();
        long secondOptionId = question.path("options").get(1).path("objectiveOptionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [%d, %d]
                    }
                  ]
                }
                """.formatted(questionId, firstOptionId, secondOptionId), 400, "ANSWER_006");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("OBJECTIVE 복수 선택 문항에서 max 초과 선택이면 ANSWER_006을 반환한다")
    void returnsAnswer006WhenObjectiveSelectCountExceedsMax() throws Exception {
        TestActors actors = createActors();
        seedQuestions(actors.testId(), actors.makerToken(), """
                {
                  "questions": [
                    {
                      "type": "OBJECTIVE",
                      "title": "객관식 질문",
                      "description": "설명",
                      "isDuplicate": true,
                      "maxSelect": 2,
                      "minSelect": 1,
                      "isOther": false,
                      "options": [
                        { "content": "검색", "imageKey": null },
                        { "content": "결제", "imageKey": null },
                        { "content": "공유", "imageKey": null }
                      ]
                    }
                  ]
                }
                """);
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        long questionId = question.path("questionId").asLong();
        List<Long> optionIds = extractOptionIds(question, "objectiveOptionId");

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "OBJECTIVE",
                      "questionId": %d,
                      "optionIds": [%d, %d, %d]
                    }
                  ]
                }
                """.formatted(questionId, optionIds.get(0), optionIds.get(1), optionIds.get(2)), 400, "ANSWER_006");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("FIVE_SECOND 주관식에 optionIds를 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenFiveSecondSubjectiveReceivesOptionIds() throws Exception {
        TestActors actors = createActors();
        createFiveSecondSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "FIVE_SECOND",
                      "questionId": %d,
                      "text": "텍스트",
                      "optionIds": [1]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("FIVE_SECOND 객관식에 text를 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenFiveSecondObjectiveReceivesText() throws Exception {
        TestActors actors = createActors();
        createFiveSecondObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "FIVE_SECOND",
                      "questionId": %d,
                      "text": "주관식 텍스트",
                      "optionIds": [1]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("FIVE_SECOND 객관식에서 optionIds를 비우면 ANSWER_005를 반환한다")
    void returnsAnswer005WhenFiveSecondObjectiveOptionIdsAreEmpty() throws Exception {
        TestActors actors = createActors();
        createFiveSecondObjectiveQuestion(actors.testId(), actors.makerToken(), false, null, null, true);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "FIVE_SECOND",
                      "questionId": %d,
                      "optionIds": []
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_005");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("SCALE에 0을 보내면 ANSWER_007을 반환한다")
    void returnsAnswer007WhenScaleValueIsZero() throws Exception {
        TestActors actors = createActors();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SCALE",
                      "questionId": %d,
                      "value": 0
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_007");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("SCALE에 range 초과 값을 보내면 ANSWER_007을 반환한다")
    void returnsAnswer007WhenScaleValueExceedsRange() throws Exception {
        TestActors actors = createActors();
        createScaleQuestion(actors.testId(), actors.makerToken(), 5);
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SCALE",
                      "questionId": %d,
                      "value": 6
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_007");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("AB_TEST에서 selected 값이 유효하지 않으면 COMMON_002를 반환한다")
    void returnsCommon002WhenAbTestSelectedIsInvalid() throws Exception {
        TestActors actors = createActors();
        createAbTestQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        JsonNode error = submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "AB_TEST",
                      "questionId": %d,
                      "selected": "C"
                    }
                  ]
                }
                """.formatted(questionId), 400, "COMMON_002");

        assertThat(error.path("field").asText()).contains("selected");
        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("CARD_SORTING에서 존재하지 않는 category를 보내면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenCardSortingCategoryIsInvalid() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "CARD_SORTING",
                      "questionId": %d,
                      "groups": [
                        { "category": "없는 카테고리", "cardNames": ["홈", "검색", "장바구니", "공지사항"] }
                      ]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("CARD_SORTING에서 같은 카드를 두 그룹에 중복 배치하면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenCardSortingCardIsDuplicatedAcrossGroups() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "CARD_SORTING",
                      "questionId": %d,
                      "groups": [
                        { "category": "쇼핑", "cardNames": ["홈", "검색"] },
                        { "category": "정보", "cardNames": ["검색", "장바구니", "공지사항"] }
                      ]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("CARD_SORTING에서 카드 하나를 누락하면 ANSWER_005를 반환한다")
    void returnsAnswer005WhenCardSortingMissesACard() throws Exception {
        TestActors actors = createActors();
        createCardSortingQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "CARD_SORTING",
                      "questionId": %d,
                      "groups": [
                        { "category": "쇼핑", "cardNames": ["홈", "검색"] },
                        { "category": "정보", "cardNames": ["장바구니"] }
                      ]
                    }
                  ]
                }
                """.formatted(questionId), 400, "ANSWER_005");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("TREE_TEST에서 nodeId와 path 마지막 값이 다르면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenTreeNodeIdDoesNotMatchPathEnd() throws Exception {
        TestActors actors = createActors();
        createTreeTestQuestion(actors.testId(), actors.makerToken());
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        TreeNodes nodes = extractTreeNodes(question.path("features"));

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "TREE_TEST",
                      "questionId": %d,
                      "nodeId": %d,
                      "path": [%d, %d, %d]
                    }
                  ]
                }
                """.formatted(question.path("questionId").asLong(), nodes.leafId(), nodes.rootId(), nodes.parentId(), nodes.parentId()), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("TREE_TEST에서 leaf가 아닌 노드를 선택하면 ANSWER_004를 반환한다")
    void returnsAnswer004WhenTreeSelectionIsNotLeaf() throws Exception {
        TestActors actors = createActors();
        createTreeTestQuestion(actors.testId(), actors.makerToken());
        JsonNode question = getSingleQuestion(actors.testId(), actors.makerToken());
        TreeNodes nodes = extractTreeNodes(question.path("features"));

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "TREE_TEST",
                      "questionId": %d,
                      "nodeId": %d,
                      "path": [%d, %d]
                    }
                  ]
                }
                """.formatted(question.path("questionId").asLong(), nodes.parentId(), nodes.rootId(), nodes.parentId()), 400, "ANSWER_004");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("인증 없이 질문 조회하면 COMMON_008을 반환한다")
    void returnsCommon008WhenGettingQuestionsWithoutAuthentication() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());

        performExpectingError(get("/api/v1/tests/{testId}/questions", actors.testId()), 401, "COMMON_008");
    }

    @Test
    @DisplayName("인증 없이 응답 등록하면 COMMON_008을 반환한다")
    void returnsCommon008WhenSubmittingAnswerWithoutAuthentication() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        performExpectingError(post("/api/v1/tests/{testId}/answers", actors.testId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "type": "SUBJECTIVE",
                                      "questionId": %d,
                                      "text": "무인증 응답"
                                    }
                                  ]
                                }
                                """.formatted(questionId)),
                401, "COMMON_008");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("maker가 아닌 사용자도 질문 조회에 성공한다")
    void nonMakerCanGetQuestions() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());

        JsonNode response = getQuestionsArray(actors.testId(), actors.testerToken());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).path("type").asText()).isEqualTo("SUBJECTIVE");
    }

    @Test
    @DisplayName("참여 불가능한 테스트 상태면 PARTICIPATION_002를 반환한다")
    void returnsParticipation002WhenTestCannotBeParticipatedIn() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        server.MATE.domain.test.entity.Test test = testRepository.findById(actors.testId()).orElseThrow();
        ReflectionTestUtils.setField(test, "approvalStatus", ApprovalStatus.WAITING);
        testRepository.saveAndFlush(test);

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "응답"
                    }
                  ]
                }
                """.formatted(questionId), 400, "PARTICIPATION_002");

        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    @Test
    @DisplayName("모집 인원이 이미 찼으면 PARTICIPATION_004를 반환한다")
    void returnsParticipation004WhenGoalIsReached() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        server.MATE.domain.test.entity.Test test = testRepository.findById(actors.testId()).orElseThrow();
        ReflectionTestUtils.setField(test, "pplCount", 100L);
        testRepository.saveAndFlush(test);

        submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "응답"
                    }
                  ]
                }
                """.formatted(questionId), 400, "PARTICIPATION_004");

        assertNoSideEffects(actors.testId(), 0L, 100L);
    }

    @Test
    @DisplayName("answers가 빈 배열이면 COMMON_002를 반환한다")
    void returnsCommon002WhenAnswersArrayIsEmpty() throws Exception {
        TestActors actors = createActors();
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());

        JsonNode error = submitAnswerExpectingError(actors.testId(), actors.testerToken(), """
                {
                  "answers": []
                }
                """, 400, "COMMON_002");

        assertThat(error.path("field").asText()).contains("answers");
        assertNoSideEffects(actors.testId(), 0L, 0L);
    }

    private Long extractOtherOptionId(JsonNode question, String idField) {
        for (JsonNode option : question.path("options")) {
            if (option.path("isOtherOption").asBoolean(false)) {
                return option.path(idField).asLong();
            }
        }
        throw new IllegalStateException("other option not found");
    }

    private List<Long> extractOptionIds(JsonNode question, String idField) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode option : question.path("options")) {
            ids.add(option.path(idField).asLong());
        }
        return ids;
    }

    private TreeNodes extractTreeNodes(JsonNode features) {
        JsonNode root = features.get(0);
        JsonNode parent = root.path("children").get(0);
        JsonNode leaf = parent.path("children").get(0);
        return new TreeNodes(
                root.path("treeTestId").asLong(),
                parent.path("treeTestId").asLong(),
                leaf.path("treeTestId").asLong()
        );
    }

    private void assertNoSideEffects(Long testId, long answerCount, long pplCount) {
        entityManager.clear();
        assertThat(answerRepository.count()).isEqualTo(answerCount);
        assertThat(participationRepository.count()).isZero();
        assertThat(testRepository.findById(testId).orElseThrow().getPplCount()).isEqualTo(pplCount);
    }

    private void assertSideEffects(Long testId, long answerCount, long participationCount) {
        entityManager.clear();
        assertThat(answerRepository.count()).isEqualTo(answerCount);
        assertThat(participationRepository.count()).isEqualTo(participationCount);
        assertThat(testRepository.findById(testId).orElseThrow().getPplCount()).isEqualTo(1L);
    }

    private record TreeNodes(
            Long rootId,
            Long parentId,
            Long leafId
    ) {
    }
}
