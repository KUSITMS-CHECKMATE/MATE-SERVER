package server.MATE.domain.test.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.MATE.domain.participation.entity.QParticipation;
import server.MATE.domain.test.entity.QTest;
import server.MATE.domain.test.entity.QTestLike;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TestQueryRepositoryImpl implements TestQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QTest test = QTest.test;

    // ── BooleanExpression 조각 ──────────────────────────────────────────────

    private static BooleanExpression notDeleted() {
        return test.deletedAt.isNull();
    }

    private static BooleanExpression idEq(Long id) {
        return id != null ? test.id.eq(id) : null;
    }

    private static BooleanExpression makerIdEq(Long makerId) {
        return makerId != null ? test.makerId.eq(makerId) : null;
    }

    private static BooleanExpression statusIn(List<TestStatus> statuses) {
        return (statuses != null && !statuses.isEmpty())
                ? test.testStatus.in(statuses) : null;
    }

    private static BooleanExpression closedAtAfter(LocalDateTime time) {
        return time != null ? test.closedAt.goe(time) : null;
    }

    /**
     * 특정 유저가 이 테스트에 참여한 경우 true — 미래 메서드 조합용 조각.
     * 사용 예: .where(notParticipatedBy(userId)) → 내가 아직 참여 안 한 테스트 필터
     */
    private static BooleanExpression participatedBy(Long userId) {
        if (userId == null) return null;
        QParticipation p = QParticipation.participation;
        return JPAExpressions
                .selectOne()
                .from(p)
                .where(
                        p.testId.eq(test.id),
                        p.testerId.eq(userId),
                        p.deletedAt.isNull()
                )
                .exists();
    }

    private static BooleanExpression notParticipatedBy(Long userId) {
        if (userId == null) return null;
        return participatedBy(userId).not();
    }

    // ── 메서드 구현 (스텁 — Task 4, 5에서 교체) ────────────────────────────

    @Override
    public List<Test> findActiveTests(List<TestStatus> statuses, LocalDateTime closedAt) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Test> findByMakerId(Long makerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Test> findLikedTests(Long userId, List<TestStatus> statuses) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Test> findActiveById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Test> findWithCategoriesById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Test> findByIdForUpdate(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
