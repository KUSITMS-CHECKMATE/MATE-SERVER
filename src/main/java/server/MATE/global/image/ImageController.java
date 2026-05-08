package server.MATE.global.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class ImageController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ImageService imageService;

    @Operation(summary = "이미지 업로드 URL 발급", description = """
            이미지 업로드용 Presigned URL을 발급합니다. 클라이언트는 서버를 경유하지 않고 Azure Blob Storage에 직접 업로드합니다.

            **[전체 업로드 플로우]**
            1. 이 API 호출 → presignedUrl, imageKey 수령
            2. presignedUrl로 이미지 파일 직접 PUT 업로드
            3. 테스트 등록/수정 API 요청 시 imageKey 포함

            **[응답 필드]**
            - presignedUrl: 이미지 업로드에 사용할 URL (2단계에서만 사용)
            - imageKey: 등록/수정 API의 imageKeys 배열에 담아 전달하는 식별자

            **[2단계 PUT 업로드 예시]**
            - Method: PUT
            - URL: 발급받은 presignedUrl
            - Header: x-ms-blob-type: BlockBlob
            - Body: 이미지 바이너리

            **[주의사항]**
            - 허용 확장자: jpg, jpeg, png (대소문자 무관)
            - URL 유효시간: 발급 후 10분
            - 이미지 여러 장 업로드 시 이 API를 장 수만큼 반복 호출합니다

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_004 | 400 | extension 파라미터 누락 |
            | TEST_003 | 400 | 지원하지 않는 이미지 형식 (jpg, jpeg, png만 허용) |
            """)
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Parameter(description = "이미지 확장자 (점 없이 입력, 예: jpg)", example = "jpg")
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
