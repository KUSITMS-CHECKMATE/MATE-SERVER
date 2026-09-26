package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.report.event.ReportReaggregationRequestedEvent;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

// FAILED 리포트 재집계. 관리자 API와 리포트 채널 버튼이 공용으로 사용함
@Service
@RequiredArgsConstructor
public class ReportReaggregateService {

    private final TestRepository testRepository;
    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportStatus reaggregate(Long testId, String requester) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (test.getTestStatus() != TestStatus.COMPLETED) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }
        if (test.getReportStatus() != ReportStatus.FAILED) {
            throw new BaseException(BaseErrorCode.REPORT_013);
        }

        // 벌크 삭제가 영속성 컨텍스트를 비우므로 상태 변경을 먼저 반영함(flushAutomatically)
        test.restartReportAggregation();
        // 일부만 남은 행이 불일치 검사에 걸려 곧바로 재실패하는 것 방지
        reportRepository.deleteAllByTestId(testId);

        // "재집계 중" 표시가 집계 시작보다 먼저 처리되도록 요청 이벤트를 먼저 발행함
        eventPublisher.publishEvent(new ReportReaggregationRequestedEvent(testId, requester));
        eventPublisher.publishEvent(new TestCompleteEvent(testId));
        return ReportStatus.IN_PROGRESS;
    }
}
