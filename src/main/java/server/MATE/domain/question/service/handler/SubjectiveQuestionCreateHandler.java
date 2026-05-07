package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.SubjectiveCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;
import server.MATE.domain.question.repository.SubjectiveRepository;

import java.util.List;

@Component
public class SubjectiveQuestionCreateHandler extends AbstractQuestionCreateHandler<SubjectiveCreateRequest> {

    private final SubjectiveRepository subjectiveRepository;

    public SubjectiveQuestionCreateHandler(SubjectiveRepository subjectiveRepository) {
        super(SubjectiveCreateRequest.class);
        this.subjectiveRepository = subjectiveRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    protected void validateTyped(SubjectiveCreateRequest item) {
    }

    @Override
    protected void createDetailTyped(Question question, SubjectiveCreateRequest item) {
        Subjective subjective = Subjective.builder()
                .question(question)
                .imageKey(item.imageKey())
                .build();
        subjectiveRepository.save(subjective);
    }

    @Override
    protected List<String> extractImageKeysTyped(SubjectiveCreateRequest item) {
        return item.imageKey() == null || item.imageKey().isBlank()
                ? List.of()
                : List.of(item.imageKey());
    }
}
