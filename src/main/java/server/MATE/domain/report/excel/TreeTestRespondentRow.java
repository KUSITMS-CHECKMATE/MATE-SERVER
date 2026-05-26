package server.MATE.domain.report.excel;

public record TreeTestRespondentRow(
        Long respondentNumber,
        String response,
        int depth
) {
}
