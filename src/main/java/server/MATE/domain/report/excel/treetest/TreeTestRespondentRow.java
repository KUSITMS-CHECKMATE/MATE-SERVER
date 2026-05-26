package server.MATE.domain.report.excel.treetest;

public record TreeTestRespondentRow(
        Long respondentNumber,
        String response,
        int depth
) {
}
