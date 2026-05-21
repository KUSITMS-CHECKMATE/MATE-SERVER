package server.MATE.domain.report.service.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ReportHandlerUtils {

    private ReportHandlerUtils() {
    }

    static double toPercentage(int count, int total) {
        if (total == 0) return 0.0;
        return Math.round(count * 1000.0 / total) / 10.0;
    }

    @SuppressWarnings("unchecked")
    static List<Long> extractOptionIds(Map<String, Object> answerMap) {
        List<Object> raw = (List<Object>) answerMap.get("optionIds");
        if (raw == null) return List.of();
        List<Long> ids = new ArrayList<>();
        for (Object o : raw) {
            ids.add(((Number) o).longValue());
        }
        return ids;
    }
}
