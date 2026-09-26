package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long>, TestQueryRepository {

    List<Test> findByTestStatusAndDeletedAtIsNull(TestStatus testStatus);

    List<Test> findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            TestStatus testStatus, LocalDateTime threshold);

    long countByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(TestStatus testStatus, LocalDateTime threshold);

    // 재개 등으로 상태가 바뀐 뒤 늦게 끝난 내보내기가 옛 파일 위치를 다시 쓰는 것 방지용 조건부 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Test t
            set t.pdfKey = :pdfKey
            where t.id = :testId
              and t.deletedAt is null
              and t.testStatus = server.MATE.domain.test.entity.TestStatus.COMPLETED
              and t.reportStatus = server.MATE.domain.test.entity.ReportStatus.COMPLETED
            """)
    int updatePdfKeyIfReportCompleted(@Param("testId") Long testId, @Param("pdfKey") String pdfKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Test t
            set t.excelKey = :excelKey
            where t.id = :testId
              and t.deletedAt is null
              and t.testStatus = server.MATE.domain.test.entity.TestStatus.COMPLETED
              and t.reportStatus = server.MATE.domain.test.entity.ReportStatus.COMPLETED
            """)
    int updateExcelKeyIfReportCompleted(@Param("testId") Long testId, @Param("excelKey") String excelKey);
}
