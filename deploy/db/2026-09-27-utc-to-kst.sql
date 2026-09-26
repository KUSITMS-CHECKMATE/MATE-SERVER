-- #336 운영 시각 데이터 UTC → KST 이관(1회용)
-- 전제: mate·mate-bot 파드 0개, pg_dump 백업 완료
-- 실행: 저장소 루트에서 psql 접속 후 \i deploy/db/2026-09-27-utc-to-kst.sql
--      (\i 경로가 상대경로이므로 반드시 저장소 루트에서 실행)
-- 기대 출력(2026-09-27 실제 운영 스키마 덤프로 검증한 기준): 테이블 22개, 컬럼 55개 이동
--      test.reopened_at 존재 시 컬럼 +1, discord_message 존재 시 테이블 +1·컬럼 +2
-- 금지: psql -1(단일 트랜잭션 자동커밋)·GUI 툴(pgAdmin 등) 사용 금지
--      -1이나 자동커밋 모드에서는 수동 COMMIT 전 결과 확인 단계가 사라짐
-- 마무리: 출력 확인 후 COMMIT; 이상 시 ROLLBACK;
--        (COMMIT 없이 psql 종료 시 전부 롤백)
-- 성공 판정: COMMIT; 입력 후 psql 응답이 COMMIT이어야 함
--          ROLLBACK으로 응답하면 이미 에러로 중단된 트랜잭션이라 아무것도 반영되지 않은 상태임
-- 사전 점검 실패 안내:
--   '다른 접속 존재' 에러: 앱 파드가 아직 붙어 있다는 뜻(파드 0개 재확인 후 재실행)
--   '이미 KST 데이터로 보임' 에러: 대상 DB가 잘못됐거나 이미 이관된 상태(대상 DB 재확인)
\set ON_ERROR_STOP on

BEGIN;

CREATE TABLE IF NOT EXISTS public.tz_migration_log (
    name        varchar(100) PRIMARY KEY,
    executed_at timestamp    NOT NULL
);

DO $$
DECLARE
    migration_name CONSTANT text := '336-utc-to-kst';
    -- KST 벽시계로 저장된 컬럼(이관 제외)
    kst_columns CONSTANT text[] := ARRAY[
        'test.closed_at',
        'test_draft.closed_at',
        'payment.approved_at'
    ];
    -- 감사 컬럼(created_at·updated_at·deleted_at) 외 UTC 기록 컬럼(이관 대상)
    utc_columns CONSTANT text[] := ARRAY[
        'promotion_reward.executed_at',
        'promotion_reward.resolved_at',
        'toss_accounts.last_login_at',
        'toss_accounts.last_token_refreshed_at',
        'toss_accounts.unlinked_at',
        'discord_outbox.next_attempt_at',
        'discord_outbox.sent_at',
        'test.reopened_at'
    ];
    unclassified text;
    target record;
    before_max timestamp;
    after_max timestamp;
    updated_rows bigint;
    total_tables integer := 0;
    total_columns integer := 0;
    other_sessions text;
    created_at_tables text[];
    max_created_at timestamp;
    utc_now timestamp;
