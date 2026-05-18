package server.MATE.global.storage;

import java.util.List;

public interface FileStorageService {
    String generatePresignedUrl(String key);
    String generateDownloadUrl(String key);
    void deleteFiles(List<String> keys);
}
