/**
 * Hot test 질문 조회
 * ──────────────────────────────────────────────────────────────────
 * 목적: 참여 직전 동시 진입 상황에서 questions 경로 병목 확인
 *       (payload 크기와 DB 조회 비용이 주요 변수 — Blob URL 생성은 로컬 HMAC 연산으로 무시 가능)
 * API:  GET /api/v1/tests/{testId}/questions
 * 패턴: 80% hot testId / 20% distributed testId pool
 * 비교 목표:
 *   - hot 질문 조회 vs distributed 질문 조회 tail latency 차이
 *   - 질문 수 A(10)/B(30)/C(100) 세트별 latency 기울기
 *   - 경고: p95 1.5초 이상
 */

import { sleep } from 'k6';
import { getUserToken, authHeaders } from '../../lib/auth.js';
import {
  pickQuestionsTestId,
  getHotQuestionsTestId,
  getDistributedQuestionsTestId,
  getQuestionsSetTestId,
} from '../../lib/data.js';
import { get, checkStatus, bodySize } from '../../lib/request.js';
import { questionsBodySize, hotSuccessRate, distributedSuccessRate } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';

/**
 * 질문 수 규모 세트 실험 여부
 * K6_QUESTIONS_SET=A|B|C 로 지정하면 해당 세트 testId만 사용 (대용량 비교용)
 * 미지정 시 hot/distributed 패턴으로 실행
 */
const QUESTIONS_SET = __ENV.K6_QUESTIONS_SET || null;

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 3,
    duration: '3m',
  },
  load: {
    executor: 'constant-vus',
    vus: 15,
    duration: '15m',
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
      { duration: '1m', target: 90 },
      { duration: '5m', target: 90 },
      { duration: '2m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: {
    hot_test_questions: profiles[SCENARIO],
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // 경고 기준: p95 1.5초
    'http_req_duration{traffic:hot}':         ['p(95)<1500'],
    'http_req_duration{traffic:distributed}': ['p(95)<1500'],
  },
};

export default function () {
  const token = getUserToken();
  const headers = authHeaders(token);

  let testId;
  let trafficTag;

  if (QUESTIONS_SET) {
    // 질문 수 세트 비교 모드 (K6_QUESTIONS_SET=A/B/C)
    testId = getQuestionsSetTestId(QUESTIONS_SET);
    trafficTag = `set_${QUESTIONS_SET}`;
  } else {
    // 기본 hot/distributed 모드
    const isHot = Math.random() < 0.8;
    testId = isHot ? getHotQuestionsTestId() : getDistributedQuestionsTestId();
    trafficTag = isHot ? 'hot' : 'distributed';
  }

  const res = get(
    `/api/v1/tests/${testId}/questions`,
    headers,
    { scenario: 'hot_questions', traffic: trafficTag, testId: String(testId) }
  );

  const ok = checkStatus(res, 200, `hot_questions[${trafficTag}]`);

  // 커스텀 메트릭 기록
  questionsBodySize.add(bodySize(res));

  if (trafficTag === 'hot') {
    hotSuccessRate.add(ok);
  } else if (trafficTag === 'distributed') {
    distributedSuccessRate.add(ok);
  }

  sleep(1 + Math.random() * 2);
}
