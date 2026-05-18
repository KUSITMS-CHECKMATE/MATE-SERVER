package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.AbTestCreateRequest;
import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;

import java.util.List;

@Component
public class AbTestQuestionCreateHandler extends AbstractQuestionCreateHandler<AbTestCreateRequest> {

    private final AbTestRepository abTestRepository;

    public AbTestQuestionCreateHandler(AbTestRepository abTestRepository) {
        super(AbTestCreateRequest.class);
        this.abTestRepository = abTestRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.AB_TEST;
    }

    @Override
    protected void validateTyped(AbTestCreateRequest item) {
    }

    @Override
    protected void createDetailTyped(Question question, AbTestCreateRequest item) {
        AbTest abTest = AbTest.builder()
                .question(question)
                .aImageKey(item.aImageKey())
                .bImageKey(item.bImageKey())
                .imageRatio(item.imageRatio())
                .build();
        abTestRepository.save(abTest);
    }

    @Override
    protected List<String> extractImageKeysTyped(AbTestCreateRequest item) {
        return List.of(item.aImageKey(), item.bImageKey());
    }
}
