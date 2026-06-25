BEGIN;

-- ────────────────────────────────────────────────────────────────────────────
-- Hot test 상세 조회
-- ────────────────────────────────────────────────────────────────────────────

-- HOT testId (1개)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id, 'LT_HOT_001', 'LT hot read test',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM users u WHERE u.ci = 'LOADTEST_CI_MAKER_001' LIMIT 1;

-- Distributed pool (10개)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id,
    'LT_DIST_' || LPAD(gs::TEXT, 3, '0'),
    'LT dist read ' || gs,
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM generate_series(1, 10) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;


-- ────────────────────────────────────────────────────────────────────────────
-- Hot test 질문 조회
-- ────────────────────────────────────────────────────────────────────────────

-- 질문 수 세트 A/B/C (각 2개)
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id,
    'LT_QS_' || label || '_' || LPAD(gs::TEXT, 2, '0'),
    'LT questions set ' || label,
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM (VALUES ('A'), ('B'), ('C')) AS sets(label)
CROSS JOIN generate_series(1, 2) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_001' LIMIT 1) AS u;

-- 7가지 질문 유형을 다양하게 구성
--
-- 유형 배치
--   SET A (7문항):  유형별 1개,  seq 1-7
--     SCALE=1, SUBJECTIVE=2, OBJECTIVE=3, AB_TEST=4
--     CARD_SORTING=5, FIVE_SECOND=6, TREE_TEST=7
--   SET B (28문항): 유형별 4개
--     SCALE=1-4, SUBJECTIVE=5-8, OBJECTIVE=9-12, AB_TEST=13-16
--     CARD_SORTING=17-20, FIVE_SECOND=21-24, TREE_TEST=25-28
--   SET C (98문항): 유형별 14개
--     SCALE=1-14, SUBJECTIVE=15-28, OBJECTIVE=29-42, AB_TEST=43-56
--     CARD_SORTING=57-70, FIVE_SECOND=71-84, TREE_TEST=85-98
--
-- 질문 유형별 내용
--   scale        — scale_range: seq%3=1→5, seq%3=2→3, seq%3=0→7
--   subjective   — image_key:   seq%2=0→더미 이미지, 홀수→NULL
--   objective    — isDuplicate: seq%2=0→복수선택, 홀수→단일
--                  isOther:     seq%3=0→true, 나머지→false
--   ab_test      — image_ratio: seq%3=1→RATIO_1_1, seq%3=2→RATIO_4_3, seq%3=0→RATIO_9_16
--   card_sorting — cards:       seq%3=1→4개, seq%3=2→8개, seq%3=0→12개
--                  categories:  seq%3=1→2개, seq%3=2→3개, seq%3=0→0개(open sort)
--   five_second  — isObjective: seq%2=0→객관식, 홀수→주관식
--                  image_ratio: seq%3=1→RATIO_1_1, seq%3=2→RATIO_4_3, seq%3=0→RATIO_9_16
--   tree_test    — A/B: 2레벨 (root×3, child×2 = 9노드)
--                  C:   3레벨 (root×3, child×2, grandchild×2 = 21노드)

-- SET A: 7문항 (유형별 1개)
INSERT INTO question (test_id, question_type, title, sequence, created_at, updated_at)
SELECT t.id, ti.qtype, 'LT Q ' || ti.seq, ti.seq, NOW(), NOW()
FROM (VALUES
    ('SCALE',        1),
    ('SUBJECTIVE',   2),
    ('OBJECTIVE',    3),
    ('AB_TEST',      4),
    ('CARD_SORTING', 5),
    ('FIVE_SECOND',  6),
    ('TREE_TEST',    7)
) AS ti(qtype, seq)
CROSS JOIN (SELECT id FROM test WHERE title IN ('LT_QS_A_01', 'LT_QS_A_02')) AS t(id);

-- SCALE: range=5, 기본 라벨
INSERT INTO scale (question_id, min_label, max_label, scale_range, created_at, updated_at)
SELECT q.id, '전혀 아니다', '매우 그렇다', 5, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'SCALE';

-- SUBJECTIVE: 이미지 없음
INSERT INTO subjective (question_id, image_key, created_at, updated_at)
SELECT q.id, NULL, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'SUBJECTIVE';

-- OBJECTIVE: 단일 선택, 옵션 3개
INSERT INTO objective (question_id, is_duplicate, max_select, min_select, is_other, created_at, updated_at)
SELECT q.id, FALSE, NULL, NULL, FALSE, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'OBJECTIVE';

