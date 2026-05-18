package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

public record FiveSecondDetailResponse(
        Long questionId,
        Long fiveSecondId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        String imageKey,
        boolean isObjective,
        Boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,
        Boolean isOther,
        List<FiveSecondOptionDetailResponse> options
) implements QuestionDetailItem {

    public static FiveSecondDetailResponse of(Question question, FiveSecond fiveSecond) {
        return new FiveSecondDetailResponse(
                question.getId(),
                fiveSecond.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                fiveSecond.getImageKey(),
                fiveSecond.isObjective(),
                fiveSecond.getIsDuplicate(),
                fiveSecond.getMinSelect(),
                fiveSecond.getMaxSelect(),
                fiveSecond.getIsOther(),
                fiveSecond.getOptions().stream()
                        .map(FiveSecondOptionDetailResponse::from)
                        .toList()
        );
    }
}
