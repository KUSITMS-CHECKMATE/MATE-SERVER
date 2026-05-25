package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.storage.dto.ImageResponse;

public record AbTestDetailResponse(
        Long questionId,
        Long abTestId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        ImageResponse aImage,
        ImageResponse bImage,
        ImageRatio imageRatio
) implements QuestionDetailItem {

    public static AbTestDetailResponse of(Question question, AbTest abTest, ImageResponse aImage, ImageResponse bImage) {
        return new AbTestDetailResponse(
                question.getId(),
                abTest.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                aImage,
                bImage,
                abTest.getImageRatio()
        );
    }
}
