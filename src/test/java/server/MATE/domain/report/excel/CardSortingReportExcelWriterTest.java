package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardSortingReportExcelWriterTest {

    private final CardSortingReportExcelWriter writer = new CardSortingReportExcelWriter();

    @Test
    void 카드소팅_통계_템플릿_행이_생성된다() throws Exception {
        CardSortingReportExcelData data = new CardSortingReportExcelData(
                "Q01",
                "기능을 분류해주세요",
                List.of(
                        new CardSortingRespondentRow(10L, "메인 기능", 1),
                        new CardSortingRespondentRow(10L, "부가 기능", 4)
                ),
                List.of(
                        new CardSortingCategoryStatRow("메인 기능", "카드 1순위 결제", "50.00%"),
                        new CardSortingCategoryStatRow("메인 기능", "카드 2순위 검색", "50.00%"),
                        new CardSortingCategoryStatRow("부가 기능", "카드 1순위 설정", "100.00%")
                )
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("카드소팅 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("카드소팅");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("카드소팅");
            assertThat(sheet.getRow(3).getCell(4).getStringCellValue()).isEqualTo("카드소팅 - 통계");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("질문");
            assertThat(sheet.getRow(4).getCell(4).getStringCellValue()).isEqualTo("카테고리명");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("응답자 번호");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("메인 기능");
            assertThat(sheet.getRow(6).getCell(5).getStringCellValue()).isEqualTo("카드 1순위 결제");
            assertThat(sheet.getRow(7).getCell(5).getStringCellValue()).isEqualTo("카드 2순위 검색");
            assertThat(sheet.getRow(8).getCell(4).getStringCellValue()).isEqualTo("부가 기능");

            boolean hasCategoryMerge = false;
            for (int index = 0; index < sheet.getNumMergedRegions(); index++) {
                CellRangeAddress region = sheet.getMergedRegion(index);
                if (region.getFirstColumn() == 4 && region.getFirstRow() == 6 && region.getLastRow() == 7) {
                    hasCategoryMerge = true;
                }
            }
            assertThat(hasCategoryMerge).isTrue();
        }
    }
}
