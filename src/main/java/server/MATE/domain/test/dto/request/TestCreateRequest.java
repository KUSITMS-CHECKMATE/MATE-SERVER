package server.MATE.domain.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.test.entity.Category;

import java.util.List;

public record TestCreateRequest(
        @NotBlank
        @Size(max = 17, message = "테스트 이름은 최대 17자까지 입력 가능합니다.")
        String title,

        @NotBlank
        @Size(max = 60, message = "테스트 한줄 소개는 최대 60자까지 입력 가능합니다.")
        String description,

        @NotNull(message = "카테고리는 필수 선택 사항입니다.")
        @Size(min = 1, max = 3, message = "카테고리는 1개에서 3개까지 선택 가능합니다.")
        List<Category> categories,

        @Size(max = 17, message = "서비스 이름은 최대 17자까지 입력 가능합니다.")
        String serviceName,

        @Size(max = 70, message = "서비스 소개는 최대 70자까지 입력 가능합니다.")
        String serviceDescription,

        @Size(max = 10, message = "이미지는 최대 10개까지 등록 가능합니다.")
        List<@NotBlank String> imageKeys
) {
}
