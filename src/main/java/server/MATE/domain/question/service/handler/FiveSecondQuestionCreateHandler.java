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
        // 주관식 5초 테스트일 때, 객관식 설정값이 포함되어있지 않음을 검증
        if (!item.isObjective()) {
            if ((item.options() != null && !item.options().isEmpty())
                    || item.isDuplicate() != null
                    || item.minSelect() != null
                    || item.maxSelect() != null) {
                throw new BaseException(BaseErrorCode.QUESTION_008);
            }
            return;
        }

        // 객관식 5초 테스트는 옵션(선지)를 최소 2개 이상 가져야 함
        List<FiveSecondOptionRequest> options = item.options();
        if (options == null || options.size() < 2) {
            throw new BaseException(BaseErrorCode.QUESTION_004);
        }

        // 단일 선택이면 min/max 선택 개수 검증을 수행하지 않음
        if (!Boolean.TRUE.equals(item.isDuplicate())) {
            return;
        }

        int optionCount = options.size();
        Integer min = item.minSelect();
        Integer max = item.maxSelect();

        // 최소 선택 개수는 1 이상이어야 함
        if (min != null && min < 1) {
            throw new BaseException(BaseErrorCode.QUESTION_001);
        }

        // 최대 선택 개수는 최소 선택 개수보다 작을 수 없음
        if (min != null && max != null && max < min) {
            throw new BaseException(BaseErrorCode.QUESTION_002);
        }

        // min/max 선택 개수는 전체 선택지 개수를 초과할 수 없음
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
