package server.MATE.domain.participation.repository;

import server.MATE.domain.answer.dto.response.MyAnswerItemView;

import java.time.LocalDateTime;
import java.util.List;

public interface ParticipationQueryRepository {

    boolean existsActiveParticipation(Long testId, Long testerId);

    List<MyAnswerItemView> findMyAnswerItemsByTesterId(Long testerId);

    long softDeleteByTestId(Long testId, LocalDateTime deletedAt);

    long deleteByTestId(Long testId);
}
