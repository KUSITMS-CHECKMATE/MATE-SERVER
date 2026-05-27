package server.MATE.domain.testdraft.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TestDraftValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public QuestionCreateRequest validateForPayment(TestDraft draft) {
        draft.validatePaymentState();
        draft.validateAmountFields();
        draft.validatePublishableFields();
        return resolveQuestionsPayload(draft);
    }

    public QuestionCreateRequest validateForPublish(TestDraft draft) {
        draft.validatePublishState();
        draft.validatePublishableFields();
        return resolveQuestionsPayload(draft);
    }

    private QuestionCreateRequest resolveQuestionsPayload(TestDraft draft) {
        final QuestionCreateRequest request;
        try {
            request = objectMapper.convertValue(
                    draft.getQuestionsPayload(),
                    QuestionCreateRequest.class
            );
        } catch (IllegalArgumentException e) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BaseException(BaseErrorCode.DRAFT_006);
        }
        return request;
    }
}
