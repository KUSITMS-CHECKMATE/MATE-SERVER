package server.MATE.global.storage;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import server.MATE.global.storage.service.FileStorageService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OversizedFileCleanupService {

    private final FileStorageService fileStorageService;

    public void cleanupOversizedFiles() {
        List<String> oversizedKeys = fileStorageService.findOversizedFiles(FileStorageService.MAX_UPLOAD_SIZE_BYTES);
        if (oversizedKeys.isEmpty()) {
            return;
        }

        log.warn("업로드 용량 제한({}bytes) 초과 파일 {}건 발견, 삭제합니다: {}",
                FileStorageService.MAX_UPLOAD_SIZE_BYTES, oversizedKeys.size(), oversizedKeys);
        fileStorageService.deleteFiles(oversizedKeys);
    }
}
