/**
 * 응답 제출 경합
 * ──────────────────────────────────────────────────────────────────
 * 목적: 하나의 IN_PROGRESS 테스트에 동시 제출이 몰릴 때 초과 저장, 중복 처리, lock contention 등을 확인
 * API:  POST /api/v1/tests/{testId}/answers
 * 패턴: 모든 VU가 하나의 hot testId에 집중 (경합 극대화)
 * 비교 목표:
 *   - pplCount ≤ goalPpl 유지 (초과 저장 없음)
 *   - 중복 participation 0건 (동시 제출에서 DB 유니크 제약 동작)
 *   - 5xx 없음 (lock contention 이 서버 오류로 이어지지 않음)
 *   - 경고: pplCount ≠ 실제 participation 수 (정합성 불일치)
 *
 * ⚠️  answer 제출은 questionId 가 해당 test에 속해야 합니다.
 *     distributed pool testId를 혼합하면 questionId 불일치로 400 발생.
 *     경합 확인이 목적이므로 hot testId에만 집중합니다.
 * ⚠️  이 시나리오는 데이터를 소비합니다.
 *     각 사용자는 testId당 1회만 응답 가능 → 충분한 미응답 사용자 풀 필요
 *     실행 전 sql/seed/write_concurrency.sql 로 미응답 사용자 & IN_PROGRESS 테스트 준비 필요
 */

import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import http from 'k6/http';
import { getUserTokenByIteration, authHeaders } from '../../lib/auth.js';
import { post, checkStatus } from '../../lib/request.js';
import { answerSubmitSuccess, answerSubmitFailure, integrityFailures } from '../../lib/metrics.js';

// 400/409는 예상된 응답 — http_req_failed 에서 제외
http.setResponseCallback(http.expectedStatuses(
  { min: 200, max: 399 }, 400, 409
));

const assetData = new SharedArray('testIds', function () {
  return [JSON.parse(open('../../data/test-ids.json'))];
});

function getAssets() {
  return assetData[0];
}

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 2,
    duration: '3m',
  },
  load: {
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
    answers_contention: profiles[SCENARIO],
  },
  thresholds: {
    // 정합성 오류 0
    'integrity_failures_total': ['count==0'],
    // 5xx 등 예상치 못한 서버 오류만 체크 (400/409는 setResponseCallback으로 제외)
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  // 이터레이션마다 다른 사용자 순환 → 다양한 사용자가 동시 제출해야 경합 의미 있음
  const token = getUserTokenByIteration();
  const headers = authHeaders(token);

  const assets = getAssets();
  // 모든 VU가 동일 hot testId에 제출 → 경합 극대화
  const testId  = assets.answers.contention.hotTestId;
  const payload = { answers: assets.answerPayloads.contention.answers };

  const res = post(
    `/api/v1/tests/${testId}/answers`,
    payload,
    headers,
    {
      scenario: 'answers_contention',
      expected_failure: 'false',
    }
  );

  // 201: 정상 제출 성공
  // 409: 중복 제출
  // 400: 정원 초과 또는 유효성 실패
  // 5xx: 서버 오류 → 정합성 이슈 가능성
  if (res.status === 201) {
    answerSubmitSuccess.add(1);
    checkStatus(res, 201, 'answers_contention');

  } else if (res.status === 400) {
    answerSubmitFailure.add(1, { reason: 'expected' });

    // 예상된 에러 코드가 아니면 정합성 이슈
    // PARTICIPATION_003: 이미 참여한 테스트 (중복 제출)
    // PARTICIPATION_004: 테스트 참여 인원 마감 (정원 초과)
    try {
      const body = res.json();
      if (body && body.code !== 'PARTICIPATION_003'
               && body.code !== 'PARTICIPATION_004') {
        integrityFailures.add(1, { reason: 'unexpected_4xx', code: String(body.code || '') });
      }
    } catch (_) {
      integrityFailures.add(1, { reason: 'parse_error', status: String(res.status) });
    }

  } else {
    answerSubmitFailure.add(1, { reason: 'server_error' });
    integrityFailures.add(1, { reason: '5xx', status: String(res.status) });
  }

  sleep(0.5 + Math.random() * 1.5);
}
