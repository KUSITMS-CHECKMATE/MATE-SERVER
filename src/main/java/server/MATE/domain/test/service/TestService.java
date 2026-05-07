package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.test.dto.response.TestUpdateResponse;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.event.ImageCleanupEvent;
import server.MATE.global.image.event.ImageDeleteEvent;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TestSummaryResponse> listTests() {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");
        return testRepository.findAll(newestFirst).stream()
                .map(TestSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetailResponse getTest(Long testId) {
        Test test = testRepository.findById(testId)
                .filter(t -> t.getDeletedAt() == null)
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
        eventPublisher.publishEvent(new ImageCleanupEvent(imageKeys));
        testRepository.save(test);

        return TestCreateResponse.from(test);
    }

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
                eventPublisher.publishEvent(new ImageCleanupEvent(addedKeys));
            }
            if (!removedKeys.isEmpty()) {
                eventPublisher.publishEvent(new ImageDeleteEvent(removedKeys));
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

    public void deleteTest(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        List<String> imageKeys = List.copyOf(test.getImageKeys());
        if (!imageKeys.isEmpty()) {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageKeys));
        }

        test.delete(LocalDateTime.now(clock));
    }
}
