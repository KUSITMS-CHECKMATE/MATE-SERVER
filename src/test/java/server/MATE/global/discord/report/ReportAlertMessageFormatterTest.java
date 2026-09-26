package server.MATE.global.discord.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAlertMessageFormatterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 14, 3, 22, 415_000_000);
    private static final ReportAlertTarget TARGET = new ReportAlertTarget(
            15L, "온보딩 화면 사용성 테스트", "신규 가입자의 온보딩 3단계 화면을 보고 어떤 점이 헷갈렸는지 알려주세요.",
            98L, LocalDateTime.of(2026, 9, 27, 23, 59, 59));

    @Test
    @DisplayName("집계 실패 임베드: 예외 종류·원인 분석·위치·환경")
    void aggregationFailedEmbed_withException() {
        DiscordEmbed embed = ReportAlertMessageFormatter.aggregationFailedEmbed(
                TARGET, "재시도 3회 실패", null, new NullPointerException("objective is null"), NOW, "prod");

        assertThat(embed.title()).isEqualTo("🚨 리포트 집계 실패");
        assertThat(embed.color()).isEqualTo(EmbedColor.ERROR);
        assertThat(embed.description())
                .contains("**에러 메시지** : \n```NullPointerException: objective is null```")
                .contains("**시간** : `2026-09-27 14:03:22.415`")
                .contains("**테스트** : `#15 온보딩 화면 사용성 테스트`")
                .contains("**원인 분석** : `재시도 3회 실패`")
                .contains("**위치** : `server.MATE.global.discord.report.ReportAlertMessageFormatterTest")
                .endsWith("**환경** : `prod`");
    }

    @Test
    @DisplayName("예외 없는 불일치: 에러 메시지는 전달된 문구, 위치는 N/A")
    void aggregationFailedEmbed_withoutException() {
        DiscordEmbed embed = ReportAlertMessageFormatter.aggregationFailedEmbed(
                TARGET, "리포트 불일치 (질문 5개, 리포트 3개)", "리포트 불일치: 질문 5개, 리포트 3개", null, NOW, "prod");

        assertThat(embed.description())
                .contains("```리포트 불일치: 질문 5개, 리포트 3개```")
                .contains("**위치** : `N/A`");
    }

    @Test
    @DisplayName("recover 실패 임베드: 제목 없으면 #ID만, 원인 분석 안내문")
    void aggregationCrashedEmbed() {
        DiscordEmbed embed = ReportAlertMessageFormatter.aggregationCrashedEmbed(
                ReportAlertTarget.idOnly(15L), new IllegalStateException("db down"), NOW, "prod");

        assertThat(embed.title()).isEqualTo("🚨 리포트 집계 처리 오류");
        assertThat(embed.description())
                .contains("**테스트** : `#15`")
                .contains("**원인 분석** : 리포트 상태가 IN_PROGRESS에 멈췄을 수 있습니다. DB에서 report_status를 확인해 주세요.");
    }

    @Test
    @DisplayName("AI 실패 임베드: 원인 분석에 실패/시도 수, 코드 블록에 문항별 원인")
    void aiDegradedEmbed() {
        DiscordEmbed embed = ReportAlertMessageFormatter.aiDegradedEmbed(TARGET, 5, List.of(
                Map.entry(101L, "WebClientResponseException$Unauthorized: 401 Unauthorized"),
                Map.entry(104L, "IllegalStateException: 필드 누락")), NOW, "prod");

        assertThat(embed.title()).isEqualTo("⚠️ AI 요약 실패");
        assertThat(embed.color()).isEqualTo(EmbedColor.WARN);
        assertThat(embed.description())
                .contains("**원인 분석** : `2 / 5` 문항 (AI 분석 대상 기준)\n```Q#101 WebClientResponseException$Unauthorized: 401 Unauthorized\nQ#104 IllegalStateException: 필드 누락```");
    }

    @Test
    @DisplayName("AI 실패 원인 줄은 10줄까지, 나머지는 외 N건")
    void aiDegradedEmbed_truncatesLines() {
        List<Map.Entry<Long, String>> failures = new ArrayList<>();
        IntStream.rangeClosed(1, 12).forEach(i -> failures.add(Map.entry((long) i, "E: x")));

        DiscordEmbed embed = ReportAlertMessageFormatter.aiDegradedEmbed(TARGET, 12, failures, NOW, "prod");

        assertThat(embed.description()).contains("Q#10 E: x\n… 외 2건```").doesNotContain("Q#11");
    }

    @Test
    @DisplayName("실패 상태 메시지: 설명·참여 인원·마감·원인 + 재집계 버튼")
    @SuppressWarnings("unchecked")
    void statusMessageBody_failed() {
        Map<String, Object> body = ReportAlertMessageFormatter.statusMessageBody(
                TARGET, "재시도 3회 실패", ReportAlertMessageFormatter.MessageState.FAILED);

        Map<String, Object> embed = ((List<Map<String, Object>>) body.get("embeds")).getFirst();
        assertThat(embed.get("title")).isEqualTo("리포트 집계 실패 · #15 온보딩 화면 사용성 테스트");
        assertThat(embed.get("color")).isEqualTo(0xC17E7E);
        assertThat((String) embed.get("description"))
                .startsWith("신규 가입자의 온보딩 3단계 화면을 보고 어떤 점이 헷갈렸는지 알려주세요.\n\n")
                .contains("👥 참여 인원 | `98명`")
                .contains("🕒 마감 | <t:1790521199:R>")
                .endsWith("⚠️ 원인 | `재시도 3회 실패`");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("components");
        Map<String, Object> button = ((List<Map<String, Object>>) rows.getFirst().get("components")).getFirst();
        assertThat(button).containsEntry("custom_id", "report:reaggregate:15").containsEntry("label", "재집계");
    }

    @Test
    @DisplayName("재집계 중 메시지: 초록, 안내문, 버튼 없음")
    @SuppressWarnings("unchecked")
    void statusMessageBody_reaggregating() {
        Map<String, Object> body = ReportAlertMessageFormatter.statusMessageBody(
                TARGET, "재시도 3회 실패", ReportAlertMessageFormatter.MessageState.REAGGREGATING);

        Map<String, Object> embed = ((List<Map<String, Object>>) body.get("embeds")).getFirst();
        assertThat(embed.get("color")).isEqualTo(0x82A783);
        assertThat((String) embed.get("description"))
                .endsWith("⚠️ 원인 | `재시도 3회 실패`\n\n> 재집계를 시작했습니다. 완료되면 메이커에게 리포트 완성 알림이 갑니다.");
        assertThat((List<?>) body.get("components")).isEmpty();
    }

    @Test
    @DisplayName("설명이 비어 있으면 설명 줄 생략")
    @SuppressWarnings("unchecked")
    void statusMessageBody_withoutDescription() {
        ReportAlertTarget noDesc = new ReportAlertTarget(15L, "t", null, 1L, TARGET.closedAt());

        Map<String, Object> body = ReportAlertMessageFormatter.statusMessageBody(
                noDesc, "c", ReportAlertMessageFormatter.MessageState.COMPLETED);

        String description = (String) ((List<Map<String, Object>>) body.get("embeds")).getFirst().get("description");
        assertThat(description).startsWith("👥 참여 인원");
    }

    @Test
    @DisplayName("처리 기록 줄과 원인 추출")
    void linesAndExtractCause() {
        Instant at = Instant.ofEpochSecond(1759000000L);

        assertThat(ReportAlertMessageFormatter.reaggregateRequestLine(
                ReportAlertMessageFormatter.memberRequester("💙", "소윤"), at))
                .isEqualTo("💙 **소윤** 님이 재집계 요청 · <t:1759000000:f>");
        assertThat(ReportAlertMessageFormatter.reaggregateRequestLine(ReportAlertMessageFormatter.API_REQUESTER, at))
                .isEqualTo("🛠️ 관리자 API로 재집계 요청 · <t:1759000000:f>");
        assertThat(ReportAlertMessageFormatter.completedLine(at)).isEqualTo("✅ 리포트 집계 완료 · <t:1759000000:f>");
        assertThat(ReportAlertMessageFormatter.extractCause(Map.of("embeds", List.of(Map.of(
                "description", "👥 참여 인원 | `1명`\n⚠️ 원인 | `재시도 3회 실패`"))))).isEqualTo("재시도 3회 실패");
        assertThat(ReportAlertMessageFormatter.extractCause(Map.of())).isEqualTo("-");
    }
}
