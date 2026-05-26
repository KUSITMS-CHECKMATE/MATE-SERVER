package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectiveReportExcelWriterTest {

    private final SubjectiveReportExcelWriter writer = new SubjectiveReportExcelWriter();

    @Test
    void 주관식_통계_템플릿_행이_생성된다() throws Exception {
        SubjectiveReportExcelData data = new SubjectiveReportExcelData(
                "Q01",
                "서비스 이용 목적을 적어주세요",
                List.of(
                        new SubjectiveRespondentRow(1L, "빠른 검색"),
                        new SubjectiveRespondentRow(2L, "알림 기능")
                )
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("주관식 통계");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Q01 - 질문 설정");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("주관식");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("서비스 이용 목적을 적어주세요");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("주관식");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("질문");
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("서비스 이용 목적을 적어주세요");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("응답자 번호");
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("답변 내용");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("빠른 검색");
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("알림 기능");
        }
    }
}
