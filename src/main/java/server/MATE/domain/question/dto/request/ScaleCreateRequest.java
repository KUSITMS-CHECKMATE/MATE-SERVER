package server.MATE.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScaleCreateRequest(
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Size(max = 50, message = "질문 설명은 최대 55자까지 입력 가능합니다.")
        String description,

        String imageKey,

        // TODO. 기획 확인 필요
        @Size(max = 100, message = "최소 점수 라벨은 최대 100자까지 입력 가능합니다.")
        String minLabel,

        // TODO. 기획 확인 필요
        @Size(max = 100, message = "최대 점수 라벨은 최대 100자까지 입력 가능합니다.")
        String maxLabel,

        @NotNull(message = "척도 범위는 필수입니다. 5 또는 7을 입력해주세요.")
        Integer range
) {
}
