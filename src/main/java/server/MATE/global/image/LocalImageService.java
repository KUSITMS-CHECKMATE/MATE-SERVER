package server.MATE.global.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// TODO: Azure Storage 연동 완료 후 이 클래스와 @Profile 분기 제거
// AzureBlobImageService에 @Profile("prod") 제거 후 단일 구현체로 통합
@Slf4j
@Service
@Profile("local")
public class LocalImageService implements ImageService {

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        log.info("LocalImageService: 로컬 환경에서는 이미지 업로드를 건너뜁니다. ({}개)", files.size());
        return List.of();
    }

    @Override
    public void deleteFiles(List<String> keys) {
        log.info("LocalImageService: 로컬 환경에서는 이미지 삭제를 건너뜁니다.");
    }
}
