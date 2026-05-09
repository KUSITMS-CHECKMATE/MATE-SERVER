package server.MATE.domain.question.service.fetcher;

import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;
import java.util.Map;

public interface QuestionDetailFetcher {

    QuestionType supports();

    Map<Long, QuestionDetailItem> fetch(List<Question> questions);
}
