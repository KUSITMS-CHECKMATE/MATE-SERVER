package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.CardSortingAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CardSortingAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.CARD_SORTING;
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        CardSortingAnswerCreateRequest request = (CardSortingAnswerCreateRequest) item;

        if (request.groups() == null || request.groups().isEmpty()) throw new BaseException(BaseErrorCode.ANSWER_005);

        CardSorting cardSorting = context.cardSortings().get(request.questionId());
        if (cardSorting == null) throw new BaseException(BaseErrorCode.QUESTION_005);

        Set<String> validCategories = new HashSet<>(cardSorting.getCategories());
        Set<String> validCards = new HashSet<>(cardSorting.getCards());

        Set<String> assignedCards = new HashSet<>();
        Set<String> usedCategories = new HashSet<>();
        for (CardSortingAnswerCreateRequest.GroupItem group : request.groups()) {
            if (group == null || group.category() == null || !validCategories.contains(group.category())) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }
            if (!usedCategories.add(group.category())) throw new BaseException(BaseErrorCode.ANSWER_004);
            if (group.cardNames() == null) throw new BaseException(BaseErrorCode.ANSWER_004);
            for (String cardName : group.cardNames()) {
                if (!validCards.contains(cardName)) throw new BaseException(BaseErrorCode.ANSWER_004);
                if (!assignedCards.add(cardName)) throw new BaseException(BaseErrorCode.ANSWER_004);
            }
        }

        if (assignedCards.size() != validCards.size()) throw new BaseException(BaseErrorCode.ANSWER_005);

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.CARD_SORTING)
                .answer(Map.of("groups", request.groups()))
                .build();
    }
}
