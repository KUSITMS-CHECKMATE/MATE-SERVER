package server.MATE.domain.report.excel.treetest;

public record TreeTestPathStatRow(
        String pathLabel,
        int responseCount,
        String ratioPercent
) {
}
