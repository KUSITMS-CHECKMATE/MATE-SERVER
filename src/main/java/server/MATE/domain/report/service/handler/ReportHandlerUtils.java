package server.MATE.domain.report.service.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ReportHandlerUtils {

    private ReportHandlerUtils() {
    }

    static double toRatio(int count, int total) {
        if (total == 0) return 0.0;
        return Math.round(count * 1000.0 / total) / 1000.0;
    }

    // TODO: AI 연동 후 의미론적 유사도 기반 그룹핑으로 교체
    static List<String> topAnswers(List<String> texts) {
        return texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(Map.Entry::getKey)
                .toList();
    }

    @SuppressWarnings("unchecked")
    static List<Long> extractOptionIds(Map<String, Object> answerMap) {
        List<Object> raw = (List<Object>) answerMap.get("optionIds");
        if (raw == null) return List.of();
        List<Long> ids = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Number n) ids.add(n.longValue());
        }
        return ids;
    }
}
