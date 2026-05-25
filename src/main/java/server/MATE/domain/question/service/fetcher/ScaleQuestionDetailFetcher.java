package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.ScaleDetailResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.global.storage.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScaleQuestionDetailFetcher implements QuestionDetailFetcher {

    private final ScaleRepository scaleRepository;
    private final FileStorageService fileStorageService;

    @Override
    public QuestionType supports() {
        return QuestionType.SCALE;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Scale> scales = scaleRepository.findAllByIdIn(questionMap.keySet());
        return scales.stream()
                .collect(Collectors.toMap(
                        Scale::getId,
                        scale -> ScaleDetailResponse.of(
                                questionMap.get(scale.getId()),
                                scale,
                                toImageUrl(scale.getImageKey())
                        )
                ));
    }

    private String toImageUrl(String key) {
        if (key == null) return null;
        return fileStorageService.generateDownloadUrl(key);
    }
}
