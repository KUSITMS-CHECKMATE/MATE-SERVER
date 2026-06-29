-- answers.contention ID
SELECT '=== answers.contention.hotTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_ANS_CTN_HOT';

SELECT '=== answers.contention.distributedPool ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title)
FROM test WHERE title LIKE 'LT_ANS_CTN_%' AND title != 'LT_ANS_CTN_HOT';

-- answers.failure ID
SELECT '=== answers.failure.duplicateTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_ANS_DUP';

SELECT '=== answers.failure.capacityExceededTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_ANS_CAP';

-- likes ID
SELECT '=== likes.hotTestId ===' AS section;
SELECT id FROM test WHERE title = 'LT_LIKE_HOT';

SELECT '=== likes.distributedPool ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title)
FROM test WHERE title LIKE 'LT_LIKE_%' AND title != 'LT_LIKE_HOT';

-- drafts ID
SELECT '=== drafts.small ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test_draft WHERE title LIKE 'LT_DRAFT_S_%';

SELECT '=== drafts.medium ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test_draft WHERE title LIKE 'LT_DRAFT_M_%';

SELECT '=== drafts.large ===' AS section;
SELECT ARRAY_AGG(id ORDER BY title) FROM test_draft WHERE title LIKE 'LT_DRAFT_L_%';

-- answerPayloads용 questionId 조회
SELECT '=== LT_ANS_CTN_HOT questions (answerPayloads.contention) ===' AS section;
SELECT q.id AS question_id, q.sequence
FROM question q JOIN test t ON t.id = q.test_id
WHERE t.title = 'LT_ANS_CTN_HOT'
ORDER BY q.sequence;

-- contention 테스트 질문 수 확인
SELECT '=== contention 테스트 질문 수 (각 5개여야 함) ===' AS section;
SELECT t.title, COUNT(q.id) AS question_count
FROM test t LEFT JOIN question q ON q.test_id = t.id
WHERE t.title IN ('LT_ANS_CTN_HOT','LT_ANS_CTN_1','LT_ANS_CTN_2','LT_ANS_CTN_3')
GROUP BY t.title ORDER BY t.title;

-- FAILURE_DUPLICATE participation 사전 삽입 확인
SELECT '=== LT_ANS_DUP participation 수 (50이어야 함) ===' AS section;
SELECT COUNT(*) FROM participation
WHERE test_id = (SELECT id FROM test WHERE title = 'LT_ANS_DUP')
  AND deleted_at IS NULL;
