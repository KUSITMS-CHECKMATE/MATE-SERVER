package server.MATE.domain.question.dto.response;

public record QuestionDetailResponse(
        Long testId,
        QuestionDetailItem question
) {
}
