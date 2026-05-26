package server.MATE.domain.report.excel.scale;

public record ScaleValueStatRow(
        int responseValue,
        int responseCount,
        String ratioPercent
) {
}
