package server.MATE.domain.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FiveSecondCreateRequest(
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Size(max = 50, message = "질문 설명은 최대 50자까지 입력 가능합니다.")
        String description,

        @NotBlank(message = "이미지는 필수 입력 사항입니다.")
        String imageKey,

        @NotNull(message = "객관식 전환 여부는 필수 입력 사항입니다.")
        Boolean isObjective,

        Boolean isDuplicate,
        Integer minSelect,
        Integer maxSelect,

        @Size(max = 10, message = "선택지는 최대 10개까지 추가 가능합니다.")
        List<@Valid FiveSecondOptionRequest> options
) {
}
