package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;

public record ScaleDetailResponse(
        Long questionId,
        Long scaleId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        String imageUrl,
        String minLabel,
        String maxLabel,
        Integer range
) implements QuestionDetailItem {

    public static ScaleDetailResponse of(Question question, Scale scale, String imageUrl) {
        return new ScaleDetailResponse(
                question.getId(),
                scale.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                imageUrl,
                scale.getMinLabel(),
                scale.getMaxLabel(),
                scale.getRange()
        );
    }
}
