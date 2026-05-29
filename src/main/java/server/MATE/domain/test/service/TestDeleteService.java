package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.question.entity.*;
import server.MATE.domain.question.repository.*;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.dto.request.TestDeleteMode;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestCategoryRepository;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.security.config.AdminProperties;
import server.MATE.global.storage.event.FileDeleteEvent;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TestDeleteService {

    private final TestRepository testRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ParticipationRepository participationRepository;
    private final ReportRepository reportRepository;
    private final PaymentRepository paymentRepository;
    private final PromotionRewardRepository promotionRewardRepository;
    private final TestCategoryRepository testCategoryRepository;
    private final TestLikeRepository testLikeRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ObjectiveOptionRepository objectiveOptionRepository;
    private final SubjectiveRepository subjectiveRepository;
    private final AbTestRepository abTestRepository;
    private final ScaleRepository scaleRepository;
    private final CardSortingRepository cardSortingRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final FiveSecondOptionRepository fiveSecondOptionRepository;
    private final TreeTestRepository treeTestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminProperties adminProperties;
    private final Clock clock;

    public void deleteTest(Long testId, Role role, TestDeleteMode mode, String hardDeleteKey) {
        validateAdmin(role);

        if (mode == TestDeleteMode.HARD) {
            Test test = testRepository.findByIdIncludingDeleted(testId)
                    .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
            validateHardDeleteKey(hardDeleteKey);
            hardDeleteTest(test);
            return;
        }

        if (mode == TestDeleteMode.SOFT) {
            testRepository.findActiveById(testId)
                    .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
            softDeleteTest(testId);
            return;
        }

        throw new BaseException(BaseErrorCode.COMMON_002);
    }

    private void softDeleteTest(Long testId) {
        LocalDateTime deletedAt = LocalDateTime.now(clock);
        List<Question> questions = questionRepository.findQuestionsInTest(testId);
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        reportRepository.softDeleteAllByTestId(testId, deletedAt);
        if (!questionIds.isEmpty()) {
            answerRepository.softDeleteAllByQuestionIds(questionIds, deletedAt);
        }
        participationRepository.softDeleteAllByTestId(testId, deletedAt);
        questionRepository.softDeleteByTestId(testId, deletedAt);
        testRepository.softDeleteById(testId, deletedAt);
    }

    private void hardDeleteTest(Test test) {
        List<Question> questions = questionRepository.findQuestionsInTestIncludingDeleted(test.getId());
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        List<String> fileKeys = new ArrayList<>(test.getImageKeys());

        promotionRewardRepository.deleteAllByTestId(test.getId());
        if (!questionIds.isEmpty()) {
            answerRepository.deleteAllByQuestionIds(questionIds);
        }
        reportRepository.deleteAllByTestId(test.getId());
        testLikeRepository.deleteAllByTestId(test.getId());

        deleteQuestionDetails(questions, fileKeys);

        if (!questions.isEmpty()) {
            questionRepository.deleteAllInBatch(questions);
        }
        testCategoryRepository.deleteAllByTestId(test.getId());
        participationRepository.deleteAllByTestId(test.getId());
        paymentRepository.deleteAllByTestId(test.getId());
        testRepository.delete(test);

        publishFileDeleteEvent(fileKeys);
    }

    private void deleteQuestionDetails(List<Question> questions, List<String> fileKeys) {
        Map<QuestionType, List<Long>> questionIdsByType = questions.stream()
                .collect(Collectors.groupingBy(
                        Question::getQuestionType,
                        Collectors.mapping(Question::getId, Collectors.toList())
                ));

        deleteObjectives(questionIdsByType.get(QuestionType.OBJECTIVE), fileKeys);
        deleteSubjectives(questionIdsByType.get(QuestionType.SUBJECTIVE), fileKeys);
        deleteAbTests(questionIdsByType.get(QuestionType.AB_TEST), fileKeys);
        deleteScales(questionIdsByType.get(QuestionType.SCALE), fileKeys);
        deleteCardSortings(questionIdsByType.get(QuestionType.CARD_SORTING));
        deleteFiveSeconds(questionIdsByType.get(QuestionType.FIVE_SECOND), fileKeys);
        deleteTreeTests(questionIdsByType.get(QuestionType.TREE_TEST));
    }

    private void deleteObjectives(List<Long> questionIds, List<String> fileKeys) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<Objective> objectives = objectiveRepository.findAllByQuestion_IdIn(questionIds);
        if (objectives.isEmpty()) {
            return;
        }

        objectives.stream()
                .flatMap(objective -> objective.getOptions().stream())
                .map(ObjectiveOption::getImageKey)
                .forEach(fileKeys::add);

        List<Long> objectiveIds = objectives.stream()
                .map(Objective::getId)
                .toList();

        objectiveOptionRepository.deleteAllByObjectiveIds(objectiveIds);
        objectiveRepository.deleteAllInBatch(objectives);
    }

    private void deleteSubjectives(List<Long> questionIds, List<String> fileKeys) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<Subjective> subjectives = subjectiveRepository.findAllByQuestion_IdIn(questionIds);
        subjectives.stream()
                .map(Subjective::getImageKey)
                .forEach(fileKeys::add);

        if (!subjectives.isEmpty()) {
            subjectiveRepository.deleteAllInBatch(subjectives);
        }
    }

    private void deleteAbTests(List<Long> questionIds, List<String> fileKeys) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<AbTest> abTests = abTestRepository.findAllByQuestion_IdIn(questionIds);
        abTests.forEach(abTest -> {
            fileKeys.add(abTest.getAImageKey());
            fileKeys.add(abTest.getBImageKey());
        });

        if (!abTests.isEmpty()) {
            abTestRepository.deleteAllInBatch(abTests);
        }
    }

    private void deleteScales(List<Long> questionIds, List<String> fileKeys) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<Scale> scales = scaleRepository.findAllByQuestion_IdIn(questionIds);
        scales.stream()
                .map(Scale::getImageKey)
                .forEach(fileKeys::add);

        if (!scales.isEmpty()) {
            scaleRepository.deleteAllInBatch(scales);
        }
    }

    private void deleteCardSortings(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<CardSorting> cardSortings = cardSortingRepository.findAllByQuestion_IdIn(questionIds);
        if (!cardSortings.isEmpty()) {
            cardSortingRepository.deleteAllInBatch(cardSortings);
        }
    }

    private void deleteFiveSeconds(List<Long> questionIds, List<String> fileKeys) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<FiveSecond> fiveSeconds = fiveSecondRepository.findAllByQuestion_IdIn(questionIds);
        if (fiveSeconds.isEmpty()) {
            return;
        }

        fiveSeconds.stream()
                .map(FiveSecond::getImageKey)
                .forEach(fileKeys::add);

        List<Long> fiveSecondIds = fiveSeconds.stream()
                .map(FiveSecond::getId)
                .toList();

        fiveSecondOptionRepository.deleteAllByFiveSecondIds(fiveSecondIds);
        fiveSecondRepository.deleteAllInBatch(fiveSeconds);
    }

    private void deleteTreeTests(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<TreeTest> nodes = treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(questionIds);
        nodes.stream()
                .sorted(Comparator.comparing((TreeTest node) -> node.getQuestion().getId())
                        .thenComparing(TreeTest::getDepth, Comparator.reverseOrder())
                        .thenComparing(TreeTest::getSequence)
                        .thenComparing(TreeTest::getId))
                .forEach(node -> treeTestRepository.deleteDirectById(node.getId()));
    }

    private void validateAdmin(Role role) {
        if (role != Role.ADMIN) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
    }

    private void validateHardDeleteKey(String hardDeleteKey) {
        if (hardDeleteKey == null || hardDeleteKey.isBlank()) {
            throw new BaseException(BaseErrorCode.TEST_009);
        }
        if (!hardDeleteKey.equals(adminProperties.hardDeleteKey())) {
            throw new BaseException(BaseErrorCode.TEST_010);
        }
    }

    private void publishFileDeleteEvent(List<String> fileKeys) {
        List<String> keysToDelete = fileKeys.stream()
                .filter(Objects::nonNull)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();

        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new FileDeleteEvent(keysToDelete));
        }
    }
}
