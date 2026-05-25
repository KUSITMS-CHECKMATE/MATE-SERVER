package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.AbTestDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;
import server.MATE.global.storage.FileStorageService;
import server.MATE.global.storage.dto.ImageResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AbTestQuestionDetailFetcher implements QuestionDetailFetcher {

    private final AbTestRepository abTestRepository;
    private final FileStorageService fileStorageService;

    @Override
    public QuestionType supports() {
        return QuestionType.AB_TEST;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<AbTest> abTests = abTestRepository.findAllByIdIn(questionMap.keySet());
        return abTests.stream()
                .collect(Collectors.toMap(
                        AbTest::getId,
                        abTest -> AbTestDetailResponse.of(
                                questionMap.get(abTest.getId()),
                                abTest,
                                toImageResponse(abTest.getAImageKey()),
                                toImageResponse(abTest.getBImageKey())
                        )
                ));
    }

    private ImageResponse toImageResponse(String key) {
        if (key == null) return null;
        return new ImageResponse(key, fileStorageService.generateDownloadUrl(key));
    }
}
