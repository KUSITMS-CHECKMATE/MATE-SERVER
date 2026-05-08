package server.MATE.domain.question.dto.response;

import java.util.List;

public record QuestionCreateResponse(
        List<QuestionCreateResult> questions
) {
}
