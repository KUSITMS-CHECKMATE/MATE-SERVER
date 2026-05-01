package server.MATE.global.image;

import java.util.List;

public interface ImageService {
    String generatePresignedUrl(String imageKey);
    void deleteFiles(List<String> keys);
}
