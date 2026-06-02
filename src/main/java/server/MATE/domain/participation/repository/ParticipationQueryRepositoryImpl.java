package server.MATE.domain.participation.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.MATE.domain.answer.dto.response.MyAnswerItemView;

import java.time.LocalDateTime;
import java.util.List;

import static server.MATE.domain.participation.entity.QParticipation.participation;
import static server.MATE.domain.test.entity.QTest.test;

@Repository
@RequiredArgsConstructor
public class ParticipationQueryRepositoryImpl implements ParticipationQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    private static BooleanExpression participationNotDeleted() {
        return participation.deletedAt.isNull();
    }

    private static BooleanExpression testNotDeleted() {
        return test.deletedAt.isNull();
    }

    private static BooleanExpression testIdEq(Long testId) {
        return participation.testId.eq(testId);
    }

    private static BooleanExpression testerIdEq(Long testerId) {
        return participation.testerId.eq(testerId);
    }

    @Override
    public boolean existsActiveParticipation(Long testId, Long testerId) {
        if (testId == null || testerId == null) {
            return false;
        }

        Integer found = queryFactory
                .selectOne()
                .from(participation)
                .where(testIdEq(testId), testerIdEq(testerId), participationNotDeleted())
                .fetchFirst();

        return found != null;
    }

    @Override
    public List<MyAnswerItemView> findMyAnswerItemsByTesterId(Long testerId) {
        if (testerId == null) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        MyAnswerItemView.class,
                        test.id,
                        test.title,
                        participation.createdAt,
                        test.reward
                ))
                .from(participation)
                .join(test).on(test.id.eq(participation.testId))
                .where(testerIdEq(testerId), participationNotDeleted(), testNotDeleted())
                .orderBy(participation.createdAt.desc())
                .fetch();
    }

    @Override
    public long softDeleteByTestId(Long testId, LocalDateTime deletedAt) {
        if (testId == null || deletedAt == null) {
            return 0L;
        }

        em.flush();
        long affected = queryFactory
                .update(participation)
                .set(participation.deletedAt, deletedAt)
                .where(testIdEq(testId), participationNotDeleted())
                .execute();
        em.clear();
        return affected;
    }

    @Override
    public long deleteByTestId(Long testId) {
        if (testId == null) {
            return 0L;
        }

        em.flush();
        long affected = queryFactory
                .delete(participation)
                .where(testIdEq(testId))
                .execute();
        em.clear();
        return affected;
    }
}
