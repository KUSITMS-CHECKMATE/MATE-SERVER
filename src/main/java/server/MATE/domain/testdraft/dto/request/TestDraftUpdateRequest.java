package server.MATE.domain.testdraft.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.testdraft.validator.ClosedAtFormat;

import java.util.List;

public record TestDraftUpdateRequest(
        String title,

        String description,

        String serviceName,

        String serviceDescription,

        @Size(max = 10, message = "이미지는 최대 10개까지 등록 가능합니다.")
        List<@Size(min = 1, message = "이미지 키는 비어 있을 수 없습니다.") String> imageKeys,

        @Size(min = 1, max = 3, message = "카테고리는 1개에서 3개까지 선택 가능합니다.")
        List<Category> categories,

        @Min(value = 1, message = "목표 인원은 1명 이상이어야 합니다.")
        Integer goalPpl,

        @Min(value = 0, message = "리워드는 0원 이상이어야 합니다.")
        Integer reward,

        @ClosedAtFormat
        String closedAt,

        JsonNode questionsPayload
) {
}
