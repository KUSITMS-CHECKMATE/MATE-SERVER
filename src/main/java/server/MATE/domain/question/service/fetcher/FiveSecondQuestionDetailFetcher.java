package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.FiveSecondDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.global.storage.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FiveSecondQuestionDetailFetcher implements QuestionDetailFetcher {

    private final FiveSecondRepository fiveSecondRepository;
    private final FileStorageService fileStorageService;

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<FiveSecond> fiveSeconds = fiveSecondRepository.findAllByIdIn(questionMap.keySet());
        return fiveSeconds.stream()
                .collect(Collectors.toMap(
                        FiveSecond::getId,
                        fiveSecond -> FiveSecondDetailResponse.of(
                                questionMap.get(fiveSecond.getId()),
                                fiveSecond,
                                toImageUrl(fiveSecond.getImageKey())
                        )
                ));
    }

    private String toImageUrl(String key) {
        if (key == null) return null;
        return fileStorageService.generateDownloadUrl(key);
    }
}
