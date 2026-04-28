package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.image.ImageService;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private static final int MAX_IMAGE_COUNT = 10;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final TestRepository testRepository;
    private final ImageService imageService;

    public TestCreateResponse createTest(TestCreateRequest request, List<MultipartFile> images, Long makerId) {
        validateImages(images);

        List<String> imageKeys = uploadImages(images);

        Test test = Test.builder()
                .makerId(makerId)
                .title(request.title())
                .description(request.description())
                .serviceName(request.serviceName())
                .serviceDescription(request.serviceDescription())
                .imageKeys(imageKeys)
                .build();

        test.addCategories(request.categories());
        testRepository.save(test);

        return TestCreateResponse.from(test);
    }

    private void validateImages(List<MultipartFile> images) {
        if (CollectionUtils.isEmpty(images)) {
            return;
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BaseException(ErrorCode.TEST_002);
        }
        for (MultipartFile image : images) {
            if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
                throw new BaseException(ErrorCode.TEST_003);
            }
        }
    }

    private List<String> uploadImages(List<MultipartFile> images) {
        if (CollectionUtils.isEmpty(images)) {
            return List.of();
        }
        return imageService.uploadFiles(images);
    }
}