INSERT INTO objective_option (objective_id, content, image_key, sequence, is_other_option, created_at, updated_at)
SELECT o.question_id, opt.content, NULL, opt.seq, FALSE, NOW(), NOW()
FROM objective o
JOIN question q ON o.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('선택지1', 1), ('선택지2', 2), ('선택지3', 3)) AS opt(content, seq)
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02');

-- AB_TEST: RATIO_1_1
INSERT INTO ab_test (question_id, a_image_key, b_image_key, image_ratio, created_at, updated_at)
SELECT q.id, 'LT_AB_A', 'LT_AB_B', 'RATIO_1_1', NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'AB_TEST';

-- CARD_SORTING: 카드 6개, 카테고리 2개
INSERT INTO card_sorting (question_id, cards, categories, created_at, updated_at)
SELECT q.id,
    '["카드A","카드B","카드C","카드D","카드E","카드F"]'::json,
    '["카테고리1","카테고리2"]'::json,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'CARD_SORTING';

-- FIVE_SECOND: 주관식, RATIO_1_1
INSERT INTO five_second (question_id, image_key, image_ratio, is_objective,
                         is_duplicate, min_select, max_select, is_other, created_at, updated_at)
SELECT q.id, 'LT_FS_IMG', 'RATIO_1_1', FALSE, NULL, NULL, NULL, NULL, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'FIVE_SECOND';

-- TREE_TEST: root×3 + child×2 = 9 노드
INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT q.id, NULL, node.label, node.seq, 0, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('메뉴A', 1), ('메뉴B', 2), ('메뉴C', 3)) AS node(label, seq)
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02') AND q.question_type = 'TREE_TEST';

INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT root.question_id, root.id, child.label, child.seq, 1, NOW(), NOW()
FROM tree_test root
JOIN question q ON root.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('항목1', 1), ('항목2', 2)) AS child(label, seq)
WHERE t.title IN ('LT_QS_A_01', 'LT_QS_A_02')
  AND q.question_type = 'TREE_TEST'
  AND root.parent_id IS NULL;

-- SET B: 28문항 (유형별 4개)
INSERT INTO question (test_id, question_type, title, sequence, created_at, updated_at)
SELECT t.id, ti.qtype, 'LT Q ' || ti.gs, ti.gs, NOW(), NOW()
FROM (
    SELECT gs, 'SCALE'        AS qtype FROM generate_series(1,  4)  gs UNION ALL
    SELECT gs, 'SUBJECTIVE'            FROM generate_series(5,  8)  gs UNION ALL
    SELECT gs, 'OBJECTIVE'             FROM generate_series(9,  12) gs UNION ALL
    SELECT gs, 'AB_TEST'               FROM generate_series(13, 16) gs UNION ALL
    SELECT gs, 'CARD_SORTING'          FROM generate_series(17, 20) gs UNION ALL
    SELECT gs, 'FIVE_SECOND'           FROM generate_series(21, 24) gs UNION ALL
    SELECT gs, 'TREE_TEST'             FROM generate_series(25, 28) gs
) AS ti(gs, qtype)
CROSS JOIN (SELECT id FROM test WHERE title IN ('LT_QS_B_01', 'LT_QS_B_02')) AS t(id);

