package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        @Schema(example = "[\"검색\", \"결제\", \"주문내역\", \"프로필\"]")
        @Size(min = 4, max = 10, message = "카테고리는 최소 4개, 최대 10개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "카테고리 이름은 비어 있을 수 없습니다.") String> categories
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.CARD_SORTING;
    }
}
