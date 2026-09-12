package server.MATE.domain.participation.repository;

import server.MATE.domain.answer.dto.response.MyAnswerItemView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface ParticipationQueryRepository {

    boolean existsActiveParticipation(Long testId, Long testerId);

    Set<Long> findParticipatedTestIds(List<Long> testIds, Long testerId);

    List<MyAnswerItemView> findMyAnswerItemsByTesterId(Long testerId);

    long softDeleteByTestId(Long testId, LocalDateTime deletedAt);

    long deleteByTestId(Long testId);
}
