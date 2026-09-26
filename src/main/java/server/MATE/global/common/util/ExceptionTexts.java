package server.MATE.global.common.util;

import java.util.Arrays;

// 알림용 예외 요약 문자열 생성 도우미
public final class ExceptionTexts {

    private static final String APP_PACKAGE = "server.MATE";
    // Discord 임베드 설명 한도(4096자) 초과로 알림이 누락되는 것 방지용
    private static final int MAX_MESSAGE_LENGTH = 1500;

    private ExceptionTexts() {
    }

    public static String describe(Throwable e) {
        String name = e.getClass().getName();
        String shortName = name.substring(name.lastIndexOf('.') + 1);
        String message = e.getMessage() != null ? e.getMessage() : "(no message)";
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH) + "…";
        }
        return shortName + ": " + message;
    }

    public static String location(Throwable e) {
        if (e == null) {
            return "N/A";
        }
        return Arrays.stream(e.getStackTrace())
                .filter(frame -> frame.getClassName().startsWith(APP_PACKAGE))
                .findFirst()
                .map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
                .orElse("N/A");
    }
}
