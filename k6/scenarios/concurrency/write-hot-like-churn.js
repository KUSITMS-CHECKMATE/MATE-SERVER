/**
 * hot like/unlike churn
 * ──────────────────────────────────────────────────────────────────
 * 목적: 같은 testId 에 찜/취소가 몰릴 때 like_count 정합성과 경합 상태 확인
 * API:  POST   /api/v1/tests/{testId}/likes
 *       DELETE /api/v1/tests/{testId}/likes
 * 패턴: 80% hot testId / 20% distributed (VU별 짝수→like, 홀수→unlike 반복)
 * 비교 목표:
 *   - like_count 정합성 (stored = actual COUNT(*) FROM test_like)
 *   - 음수 없음
 *   - 경고: like_count < 0 또는 stored ≠ actual (정합성 불일치)
 */

import { sleep } from 'k6';
import { getUserToken, authHeaders } from '../../lib/auth.js';
import { getLikeTestId } from '../../lib/data.js';
import { post, del, checkStatus } from '../../lib/request.js';
import { likeSuccess, unlikeSuccess, integrityFailures } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 3,
    duration: '3m',
  },
  load: {
    executor: 'constant-vus',
    vus: 15,
    duration: '10m',
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 15 },
      { duration: '5m', target: 15 },
      { duration: '1m', target: 30 },
      { duration: '5m', target: 30 },
      { duration: '1m', target: 60 },
      { duration: '5m', target: 60 },
      { duration: '1m', target: 100 },
      { duration: '5m', target: 100 },
      { duration: '2m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: {
    hot_like_churn: profiles[SCENARIO],
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // 경고 기준: like/unlike 는 가벼운 write이므로 p95 1초
    'http_req_duration{action:like}':   ['p(95)<1000'],
    'http_req_duration{action:unlike}': ['p(95)<1000'],
    // 정합성 오류 0
    'integrity_failures_total': ['count==0'],
  },
};

// 이터레이션 카운터 (VU별 로컬 상태)
let iterationCount = 0;

export default function () {
  const token = getUserToken();
  const headers = authHeaders(token);

  const testId = getLikeTestId();

  // 짝수는 like, 홀수는 unlike
  const doLike = iterationCount % 2 === 0;
  iterationCount++;

  let res;
  if (doLike) {
    res = post(
      `/api/v1/tests/${testId}/likes`,
      {},
      headers,
      { scenario: 'like_churn', action: 'like', testId: String(testId) }
    );

    // 200: 성공 (이미 찜 상태여도 200 반환함)
    if (checkStatus(res, 200, 'like')) {
      likeSuccess.add(1);
    } else if (res.status >= 500) {
      integrityFailures.add(1, { action: 'like', status: String(res.status) });
    }
  } else {
    res = del(
      `/api/v1/tests/${testId}/likes`,
      headers,
      { scenario: 'like_churn', action: 'unlike', testId: String(testId) }
    );

    if (checkStatus(res, 200, 'unlike')) {
      unlikeSuccess.add(1);
    } else if (res.status >= 500) {
      integrityFailures.add(1, { action: 'unlike', status: String(res.status) });
    }
  }

  sleep(0.3 + Math.random() * 0.7);
}
