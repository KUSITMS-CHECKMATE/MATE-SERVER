SELECT '=== 경합: pplCount vs 실제 participation 수 ===' AS section;
WITH ct AS (SELECT id, goal_ppl, ppl_count FROM test WHERE title = 'LT_ANS_CTN_HOT')
SELECT
    ct.id AS test_id,
    ct.goal_ppl,
    ct.ppl_count AS stored_ppl_count,
    COUNT(p.id) AS actual_participation,
    (ct.ppl_count = COUNT(p.id)) AS counts_match,
    (ct.ppl_count <= ct.goal_ppl) AS within_capacity
FROM ct
LEFT JOIN participation p ON p.test_id = ct.id AND p.deleted_at IS NULL
GROUP BY ct.id, ct.goal_ppl, ct.ppl_count;

SELECT '=== 경합: 중복 participation (0행이어야 함) ===' AS section;
SELECT test_id, tester_id, COUNT(*) AS cnt
FROM participation
WHERE test_id = (SELECT id FROM test WHERE title = 'LT_ANS_CTN_HOT')
  AND deleted_at IS NULL
GROUP BY test_id, tester_id HAVING COUNT(*) > 1;

SELECT '=== 실패 시나리오: capacity pplCount ≤ goalPpl ===' AS section;
SELECT id, title, goal_ppl, ppl_count, (ppl_count <= goal_ppl) AS ok
FROM test WHERE title = 'LT_ANS_CAP';

SELECT '=== like: like_count 정합성 ===' AS section;
WITH lt AS (SELECT id, like_count FROM test WHERE title = 'LT_LIKE_HOT')
SELECT
    lt.id AS test_id,
    lt.like_count AS stored_like_count,
    COUNT(tl.id) AS actual_like_count,
    (lt.like_count = COUNT(tl.id)) AS counts_match,
    (lt.like_count >= 0) AS no_negative
FROM lt
LEFT JOIN test_like tl ON tl.test_id = lt.id
GROUP BY lt.id, lt.like_count;

SELECT '=== like: 중복 like (0행이어야 함) ===' AS section;
SELECT test_id, user_id, COUNT(*) AS cnt
FROM test_like
WHERE test_id = (SELECT id FROM test WHERE title = 'LT_LIKE_HOT')
GROUP BY test_id, user_id HAVING COUNT(*) > 1;

SELECT '=== draft: 최근 수정 상태 (updated_at이 테스트 시간대이면 갱신 확인됨) ===' AS section;
SELECT id, title, goal_ppl, reward, closed_at, status, updated_at
FROM test_draft
WHERE id BETWEEN 148 AND 162
ORDER BY id;

SELECT '=== draft: 핵심 필드 null 체크 (0행이어야 정상) ===' AS section;
SELECT id, title
FROM test_draft
WHERE id BETWEEN 148 AND 162
  AND (title IS NULL OR goal_ppl IS NULL OR reward IS NULL OR closed_at IS NULL);
