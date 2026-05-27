package server.MATE.domain.report.service.excel.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportExcelExportContextLoader {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final QuestionRepository questionRepository;
    private final ReportRepository reportRepository;
    private final AnswerRepository answerRepository;
    private final ObjectiveRepository objectiveRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final ScaleRepository scaleRepository;
    private final CardSortingRepository cardSortingRepository;
    private final TreeTestRepository treeTestRepository;

    public ReportExcelExportContext load(Long testId, Long makerId) {
        Test test = reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        List<Question> questions = questionRepository.findQuestionsInTest(testId);
        List<QuestionSummaryItem> questionSummaries = questions.stream()
                .map(question -> new QuestionSummaryItem(
                        question.getId(),
                        question.getSequence(),
                        question.getTitle(),
                        question.getQuestionType()
                ))
                .toList();
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        Map<Long, Question> questionById = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        Map<Long, Map<String, Object>> reportResultByQuestionId = reportRepository.findAllByTestId(testId)
                .stream()
                .collect(Collectors.toMap(Report::getQuestionId, Report::getResult));

        Map<Long, List<Answer>> answersByQuestionId = groupAnswersByQuestionId(
                questionIds.isEmpty()
                        ? List.of()
                        : answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(questionIds)
        );

        Map<QuestionType, List<Long>> questionIdsByType = questionSummaries.stream()
                .collect(Collectors.groupingBy(
                        QuestionSummaryItem::type,
                        Collectors.mapping(QuestionSummaryItem::questionId, Collectors.toList())
                ));

        Map<Long, Objective> objectiveByQuestionId = objectiveRepository
                .findAllByIdIn(questionIdsByType.getOrDefault(QuestionType.OBJECTIVE, List.of()))
                .stream()
                .collect(Collectors.toMap(Objective::getId, Function.identity()));
        Map<Long, FiveSecond> fiveSecondByQuestionId = fiveSecondRepository
                .findAllByIdIn(questionIdsByType.getOrDefault(QuestionType.FIVE_SECOND, List.of()))
                .stream()
                .collect(Collectors.toMap(FiveSecond::getId, Function.identity()));
        Map<Long, Scale> scaleByQuestionId = scaleRepository
                .findAllByIdIn(questionIdsByType.getOrDefault(QuestionType.SCALE, List.of()))
                .stream()
                .collect(Collectors.toMap(Scale::getId, Function.identity()));
        Map<Long, CardSorting> cardSortingByQuestionId = cardSortingRepository
                .findAllByIdIn(questionIdsByType.getOrDefault(QuestionType.CARD_SORTING, List.of()))
                .stream()
                .collect(Collectors.toMap(CardSorting::getId, Function.identity()));
        Map<Long, Map<Long, TreeTest>> treeNodeByIdByQuestionId = groupTreeNodesByQuestionId(
                treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(
                        questionIdsByType.getOrDefault(QuestionType.TREE_TEST, List.of())
                )
        );

        return ReportExcelExportContext.builder()
                .test(test)
                .testId(testId)
                .questionSummaries(questionSummaries)
                .questionById(questionById)
                .reportResultByQuestionId(reportResultByQuestionId)
                .answersByQuestionId(answersByQuestionId)
                .objectiveByQuestionId(objectiveByQuestionId)
                .fiveSecondByQuestionId(fiveSecondByQuestionId)
                .scaleByQuestionId(scaleByQuestionId)
                .cardSortingByQuestionId(cardSortingByQuestionId)
                .treeNodeByIdByQuestionId(treeNodeByIdByQuestionId)
                .build();
    }

    private Map<Long, List<Answer>> groupAnswersByQuestionId(List<Answer> answers) {
        Map<Long, List<Answer>> grouped = new HashMap<>();
        for (Answer answer : answers) {
            grouped.computeIfAbsent(answer.getQuestionId(), ignored -> new ArrayList<>()).add(answer);
        }
        grouped.values().forEach(questionAnswers ->
                questionAnswers.sort(Comparator.comparing(Answer::getParticipationId))
        );
        return grouped;
    }

    private Map<Long, Map<Long, TreeTest>> groupTreeNodesByQuestionId(List<TreeTest> treeNodes) {
        Map<Long, Map<Long, TreeTest>> grouped = new HashMap<>();
        for (TreeTest treeNode : treeNodes) {
            Long questionId = treeNode.getQuestion().getId();
            grouped.computeIfAbsent(questionId, ignored -> new HashMap<>())
                    .put(treeNode.getId(), treeNode);
        }
        return grouped;
    }
}
