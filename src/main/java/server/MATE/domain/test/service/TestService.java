package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.test.dto.response.*;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.FileStorageService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TestRepository testRepository;
    private final TestLikeRepository testLikeRepository;
    private final ParticipationRepository participationRepository;
    private final FileStorageService fileStorageService;
    private final TestCloseProcessor testCloseProcessor;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TestSummaryListResponse listTests(Long userId) {
        LocalDateTime threshold = LocalDate.now(clock.withZone(KST)).atStartOfDay();
        List<Test> tests = testRepository.findAvailableTestsForUser(
                List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING),
                threshold,
                userId
        );
        return TestSummaryListResponse.from(toSummaryResponses(userId, tests));
    }

    @Transactional(readOnly = true)
    public MyTestSummaryResponse listMyTests(Long makerId) {
        List<Test> tests = testRepository.findByMakerId(makerId);
        List<MyTestSummaryItem> items = tests.stream()
                .map(MyTestSummaryItem::from)
                .toList();
        return MyTestSummaryResponse.from(items);
    }

    @Transactional(readOnly = true)
    public LikedTestSummaryResponse listLikedTests(Long userId) {
        List<Test> tests = testRepository.findLikedTests(
                userId,
                List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING, TestStatus.COMPLETED)
        );
        List<LikedTestSummaryItem> items = tests.stream()
                .map(test -> LikedTestSummaryItem.from(test, toThumbnailUrl(test.getImageKeys())))
                .toList();
        return LikedTestSummaryResponse.from(items);
    }

    @Transactional(readOnly = true)
    public TestDetailResponse getTest(Long testId, Long userId) {
        Test test = testRepository.findWithCategoriesById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        boolean hasResponded = participationRepository.existsActiveParticipation(testId, userId);
        return TestDetailResponse.from(test, toImageUrls(test.getImageKeys()), hasResponded);
    }

    public TestStatusUpdateResponse updateTestStatus(Long testId, Long userId, Role role, TestStatus status) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        switch (status) {
            case IN_PROGRESS -> {
                if (role != Role.ADMIN) {
                    throw new BaseException(BaseErrorCode.COMMON_009);
                }
                if (test.getTestStatus() != TestStatus.WAITING) {
                    throw new BaseException(BaseErrorCode.TEST_007);
                }
                test.start();
                scheduleClose(test.getId(), test.getClosedAt());
            }
            case REJECTED -> {
                if (role != Role.ADMIN) {
                    throw new BaseException(BaseErrorCode.COMMON_009);
                }
                if (test.getTestStatus() != TestStatus.WAITING) {
                    throw new BaseException(BaseErrorCode.TEST_007);
                }
                test.reject();
            }
            default -> throw new BaseException(BaseErrorCode.COMMON_002);
        }

        return new TestStatusUpdateResponse(testId, test.getTestStatus());
    }

    public TestLikeResponse likeTest(Long testId, Long userId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!testLikeRepository.existsByUserIdAndTestId(userId, testId)) {
            testLikeRepository.save(TestLike.builder()
                    .userId(userId)
                    .testId(testId)
                    .build());
            test.incrementLikeCount();
        }

        return new TestLikeResponse(test.getId(), true, test.getLikeCount());
    }

    public TestLikeResponse unlikeTest(Long testId, Long userId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        testLikeRepository.findByUserIdAndTestId(userId, testId)
                .ifPresent(testLike -> {
                    testLikeRepository.delete(testLike);
                    test.decrementLikeCount();
                });

        return new TestLikeResponse(test.getId(), false, test.getLikeCount());
    }

    @Transactional
    public void closeTestByMaker(Long testId, Long makerId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
        if (test.getTestStatus() != TestStatus.IN_PROGRESS) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }

        test.markClosedByMaker();
        testCloseProcessor.processClose(test);
    }

    @Transactional
    public void waiveRefund(Long testId, Long makerId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
        if (test.getTestStatus() != TestStatus.IN_PROGRESS) {
            throw new BaseException(BaseErrorCode.TEST_007);
        }

        test.waiveRefund();
    }

    private List<String> toImageUrls(List<String> keys) {
        return keys.stream()
                .map(fileStorageService::generateDownloadUrl)
                .toList();
    }

    private String toThumbnailUrl(List<String> keys) {
        return keys.isEmpty() ? null : fileStorageService.generateDownloadUrl(keys.getFirst());
    }

    private List<TestSummaryResponse> toSummaryResponses(Long userId, List<Test> tests) {
        Set<Long> likedTestIds = findLikedTestIds(userId, tests);

        return tests.stream()
                .map(test -> TestSummaryResponse.from(test, likedTestIds.contains(test.getId()), toThumbnailUrl(test.getImageKeys())))
                .toList();
    }

    private Set<Long> findLikedTestIds(Long userId, List<Test> tests) {
        if (tests.isEmpty()) {
            return Set.of();
        }

        List<Long> testIds = tests.stream()
                .map(Test::getId)
                .toList();
        return new HashSet<>(testLikeRepository.findLikedTestIds(userId, testIds));
    }

    private void scheduleClose(Long testId, LocalDateTime closedAt) {
        if (closedAt == null) {
            log.warn("테스트 {} 마감 시각이 없어 자동 종료 예약을 건너뜁니다", testId);
            return;
        }

        Instant triggerAt = closedAt.atZone(KST).toInstant();
        taskScheduler.schedule(() -> {
            try {
                testCloseProcessor.process(testId);
            } catch (Exception e) {
                log.error("테스트 {} 자동 종료 실패 - 자정 스케줄러에서 재처리됩니다", testId, e);
            }
        }, triggerAt);
        log.info("테스트 {} 자동 종료 예약 완료 - {}", testId, closedAt);
    }
}
