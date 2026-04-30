package server.MATE.global.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.image.dto.PresignedUrlResponse;

import java.util.Set;
import java.util.UUID;

@Tag(name = "[IMAGE] 이미지 API", description = "이미지 업로드 관련 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ImageService imageService;

    @Operation(summary = "이미지 업로드 URL 발급", description = "Azure Blob Storage에 직접 업로드할 수 있는 Presigned URL을 발급합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @RequestParam String extension
    ) {
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BaseException(BaseErrorCode.TEST_003);
        }
        String imageKey = UUID.randomUUID() + "." + extension.toLowerCase();
        String presignedUrl = imageService.generatePresignedUrl(imageKey);
        return ResponseEntity.ok(ApiResponse.ok("Presigned URL이 발급되었습니다.", new PresignedUrlResponse(presignedUrl, imageKey)));
    }
}
