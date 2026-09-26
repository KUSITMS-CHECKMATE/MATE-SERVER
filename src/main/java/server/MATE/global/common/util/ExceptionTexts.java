package server.MATE.global.common.util;

import java.util.Arrays;

// 알림용 예외 요약 문자열 생성 도우미
public final class ExceptionTexts {

    private static final String APP_PACKAGE = "server.MATE";

    private ExceptionTexts() {
    }

    public static String describe(Throwable e) {
        String name = e.getClass().getName();
        String shortName = name.substring(name.lastIndexOf('.') + 1);
        return shortName + ": " + (e.getMessage() != null ? e.getMessage() : "(no message)");
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
