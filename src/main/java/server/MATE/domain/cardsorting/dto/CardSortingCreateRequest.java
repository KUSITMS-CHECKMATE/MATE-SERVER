package server.MATE.domain.cardsorting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CardSortingCreateRequest(
        @Size(min = 4, max = 10, message = "카테고리는 최소 4개, 최대 10개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "카테고리 이름은 비어 있을 수 없습니다.") String> categories
) {
}
