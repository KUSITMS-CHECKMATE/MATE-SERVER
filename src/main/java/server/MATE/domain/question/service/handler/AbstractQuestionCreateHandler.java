package server.MATE.domain.question.service.handler;

import server.MATE.domain.question.dto.request.QuestionCreateItem;
import server.MATE.domain.question.entity.Question;

import java.util.List;

public abstract class AbstractQuestionCreateHandler<T extends QuestionCreateItem> implements QuestionCreateHandler {

    private final Class<T> itemType;

    protected AbstractQuestionCreateHandler(Class<T> itemType) {
        this.itemType = itemType;
    }

    @Override
    public final void validate(QuestionCreateItem item) {
        validateTyped(cast(item));
    }

    @Override
    public final void createDetail(Question question, QuestionCreateItem item) {
        createDetailTyped(question, cast(item));
    }

    @Override
    public final List<String> extractImageKeys(QuestionCreateItem item) {
        return extractImageKeysTyped(cast(item));
    }

    protected abstract void validateTyped(T item);

    protected abstract void createDetailTyped(Question question, T item);

    protected abstract List<String> extractImageKeysTyped(T item);

    protected final T cast(QuestionCreateItem item) {
        return itemType.cast(item);
    }
}
