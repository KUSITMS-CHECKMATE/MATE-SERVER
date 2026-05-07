package server.MATE.domain.question.service.handler;

import server.MATE.domain.question.dto.request.QuestionCreateItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

public interface QuestionCreateHandler {

    QuestionType supports();

    void validate(QuestionCreateItem item);

    void createDetail(Question question, QuestionCreateItem item);

    List<String> extractImageKeys(QuestionCreateItem item);
}
