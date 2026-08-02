package server.MATE.global.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.storage.dto.DownloadUrlResponse;
import server.MATE.global.storage.dto.UploadUrlResponse;

import java.util.UUID;

@Tag(name = "[FILE] 파일 API", description = "파일 업로드/다운로드 Presigned URL 발급 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "파일 업로드 URL 발급", description = """
            파일 업로드용 Presigned URL을 발급합니다. 클라이언트는 서버를 경유하지 않고 Azure Blob Storage에 직접 업로드합니다.

            **[허용 확장자]**
            - 이미지: jpg, jpeg, png
            - 리포트: pdf, xlsx

            **[업로드 방법]**
            - Method: PUT
            - URL: 발급받은 presignedUrl
            - Header: x-ms-blob-type: BlockBlob
            - Body: 파일 바이너리

            **[URL 유효시간]** 10분

            **[파일 크기 제한]** 최대 50MB

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_004 | 400 | extension 또는 fileSizeBytes 파라미터 누락 |
            | FILE_002 | 400 | 지원하지 않는 파일 형식 |
            | FILE_003 | 400 | 파일 크기 50MB 초과 |
            """)
    @PostMapping("/presigned-url/upload")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> generateUploadUrl(
            @Parameter(description = "파일 확장자 (점 없이 입력, 예: jpg, pdf)", example = "jpg")
            @RequestParam String extension,
            @Parameter(description = "파일 크기 (bytes)", example = "1048576")
            @RequestParam long fileSizeBytes
    ) {
        if (fileSizeBytes > FileStorageService.MAX_UPLOAD_SIZE_BYTES) {
            throw new BaseException(BaseErrorCode.FILE_003);
        }
        String ext = extension.toLowerCase();
        String fileKey = switch (ext) {
            case "jpg", "jpeg", "png" -> "media/" + UUID.randomUUID() + "." + ext;
            case "pdf" -> "reports/pdf/" + UUID.randomUUID() + "." + ext;
            case "xlsx" -> "reports/excel/" + UUID.randomUUID() + "." + ext;
            default -> throw new BaseException(BaseErrorCode.FILE_002);
        };
        String presignedUrl = fileStorageService.generatePresignedUrl(fileKey);
        return ResponseEntity.ok(ApiResponse.ok("업로드 URL이 발급되었습니다.",
                new UploadUrlResponse(presignedUrl, fileKey)));
    }

    @Operation(summary = "파일 다운로드 URL 발급", description = """
            저장된 공개 미디어 파일(media/ 접두사)의 다운로드용 Presigned URL을 발급합니다.
            리포트(reports/) 등 비공개 파일은 이 API로 발급할 수 없으며, 해당 도메인의 전용 다운로드 API를 사용해야 합니다.

            **[URL 유효시간]** 30분

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_004 | 400 | fileKey 파라미터 누락 |
            | FILE_004 | 403 | media/ 접두사가 아닌 파일은 다운로드 URL 발급 대상이 아님 |
            """)
    @GetMapping("/presigned-url/download")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @Parameter(description = "업로드 시 발급받은 fileKey", example = "media/uuid.jpg")
            @RequestParam String fileKey
    ) {
        if (!fileKey.startsWith("media/")) {
            throw new BaseException(BaseErrorCode.FILE_004);
        }
        String presignedUrl = fileStorageService.generateDownloadUrl(fileKey);
        return ResponseEntity.ok(ApiResponse.ok("다운로드 URL이 발급되었습니다.",
                new DownloadUrlResponse(presignedUrl)));
    }
}
