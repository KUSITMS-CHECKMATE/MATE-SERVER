package server.MATE.global.storage;

import java.util.List;

public interface FileStorageService {
    String generatePresignedUrl(String key);
    String generateDownloadUrl(String key);
    String generateDownloadUrl(String key, String downloadFilename);
    void deleteFiles(List<String> keys);
    void upload(String key, byte[] content, String contentType);
    byte[] download(String key);
}
