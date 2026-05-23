package server.MATE.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.report.entity.Report;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByTestId(Long testId);

    boolean existsByTestId(Long testId);

    long countByTestId(Long testId);
}
