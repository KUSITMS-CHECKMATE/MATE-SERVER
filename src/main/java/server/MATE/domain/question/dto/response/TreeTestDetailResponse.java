package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

public record TreeTestDetailResponse(
        Long questionId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        List<TreeTestNodeDetailResponse> features
) implements QuestionDetailItem {

    public static TreeTestDetailResponse of(Question question, List<TreeTestNodeDetailResponse> features) {
        return new TreeTestDetailResponse(
                question.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                features
        );
    }
}
