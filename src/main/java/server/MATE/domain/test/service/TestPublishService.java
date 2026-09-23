package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PublishStatus;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCreatedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.event.FileCleanupEvent;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TestPublishService {

    private final TestDraftRepository testDraftRepository;
    private final PaymentRepository paymentRepository;
    private final TestRepository testRepository;
    private final QuestionService questionService;
    private final ApplicationEventPublisher eventPublisher;
    private final TestDraftValidator testDraftValidator;

    public Long publish(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));

        if (payment.getTestId() != null) {
            payment.markPublished(payment.getTestId());
            return payment.getTestId();
        }

        TestDraft draft = testDraftRepository.findByIdForUpdate(payment.getDraftId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));

        if (draft.getPublishedTestId() != null) {
            payment.markPublished(draft.getPublishedTestId());
            return draft.getPublishedTestId();
        }

        // 이미 발행된 케이스(위 두 분기)가 아닌데 FAILED로 표시돼 있으면, 호출부가 필터링을
        // 누락했어도 여기서 최종적으로 막는다.
        if (payment.getPublishStatus() == PublishStatus.FAILED) {
            throw new BaseException(BaseErrorCode.RETRY_LIMIT_EXCEEDED);
        }

        if (payment.getPayStatus() != PayStatus.PAY_SUCCEEDED) {
            throw new BaseException(BaseErrorCode.PAYMENT_003);
        }

        QuestionCreateRequest questionCreateRequest = testDraftValidator.validateForPublish(draft);
        draft.markPublishing();

        Test test = testRepository.save(Test.builder()
                .makerId(draft.getMakerId())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .serviceName(draft.getServiceName())
                .serviceDescription(draft.getServiceDescription())
                .imageKeys(draft.getImageKeys())
                .goalPpl(payment.getGoalPpl())
                .reward(payment.getReward())
                .closedAt(draft.getClosedAt())
                .testStatus(TestStatus.WAITING)
                .build());

        if (draft.getCategories() != null && !draft.getCategories().isEmpty()) {
            test.addCategories(toCategories(draft.getCategories()));
        }
        if (draft.getImageKeys() != null && !draft.getImageKeys().isEmpty()) {
            eventPublisher.publishEvent(new FileCleanupEvent(draft.getImageKeys()));
        }

        questionService.createQuestions(test.getId(), draft.getMakerId(), questionCreateRequest);

        payment.markPublished(test.getId());
        draft.markPublished(test.getId());
        testDraftRepository.delete(draft);

        eventPublisher.publishEvent(new TestCreatedEvent(test.getId(), test.getTitle(), test.getReward(), test.getCreatedAt()));

        return test.getId();
    }

    private List<Category> toCategories(List<String> categoryNames) {
        return categoryNames.stream()
                .map(Category::valueOf)
                .toList();
    }
}
