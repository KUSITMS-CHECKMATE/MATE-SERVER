package server.MATE.domain.test.dto.request;

import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 종료 테스트 목록 커서. 정렬 기준(completedAt desc, id desc)의 마지막 항목 값을 담음.
 * 클라이언트에는 Base64URL 문자열로 전달하며, 클라이언트는 내용을 해석하지 않고 그대로 돌려보냄
 */
public record CompletedTestCursor(LocalDateTime completedAt, Long id) {

    private static final String DELIMITER = "_";

    public static CompletedTestCursor from(Test test) {
        return new CompletedTestCursor(test.getCompletedAt(), test.getId());
    }

    public static CompletedTestCursor decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(DELIMITER, 2);
            if (parts.length != 2) {
                throw new BaseException(BaseErrorCode.COMMON_002);
            }
            return new CompletedTestCursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }
    }

    public String encode() {
        String raw = completedAt + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
