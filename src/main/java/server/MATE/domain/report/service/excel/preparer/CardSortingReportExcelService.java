package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.report.excel.cardsorting.CardSortingCategoryStatRow;
import server.MATE.domain.report.excel.cardsorting.CardSortingReportExcelData;
import server.MATE.domain.report.excel.cardsorting.CardSortingRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CardSortingReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final CardSortingRepository cardSortingRepository;

    public CardSortingReportExcelData prepareData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.CARD_SORTING, BaseErrorCode.REPORT_006
        );
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);

        CardSorting cardSorting = cardSortingRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        List<CardSortingRespondentRow> respondents = buildRespondentRows(cardSorting, answers);
        List<CardSortingCategoryStatRow> categoryStats = ReportExcelResultMapper.toCardSortingCategoryStats(reportResult);

        return new CardSortingReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                categoryStats
        );
    }

    private List<CardSortingRespondentRow> buildRespondentRows(CardSorting cardSorting, List<Answer> answers) {
        Map<String, Integer> cardNumberByName = new HashMap<>();
        List<String> cards = cardSorting.getCards();
        for (int index = 0; index < cards.size(); index++) {
            cardNumberByName.put(cards.get(index), index + 1);
        }

        List<CardSortingRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            Map<String, List<String>> cardsByCategory = extractCardsByCategory(answer.getAnswer());
            for (String category : cardSorting.getCategories()) {
                for (String cardName : cardsByCategory.getOrDefault(category, List.of())) {
                    rows.add(new CardSortingRespondentRow(
                            answer.getParticipationId(),
                            category,
                            cardNumberByName.getOrDefault(cardName, 0)
                    ));
                }
            }
        }
        return rows;
    }

    private Map<String, List<String>> extractCardsByCategory(Map<String, Object> answerMap) {
        Object groupsObject = answerMap.get("groups");
        if (!(groupsObject instanceof List<?> groups)) {
            return Map.of();
        }

        Map<String, List<String>> cardsByCategory = new LinkedHashMap<>();
        for (Object groupObject : groups) {
            if (!(groupObject instanceof Map<?, ?> groupMap)) {
                continue;
            }
            Object categoryObject = groupMap.get("category");
            Object cardNamesObject = groupMap.get("cardNames");
            if (!(categoryObject instanceof String category) || !(cardNamesObject instanceof List<?> rawCardNames)) {
                continue;
            }
            List<String> cardNames = new ArrayList<>();
            for (Object cardNameObject : rawCardNames) {
                if (cardNameObject instanceof String cardName) {
                    cardNames.add(cardName);
                }
            }
            cardsByCategory.put(category, cardNames);
        }
        return cardsByCategory;
    }
}
