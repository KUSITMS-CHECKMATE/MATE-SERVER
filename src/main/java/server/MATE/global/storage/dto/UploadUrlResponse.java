package server.MATE.global.storage.dto;

public record UploadUrlResponse(
        String presignedUrl,
        String fileKey
) {
}
