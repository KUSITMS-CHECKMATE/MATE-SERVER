package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.CardSortingCategoryStatRow;
import server.MATE.domain.report.excel.CardSortingReportExcelData;
import server.MATE.domain.report.excel.CardSortingReportExcelWriter;
import server.MATE.domain.report.excel.CardSortingRespondentRow;
import server.MATE.domain.report.service.handler.CardSortingReportHandler;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
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

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final CardSortingRepository cardSortingRepository;
    private final AnswerRepository answerRepository;
    private final CardSortingReportHandler cardSortingReportHandler;
    private final CardSortingReportExcelWriter cardSortingReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        Question question = questionRepository.findByIdAndTestIdAndDeletedAtIsNull(questionId, testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        if (question.getQuestionType() != QuestionType.CARD_SORTING) {
            throw new BaseException(BaseErrorCode.REPORT_006);
        }

        CardSorting cardSorting = cardSortingRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<Answer> answers = answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(questionId);
        Map<String, Object> report = cardSortingReportHandler.compute(
                List.of(question),
                Map.of(questionId, answers)
        ).get(questionId);

        List<CardSortingRespondentRow> respondents = buildRespondentRows(cardSorting, answers);
        List<CardSortingCategoryStatRow> categoryStats = buildCategoryStatRows(report);

        CardSortingReportExcelData data = new CardSortingReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                categoryStats
        );

        byte[] content = cardSortingReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
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

    @SuppressWarnings("unchecked")
    private List<CardSortingCategoryStatRow> buildCategoryStatRows(Map<String, Object> report) {
        Object byCategoryObject = report.get("byCategory");
        if (!(byCategoryObject instanceof List<?> byCategory)) {
            return List.of();
        }

        List<CardSortingCategoryStatRow> rows = new ArrayList<>();
        for (Object categoryObject : byCategory) {
            if (!(categoryObject instanceof Map<?, ?> categoryMap)) {
                continue;
            }
            String categoryName = String.valueOf(categoryMap.get("category"));
            Object cardsObject = categoryMap.get("cards");
            if (!(cardsObject instanceof List<?> cards)) {
                continue;
            }
            for (Object cardObject : cards) {
                if (!(cardObject instanceof Map<?, ?> cardMap)) {
                    continue;
                }
                int rank = ((Number) cardMap.get("rank")).intValue();
                String cardName = String.valueOf(cardMap.get("cardName"));
                double ratio = ((Number) cardMap.get("ratio")).doubleValue();
                rows.add(new CardSortingCategoryStatRow(
                        categoryName,
                        String.format("카드 %d순위 %s", rank, cardName),
                        formatRatioPercent(ratio)
                ));
            }
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
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

    private String formatRatioPercent(double ratio) {
        return String.format("%.2f%%", ratio * 100);
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-card-sorting-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
