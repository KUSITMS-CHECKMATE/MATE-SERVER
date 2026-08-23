package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("SUBJECTIVE")
public record SubjectiveCreateRequest(
        @Schema(example = "개선이 필요한 점은 무엇인가요?")
        @NotBlank
        String title,

        @Schema(example = "자유롭게 작성해주세요.")
        String description,

        @Schema(example = "subjective-image-key", nullable = true)
        String imageKey
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.SUBJECTIVE;
    }
}
