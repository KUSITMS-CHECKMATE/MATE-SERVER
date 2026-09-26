package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestProgressResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestApprovedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.service.FileStorageService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminTestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 59, 59);

    private final TestRepository testRepository;
    private final FileStorageService fileStorageService;
    private final TestCloseScheduler testCloseScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminTestListResponse listTests(TestStatus status, int page, int size) {
        TestStatus filterStatus = status == null ? TestStatus.WAITING : status;
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        List<Test> tests = testRepository.findByStatusForAdmin(filterStatus, offset, safeSize);
        long totalCount = testRepository.countByStatusForAdmin(filterStatus);
        return AdminTestListResponse.of(safePage, safeSize, totalCount, tests);
    }

    @Transactional(readOnly = true)
    public AdminTestDetailResponse getTest(Long testId) {
        Test test = testRepository.findWithCategoriesByIdForAdmin(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        return AdminTestDetailResponse.from(test, toImageUrls(test.getImageKeys()));
    }

    @Transactional(readOnly = true)
    public AdminTestProgressResponse getProgress(Long testId) {
        Test test = testRepository.findWithCategoriesByIdForAdmin(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        List<String> imageKeys = test.getImageKeys();
        String thumbnailUrl = imageKeys.isEmpty() ? null : fileStorageService.generateDownloadUrl(imageKeys.get(0));
        return AdminTestProgressResponse.from(test, thumbnailUrl);
    }

    public AdminTestStatusResponse approve(Long testId) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (test.getTestStatus() != TestStatus.WAITING && test.getTestStatus() != TestStatus.REJECTED) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }

        test.approve();

        scheduleCloseAfterCommit(test.getId(), test.getClosedAt());

        eventPublisher.publishEvent(new TestApprovedEvent(test.getId(), test.getTitle()));

        return new AdminTestStatusResponse(testId, test.getTestStatus());
    }

    public AdminTestStatusResponse reject(Long testId, String reason) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (test.getTestStatus() != TestStatus.WAITING && test.getTestStatus() != TestStatus.IN_PROGRESS) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }

        test.reject(reason);
        return new AdminTestStatusResponse(testId, test.getTestStatus());
    }

    public AdminTestStatusResponse reopen(Long testId, LocalDate closedDate) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (test.getTestStatus() != TestStatus.COMPLETED) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }
        if (!closedDate.isAfter(LocalDate.now(clock.withZone(KST)))) {
            throw new BaseException(BaseErrorCode.TEST_011);
        }
        if (test.getPplCount() >= test.getGoalPpl().longValue()) {
            throw new BaseException(BaseErrorCode.TEST_012);
        }
        if (test.getReportStatus() == ReportStatus.IN_PROGRESS) {
            throw new BaseException(BaseErrorCode.TEST_013);
        }
        if (isRefundStarted(testId)) {
            throw new BaseException(BaseErrorCode.TEST_014);
        }

        // 테스트 생성 시 마감 기한 규칙(해당 날짜 23:59:59)과 통일용
        LocalDateTime closedAt = closedDate.atTime(CLOSING_TIME);

        // report.created_at(Auditing)과 같은 시계·정밀도로 기록해 재집계 판별 기준 일치용
        test.reopen(closedAt, LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS));
        scheduleCloseAfterCommit(testId, closedAt);
        return new AdminTestStatusResponse(testId, test.getTestStatus());
    }

    private boolean isRefundStarted(Long testId) {
        return paymentRepository.findByTestId(testId)
                .map(Payment::getPayStatus)
                .filter(status -> status == PayStatus.REFUND_PENDING || status == PayStatus.REFUNDED)
                .isPresent();
    }

    private void scheduleCloseAfterCommit(Long testId, LocalDateTime closedAt) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                testCloseScheduler.schedule(testId, closedAt);
            }
        });
    }

    private List<String> toImageUrls(List<String> keys) {
        return keys.stream()
                .map(fileStorageService::generateDownloadUrl)
                .toList();
    }
}
