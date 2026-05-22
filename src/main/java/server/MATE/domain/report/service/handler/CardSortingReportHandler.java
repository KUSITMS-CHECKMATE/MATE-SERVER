package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
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
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, CardSorting> cardSortingMap = cardSortingRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(CardSorting::getId, cs -> cs));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            CardSorting cardSorting = cardSortingMap.get(question.getId());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForCardSorting(cardSorting, answers));
        }
        return result;
    }

    private Map<String, Object> computeForCardSorting(CardSorting cardSorting, List<Answer> answers) {
        // category → card → count
        Map<String, Map<String, Integer>> categoryCardCounts = new LinkedHashMap<>();
        for (String category : cardSorting.getCategories()) {
            Map<String, Integer> cardCounts = new LinkedHashMap<>();
            for (String card : cardSorting.getCards()) {
                cardCounts.put(card, 0);
            }
            categoryCardCounts.put(category, cardCounts);
        }

        // card → category → count
        Map<String, Map<String, Integer>> cardCategoryCounts = new LinkedHashMap<>();
        for (String card : cardSorting.getCards()) {
            Map<String, Integer> catCounts = new LinkedHashMap<>();
            for (String category : cardSorting.getCategories()) {
                catCounts.put(category, 0);
            }
            cardCategoryCounts.put(card, catCounts);
        }

        int total = answers.size();
        for (Answer answer : answers) {
            for (Map<String, Object> group : extractGroups(answer.getAnswer())) {
                String category = (String) group.get("category");
                if (!(group.get("cardNames") instanceof List<?> rawList)) continue;
                for (Object item : rawList) {
                    if (item instanceof String cardName) {
                        Map<String, Integer> cardCounts = categoryCardCounts.get(category);
                        if (cardCounts != null) cardCounts.merge(cardName, 1, Integer::sum);
                        Map<String, Integer> catCounts = cardCategoryCounts.get(cardName);
                        if (catCounts != null) catCounts.merge(category, 1, Integer::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : categoryCardCounts.entrySet()) {
            List<Map.Entry<String, Integer>> sorted = entry.getValue().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                    .toList();

            List<Map<String, Object>> cards = new ArrayList<>();
            int rank = 1;
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0 && !sorted.get(i).getValue().equals(sorted.get(i - 1).getValue())) {
                    rank = i + 1;
                }
                Map.Entry<String, Integer> e = sorted.get(i);
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("rank", rank);
                card.put("cardName", e.getKey());
                card.put("count", e.getValue());
                card.put("ratio", ReportHandlerUtils.toRatio(e.getValue(), total));
                cards.add(card);
            }

            Map<String, Object> categoryMap = new LinkedHashMap<>();
            categoryMap.put("category", entry.getKey());
            categoryMap.put("cards", cards);
            byCategory.add(categoryMap);
        }

        List<Map<String, Object>> byCard = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : cardCategoryCounts.entrySet()) {
            Map<String, Object> cardMap = new LinkedHashMap<>();
            cardMap.put("cardName", entry.getKey());
            cardMap.put("categories", entry.getValue());
            byCard.add(cardMap);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byCard", byCard);
        result.put("byCategory", byCategory);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractGroups(Map<String, Object> answerMap) {
        List<Map<String, Object>> groups = (List<Map<String, Object>>) answerMap.get("groups");
        return groups != null ? groups : List.of();
    }
}
