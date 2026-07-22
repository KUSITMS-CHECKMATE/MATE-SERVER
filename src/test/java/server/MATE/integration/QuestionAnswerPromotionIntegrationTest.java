package server.MATE.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "toss.promotion.answer.enabled=true",
        "toss.promotion.answer.promotion-code=test-answer-promotion"
})
class QuestionAnswerPromotionIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    @MockitoBean
    TossPromotionGateway tossPromotionGateway;

    @BeforeEach
    void setUpGateway() {
        given(tossPromotionGateway.issueKey(any())).willReturn("test-key");
        given(tossPromotionGateway.getExecutionStatus(any(), any(), any())).willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("응답 등록 후 linked toss account가 있으면 promotion 지급 row가 성공 상태로 저장된다")
    void submitsAnswerAndCreatesSucceededPromotionReward() throws Exception {
        TestActors actors = createActors();
        linkTossAccount(usersRepository.findById(actors.testerId()).orElseThrow(), 777L);
        createSingleSubjectiveQuestion(actors.testId(), actors.makerToken());
        long questionId = getSingleQuestion(actors.testId(), actors.makerToken()).path("questionId").asLong();

        var response = submitAnswerExpectSuccess(actors.testId(), actors.testerToken(), """
                {
                  "answers": [
                    {
                      "type": "SUBJECTIVE",
                      "questionId": %d,
                      "text": "프로모션 지급 검증 응답"
                    }
                  ]
                }
                """.formatted(questionId));

        long participationId = response.path("data").path("participationId").asLong();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var reward = promotionRewardRepository.findByParticipationId(participationId).orElseThrow();
                    assertThat(reward.getTesterId()).isEqualTo(actors.testerId());
                    assertThat(reward.getTossUserKey()).isEqualTo(777L);
                    assertThat(reward.getPromotionCode()).isEqualTo("test-answer-promotion");
                    assertThat(reward.getStatus()).isEqualTo(PromotionRewardStatus.SUCCEEDED);
                    assertThat(reward.getRewardKey()).isNotBlank();
                });
    }
}
