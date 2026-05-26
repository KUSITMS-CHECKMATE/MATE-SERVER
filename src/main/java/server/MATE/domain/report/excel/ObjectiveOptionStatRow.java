package server.MATE.domain.report.excel;

public record ObjectiveOptionStatRow(
        String optionLabel,
        int responseCount,
        String ratioPercent
) {
}
