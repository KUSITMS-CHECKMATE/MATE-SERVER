package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.dto.response.LikedTestSummaryItem;
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.response.LikedTestSummaryResponse;
import server.MATE.domain.test.dto.response.MyTestSummaryItem;
import server.MATE.domain.test.dto.response.MyTestSummaryResponse;
import server.MATE.domain.test.dto.response.TestSummaryListResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.FileStorageService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TestLikeRepository testLikeRepository;
    private final ParticipationRepository participationRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public TestSummaryListResponse listTests(Long userId) {
        List<Test> tests = testRepository.findAllByTestStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
                List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING)
        );
        LocalDate today = LocalDate.now();
        List<Test> participatableTests = tests.stream()
                .filter(test -> test.isParticipationPeriodOpen(today))
                .toList();
        return TestSummaryListResponse.from(toSummaryResponses(userId, participatableTests));
    }

    @Transactional(readOnly = true)
    public MyTestSummaryResponse listMyTests(Long makerId) {
        List<Test> tests = testRepository.findAllByMakerIdAndDeletedAtIsNullOrderByCreatedAtDesc(makerId);
        List<MyTestSummaryItem> items = tests.stream()
                .map(MyTestSummaryItem::from)
                .toList();
        return MyTestSummaryResponse.from(items);
    }

    @Transactional(readOnly = true)
    public LikedTestSummaryResponse listLikedTests(Long userId) {
        List<Test> tests = testRepository.findLikedTestsByUserId(
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
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        boolean hasResponded = participationRepository.existsByTestIdAndTesterIdAndDeletedAtIsNull(testId, userId);
        return TestDetailResponse.from(test, toImageUrls(test.getImageKeys()), hasResponded);
    }

    public TestLikeResponse likeTest(Long testId, Long userId) {
        Test test = testRepository.findByIdAndDeletedAtIsNullForUpdate(testId)
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
        Test test = testRepository.findByIdAndDeletedAtIsNullForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        testLikeRepository.findByUserIdAndTestId(userId, testId)
                .ifPresent(testLike -> {
                    testLikeRepository.delete(testLike);
                    test.decrementLikeCount();
                });

        return new TestLikeResponse(test.getId(), false, test.getLikeCount());
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
}
