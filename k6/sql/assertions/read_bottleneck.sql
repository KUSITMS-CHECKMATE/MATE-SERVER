SELECT '=== 조회 시나리오: pplCount/likeCount 변화 없음 확인 ===' AS section;
SELECT id, title, ppl_count, like_count, test_status
FROM test WHERE title IN ('LT_HOT_001') OR title LIKE 'LT_DIST_%';
