package server.MATE.domain.report.excel;

public record TreeTestPathStatRow(
        String pathLabel,
        int responseCount,
        String ratioPercent
) {
}
