package server.MATE.global.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 키와 렌더링용 URL 쌍")
public record ImageResponse(
        @Schema(description = "파일 키 (이미지 수정/삭제 요청 시 사용)", example = "media/uuid.jpg")
        String imageKey,

        @Schema(description = "이미지 렌더링용 URL (media/ 키는 Public URL, 그 외는 SAS URL로 30분 만료)", example = "https://…")
        String imageUrl
) {
}
