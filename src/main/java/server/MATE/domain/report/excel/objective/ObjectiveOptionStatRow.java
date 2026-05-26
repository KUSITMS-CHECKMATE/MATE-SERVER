package server.MATE.domain.report.excel.objective;

public record ObjectiveOptionStatRow(
        String optionLabel,
        int responseCount,
        String ratioPercent
) {
}
