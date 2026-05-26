package server.MATE.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.users.entity.Users;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyAnswerIntegrationTest extends BaseQuestionAnswerEndToEndTest {

    @Test
    @Transactional
    @DisplayName("내 응답 목록을 최신순으로 조회하고 totalPromotionReward를 반환한다")
    void listMyAnswers() throws Exception {
        Users maker = usersRepository.save(Users.builder()
                .ci("maker-" + System.nanoTime())
                .name("maker")
                .build());
        Users tester = usersRepository.save(Users.builder()
                .ci("tester-" + System.nanoTime())
                .name("tester")
                .build());

        server.MATE.domain.test.entity.Test firstTest = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(maker.getId())
                .title("첫 번째 테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("설명")
                .imageKeys(List.of())
                .reward(300)
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.parse("2099-05-31T23:59:59"))
                .build());

        server.MATE.domain.test.entity.Test secondTest = testRepository.save(server.MATE.domain.test.entity.Test.builder()
                .makerId(maker.getId())
                .title("두 번째 테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("설명")
                .imageKeys(List.of())
                .reward(400)
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.parse("2099-05-31T23:59:59"))
                .build());

        Participation firstParticipation = participationRepository.save(Participation.builder()
                .testId(firstTest.getId())
                .testerId(tester.getId())
                .build());
        Participation secondParticipation = participationRepository.save(Participation.builder()
                .testId(secondTest.getId())
                .testerId(tester.getId())
                .build());

        entityManager.createNativeQuery("update participation set created_at = :createdAt where id = :id")
                .setParameter("createdAt", LocalDateTime.parse("2026-05-13T10:00:00"))
                .setParameter("id", firstParticipation.getId())
                .executeUpdate();
        entityManager.createNativeQuery("update participation set created_at = :createdAt where id = :id")
                .setParameter("createdAt", LocalDateTime.parse("2026-05-20T10:00:00"))
                .setParameter("id", secondParticipation.getId())
                .executeUpdate();

        promotionRewardRepository.save(PromotionReward.builder()
                .participationId(firstParticipation.getId())
                .testId(firstTest.getId())
                .testerId(tester.getId())
                .rewardAmount(300)
                .status(PromotionRewardStatus.SUCCEEDED)
                .build());
        promotionRewardRepository.save(PromotionReward.builder()
                .participationId(secondParticipation.getId())
                .testId(secondTest.getId())
                .testerId(tester.getId())
                .rewardAmount(400)
                .status(PromotionRewardStatus.SUCCEEDED)
                .build());

        entityManager.flush();
        entityManager.clear();

        var result = mockMvc.perform(get("/api/v1/answers/me")
                        .header("Authorization", bearerToken(tester)))
                .andExpect(status().isOk())
                .andReturn();

        var body = parseBody(result);
        assertThat(body.path("message").asText()).isEqualTo("내 응답 목록을 조회했습니다.");
        assertThat(body.path("data").path("totalPromotionReward").asInt()).isEqualTo(700);
        assertThat(body.path("data").path("answers")).hasSize(2);
        assertThat(body.path("data").path("answers").get(0).path("testId").asLong()).isEqualTo(secondTest.getId());
        assertThat(body.path("data").path("answers").get(0).path("testName").asText()).isEqualTo("두 번째 테스트");
        assertThat(body.path("data").path("answers").get(0).path("createdAt").asText()).isEqualTo("2026.05.20");
        assertThat(body.path("data").path("answers").get(0).path("reward").asInt()).isEqualTo(400);
        assertThat(body.path("data").path("answers").get(1).path("testId").asLong()).isEqualTo(firstTest.getId());
        assertThat(body.path("data").path("answers").get(1).path("createdAt").asText()).isEqualTo("2026.05.13");
    }
}
