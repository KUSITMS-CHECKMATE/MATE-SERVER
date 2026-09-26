package server.MATE.domain.report.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.entity.Report;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("existsByTestIdAndCreatedAtBefore: 기준 시각 이전에 만든 같은 테스트의 리포트가 있을 때만 true")
    void existsByTestIdAndCreatedAtBefore() {
        Report saved = reportRepository.saveAndFlush(report(10L, 101L));
        LocalDateTime createdAt = saved.getCreatedAt();

        assertThat(reportRepository.existsByTestIdAndCreatedAtBefore(10L, createdAt.plusSeconds(1))).isTrue();
        assertThat(reportRepository.existsByTestIdAndCreatedAtBefore(10L, createdAt)).isFalse();
        assertThat(reportRepository.existsByTestIdAndCreatedAtBefore(20L, createdAt.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("deleteAllByTestId 후 같은 질문의 리포트를 같은 트랜잭션에서 다시 저장할 수 있다")
    void deleteAllThenReinsertSameQuestion_succeeds() {
        reportRepository.saveAndFlush(report(10L, 101L));

        reportRepository.deleteAllByTestId(10L);
        reportRepository.saveAndFlush(report(10L, 101L));

        assertThat(reportRepository.countByTestId(10L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제 없이 같은 질문의 리포트를 다시 저장하면 유니크 제약 위반")
    void reinsertWithoutDelete_violatesUniqueConstraint() {
        reportRepository.saveAndFlush(report(10L, 101L));

        assertThatThrownBy(() -> reportRepository.saveAndFlush(report(10L, 101L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Report report(Long testId, Long questionId) {
        return Report.builder()
                .testId(testId)
                .questionId(questionId)
                .questionType(QuestionType.SUBJECTIVE)
                .result(Map.of("texts", "응답"))
                .build();
    }
}
