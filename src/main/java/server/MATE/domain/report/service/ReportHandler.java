package server.MATE.domain.report.service;

import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;
import java.util.Map;

public interface ReportHandler {

    QuestionType supports();

    Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId);
}
