package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.test.dto.response.TestUpdateResponse;
import server.MATE.domain.test.entity.ApprovalStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.config.CacheNames;
import server.MATE.global.storage.event.FileCleanupEvent;
import server.MATE.global.storage.event.FileDeleteEvent;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TestLikeRepository testLikeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TestSummaryResponse> listTests(Long userId) {
        List<Test> tests = testRepository.findAllByApprovalStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ApprovalStatus.ACCEPTED);
        Set<Long> likedTestIds = findLikedTestIds(userId, tests);

        return tests.stream()
                .map(test -> TestSummaryResponse.from(test, likedTestIds.contains(test.getId())))
                .toList();
    }

    @Cacheable(value = CacheNames.LOOKUP, key = "#testId")
    @Transactional(readOnly = true)
    public TestDetailResponse getTest(Long testId) {
        Test test = testRepository.findByIdAndApprovalStatusAndDeletedAtIsNull(testId, ApprovalStatus.ACCEPTED)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        return TestDetailResponse.from(test);
    }

    public TestCreateResponse createTest(TestCreateRequest request, Long makerId) {
        List<String> imageKeys = request.imageKeys() != null ? request.imageKeys() : List.of();

        Test test = Test.builder()
                .makerId(makerId)
                .title(request.title())
                .description(request.description())
                .serviceName(request.serviceName())
                .serviceDescription(request.serviceDescription())
                .imageKeys(imageKeys)
                .build();

        test.addCategories(request.categories());
        eventPublisher.publishEvent(new FileCleanupEvent(imageKeys));
        testRepository.save(test);

        return TestCreateResponse.from(test);
    }

    @CacheEvict(value = CacheNames.LOOKUP, key = "#testId")
    public TestUpdateResponse updateTest(Long testId, TestUpdateRequest request, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        List<String> newImageKeys = request.imageKeys();
        if (newImageKeys != null) {
            Set<String> oldKeySet = Set.copyOf(test.getImageKeys());
            Set<String> newKeySet = Set.copyOf(newImageKeys);

            List<String> addedKeys = newKeySet.stream()
                    .filter(key -> !oldKeySet.contains(key))
                    .toList();
            List<String> removedKeys = oldKeySet.stream()
                    .filter(key -> !newKeySet.contains(key))
                    .toList();

            if (!addedKeys.isEmpty()) {
                eventPublisher.publishEvent(new FileCleanupEvent(addedKeys));
            }
            if (!removedKeys.isEmpty()) {
                eventPublisher.publishEvent(new FileDeleteEvent(removedKeys));
            }
        }

        test.update(
                request.title(),
                request.description(),
                request.categories(),
                request.serviceName(),
                request.serviceDescription(),
                newImageKeys
        );

        testRepository.saveAndFlush(test);
        return TestUpdateResponse.from(test);
    }

    @CacheEvict(value = CacheNames.LOOKUP, key = "#testId")
    public void deleteTest(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        List<String> imageKeys = List.copyOf(test.getImageKeys());
        if (!imageKeys.isEmpty()) {
            eventPublisher.publishEvent(new FileDeleteEvent(imageKeys));
        }

        test.delete(LocalDateTime.now(clock));
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
