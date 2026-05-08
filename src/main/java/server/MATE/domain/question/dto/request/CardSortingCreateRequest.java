package server.MATE.domain.question.dto.request;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("CARD_SORTING")
public record CardSortingCreateRequest(
        @Schema(example = "기능 카드를 그룹으로 묶어주세요.")
        @NotBlank(message = "질문 제목은 필수입니다.")
        String title,

        @Schema(example = "비슷하다고 생각하는 항목끼리 분류해주세요.")
        String description,
        @Schema(example = "[\"티셔츠\", \"꽃무늬가 들어간 티셔츠\", \"찢어진 청바지\", \"닥터마틴 워커\"]")
        @NotNull(message = "카드는 필수입니다.")
        @Size(min = 4, max = 12, message = "카드는 최소 4개, 최대 12개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "카드 이름은 비어 있을 수 없습니다.")
                @Size(max = 16, message = "카드 이름은 최대 16자까지 입력할 수 있습니다.")
                String> cards,

        @Schema(example = "[\"상의\", \"하의\", \"신발\"]")
        @NotNull(message = "카테고리는 필수입니다.")
        @Size(min = 1, max = 3, message = "카테고리는 최소 1개, 최대 3개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "카테고리 이름은 비어 있을 수 없습니다.")
                @Size(max = 12, message = "카테고리 이름은 최대 12자까지 입력할 수 있습니다.")
                String> categories
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.CARD_SORTING;
    }
}
