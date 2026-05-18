package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

public record AbTestDetailResponse(
        Long questionId,
        Long abTestId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        String aImageKey,
        String bImageKey,
        ImageRatio imageRatio
) implements QuestionDetailItem {

    public static AbTestDetailResponse of(Question question, AbTest abTest) {
        return new AbTestDetailResponse(
                question.getId(),
                abTest.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                abTest.getAImageKey(),
                abTest.getBImageKey(),
                abTest.getImageRatio()
        );
    }
}
