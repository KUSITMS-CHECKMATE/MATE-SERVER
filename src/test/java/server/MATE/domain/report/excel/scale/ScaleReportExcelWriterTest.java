package server.MATE.domain.report.excel.scale;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScaleReportExcelWriterTest {

    private final ScaleReportExcelWriter writer = new ScaleReportExcelWriter();

    @Test
    void 척도_5점_통계_템플릿_행이_생성된다() throws Exception {
        ScaleReportExcelData data = new ScaleReportExcelData(
                "Q01",
                "이 서비스를 추천하시겠습니까?",
                List.of(
                        new ScaleRespondentRow(100L, 5),
                        new ScaleRespondentRow(101L, 3)
                ),
                List.of(
                        new ScaleValueStatRow(1, 0, "0.00%"),
                        new ScaleValueStatRow(2, 0, "0.00%"),
                        new ScaleValueStatRow(3, 1, "50.00%"),
                        new ScaleValueStatRow(4, 0, "0.00%"),
                        new ScaleValueStatRow(5, 1, "50.00%")
                ),
                "4"
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("척도 테스트 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("척도");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("척도 테스트");
            assertThat(sheet.getRow(3).getCell(5).getStringCellValue()).isEqualTo("척도 테스트 - 통계");
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("이 서비스를 추천하시겠습니까?");
            assertThat(sheet.getColumnWidth(1)).isEqualTo(sheet.getColumnWidth(2));
            assertThat(sheet.getColumnWidth(2)).isEqualTo(sheet.getColumnWidth(3));
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("응답자 번호");
            assertThat(sheet.getRow(5).getCell(5).getStringCellValue()).isEqualTo("응답값");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("5");
            assertThat(sheet.getRow(10).getCell(5).getStringCellValue()).isEqualTo("5");
            assertThat(sheet.getRow(11).getCell(5).getStringCellValue()).isEqualTo("평균");
            assertThat(sheet.getRow(11).getCell(6).getStringCellValue()).isEqualTo("4");
        }
    }

    @Test
    void 응답자가_많아도_평균은_통계_바로_아래에_위치한다() throws Exception {
        List<ScaleRespondentRow> respondents = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new ScaleRespondentRow((long) i, i % 5 + 1))
                .toList();
        List<ScaleValueStatRow> valueStats = List.of(
                new ScaleValueStatRow(1, 1, "8.33%"),
                new ScaleValueStatRow(2, 2, "16.67%"),
                new ScaleValueStatRow(3, 3, "25.00%"),
                new ScaleValueStatRow(4, 2, "16.67%"),
                new ScaleValueStatRow(5, 4, "33.33%")
        );

        ScaleReportExcelData data = new ScaleReportExcelData(
                "Q01",
                "추천 의향은 어느 정도인가요?",
                respondents,
                valueStats,
                "3.5"
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(10).getCell(5).getStringCellValue()).isEqualTo("5");
            assertThat(sheet.getRow(10).getCell(7).getStringCellValue()).isEqualTo("33.33%");
            assertThat(sheet.getRow(11).getCell(5).getStringCellValue()).isEqualTo("평균");
            assertThat(sheet.getRow(11).getCell(6).getStringCellValue()).isEqualTo("3.5");
            assertThat(sheet.getRow(11).getCell(0).getStringCellValue()).isEqualTo("6");
            assertThat(sheet.getRow(12).getCell(0).getStringCellValue()).isEqualTo("7");
        }
    }

    @Test
    void 척도_7점_범위_통계_행이_7개_생성된다() throws Exception {
        List<ScaleValueStatRow> valueStats = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(score -> new ScaleValueStatRow(score, 0, "0.00%"))
                .toList();

        ScaleReportExcelData data = new ScaleReportExcelData(
                "Q01",
                "7점 척도 질문",
                List.of(),
                valueStats,
                "0"
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(6).getCell(5).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(12).getCell(5).getStringCellValue()).isEqualTo("7");
            assertThat(sheet.getRow(13).getCell(5).getStringCellValue()).isEqualTo("평균");
        }
    }
}
