package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.SubjectiveDetailResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;
import server.MATE.domain.question.repository.SubjectiveRepository;
import server.MATE.global.storage.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubjectiveQuestionDetailFetcher implements QuestionDetailFetcher {

    private final SubjectiveRepository subjectiveRepository;
    private final FileStorageService fileStorageService;

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Subjective> subjectives = subjectiveRepository.findAllByIdIn(questionMap.keySet());
        return subjectives.stream()
                .collect(Collectors.toMap(
                        Subjective::getId,
                        subjective -> SubjectiveDetailResponse.of(
                                questionMap.get(subjective.getId()),
                                subjective,
                                toImageUrl(subjective.getImageKey())
                        )
                ));
    }

    private String toImageUrl(String key) {
        if (key == null) return null;
        return fileStorageService.generateDownloadUrl(key);
    }
}
