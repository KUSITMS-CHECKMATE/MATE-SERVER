package server.MATE.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CardSortingCategoryItemRequest(
        @NotBlank(message = "카테고리 이름은 비어 있을 수 없습니다.")
        String name
) {
}
