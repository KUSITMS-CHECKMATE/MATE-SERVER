package server.MATE.global.claude;

// AI 분석 결과 또는 실패 원인. 실패 원인은 운영 알림용
public record AiAnalysisOutcome(AiAnalysisResult result, String failureReason) {

    public static AiAnalysisOutcome success(AiAnalysisResult result) {
        return new AiAnalysisOutcome(result, null);
    }

    public static AiAnalysisOutcome failure(String reason) {
        return new AiAnalysisOutcome(null, reason);
    }

    public boolean isSuccess() {
        return result != null;
    }
}
