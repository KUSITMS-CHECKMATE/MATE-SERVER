package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.FiveSecondCreateRequest;
import server.MATE.domain.question.dto.request.FiveSecondOptionRequest;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Component
public class FiveSecondQuestionCreateHandler extends AbstractQuestionCreateHandler<FiveSecondCreateRequest> {

    private final FiveSecondRepository fiveSecondRepository;

    public FiveSecondQuestionCreateHandler(FiveSecondRepository fiveSecondRepository) {
        super(FiveSecondCreateRequest.class);
        this.fiveSecondRepository = fiveSecondRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    protected void validateTyped(FiveSecondCreateRequest item) {
        if (!item.isObjective()) {
            return;
        }

        List<FiveSecondOptionRequest> options = item.options();
        if (options == null || options.size() < 2) {
            throw new BaseException(BaseErrorCode.QUESTION_004);
        }

        if (!Boolean.TRUE.equals(item.isDuplicate())) {
            return;
        }

        int optionCount = options.size();
        Integer min = item.minSelect();
        Integer max = item.maxSelect();

        if (min != null && min < 1) {
            throw new BaseException(BaseErrorCode.QUESTION_001);
        }
        if (min != null && max != null && max < min) {
            throw new BaseException(BaseErrorCode.QUESTION_002);
        }
        if (min != null && min > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
        if (max != null && max > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
    }

    @Override
    protected void createDetailTyped(Question question, FiveSecondCreateRequest item) {
        boolean isDuplicate = Boolean.TRUE.equals(item.isDuplicate());
        Integer minSelect = (item.isObjective() && isDuplicate) ? item.minSelect() : null;
        Integer maxSelect = (item.isObjective() && isDuplicate) ? item.maxSelect() : null;

        FiveSecond fiveSecond = FiveSecond.builder()
                .question(question)
                .imageKey(item.imageKey())
                .isObjective(item.isObjective())
                .isDuplicate(item.isObjective() ? item.isDuplicate() : null)
                .minSelect(minSelect)
                .maxSelect(maxSelect)
                .build();

        if (item.isObjective() && item.options() != null) {
            List<FiveSecondOptionRequest> optionRequests = item.options();
            for (int i = 0; i < optionRequests.size(); i++) {
                FiveSecondOption option = FiveSecondOption.builder()
                        .fiveSecond(fiveSecond)
                        .content(optionRequests.get(i).content())
                        .sequence(i + 1)
                        .build();
                fiveSecond.addOption(option);
            }
        }

        fiveSecondRepository.save(fiveSecond);
    }

    @Override
    protected List<String> extractImageKeysTyped(FiveSecondCreateRequest item) {
        return List.of(item.imageKey());
    }
}
