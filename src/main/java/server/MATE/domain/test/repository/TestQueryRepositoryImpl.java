package server.MATE.domain.test.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.MATE.domain.participation.entity.QParticipation;
import server.MATE.domain.test.entity.QTestLike;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static server.MATE.domain.test.entity.QTest.test;

@Repository
@RequiredArgsConstructor
public class TestQueryRepositoryImpl implements TestQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 삭제되지 않은 테스트만 가져오는 조건
    private static BooleanExpression notDeleted() {
        return test.deletedAt.isNull();
    }

    // 전달받은 ID와 테스트 ID가 같은 경우만 거르는 조건
    private static BooleanExpression idEq(Long id) {
        return test.id.eq(id);
    }

    // 전달받은 사용자와 테스트 생성자가 같은 경우만 거르는 조건
    private static BooleanExpression makerIdEq(Long makerId) {
        return test.makerId.eq(makerId);
    }

    // 전달받은 상태 목록(statuses)인 테스트만 가져오는 조건
    private static BooleanExpression statusIn(List<TestStatus> statuses) {
        return test.testStatus.in(statuses);
    }

    // 테스트 마감 기한이 기준 시간(time)과 같거나 이후인 경우만 거르는 조건
    private static BooleanExpression closedAtAfter(LocalDateTime time) {
        return test.closedAt.goe(time);
    }

    // 현재 조회 중인 테스트에 해당 유저가 응답한 경우만 거르는 조건
    private static BooleanExpression participatedBy(Long userId) {
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

    // 현재 조회 중인 테스트에 해당 유저가 응답하지 않은 경우만 거르는 조건
    private static BooleanExpression notParticipatedBy(Long userId) {
        return participatedBy(userId).not();
    }

    // REJECTED 상태가 아닌 경우만 거르는 조건 (사용자-facing 조회에서 반려된 테스트 숨김)
    private static BooleanExpression statusNotRejected() {
        return test.testStatus.ne(TestStatus.REJECTED);
    }


    @Override
    public List<Test> findAvailableTestsForUser(List<TestStatus> statuses, LocalDateTime closedAt, Long userId) {
        if (statuses == null || statuses.isEmpty() || closedAt == null || userId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(test)
                .leftJoin(test.categories).fetchJoin()
                .where(statusIn(statuses), notDeleted(), closedAtAfter(closedAt), notParticipatedBy(userId))
                .orderBy(test.createdAt.desc())
                .distinct()
                .fetch();
    }

    @Override
    public List<Test> findByMakerId(Long makerId) {
        if (makerId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(test)
                .where(makerIdEq(makerId), notDeleted())
                .orderBy(test.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Test> findLikedTests(Long userId, List<TestStatus> statuses) {
        if (userId == null || statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        QTestLike testLike = QTestLike.testLike;
        return queryFactory
                .selectFrom(test)
                .join(testLike).on(testLike.testId.eq(test.id))
                .where(
                        testLike.userId.eq(userId),
                        notDeleted(),
                        statusIn(statuses)
                )
                .orderBy(testLike.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Test> findActiveById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(test)
                        .where(idEq(id), notDeleted())
                        .fetchOne()
        );
    }

    @Override
    public Optional<Test> findByIdIncludingDeleted(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(test)
                        .where(idEq(id))
                        .fetchOne()
        );
    }

    @Override
    public long softDeleteById(Long id, LocalDateTime deletedAt) {
        if (id == null || deletedAt == null) {
            return 0L;
        }

        return queryFactory
                .update(test)
                .set(test.deletedAt, deletedAt)
                .where(idEq(id), notDeleted())
                .execute();
    }

    @Override
    public Optional<Test> findWithCategoriesById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(test)
                        // categories를 fetch join하면 테스트가 중복 조회될 수 있어 distinct로 중복 제거함
                        // 컬렉션 fetch join은 하나만 두는 편이 안전함
                        .leftJoin(test.categories).fetchJoin()
                        .where(idEq(id), notDeleted(), statusNotRejected())
                        .distinct()
                        .fetchOne()
        );
    }

    @Override
    public Optional<Test> findByIdForUpdate(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(test)
                        .where(idEq(id), notDeleted())
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public List<Test> findExpiredInProgressTests(LocalDateTime now) {
        if (now == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(test)
                .where(
                        test.testStatus.eq(TestStatus.IN_PROGRESS),
                        test.closedAt.lt(now),
                        notDeleted()
                )
                .fetch();
    }
}
