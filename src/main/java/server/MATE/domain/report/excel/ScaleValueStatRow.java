package server.MATE.domain.report.excel;

public record ScaleValueStatRow(
        int responseValue,
        int responseCount,
        String ratioPercent
) {
}
