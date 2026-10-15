-- #350 기존 종료 테스트의 종료 시각(completed_at) 보정(1회용)
-- 배경: completed_at은 #350 배포 시 ddl-auto: update로 추가되어 기존 행은 NULL임
--      종료 테스트 목록(GET /api/v1/tests/completed)은 completed_at이 NULL인 테스트를 제외하므로,
--      보정 전까지 기존 종료 테스트가 발견 탭에 노출되지 않음
-- 보정 값: LEAST(closed_at, updated_at)
--      실제 종료 시각은 기록된 적이 없어 근사치로 채움
--      updated_at은 종료 이후 찜하기·리포트 상태 변경 등으로 늦어질 수 있어 마감 예정 시각(closed_at)을 상한으로 둠
-- 전제: #350 배포 완료(test.completed_at 컬럼 생성 확인)
-- 실행: 저장소 루트에서 psql 접속 후 \i deploy/db/2026-10-15-test-completed-at-backfill.sql
--      (\i 경로가 상대경로이므로 반드시 저장소 루트에서 실행)
-- 재실행: completed_at IS NULL 행만 갱신하므로 여러 번 실행해도 결과가 같음
-- 마무리: 출력 확인 후 COMMIT; 이상 시 ROLLBACK;
--        (COMMIT 없이 psql 종료 시 전부 롤백)
\set ON_ERROR_STOP on

BEGIN;

-- 확인용: 보정 대상 건수
SELECT count(*) AS target_rows
FROM public.test
WHERE test_status = 'COMPLETED' AND completed_at IS NULL;

UPDATE public.test
SET completed_at = LEAST(closed_at, updated_at)
WHERE test_status = 'COMPLETED' AND completed_at IS NULL;

-- 확인용: 0이어야 함
SELECT count(*) AS remaining_rows
FROM public.test
WHERE test_status = 'COMPLETED' AND completed_at IS NULL;

-- 확인용: 최근 종료 순 상위 10건(completed_at이 closed_at 이하인지 확인)
SELECT id, title, closed_at, updated_at, completed_at
FROM public.test
WHERE test_status = 'COMPLETED' AND deleted_at IS NULL
ORDER BY completed_at DESC, id DESC
LIMIT 10;