-- SCALE: seq%3=1→range5, seq%3=2→range3, seq%3=0→range7
INSERT INTO scale (question_id, min_label, max_label, scale_range, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 3 = 1 THEN '전혀 아니다' ELSE NULL END,
    CASE WHEN q.sequence % 3 = 1 THEN '매우 그렇다' ELSE NULL END,
    CASE WHEN q.sequence % 3 = 1 THEN 5
         WHEN q.sequence % 3 = 2 THEN 3
         ELSE 7 END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'SCALE';

-- SUBJECTIVE: seq%2=0→더미 이미지, 홀수→NULL
INSERT INTO subjective (question_id, image_key, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 2 = 0 THEN 'LT_SUBJ_IMG' ELSE NULL END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'SUBJECTIVE';

-- OBJECTIVE: isDuplicate — seq%2=0→복수, 홀수→단일 / isOther — seq%3=0→true
INSERT INTO objective (question_id, is_duplicate, max_select, min_select, is_other, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 2 = 0 THEN TRUE ELSE FALSE END,
    CASE WHEN q.sequence % 2 = 0 THEN 2   ELSE NULL  END,
    CASE WHEN q.sequence % 2 = 0 THEN 1   ELSE NULL  END,
    CASE WHEN q.sequence % 3 = 0 THEN TRUE ELSE FALSE END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'OBJECTIVE';

INSERT INTO objective_option (objective_id, content, image_key, sequence, is_other_option, created_at, updated_at)
SELECT o.question_id, opt.content, NULL, opt.seq, FALSE, NOW(), NOW()
FROM objective o
JOIN question q ON o.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('선택지1', 1), ('선택지2', 2), ('선택지3', 3)) AS opt(content, seq)
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02');

-- isOther=true 항목에만 기타 옵션 추가
INSERT INTO objective_option (objective_id, content, image_key, sequence, is_other_option, created_at, updated_at)
SELECT o.question_id, '기타', NULL, 4, TRUE, NOW(), NOW()
FROM objective o
JOIN question q ON o.question_id = q.id
JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND o.is_other = TRUE;

-- AB_TEST: seq%3=1→RATIO_1_1, seq%3=2→RATIO_4_3, seq%3=0→RATIO_9_16
INSERT INTO ab_test (question_id, a_image_key, b_image_key, image_ratio, created_at, updated_at)
SELECT q.id,
    'LT_AB_A', 'LT_AB_B',
    CASE WHEN q.sequence % 3 = 1 THEN 'RATIO_1_1'
         WHEN q.sequence % 3 = 2 THEN 'RATIO_4_3'
         ELSE 'RATIO_9_16' END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'AB_TEST';

-- CARD_SORTING: seq%3=1→4cards/2cat, seq%3=2→8cards/3cat, seq%3=0→12cards/0cat(open)
INSERT INTO card_sorting (question_id, cards, categories, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 3 = 1 THEN
        '["카드A","카드B","카드C","카드D"]'::json
    WHEN q.sequence % 3 = 2 THEN
        '["카드A","카드B","카드C","카드D","카드E","카드F","카드G","카드H"]'::json
    ELSE
        '["카드A","카드B","카드C","카드D","카드E","카드F","카드G","카드H","카드I","카드J","카드K","카드L"]'::json
    END,
    CASE WHEN q.sequence % 3 = 1 THEN '["카테고리1","카테고리2"]'::json
         WHEN q.sequence % 3 = 2 THEN '["카테고리1","카테고리2","카테고리3"]'::json
         ELSE '[]'::json
    END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'CARD_SORTING';

-- FIVE_SECOND: seq%2=0→객관식, 홀수→주관식 / imageRatio seq%3 패턴
INSERT INTO five_second (question_id, image_key, image_ratio, is_objective,
                         is_duplicate, min_select, max_select, is_other, created_at, updated_at)
SELECT q.id,
    'LT_FS_IMG',
    CASE WHEN q.sequence % 3 = 1 THEN 'RATIO_1_1'
         WHEN q.sequence % 3 = 2 THEN 'RATIO_4_3'
         ELSE 'RATIO_9_16' END,
    CASE WHEN q.sequence % 2 = 0 THEN TRUE ELSE FALSE END,
    CASE WHEN q.sequence % 2 = 0 THEN FALSE ELSE NULL END,
    NULL, NULL, NULL,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'FIVE_SECOND';

-- isObjective=true 항목에만 선택지 추가
INSERT INTO five_second_option (five_second_id, content, sequence, is_other_option, created_at, updated_at)
SELECT fs.question_id, opt.content, opt.seq, FALSE, NOW(), NOW()
FROM five_second fs
JOIN question q ON fs.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('선택지1', 1), ('선택지2', 2), ('선택지3', 3)) AS opt(content, seq)
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND fs.is_objective = TRUE;

-- TREE_TEST: root×3 + child×2 = 9 노드
INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT q.id, NULL, node.label, node.seq, 0, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('메뉴A', 1), ('메뉴B', 2), ('메뉴C', 3)) AS node(label, seq)
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02') AND q.question_type = 'TREE_TEST';

INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT root.question_id, root.id, child.label, child.seq, 1, NOW(), NOW()
FROM tree_test root
JOIN question q ON root.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('항목1', 1), ('항목2', 2)) AS child(label, seq)
WHERE t.title IN ('LT_QS_B_01', 'LT_QS_B_02')
  AND q.question_type = 'TREE_TEST'
  AND root.parent_id IS NULL;

