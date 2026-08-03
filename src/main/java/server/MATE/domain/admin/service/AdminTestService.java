package server.MATE.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.test.service.TestCloseScheduler;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminTestService {

    private final TestRepository testRepository;
    private final FileStorageService fileStorageService;
    private final TestCloseScheduler testCloseScheduler;

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

    public AdminTestStatusResponse approve(Long testId) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (test.getTestStatus() != TestStatus.WAITING && test.getTestStatus() != TestStatus.REJECTED) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }

        test.approve();

        Long approvedTestId = test.getId();
        LocalDateTime closedAt = test.getClosedAt();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                testCloseScheduler.schedule(approvedTestId, closedAt);
            }
        });

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

    private List<String> toImageUrls(List<String> keys) {
        return keys.stream()
                .map(fileStorageService::generateDownloadUrl)
                .toList();
    }
}
