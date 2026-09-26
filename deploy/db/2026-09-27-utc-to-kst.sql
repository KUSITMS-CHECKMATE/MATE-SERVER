-- #336 운영 시각 데이터 UTC → KST 이관(1회용)
-- 전제: mate·mate-bot 파드 0개, pg_dump 백업 완료
-- 실행: psql 접속 후 \i deploy/db/2026-09-27-utc-to-kst.sql
-- 마무리: 출력 확인 후 COMMIT; 이상 시 ROLLBACK;
--        (COMMIT 없이 psql 종료 시 전부 롤백)
\set ON_ERROR_STOP on

BEGIN;

CREATE TABLE IF NOT EXISTS tz_migration_log (
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
BEGIN
    -- 두 번 실행 방지(+18시간 방지)
    IF EXISTS (SELECT 1 FROM tz_migration_log WHERE name = migration_name) THEN
        RAISE EXCEPTION '이미 실행된 이관: %', migration_name;
    END IF;

    -- 분류되지 않은 시각 컬럼 존재 시 중단(조용한 누락 방지)
    SELECT string_agg(format('%s.%s (%s)', c.table_name, c.column_name, c.data_type), ', ')
      INTO unclassified
      FROM information_schema.columns c
      JOIN information_schema.tables t
        ON t.table_schema = c.table_schema AND t.table_name = c.table_name
     WHERE c.table_schema = 'public'
       AND t.table_type = 'BASE TABLE'
       AND c.data_type LIKE 'timestamp%'
       AND c.table_name <> 'tz_migration_log'
       AND NOT (
             c.data_type = 'timestamp without time zone'
             AND (c.column_name IN ('created_at', 'updated_at', 'deleted_at')
                  OR c.table_name || '.' || c.column_name = ANY (utc_columns || kst_columns))
           );
    IF unclassified IS NOT NULL THEN
        RAISE EXCEPTION '분류되지 않은 시각 컬럼: %', unclassified;
    END IF;

    -- KST 컬럼을 뺀 모든 시각 컬럼 +9시간, 테이블당 UPDATE 한 번
    FOR target IN
        SELECT c.table_name,
               string_agg(format('%1$I = %1$I + interval ''9 hours''', c.column_name), ', '
                          ORDER BY c.column_name) AS set_clause,
               bool_or(c.column_name = 'created_at') AS has_created_at
          FROM information_schema.columns c
          JOIN information_schema.tables t
            ON t.table_schema = c.table_schema AND t.table_name = c.table_name
         WHERE c.table_schema = 'public'
           AND t.table_type = 'BASE TABLE'
           AND c.data_type = 'timestamp without time zone'
           AND c.table_name <> 'tz_migration_log'
           AND NOT (c.table_name || '.' || c.column_name = ANY (kst_columns))
         GROUP BY c.table_name
         ORDER BY c.table_name
    LOOP
        before_max := NULL;
        after_max := NULL;
        IF target.has_created_at THEN
            EXECUTE format('SELECT max(created_at) FROM %I', target.table_name) INTO before_max;
        END IF;

        EXECUTE format('UPDATE %I SET %s', target.table_name, target.set_clause);
        GET DIAGNOSTICS updated_rows = ROW_COUNT;

        IF target.has_created_at THEN
            EXECUTE format('SELECT max(created_at) FROM %I', target.table_name) INTO after_max;
        END IF;
        RAISE NOTICE '% | %행 | [%] | 최신 created_at % -> %',
            target.table_name, updated_rows, target.set_clause, before_max, after_max;
    END LOOP;

    INSERT INTO tz_migration_log (name, executed_at)
    VALUES (migration_name, now() AT TIME ZONE 'Asia/Seoul');
END
$$;

-- 확인용: 테스트 2 리포트(이관 전 2026-09-22 15:00:17.575228 → 기대 2026-09-23 00:00:17.575228)
SELECT id, test_id, created_at FROM report WHERE test_id = 2 ORDER BY id LIMIT 2;

-- 확인용: 현재 KST(테이블별 최신 created_at이 이 값보다 앞서고 가까워야 함)
SELECT now() AT TIME ZONE 'Asia/Seoul' AS kst_now;
