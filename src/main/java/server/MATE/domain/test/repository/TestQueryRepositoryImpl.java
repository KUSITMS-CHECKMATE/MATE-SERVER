package server.MATE.domain.test.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
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

    // 진행 중(IN_PROGRESS) 테스트를 검수 중(WAITING) 테스트보다 앞에 노출하기 위한 정렬 우선순위
    private static NumberExpression<Integer> inProgressFirst() {
        return new CaseBuilder()
                .when(test.testStatus.eq(TestStatus.IN_PROGRESS)).then(0)
                .otherwise(1);
    }

    // (completedAt desc, id desc) 정렬에서 커서 항목 다음에 오는 테스트만 거르는 조건. 첫 페이지면 조건 없음
    private static BooleanExpression completedBefore(LocalDateTime cursorCompletedAt, Long cursorId) {
        if (cursorCompletedAt == null || cursorId == null) {
            return null;
        }
        return test.completedAt.lt(cursorCompletedAt)
                .or(test.completedAt.eq(cursorCompletedAt).and(test.id.lt(cursorId)));
    }

    // REJECTED 상태가 아닌 경우만 거르는 조건 (사용자-facing 조회에서 반려된 테스트 숨김)
    private static BooleanExpression statusNotRejected() {
        return test.testStatus.ne(TestStatus.REJECTED);
    }


    // 컬렉션 fetch join은 offset/limit과 함께 쓰면 메모리 페이징이 되므로, categories는 @BatchSize로 지연 로딩함
    @Override
    public List<Test> findAvailableTestsForUser(List<TestStatus> statuses, LocalDateTime closedAt, Long userId,
                                                int offset, int limit) {
        if (statuses == null || statuses.isEmpty() || closedAt == null || userId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(test)
                .where(statusIn(statuses), notDeleted(), closedAtAfter(closedAt), notParticipatedBy(userId))
                .orderBy(inProgressFirst().asc(), test.closedAt.asc(), test.createdAt.desc(), test.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countAvailableTestsForUser(List<TestStatus> statuses, LocalDateTime closedAt, Long userId) {
        if (statuses == null || statuses.isEmpty() || closedAt == null || userId == null) {
            return 0L;
        }

        Long count = queryFactory
                .select(test.count())
                .from(test)
                .where(statusIn(statuses), notDeleted(), closedAtAfter(closedAt), notParticipatedBy(userId))
                .fetchOne();
        return count == null ? 0L : count;
    }

    // completedAt이 없는 종료 테스트는 정렬 기준이 없어 제외함 (기존 데이터는 배포 시 completedAt 보정 필요)
    @Override
    public List<Test> findCompletedTests(LocalDateTime cursorCompletedAt, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(test)
                .where(
                        test.testStatus.eq(TestStatus.COMPLETED),
                        notDeleted(),
                        test.completedAt.isNotNull(),
                        completedBefore(cursorCompletedAt, cursorId)
                )
                .orderBy(test.completedAt.desc(), test.id.desc())
                .limit(limit)
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

    @Override
    public List<Test> findByStatusForAdmin(TestStatus status, int offset, int limit) {
        if (status == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(test)
                .where(test.testStatus.eq(status), notDeleted())
                .orderBy(test.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByStatusForAdmin(TestStatus status) {
        if (status == null) {
            return 0L;
        }

        Long count = queryFactory
                .select(test.count())
                .from(test)
                .where(test.testStatus.eq(status), notDeleted())
                .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public Optional<Test> findWithCategoriesByIdForAdmin(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(test)
                        .leftJoin(test.categories).fetchJoin()
                        .where(idEq(id), notDeleted())
                        .distinct()
                        .fetchOne()
        );
    }
}
