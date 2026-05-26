package server.MATE.global.claude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AiAnalysisResult(String aiSummary, List<ClusterResult> clusters) {

    public record ClusterResult(String tag, String representative, int count) {}

    public List<Map<String, Object>> toClusterMaps() {
        return clusters.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tag", c.tag());
                    m.put("representative", c.representative());
                    m.put("count", c.count());
                    return m;
                })
                .toList();
    }
}
