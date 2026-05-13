package server.MATE.domain.answer.service.handler;

import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.QuestionType;

public interface AnswerCreateHandler {

    QuestionType supports();

    Answer save(Long participationId, AnswerCreateItem item);
}
