package server.MATE.domain.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CardSortingCreateRequest(
        @NotBlank(message = "질문 제목은 필수입니다.")
        String title,
        String description,
        
        @NotNull(message = "카드 목록은 필수입니다.")
        @Size(min = 4, max = 12, message = "카드는 최소 4개, 최대 12개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "카드 텍스트는 비어 있을 수 없습니다.") String> cards,
        @NotNull(message = "카테고리 목록은 필수입니다.")

        @Size(min = 1, max = 3, message = "카테고리는 최소 1개, 최대 3개까지 입력할 수 있습니다.")
        @Valid List<CardSortingCategoryItemRequest> categories
) {
}
