package server.MATE.domain.report.service;

import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;
import java.util.Map;

public interface ReportHandler {

    QuestionType supports();

    Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId);

    /**
     * includeAiAnalysis=false인 경우, AI 분석(Claude 호출)을 수행하지 않는 핸들러에서 사용.
     * 기본 구현은 AI 미사용 핸들러를 위해 기존 compute를 그대로 위임함.
     */
    default Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId, boolean includeAiAnalysis) {
        return compute(questions, answersByQuestionId);
    }
}
