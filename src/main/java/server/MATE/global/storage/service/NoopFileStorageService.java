package server.MATE.global.storage.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Service
@ConditionalOnMissingBean(FileStorageService.class)
public class NoopFileStorageService implements FileStorageService {

    private static final String STORAGE_NOT_CONFIGURED_MESSAGE = "파일 스토리지가 설정되지 않았습니다.";

    @Override
    public String generatePresignedUrl(String key) {
        throw new BaseException(BaseErrorCode.COMMON_999, STORAGE_NOT_CONFIGURED_MESSAGE);
    }

    @Override
    public String generateDownloadUrl(String key) {
        return key;
    }

    @Override
    public String generateDownloadUrl(String key, String downloadFilename) {
        return key;
    }

    @Override
    public void deleteFiles(List<String> keys) {
        // storage가 비활성화된 환경에서는 삭제 작업을 수행하지 않는다.
    }

    @Override
    public void upload(String key, byte[] content, String contentType) {
        // storage가 비활성화된 환경에서는 업로드 작업을 수행하지 않는다.
    }

    @Override
    public byte[] download(String key) {
        return new byte[0];
    }

    @Override
    public List<String> findOversizedFiles(long maxSizeBytes) {
        return List.of();
    }
}
