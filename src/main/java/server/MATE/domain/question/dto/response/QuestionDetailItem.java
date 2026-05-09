package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.QuestionType;

public sealed interface QuestionDetailItem permits
        ObjectiveDetailResponse,
        SubjectiveDetailResponse,
        FiveSecondDetailResponse,
        ScaleDetailResponse,
        AbTestDetailResponse,
        CardSortingDetailResponse,
        TreeTestDetailResponse {

    Long questionId();

    QuestionType type();

    Long sequence();

    String title();

    String description();
}
