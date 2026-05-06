package server.MATE.domain.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ObjectiveCreateRequest(
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Size(max = 55, message = "질문 설명은 최대 55자까지 입력 가능합니다.")
        String description,

        @NotNull(message = "중복 선택 여부는 필수 입력 사항입니다.")
        Boolean isDuplicate,

        Integer maxSelect,
        Integer minSelect,

        @NotNull(message = "기타 선택지 여부는 필수 입력 사항입니다.")
        Boolean isOther,

        @NotNull(message = "선택지는 필수 입력 사항입니다.")
        @Size(min = 2, max = 10, message = "선택지는 최소 2개, 최대 10개까지 추가 가능합니다.")
        List<@Valid ObjectiveOptionRequest> options
) {
}
