-- k6 부하테스트 계정 + 연관 데이터 일괄 삭제
-- ci prefix: K6_CI_USER_ / K6_CI_MAKER_
-- FK 순서: promotion_reward → answer → participation → test_like → test_draft → toss_account → users

BEGIN;

-- 1. promotion_reward (participation FK)
DELETE FROM promotion_reward
WHERE participation_id IN (
    SELECT p.id
    FROM participation p
    JOIN users u ON u.id = p.tester_id
    WHERE u.ci LIKE 'K6_CI_%'
);

-- 2. answer (participation FK)
DELETE FROM answer
WHERE participation_id IN (
    SELECT p.id
    FROM participation p
    JOIN users u ON u.id = p.tester_id
    WHERE u.ci LIKE 'K6_CI_%'
);

-- 3. participation
DELETE FROM participation
WHERE tester_id IN (SELECT id FROM users WHERE ci LIKE 'K6_CI_%');

-- 4. test_like
DELETE FROM test_like
WHERE user_id IN (SELECT id FROM users WHERE ci LIKE 'K6_CI_%');

-- 5. test_draft (메이커 계정으로 생성된 draft)
DELETE FROM test_draft
WHERE maker_id IN (SELECT id FROM users WHERE ci LIKE 'K6_CI_MAKER_%');

-- 6. toss_account (K6 계정에 연결된 경우)
DELETE FROM toss_account
WHERE user_id IN (SELECT id FROM users WHERE ci LIKE 'K6_CI_%');

-- 7. users
DELETE FROM users WHERE ci LIKE 'K6_CI_USER_%';
DELETE FROM users WHERE ci LIKE 'K6_CI_MAKER_%';

COMMIT;

-- 확인 (둘 다 0)
SELECT COUNT(*) AS k6_users FROM users WHERE ci LIKE 'K6_CI_USER_%';
SELECT COUNT(*) AS k6_makers FROM users WHERE ci LIKE 'K6_CI_MAKER_%';
