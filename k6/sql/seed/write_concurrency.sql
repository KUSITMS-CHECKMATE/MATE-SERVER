BEGIN;

-- ────────────────────────────────────────────────────────────────────────────
-- 응답 제출 경합
-- ────────────────────────────────────────────────────────────────────────────

-- contention hot (정원 100, IN_PROGRESS)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_ANS_CTN_HOT', 'LT answers contention',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 100, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

-- contention distributed (3개)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_ANS_CTN_' || gs, 'LT ans ctn dist ' || gs,
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM generate_series(1, 3) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

-- contention 테스트들에 5문항 생성
DO $$
DECLARE
    t_id BIGINT;
    titles TEXT[] := ARRAY['LT_ANS_CTN_HOT','LT_ANS_CTN_1','LT_ANS_CTN_2','LT_ANS_CTN_3'];
    t_title TEXT;
BEGIN
    FOREACH t_title IN ARRAY titles LOOP
        SELECT id INTO t_id FROM test WHERE title = t_title;
        IF t_id IS NOT NULL THEN
            INSERT INTO question (test_id, question_type, title, sequence, created_at, updated_at)
            SELECT t_id, 'SCALE', 'LT Q ' || gs, gs, NOW(), NOW()
            FROM generate_series(1, 5) AS gs;

            INSERT INTO scale (question_id, min_label, max_label, scale_range, created_at, updated_at)
            SELECT q.id, 'low', 'high', 5, NOW(), NOW()
            FROM question q WHERE q.test_id = t_id AND q.question_type = 'SCALE';
        END IF;
    END LOOP;
END $$;


-- ────────────────────────────────────────────────────────────────────────────
-- 응답 제출 실패
-- ────────────────────────────────────────────────────────────────────────────

-- 중복 제출 실패용 (participation 사전 삽입)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_ANS_DUP', 'LT answers duplicate fail',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

INSERT INTO question (test_id, question_type, title, sequence, created_at, updated_at)
SELECT t.id, 'SCALE', 'LT Q ' || gs, gs, NOW(), NOW()
FROM generate_series(1, 5) AS gs
CROSS JOIN (SELECT id FROM test WHERE title = 'LT_ANS_DUP') AS t;

INSERT INTO scale (question_id, min_label, max_label, scale_range, created_at, updated_at)
SELECT q.id, 'low', 'high', 5, NOW(), NOW()
FROM question q
WHERE q.test_id = (SELECT id FROM test WHERE title = 'LT_ANS_DUP');

-- 모든 LOADTEST 사용자 participation 사전 삽입
-- (participation 체크가 question 검증보다 먼저 실행 → questionId: 0 payload도 항상 400)
INSERT INTO participation (test_id, tester_id, created_at, updated_at)
SELECT t.id, u.id, NOW(), NOW()
FROM test t
CROSS JOIN users u
WHERE t.title = 'LT_ANS_DUP'
  AND u.ci LIKE 'LOADTEST_CI_USER_%'
ON CONFLICT (test_id, tester_id) DO NOTHING;

UPDATE test
SET ppl_count = (
    SELECT COUNT(*) FROM participation
    WHERE test_id = test.id AND deleted_at IS NULL
)
WHERE title = 'LT_ANS_DUP';

-- 정원 초과 실패용 (pplCount = goalPpl = 10)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_ANS_CAP', 'LT answers capacity fail',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 10, 1000, 10, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;


-- ────────────────────────────────────────────────────────────────────────────
-- like/unlike 경합
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_LIKE_HOT', 'LT like churn hot',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_LIKE_' || gs, 'LT like dist ' || gs,
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM generate_series(1, 3) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;


-- ────────────────────────────────────────────────────────────────────────────
-- draft 반복 수정
-- ────────────────────────────────────────────────────────────────────────────

INSERT INTO test_draft (maker_id, title, description, categories, status, created_at, updated_at)
SELECT u.id,
    'LT_DRAFT_S_' || LPAD(gs::TEXT, 2, '0'),
    'LT draft small',
    '[]'::jsonb, 'DRAFT', NOW(), NOW()
FROM generate_series(1, 5) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

INSERT INTO test_draft (maker_id, title, description, categories, status, created_at, updated_at)
SELECT u.id,
    'LT_DRAFT_M_' || LPAD(gs::TEXT, 2, '0'),
    'LT draft medium',
    '[]'::jsonb, 'DRAFT', NOW(), NOW()
FROM generate_series(1, 5) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_002' LIMIT 1) AS u;

INSERT INTO test_draft (maker_id, title, description, categories, status, created_at, updated_at)
SELECT u.id,
    'LT_DRAFT_L_' || LPAD(gs::TEXT, 2, '0'),
    'LT draft large',
    '[]'::jsonb, 'DRAFT', NOW(), NOW()
FROM generate_series(1, 5) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_003' LIMIT 1) AS u;

COMMIT;
