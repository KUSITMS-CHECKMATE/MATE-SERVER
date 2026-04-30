package server.MATE.global.image.dto;

public record PresignedUrlResponse(
        String presignedUrl,
        String imageKey
) {
}
