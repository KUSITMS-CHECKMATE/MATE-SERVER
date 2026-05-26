package server.MATE.global.claude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class SubjectiveAiService {

    private static final String SYSTEM_PROMPT = """
            당신은 사용자 리서치 전문가입니다. 주관식 설문 응답을 분석하여 반드시 순수 JSON만 출력하세요. 마크다운 코드블록을 사용하지 마세요.

            규칙:
            - aiSummary: 2~3문장, 사실 기반, 추측 표현 금지("~인 것 같습니다" 사용 불가), "응답자들은"으로 시작
            - clusters: 의미론적으로 유사한 응답 그룹. 최소 3개 최대 21개
            - mappings: 각 응답(0-based 인덱스)이 속하는 클러스터 인덱스 배열. 길이는 반드시 응답 수와 동일해야 함. 한 응답은 하나의 클러스터에만 속함
            """;

    private final WebClient claudeWebClient;
    private final ClaudeProperties properties;
    private final ObjectMapper objectMapper;

    public SubjectiveAiService(
            @Qualifier("claudeWebClient") WebClient claudeWebClient,
            ClaudeProperties properties,
            ObjectMapper objectMapper
    ) {
        this.claudeWebClient = claudeWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public int getMinResponseThreshold() {
        return properties.minResponseThreshold();
    }

    public Optional<AiAnalysisResult> analyze(List<String> texts) {
        try {
            String rawJson = callClaude(texts);
            return Optional.of(parseAndValidate(rawJson, texts.size()));
        } catch (Exception e) {
            log.warn("Claude AI 분석 실패, 폴백 처리: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String callClaude(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        sb.append("응답 수: ").append(texts.size()).append("개\n\n");
        for (int i = 0; i < texts.size(); i++) {
            sb.append("[").append(i).append("] ").append(texts.get(i)).append("\n");
        }
        sb.append("""

                다음 JSON 형식으로만 응답하세요:
                {
                  "aiSummary": "응답자들은 ...",
                  "clusters": [
                    {"tag": "태그1~2단어", "representative": "대표 문장 1줄"}
                  ],
                  "mappings": [0, 1, 0, 2]
                }
                """);

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "max_tokens", 4096,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", sb.toString()))
        );

        Map<String, Object> response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(60));

        if (response == null || !response.containsKey("content")) {
            throw new IllegalStateException("Claude API 응답이 비어있거나 올바르지 않습니다.");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("Claude API 응답의 content 필드가 비어있습니다.");
        }
        return stripMarkdown((String) content.get(0).get("text"));
    }

    private String stripMarkdown(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
            return trimmed.substring(start).trim();
        }
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private AiAnalysisResult parseAndValidate(String json, int textCount) throws Exception {
        Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
        if (parsed == null) {
            throw new IllegalStateException("JSON 파싱 결과가 null입니다.");
        }

        String aiSummary = (String) parsed.get("aiSummary");
        List<Map<String, Object>> rawClusters = (List<Map<String, Object>>) parsed.get("clusters");
        List<Object> rawMappings = (List<Object>) parsed.get("mappings");

        if (aiSummary == null || rawClusters == null || rawMappings == null) {
            throw new IllegalStateException("LLM 응답에 필수 필드(aiSummary, clusters, mappings)가 누락되었습니다.");
        }

        if (rawMappings.size() != textCount) {
            throw new IllegalStateException("mappings 길이 불일치: " + rawMappings.size() + " != " + textCount);
        }

        int[] mappings = rawMappings.stream()
                .mapToInt(o -> {
                    if (o instanceof Number n) return n.intValue();
                    throw new IllegalStateException("mappings에 숫자가 아닌 값이 포함되어 있습니다: " + o);
                })
                .toArray();

        boolean validIndices = IntStream.of(mappings).allMatch(idx -> idx >= 0 && idx < rawClusters.size());
        if (!validIndices) {
            throw new IllegalStateException("mappings에 유효하지 않은 클러스터 인덱스가 포함되어 있습니다");
        }

        Map<Integer, Long> countByCluster = IntStream.range(0, mappings.length)
                .boxed()
                .collect(Collectors.groupingBy(i -> mappings[i], Collectors.counting()));

        if (countByCluster.size() < 3 || countByCluster.size() > 21) {
            throw new IllegalStateException("클러스터 수 범위 초과: " + countByCluster.size());
        }

        List<AiAnalysisResult.ClusterResult> clusters = IntStream.range(0, rawClusters.size())
                .mapToObj(i -> {
                    Map<String, Object> c = rawClusters.get(i);
                    if (c == null) {
                        throw new IllegalStateException("클러스터 정보가 올바르지 않습니다 (null).");
                    }
                    int count = countByCluster.getOrDefault(i, 0L).intValue();
                    return new AiAnalysisResult.ClusterResult(
                            (String) c.get("tag"),
                            (String) c.get("representative"),
                            count
                    );
                })
                .filter(c -> c.count() > 0)
                .sorted(Comparator.comparingInt(AiAnalysisResult.ClusterResult::count).reversed())
                .toList();

        return new AiAnalysisResult(aiSummary, clusters);
    }
}
