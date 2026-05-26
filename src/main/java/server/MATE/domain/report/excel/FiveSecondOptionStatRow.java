package server.MATE.domain.report.excel;

public record FiveSecondOptionStatRow(
        String responseContent,
        int responseCount,
        String ratioPercent
) {
}
