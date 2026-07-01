-- k6 부하테스트 전용 계정 (운영 DB에 1회 실행)
-- ci prefix: K6_CI_USER_ / K6_CI_MAKER_

BEGIN;

INSERT INTO users (ci, name, role, created_at, updated_at)
SELECT
    'K6_CI_USER_' || LPAD(gs::TEXT, 4, '0'),
    'k6_user_' || LPAD(gs::TEXT, 4, '0'),
    'USER',
    NOW(),
    NOW()
FROM generate_series(1, 1000) AS gs
ON CONFLICT (ci) DO NOTHING;

INSERT INTO users (ci, name, role, created_at, updated_at)
SELECT
    'K6_CI_MAKER_' || LPAD(gs::TEXT, 3, '0'),
    'k6_maker_' || LPAD(gs::TEXT, 3, '0'),
    'USER',
    NOW(),
    NOW()
FROM generate_series(1, 100) AS gs
ON CONFLICT (ci) DO NOTHING;

COMMIT;

-- 확인
SELECT role, COUNT(*) FROM users WHERE ci LIKE 'K6_CI_%' GROUP BY role;
