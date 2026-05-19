package server.MATE.domain.question.dto.response;

import java.util.List;

public record QuestionsDetailResponse(
        Long testId,
        List<QuestionDetailItem> questions
) {
}
