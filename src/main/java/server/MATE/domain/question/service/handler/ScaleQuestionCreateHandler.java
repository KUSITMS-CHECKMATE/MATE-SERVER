package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Component
public class ScaleQuestionCreateHandler extends AbstractQuestionCreateHandler<ScaleCreateRequest> {

    private final ScaleRepository scaleRepository;

    public ScaleQuestionCreateHandler(ScaleRepository scaleRepository) {
        super(ScaleCreateRequest.class);
        this.scaleRepository = scaleRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.SCALE;
    }

    @Override
    protected void validateTyped(ScaleCreateRequest item) {
        if (item.range() != 5 && item.range() != 7) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }
    }

    @Override
    protected void createDetailTyped(Question question, ScaleCreateRequest item) {
        Scale scale = Scale.builder()
                .question(question)
                .imageKey(item.imageKey())
                .minLabel(item.minLabel())
                .maxLabel(item.maxLabel())
                .range(item.range())
                .build();
        scaleRepository.save(scale);
    }

    @Override
    protected List<String> extractImageKeysTyped(ScaleCreateRequest item) {
        return item.imageKey() == null || item.imageKey().isBlank()
                ? List.of()
                : List.of(item.imageKey());
    }
}
