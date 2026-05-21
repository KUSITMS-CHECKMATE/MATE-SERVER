package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.report.dto.response.CardSortingCardResult;
import server.MATE.domain.report.dto.response.CardSortingGroupResult;
import server.MATE.domain.report.dto.response.CardSortingReportResult;
import server.MATE.domain.report.service.ReportHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CardSortingReportHandler implements ReportHandler {

    private final CardSortingRepository cardSortingRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.CARD_SORTING;
    }

    @Override
    public Map<Long, Object> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, CardSorting> cardSortingMap = cardSortingRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(CardSorting::getId, cs -> cs));

        Map<Long, Object> result = new LinkedHashMap<>();
        for (Question question : questions) {
            CardSorting cardSorting = cardSortingMap.get(question.getId());
            if (cardSorting == null) continue;
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForCardSorting(cardSorting, answers));
        }
        return result;
    }

    private CardSortingReportResult computeForCardSorting(CardSorting cardSorting, List<Answer> answers) {
        // category → card → count
        Map<String, Map<String, Integer>> categoryCardCounts = new LinkedHashMap<>();
        for (String category : cardSorting.getCategories()) {
            Map<String, Integer> cardCounts = new LinkedHashMap<>();
            for (String card : cardSorting.getCards()) {
                cardCounts.put(card, 0);
            }
            categoryCardCounts.put(category, cardCounts);
        }

        int total = answers.size();
        for (Answer answer : answers) {
            for (Map<String, Object> group : extractGroups(answer.getAnswer())) {
                String category = (String) group.get("category");
                Map<String, Integer> cardCounts = categoryCardCounts.get(category);
                if (cardCounts == null || !(group.get("cardNames") instanceof List<?> rawList)) continue;
                for (Object item : rawList) {
                    if (item instanceof String cardName) {
                        cardCounts.merge(cardName, 1, Integer::sum);
                    }
                }
            }
        }

        List<CardSortingGroupResult> groups = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : categoryCardCounts.entrySet()) {
            List<Map.Entry<String, Integer>> sorted = entry.getValue().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                    .toList();

            List<CardSortingCardResult> cards = new ArrayList<>();
            int rank = 1;
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0 && !sorted.get(i).getValue().equals(sorted.get(i - 1).getValue())) {
                    rank = i + 1;
                }
                Map.Entry<String, Integer> e = sorted.get(i);
                cards.add(new CardSortingCardResult(rank, e.getKey(), e.getValue(), ReportHandlerUtils.toPercentage(e.getValue(), total)));
            }
            groups.add(new CardSortingGroupResult(entry.getKey(), cards));
        }

        return new CardSortingReportResult(groups);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractGroups(Map<String, Object> answerMap) {
        List<Map<String, Object>> groups = (List<Map<String, Object>>) answerMap.get("groups");
        return groups != null ? groups : List.of();
    }
}
