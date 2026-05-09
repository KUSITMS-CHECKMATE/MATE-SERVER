package server.MATE.domain.question.dto.response;

import java.util.List;

public record QuestionDetailResponse(
        Long testId,
        List<QuestionDetailItem> questions
) {
}
