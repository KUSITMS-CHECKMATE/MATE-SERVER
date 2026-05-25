package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.storage.dto.ImageResponse;

import java.util.List;
import java.util.function.Function;

public record ObjectiveDetailResponse(
        Long questionId,
        Long objectiveId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,
        boolean isOther,
        List<ObjectiveOptionDetailResponse> options
) implements QuestionDetailItem {

    public static ObjectiveDetailResponse of(Question question, Objective objective, Function<String, ImageResponse> keyToImage) {
        return new ObjectiveDetailResponse(
                question.getId(),
                objective.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                objective.isDuplicate(),
                objective.getMinSelect(),
                objective.getMaxSelect(),
                objective.isOther(),
                objective.getOptions().stream()
                        .map(option -> ObjectiveOptionDetailResponse.from(
                                option,
                                keyToImage.apply(option.getImageKey())
                        ))
                        .toList()
        );
    }
}
