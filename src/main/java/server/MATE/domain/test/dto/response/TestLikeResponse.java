package server.MATE.domain.test.dto.response;

public record TestLikeResponse(
        Long testId,
        Boolean isLiked,
        Long likeCount
) {
}
