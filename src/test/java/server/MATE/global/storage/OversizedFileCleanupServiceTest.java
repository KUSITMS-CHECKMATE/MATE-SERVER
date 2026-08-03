package server.MATE.global.storage;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.global.storage.service.FileStorageService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OversizedFileCleanupServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private OversizedFileCleanupService oversizedFileCleanupService;

    @Test
    @DisplayName("용량 제한을 초과한 파일이 있으면 삭제한다")
    void deletesOversizedFiles() {
        List<String> oversizedKeys = List.of("media/big.jpg", "reports/pdf/1.pdf");
        given(fileStorageService.findOversizedFiles(FileStorageService.MAX_UPLOAD_SIZE_BYTES))
                .willReturn(oversizedKeys);

        oversizedFileCleanupService.cleanupOversizedFiles();

        verify(fileStorageService).deleteFiles(oversizedKeys);
    }

    @Test
    @DisplayName("용량 제한을 초과한 파일이 없으면 삭제를 호출하지 않는다")
    void doesNothingWhenNoOversizedFiles() {
        given(fileStorageService.findOversizedFiles(FileStorageService.MAX_UPLOAD_SIZE_BYTES))
                .willReturn(List.of());

        oversizedFileCleanupService.cleanupOversizedFiles();

        verify(fileStorageService, never()).deleteFiles(org.mockito.ArgumentMatchers.anyList());
    }
}