-- SET C: 98문항 (유형별 14개)
INSERT INTO question (test_id, question_type, title, sequence, created_at, updated_at)
SELECT t.id, ti.qtype, 'LT Q ' || ti.gs, ti.gs, NOW(), NOW()
FROM (
    SELECT gs, 'SCALE'        AS qtype FROM generate_series(1,  14) gs UNION ALL
    SELECT gs, 'SUBJECTIVE'            FROM generate_series(15, 28) gs UNION ALL
    SELECT gs, 'OBJECTIVE'             FROM generate_series(29, 42) gs UNION ALL
    SELECT gs, 'AB_TEST'               FROM generate_series(43, 56) gs UNION ALL
    SELECT gs, 'CARD_SORTING'          FROM generate_series(57, 70) gs UNION ALL
    SELECT gs, 'FIVE_SECOND'           FROM generate_series(71, 84) gs UNION ALL
    SELECT gs, 'TREE_TEST'             FROM generate_series(85, 98) gs
) AS ti(gs, qtype)
CROSS JOIN (SELECT id FROM test WHERE title IN ('LT_QS_C_01', 'LT_QS_C_02')) AS t(id);

-- SCALE: seq%3=1→range5, seq%3=2→range3, seq%3=0→range7
INSERT INTO scale (question_id, min_label, max_label, scale_range, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 3 = 1 THEN '전혀 아니다' ELSE NULL END,
    CASE WHEN q.sequence % 3 = 1 THEN '매우 그렇다' ELSE NULL END,
    CASE WHEN q.sequence % 3 = 1 THEN 5
         WHEN q.sequence % 3 = 2 THEN 3
         ELSE 7 END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'SCALE';

-- SUBJECTIVE: seq%2=0→더미 이미지, 홀수→NULL
INSERT INTO subjective (question_id, image_key, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 2 = 0 THEN 'LT_SUBJ_IMG' ELSE NULL END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'SUBJECTIVE';

-- OBJECTIVE: isDuplicate — seq%2=0→복수, 홀수→단일 / isOther — seq%3=0→true
INSERT INTO objective (question_id, is_duplicate, max_select, min_select, is_other, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 2 = 0 THEN TRUE ELSE FALSE END,
    CASE WHEN q.sequence % 2 = 0 THEN 2   ELSE NULL  END,
    CASE WHEN q.sequence % 2 = 0 THEN 1   ELSE NULL  END,
    CASE WHEN q.sequence % 3 = 0 THEN TRUE ELSE FALSE END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'OBJECTIVE';

INSERT INTO objective_option (objective_id, content, image_key, sequence, is_other_option, created_at, updated_at)
SELECT o.question_id, opt.content, NULL, opt.seq, FALSE, NOW(), NOW()
FROM objective o
JOIN question q ON o.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('선택지1', 1), ('선택지2', 2), ('선택지3', 3)) AS opt(content, seq)
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02');

INSERT INTO objective_option (objective_id, content, image_key, sequence, is_other_option, created_at, updated_at)
SELECT o.question_id, '기타', NULL, 4, TRUE, NOW(), NOW()
FROM objective o
JOIN question q ON o.question_id = q.id
JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND o.is_other = TRUE;

-- AB_TEST: seq%3=1→RATIO_1_1, seq%3=2→RATIO_4_3, seq%3=0→RATIO_9_16
INSERT INTO ab_test (question_id, a_image_key, b_image_key, image_ratio, created_at, updated_at)
SELECT q.id,
    'LT_AB_A', 'LT_AB_B',
    CASE WHEN q.sequence % 3 = 1 THEN 'RATIO_1_1'
         WHEN q.sequence % 3 = 2 THEN 'RATIO_4_3'
         ELSE 'RATIO_9_16' END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'AB_TEST';

-- CARD_SORTING: seq%3=1→4cards/2cat, seq%3=2→8cards/3cat, seq%3=0→12cards/0cat(open)
INSERT INTO card_sorting (question_id, cards, categories, created_at, updated_at)
SELECT q.id,
    CASE WHEN q.sequence % 3 = 1 THEN
        '["카드A","카드B","카드C","카드D"]'::json
    WHEN q.sequence % 3 = 2 THEN
        '["카드A","카드B","카드C","카드D","카드E","카드F","카드G","카드H"]'::json
    ELSE
        '["카드A","카드B","카드C","카드D","카드E","카드F","카드G","카드H","카드I","카드J","카드K","카드L"]'::json
    END,
    CASE WHEN q.sequence % 3 = 1 THEN '["카테고리1","카테고리2"]'::json
         WHEN q.sequence % 3 = 2 THEN '["카테고리1","카테고리2","카테고리3"]'::json
         ELSE '[]'::json
    END,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'CARD_SORTING';

-- FIVE_SECOND: seq%2=0→객관식, 홀수→주관식 / imageRatio seq%3 패턴
INSERT INTO five_second (question_id, image_key, image_ratio, is_objective,
                         is_duplicate, min_select, max_select, is_other, created_at, updated_at)
SELECT q.id,
    'LT_FS_IMG',
    CASE WHEN q.sequence % 3 = 1 THEN 'RATIO_1_1'
         WHEN q.sequence % 3 = 2 THEN 'RATIO_4_3'
         ELSE 'RATIO_9_16' END,
    CASE WHEN q.sequence % 2 = 0 THEN TRUE ELSE FALSE END,
    CASE WHEN q.sequence % 2 = 0 THEN FALSE ELSE NULL END,
    NULL, NULL, NULL,
    NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'FIVE_SECOND';

INSERT INTO five_second_option (five_second_id, content, sequence, is_other_option, created_at, updated_at)
SELECT fs.question_id, opt.content, opt.seq, FALSE, NOW(), NOW()
FROM five_second fs
JOIN question q ON fs.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('선택지1', 1), ('선택지2', 2), ('선택지3', 3)) AS opt(content, seq)
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND fs.is_objective = TRUE;

-- TREE_TEST: root×3 + child×2 + grandchild×2 = 21 노드 (깊이 3)
INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT q.id, NULL, node.label, node.seq, 0, NOW(), NOW()
FROM question q JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('메뉴A', 1), ('메뉴B', 2), ('메뉴C', 3)) AS node(label, seq)
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02') AND q.question_type = 'TREE_TEST';

INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT root.question_id, root.id, child.label, child.seq, 1, NOW(), NOW()
FROM tree_test root
JOIN question q ON root.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('항목1', 1), ('항목2', 2)) AS child(label, seq)
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02')
  AND q.question_type = 'TREE_TEST'
  AND root.parent_id IS NULL;

INSERT INTO tree_test (question_id, parent_id, label, sequence, depth, created_at, updated_at)
SELECT child.question_id, child.id, gchild.label, gchild.seq, 2, NOW(), NOW()
FROM tree_test child
JOIN question q ON child.question_id = q.id
JOIN test t ON q.test_id = t.id
CROSS JOIN (VALUES ('세부1', 1), ('세부2', 2)) AS gchild(label, seq)
WHERE t.title IN ('LT_QS_C_01', 'LT_QS_C_02')
  AND q.question_type = 'TREE_TEST'
  AND child.depth = 1;

COMMIT;


-- ────────────────────────────────────────────────────────────────────────────
-- 대용량 목록 조회
-- ────────────────────────────────────────────────────────────────────────────

-- Variant A: seed 테스트 17개(HOT×1 + DIST×10 + QS×6) + 33개 추가 = ~50개
INSERT INTO test (
    maker_id, title, description, service_name, service_description,
    test_status, report_status, goal_ppl, reward, ppl_count, like_count,
    closed_at, closed_by_maker, refund_waived, created_at, updated_at
)
SELECT u.id,
    'LT_LIST_A_' || LPAD(gs::TEXT, 3, '0'),
    'LT list filler A',
    'LOADTEST_SVC', 'loadtest',
    'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
    NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
FROM generate_series(1, 33) AS gs
CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_002' LIMIT 1) AS u;

-- Variant B: Load 실행 직전에 단독 실행 (+150개, 총 ~200개)
-- BEGIN;
-- INSERT INTO test ( ... )
-- SELECT u.id, 'LT_LIST_B_' || LPAD(gs::TEXT, 3, '0'), 'LT list filler B',
--     'LOADTEST_SVC', 'loadtest', 'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
--     NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
-- FROM generate_series(1, 150) AS gs
-- CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_002' LIMIT 1) AS u;
-- COMMIT;

-- Variant C: Load 실행 직전에 단독 실행 (+300개, 총 ~500개)
-- BEGIN;
-- INSERT INTO test ( ... )
-- SELECT u.id, 'LT_LIST_C_' || LPAD(gs::TEXT, 3, '0'), 'LT list filler C',
--     'LOADTEST_SVC', 'loadtest', 'IN_PROGRESS', 'PENDING', 500, 1000, 0, 0,
--     NOW() + INTERVAL '30 days', FALSE, FALSE, NOW(), NOW()
-- FROM generate_series(1, 300) AS gs
-- CROSS JOIN (SELECT id FROM users WHERE ci = 'LOADTEST_CI_MAKER_002' LIMIT 1) AS u;
-- COMMIT;
