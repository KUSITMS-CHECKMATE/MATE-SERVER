/**
 * Hot test 상세 조회
 * ──────────────────────────────────────────────────────────────────
 * 목적: 하나의 인기 testId 에 요청이 몰릴 때 p95/p99가 어떻게 변하는지 확인
 * API:  GET /api/v1/tests/{testId}
 * 패턴: 80% hot testId / 20% distributed testId pool
 * 비교 목표:
 *   - distributed 상세 조회(읽기 기준선) 대비 p95 증가폭
 *   - hot row / 쿼리 집중 징후 (Hikari pending, slow query 로그)
 *   - 경고: same testId 구간에서 distributed 대비 p95 2배 이상
 */

import { sleep } from 'k6';
import { getUserToken, authHeaders } from '../../lib/auth.js';
import { getHotReadTestId, getDistributedReadTestId } from '../../lib/data.js';
import { get, checkStatus, bodySize } from '../../lib/request.js';
import { detailBodySize, hotSuccessRate, distributedSuccessRate } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 3,
    duration: '3m',
  },
  load: {
    executor: 'constant-vus',
    vus: 20,
    duration: '15m',
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 20 },
      { duration: '5m', target: 20 },
      { duration: '1m', target: 40 },
      { duration: '5m', target: 40 },
      { duration: '1m', target: 80 },
      { duration: '5m', target: 80 },
      { duration: '1m', target: 120 },
      { duration: '5m', target: 120 },
      { duration: '2m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: {
    hot_test_detail: profiles[SCENARIO],
  },
  thresholds: {
    // Load 기준 성공 기준: 에러율 1% 미만
    http_req_failed: ['rate<0.01'],
    // 경고 기준: p95 2초 초과 시 주의 (baseline 확보 후 조정)
    'http_req_duration{traffic:hot}':         ['p(95)<2000'],
    'http_req_duration{traffic:distributed}': ['p(95)<2000'],
  },
};

export default function () {
  const token = getUserToken();
  const headers = authHeaders(token);

  // 80% hot / 20% distributed
  const isHot = Math.random() < 0.8;
  const testId = isHot ? getHotReadTestId() : getDistributedReadTestId();
  const trafficTag = isHot ? 'hot' : 'distributed';

  const res = get(
    `/api/v1/tests/${testId}`,
    headers,
    { scenario: 'hot_detail', traffic: trafficTag, testId: String(testId) }
  );

  const ok = checkStatus(res, 200, `hot_detail[${trafficTag}]`);

  // 커스텀 메트릭 기록
  detailBodySize.add(bodySize(res));
  if (isHot) {
    hotSuccessRate.add(ok);
  } else {
    distributedSuccessRate.add(ok);
  }

  sleep(1 + Math.random() * 2);
}
