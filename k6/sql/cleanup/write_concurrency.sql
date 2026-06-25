-- read_bottleneck 데이터(LT_HOT_*, LT_DIST_*, LT_QS_*, LT_LIST_*), 유저(accounts) 데아터 유지
-- LT_ANS_*, LT_LIKE_* 테스트, LT_DRAFT_* 초안 을 삭제함

BEGIN;

-- answer 삭제
DELETE FROM answer
WHERE participation_id IN (
    SELECT p.id FROM participation p
    JOIN test t ON t.id = p.test_id
    WHERE t.title SIMILAR TO 'LT_(ANS|LIKE)%'
);

-- participation 삭제
DELETE FROM participation
WHERE test_id IN (SELECT id FROM test WHERE title SIMILAR TO 'LT_(ANS|LIKE)%');

-- test_like 삭제
DELETE FROM test_like
WHERE test_id IN (SELECT id FROM test WHERE title SIMILAR TO 'LT_(ANS|LIKE)%');

-- 질문 세부 테이블 삭제 (write_concurrency는 SCALE 단일 유형)
DELETE FROM scale
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(ANS|LIKE)%'
);

-- question 삭제
DELETE FROM question
WHERE test_id IN (SELECT id FROM test WHERE title SIMILAR TO 'LT_(ANS|LIKE)%');

-- test 삭제
DELETE FROM test WHERE title SIMILAR TO 'LT_(ANS|LIKE)%';

-- test_draft 삭제
DELETE FROM test_draft WHERE title LIKE 'LT_DRAFT_%' OR id BETWEEN 148 AND 162;

COMMIT;


SELECT COUNT(*) AS remaining_tests  FROM test       WHERE title SIMILAR TO 'LT_(ANS|LIKE)%';
SELECT COUNT(*) AS remaining_drafts FROM test_draft WHERE title LIKE 'LT_DRAFT_%' OR id BETWEEN 148 AND 162;
