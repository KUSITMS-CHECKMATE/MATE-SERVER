package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.image.ImageService;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final ImageService imageService;

    public TestCreateResponse createTest(TestCreateRequest request, Long makerId) {
        List<String> imageKeys = request.imageKeys() != null ? request.imageKeys() : List.of();

        Test test = Test.builder()
                .makerId(makerId)
                .title(request.title())
                .description(request.description())
                .serviceName(request.serviceName())
                .serviceDescription(request.serviceDescription())
                .imageKeys(imageKeys)
                .build();

        test.addCategories(request.categories());
        try {
            testRepository.save(test);
        } catch (Exception e) {
            if (!imageKeys.isEmpty()) {
                imageService.deleteFiles(imageKeys);
            }
            throw e;
        }

        return TestCreateResponse.from(test);
    }
}
