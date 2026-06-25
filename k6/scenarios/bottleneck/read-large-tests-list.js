/**
 * 대용량 목록 조회
 * ──────────────────────────────────────────────────────────────────
 * 목적: 페이지네이션 미도입 구조에서 데이터 수 증가에 따른 성능 악화 수치화
 * API:  GET /api/v1/tests
 * 패턴: distributed traffic (단, K6_LIST_VARIANT 로 목록 규모 제어)
 * 비교 목표:
 *   - 목록 A vs B vs C 에서 p95/p99 변화
 *   - 응답 body 크기 증가 추이
 *   - DB heap/GC/CPU 영향
 *   - 경고: A 대비 C p95 3배 이상
 *
 * ⚠️  이 시나리오는 DB 데이터 규모가 핵심 변수입니다.
 *     스크립트 실행 전 sql/seed/read_bottleneck.sql 로 목록 A/B/C 세트를 미리 세팅하세요.
 */

import { sleep } from 'k6';
import { getUserToken, authHeaders } from '../../lib/auth.js';
import { get, checkStatus, bodySize } from '../../lib/request.js';
import { listBodySize, distributedSuccessRate } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';
const LIST_VARIANT = __ENV.K6_LIST_VARIANT || 'A';

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 3,
    duration: '3m',
  },
  load: {
    // 동시성 확대보다 데이터 규모 비교가 목적이므로 VU 고정
    executor: 'constant-vus',
    vus: 10,
    duration: '10m',
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 10 },
      { duration: '5m', target: 10 },
      { duration: '1m', target: 20 },
      { duration: '5m', target: 20 },
      { duration: '1m', target: 40 },
      { duration: '5m', target: 40 },
      { duration: '1m', target: 80 },
      { duration: '5m', target: 80 },
      { duration: '2m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: {
    large_tests_list: profiles[SCENARIO],
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // 목록 조회는 데이터 규모별 임계치가 달라지므로 절대값은 참고용
    http_req_duration: ['p(95)<5000'],
  },
};

export default function () {
  const token = getUserToken();
  const headers = authHeaders(token);

  const res = get(
    '/api/v1/tests',
    headers,
    { scenario: 'large_list', variant: LIST_VARIANT }
  );

  const ok = checkStatus(res, 200, `large_list[${LIST_VARIANT}]`);

  // 응답 크기 기록 (실험군별 비교 핵심 지표로 활용)
  const size = bodySize(res);
  listBodySize.add(size, { variant: LIST_VARIANT });

  distributedSuccessRate.add(ok);

  sleep(1 + Math.random() * 2);
}
