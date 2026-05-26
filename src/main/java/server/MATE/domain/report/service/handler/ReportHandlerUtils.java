package server.MATE.domain.report.service.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ReportHandlerUtils {

    private static final int TEXT_SAMPLE_SIZE = 15;

    private ReportHandlerUtils() {
    }

    static double toRatio(int count, int total) {
        if (total == 0) return 0.0;
        return Math.round(count * 1000.0 / total) / 1000.0;
    }

    // TODO: AI 연동 후 의미론적 유사도 기반 그룹핑으로 교체
    static List<Map<String, Object>> buildClusters(List<String> texts) {
        return texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .collect(Collectors.groupingBy(Function.identity()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, List<String>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .map(e -> {
                    Map<String, Object> cluster = new LinkedHashMap<>();
                    cluster.put("representative", e.getKey());
                    cluster.put("count", e.getValue().size());
                    cluster.put("responses", List.copyOf(e.getValue()));
                    return cluster;
                })
                .toList();
    }

    static List<String> sampleTexts(List<String> texts) {
        return texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .limit(TEXT_SAMPLE_SIZE)
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
