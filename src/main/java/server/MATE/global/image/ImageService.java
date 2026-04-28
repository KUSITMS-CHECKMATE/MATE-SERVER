package server.MATE.global.image;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    List<String> uploadFiles(List<MultipartFile> files);
}
