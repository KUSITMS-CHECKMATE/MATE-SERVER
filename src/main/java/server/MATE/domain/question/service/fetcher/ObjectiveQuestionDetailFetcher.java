package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.ObjectiveDetailResponse;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.global.storage.service.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObjectiveQuestionDetailFetcher implements QuestionDetailFetcher {

    private final ObjectiveRepository objectiveRepository;
    private final FileStorageService fileStorageService;

    @Override
    public QuestionType supports() {
        return QuestionType.OBJECTIVE;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Objective> objectives = objectiveRepository.findAllByIdIn(questionMap.keySet());
        return objectives.stream()
                .collect(Collectors.toMap(
                        Objective::getId,
                        objective -> ObjectiveDetailResponse.of(
                                questionMap.get(objective.getId()),
                                objective,
                                this::toImageUrl
                        )
                ));
    }

    private String toImageUrl(String key) {
        if (key == null) return null;
        return fileStorageService.generateDownloadUrl(key);
    }
}
