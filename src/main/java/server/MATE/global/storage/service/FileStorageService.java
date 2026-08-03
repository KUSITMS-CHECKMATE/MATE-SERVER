package server.MATE.global.storage.service;

import java.util.List;

public interface FileStorageService {
    long MAX_UPLOAD_SIZE_BYTES = 50L * 1024 * 1024;

    String generatePresignedUrl(String key);
    String generateDownloadUrl(String key);
    String generateDownloadUrl(String key, String downloadFilename);
    void deleteFiles(List<String> keys);
    void upload(String key, byte[] content, String contentType);
    byte[] download(String key);
    List<String> findOversizedFiles(long maxSizeBytes);
}