BEGIN
    -- 두 번 실행 방지(+18시간 방지)
    IF EXISTS (SELECT 1 FROM public.tz_migration_log WHERE name = migration_name) THEN
        RAISE EXCEPTION '이미 실행된 이관: %', migration_name;
    END IF;

    -- 다른 접속 세션 존재 시 중단(앱 파드 잔존 의심): HikariCP 등 잔존 연결이 있으면 이관 중 데이터 변경 경합 위험
    SELECT string_agg(format('%s / %s / %s ×%s', usename, application_name, client_addr, cnt), ', ')
      INTO other_sessions
      FROM (
            SELECT usename, application_name, client_addr, count(*) AS cnt
              FROM pg_stat_activity
             WHERE datname = current_database()
               AND pid <> pg_backend_pid()
               AND backend_type = 'client backend'
             GROUP BY usename, application_name, client_addr
           ) s;
    IF other_sessions IS NOT NULL THEN
        RAISE EXCEPTION '다른 접속 존재(앱 파드 잔존 의심): %', other_sessions;
    END IF;

    -- 이미 KST로 이관된 데이터로 보이면 중단(잘못된 대상 DB 실행 방지)
    SELECT array_agg(DISTINCT c.relname)
      INTO created_at_tables
      FROM pg_catalog.pg_attribute a
      JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
      JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public'
       AND c.relkind IN ('r', 'p')
       AND a.attnum > 0
       AND NOT a.attisdropped
       AND c.relname <> 'tz_migration_log'
       AND a.attname = 'created_at'
       AND a.atttypid = 'timestamp'::regtype;

    IF created_at_tables IS NOT NULL THEN
        EXECUTE (
            SELECT 'SELECT max(x) FROM (' ||
                   string_agg(format('SELECT max(created_at) AS x FROM public.%I', t), ' UNION ALL ') ||
                   ') s'
              FROM unnest(created_at_tables) AS t
        ) INTO max_created_at;
    END IF;

    utc_now := now() AT TIME ZONE 'UTC';
    IF max_created_at IS NOT NULL AND max_created_at > utc_now + interval '10 minutes' THEN
        RAISE EXCEPTION '최신 created_at(%)이 현재 UTC(%)보다 늦음 — 이미 KST 데이터로 보임', max_created_at, utc_now;
    END IF;

    -- 분류되지 않은 시각 컬럼 존재 시 중단(조용한 누락 방지)
    -- information_schema 대신 pg_catalog 사용: 실행 롤의 권한이 없는 테이블도 누락 없이 탐지
    SELECT string_agg(format('%s.%s (%s)', c.relname, a.attname,
                             pg_catalog.format_type(a.atttypid, a.atttypmod)), ', '
                       ORDER BY c.relname, a.attname)
      INTO unclassified
      FROM pg_catalog.pg_attribute a
      JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
      JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public'
       AND c.relkind IN ('r', 'p')
       AND a.attnum > 0
       AND NOT a.attisdropped
       AND c.relname <> 'tz_migration_log'
       AND (
             -- timestamptz·date·time·timetz는 분류 대상 밖이므로 무조건 미분류 처리
             a.atttypid IN ('timestamptz'::regtype, 'date'::regtype, 'time'::regtype, 'timetz'::regtype)
             OR (
                   a.atttypid = 'timestamp'::regtype
                   AND NOT (a.attname IN ('created_at', 'updated_at', 'deleted_at')
                            OR c.relname || '.' || a.attname = ANY (utc_columns || kst_columns))
                 )
           );
    IF unclassified IS NOT NULL THEN
        RAISE EXCEPTION '분류되지 않은 시각 컬럼: %', unclassified;
    END IF;

    -- KST 컬럼을 뺀 모든 시각 컬럼 +9시간, 테이블당 UPDATE 한 번
    FOR target IN
        SELECT c.relname AS table_name,
               string_agg(format('%1$I = %1$I + interval ''9 hours''', a.attname), ', '
                          ORDER BY a.attname) AS set_clause,
               bool_or(a.attname = 'created_at') AS has_created_at,
               count(*) AS column_count
          FROM pg_catalog.pg_attribute a
          JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
          JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'public'
           AND c.relkind IN ('r', 'p')
           AND a.attnum > 0
           AND NOT a.attisdropped
           AND c.relname <> 'tz_migration_log'
           AND a.atttypid = 'timestamp'::regtype
           AND NOT (c.relname || '.' || a.attname = ANY (kst_columns))
         GROUP BY c.relname
         ORDER BY c.relname
    LOOP
        before_max := NULL;
        after_max := NULL;
        IF target.has_created_at THEN
            EXECUTE format('SELECT max(created_at) FROM public.%I', target.table_name) INTO before_max;
        END IF;

        EXECUTE format('UPDATE public.%I SET %s', target.table_name, target.set_clause);
        GET DIAGNOSTICS updated_rows = ROW_COUNT;

        IF target.has_created_at THEN
            EXECUTE format('SELECT max(created_at) FROM public.%I', target.table_name) INTO after_max;
        END IF;
        RAISE NOTICE '% | %행 | [%] | 최신 created_at % -> %',
            target.table_name, updated_rows, target.set_clause, before_max, after_max;

        total_tables := total_tables + 1;
        total_columns := total_columns + target.column_count;
    END LOOP;

    -- 요약: 실제 반영 대상은 운영 스키마 존재 여부(test.reopened_at·discord_message)에 따라 달라질 수 있음
    RAISE NOTICE '요약 | 테이블 %개 | 컬럼 %개 이동', total_tables, total_columns;

    INSERT INTO public.tz_migration_log (name, executed_at)
    VALUES (migration_name, now() AT TIME ZONE 'Asia/Seoul');
END
$$;

-- 확인용: 테스트 2 리포트(이관 전 2026-09-22 15:00:17.575228 → 기대 2026-09-23 00:00:17.575228)
SELECT id, test_id, created_at FROM public.report WHERE test_id = 2 ORDER BY id LIMIT 2;

-- 확인용: 현재 KST(테이블별 최신 created_at이 이 값보다 앞서고 가까워야 함)
SELECT now() AT TIME ZONE 'Asia/Seoul' AS kst_now;
