package server.MATE.domain.test.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class TestRepositoryReportKeyTest {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestEntityManager em;

    private server.MATE.domain.test.entity.Test buildCompletedTest() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("완료된 테스트")
                .testStatus(TestStatus.IN_PROGRESS)
                .goalPpl(10)
                .reward(300)
                .closedAt(LocalDateTime.now().minusDays(1))
                .build();
        test.complete();
        return test;
    }

    @Test
    @DisplayName("updatePdfKeyIfReportCompleted: 리포트가 완료 상태면 PDF 키를 저장하고 1을 반환한다")
    void updatePdfKeyIfReportCompleted_reportCompleted_updatesKey() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        test.completeReportAggregation();
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        int updated = testRepository.updatePdfKeyIfReportCompleted(saved.getId(), "reports/pdf/1.pdf");

        assertThat(updated).isEqualTo(1);
        assertThat(testRepository.findActiveById(saved.getId()).orElseThrow().getPdfKey())
                .isEqualTo("reports/pdf/1.pdf");
    }

    @Test
    @DisplayName("updatePdfKeyIfReportCompleted: 재개된 테스트는 PDF 키를 저장하지 않고 0을 반환한다")
    void updatePdfKeyIfReportCompleted_reopenedTest_returnsZero() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        test.completeReportAggregation();
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        server.MATE.domain.test.entity.Test managed =
                em.getEntityManager().find(server.MATE.domain.test.entity.Test.class, saved.getId());
        managed.reopen(LocalDateTime.now().plusDays(3), LocalDateTime.now());
        em.persistAndFlush(managed);
        em.clear();

        int updated = testRepository.updatePdfKeyIfReportCompleted(saved.getId(), "reports/pdf/1.pdf");

        assertThat(updated).isEqualTo(0);
        assertThat(testRepository.findActiveById(saved.getId()).orElseThrow().getPdfKey()).isNull();
    }

    @Test
    @DisplayName("updatePdfKeyIfReportCompleted: 리포트가 집계 중이면 PDF 키를 저장하지 않고 0을 반환한다")
    void updatePdfKeyIfReportCompleted_reportInProgress_returnsZero() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        int updated = testRepository.updatePdfKeyIfReportCompleted(saved.getId(), "reports/pdf/1.pdf");

        assertThat(updated).isEqualTo(0);
        assertThat(testRepository.findActiveById(saved.getId()).orElseThrow().getPdfKey()).isNull();
    }

    @Test
    @DisplayName("updatePdfKeyIfReportCompleted: 삭제된 테스트는 PDF 키를 저장하지 않고 0을 반환한다")
    void updatePdfKeyIfReportCompleted_softDeletedTest_returnsZero() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        test.completeReportAggregation();
        test.delete(LocalDateTime.now());
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        int updated = testRepository.updatePdfKeyIfReportCompleted(saved.getId(), "reports/pdf/1.pdf");

        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("updateExcelKeyIfReportCompleted: 리포트가 완료 상태면 엑셀 키를 저장하고 1을 반환한다")
    void updateExcelKeyIfReportCompleted_reportCompleted_updatesKey() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        test.completeReportAggregation();
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        int updated = testRepository.updateExcelKeyIfReportCompleted(saved.getId(), "reports/excel/1.xlsx");

        assertThat(updated).isEqualTo(1);
        assertThat(testRepository.findActiveById(saved.getId()).orElseThrow().getExcelKey())
                .isEqualTo("reports/excel/1.xlsx");
    }

    @Test
    @DisplayName("updateExcelKeyIfReportCompleted: 재개된 테스트는 엑셀 키를 저장하지 않고 0을 반환한다")
    void updateExcelKeyIfReportCompleted_reopenedTest_returnsZero() {
        server.MATE.domain.test.entity.Test test = buildCompletedTest();
        test.startReportAggregation();
        test.completeReportAggregation();
        server.MATE.domain.test.entity.Test saved = em.persistAndFlush(test);
        em.clear();

        server.MATE.domain.test.entity.Test managed =
                em.getEntityManager().find(server.MATE.domain.test.entity.Test.class, saved.getId());
        managed.reopen(LocalDateTime.now().plusDays(3), LocalDateTime.now());
        em.persistAndFlush(managed);
        em.clear();

        int updated = testRepository.updateExcelKeyIfReportCompleted(saved.getId(), "reports/excel/1.xlsx");

        assertThat(updated).isEqualTo(0);
        assertThat(testRepository.findActiveById(saved.getId()).orElseThrow().getExcelKey()).isNull();
    }
}
