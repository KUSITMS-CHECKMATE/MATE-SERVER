-- write_concurrency 데이터(LT_ANS_*, LT_LIKE_*, LT_DRAFT_*), 유저(accounts) 데아터 유지
-- LT_HOT_*, LT_DIST_*, LT_QS_*, LT_LIST_* 를 삭제함

BEGIN;

-- 질문 세부 테이블 삭제
DELETE FROM tree_test
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM five_second_option
WHERE five_second_id IN (
    SELECT fs.question_id FROM five_second fs
    JOIN question q ON q.id = fs.question_id
    JOIN test t ON t.id = q.test_id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM five_second
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM card_sorting
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM ab_test
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM objective_option
WHERE objective_id IN (
    SELECT ob.question_id FROM objective ob
    JOIN question q ON q.id = ob.question_id
    JOIN test t ON t.id = q.test_id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM objective
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM subjective
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

DELETE FROM scale
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON q.test_id = t.id
    WHERE t.title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%'
);

-- question 삭제
DELETE FROM question
WHERE test_id IN (SELECT id FROM test WHERE title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%');

-- test 삭제
DELETE FROM test WHERE title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%';

COMMIT;


SELECT COUNT(*) AS remaining FROM test WHERE title SIMILAR TO 'LT_(HOT|DIST|QS|LIST)%';
