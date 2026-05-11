package server.MATE.domain.answer.dto.response;

import server.MATE.domain.answer.entity.Answer;

public record AnswerCreateResponse(Long answerId) {

    public static AnswerCreateResponse from(Answer answer) {
        return new AnswerCreateResponse(answer.getId());
    }
}
