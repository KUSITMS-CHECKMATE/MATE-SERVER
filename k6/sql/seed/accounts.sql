-- 다른 seed 파일보다 가장 먼저 실행하기

BEGIN;

INSERT INTO users (ci, name, role, created_at, updated_at)
SELECT
    'LOADTEST_CI_USER_' || LPAD(gs::TEXT, 3, '0'),
    'lt_user_' || LPAD(gs::TEXT, 3, '0'),
    'USER', NOW(), NOW()
FROM generate_series(1, 50) AS gs
ON CONFLICT (ci) DO NOTHING;

INSERT INTO users (ci, name, role, created_at, updated_at)
SELECT
    'LOADTEST_CI_MAKER_' || LPAD(gs::TEXT, 3, '0'),
    'lt_maker_' || LPAD(gs::TEXT, 3, '0'),
    'USER', NOW(), NOW()
FROM generate_series(1, 10) AS gs
ON CONFLICT (ci) DO NOTHING;

COMMIT;
