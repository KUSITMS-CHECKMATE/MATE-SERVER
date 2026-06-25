-- 모든 LOADTEST_SVC 데이터, 계정을 삭제

BEGIN;

-- answer 삭제
DELETE FROM answer
WHERE participation_id IN (
    SELECT p.id FROM participation p
    JOIN test t ON t.id = p.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

-- participation 삭제
DELETE FROM participation
WHERE test_id IN (SELECT id FROM test WHERE service_name = 'LOADTEST_SVC');

-- test_like 삭제
DELETE FROM test_like
WHERE test_id IN (SELECT id FROM test WHERE service_name = 'LOADTEST_SVC');

-- 질문 유형별 테이블 삭제
DELETE FROM scale
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM subjective
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM objective_option
WHERE objective_id IN (
    SELECT ob.question_id FROM objective ob
    JOIN question q ON q.id = ob.question_id
    JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM objective
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM ab_test
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM card_sorting
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM five_second_option
WHERE five_second_id IN (
    SELECT fs.question_id FROM five_second fs
    JOIN question q ON q.id = fs.question_id
    JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM five_second
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

DELETE FROM tree_test
WHERE question_id IN (
    SELECT q.id FROM question q JOIN test t ON t.id = q.test_id
    WHERE t.service_name = 'LOADTEST_SVC'
);

-- question 삭제
DELETE FROM question
WHERE test_id IN (SELECT id FROM test WHERE service_name = 'LOADTEST_SVC');

-- test_category 삭제
DELETE FROM test_category
WHERE test_id IN (SELECT id FROM test WHERE service_name = 'LOADTEST_SVC');

-- test_image 삭제
DELETE FROM test_image
WHERE test_id IN (SELECT id FROM test WHERE service_name = 'LOADTEST_SVC');

-- test 삭제
DELETE FROM test WHERE service_name = 'LOADTEST_SVC';

-- test_draft 삭제
DELETE FROM test_draft WHERE title LIKE 'LT_DRAFT_%' OR id BETWEEN 148 AND 162;

-- 계정 삭제
DELETE FROM users WHERE ci LIKE 'LOADTEST_CI_%';

COMMIT;


SELECT COUNT(*) AS remaining_tests   FROM test        WHERE service_name = 'LOADTEST_SVC';
SELECT COUNT(*) AS remaining_drafts  FROM test_draft  WHERE title LIKE 'LT_DRAFT_%' OR id BETWEEN 148 AND 162;
SELECT COUNT(*) AS remaining_users   FROM users       WHERE ci LIKE 'LOADTEST_CI_%';
