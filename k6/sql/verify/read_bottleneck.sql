-- 계정 확인
SELECT '=== 계정 수 ===' AS section;
SELECT role, COUNT(*) AS count
FROM users WHERE ci LIKE 'LOADTEST_CI_%'
GROUP BY role;

-- read.hotTestId
SELECT '=== read.hotTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_HOT_001';

-- read.distributedPool
SELECT '=== read.distributedPool (10개) ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test WHERE title LIKE 'LT_DIST_%';

-- questions.hotTestId
SELECT '=== questions.hotTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_HOT_001';

-- questions 세트 ID
SELECT '=== questions.setsA ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test WHERE title LIKE 'LT_QS_A_%';

SELECT '=== questions.setsB ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test WHERE title LIKE 'LT_QS_B_%';

SELECT '=== questions.setsC ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test WHERE title LIKE 'LT_QS_C_%';

-- 질문 수 확인 (A=10, B=30, C=100)
SELECT '=== 질문 수 ===' AS section;
SELECT t.title, COUNT(q.id) AS question_count
FROM test t LEFT JOIN question q ON q.test_id = t.id
WHERE t.title LIKE 'LT_QS_%'
GROUP BY t.title ORDER BY t.title;

-- LIST Variant A 기준선 총 수 (~50개)
SELECT '=== LOADTEST_SVC 테스트 총 수 (Variant A 기준 ~50) ===' AS section;
SELECT COUNT(*) AS total FROM test WHERE service_name = 'LOADTEST_SVC' AND deleted_at IS NULL;

-- 정합성 체크
SELECT '=== pplCount > goalPpl 이상 없음 ===' AS section;
SELECT id, title, ppl_count, goal_ppl
FROM test WHERE service_name = 'LOADTEST_SVC' AND ppl_count > goal_ppl;
