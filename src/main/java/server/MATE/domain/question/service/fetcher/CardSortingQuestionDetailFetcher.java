package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.CardSortingDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CardSortingQuestionDetailFetcher implements QuestionDetailFetcher {

    private final CardSortingRepository cardSortingRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.CARD_SORTING;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<CardSorting> cardSortings = cardSortingRepository.findAllByIdIn(questionMap.keySet());
        return cardSortings.stream()
                .collect(Collectors.toMap(
                        CardSorting::getId,
                        cardSorting -> CardSortingDetailResponse.of(questionMap.get(cardSorting.getId()), cardSorting)
                ));
    }
}
