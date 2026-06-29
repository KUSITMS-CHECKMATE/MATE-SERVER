/**
 * draft 반복 수정
 * ──────────────────────────────────────────────────────────────────
 * 목적: autosave 형태의 반복 수정에서 데이터 손실/덮어쓰기 이상 확인
 *       payload 크기별(small/medium/large) latency 차이 관찰
 * API:  PATCH /api/v1/test-drafts/{draftId}
 * 패턴: 반복 write (메이커 계정, K6_DRAFT_SIZE 로 payload 크기 선택)
 * 비교 목표:
 *   - small / medium / large payload 간 p95 latency 비교
 *   - 각 draftId 의 마지막 저장 값이 최종 전송 payload 와 일치
 *   - 필드 null 초기화 없음
 *   - 경고: large payload 에서 p95 2초 이상
 */

import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { getMakerTokenByIndex, authHeaders } from '../../lib/auth.js';
import { vu } from 'k6/execution';
import { getDraftId } from '../../lib/data.js';
import { patch, checkStatus } from '../../lib/request.js';
import { draftUpdateSuccess, draftUpdateFailure, integrityFailures } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';
const DRAFT_SIZE = __ENV.K6_DRAFT_SIZE || 'medium';

// payload 로드
const payloads = new SharedArray('draftPayloads', function () {
  return [
    JSON.parse(open(`../../data/payloads/draft-${__ENV.K6_DRAFT_SIZE || 'medium'}.json`))
  ];
});

function getDraftPayload() {
  return payloads[0];
}

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 2,
    duration: '3m',
  },
  load: {
    executor: 'constant-vus',
    vus: 5,
    duration: '10m',
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 5 },
      { duration: '5m', target: 5 },
      { duration: '1m', target: 10 },
      { duration: '5m', target: 10 },
      { duration: '1m', target: 20 },
      { duration: '5m', target: 20 },
      { duration: '1m', target: 30 },
      { duration: '5m', target: 30 },
      { duration: '2m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: {
    draft_repeat: profiles[SCENARIO],
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // 경고 기준: large payload 에서 p95 2초 이상
    'http_req_duration{size:small}':  ['p(95)<1000'],
    'http_req_duration{size:medium}': ['p(95)<1500'],
    'http_req_duration{size:large}':  ['p(95)<2000'],
    // 정합성 오류 0
    'integrity_failures_total': ['count==0'],
  },
};

// seed SQL 기준 size를 maker 인덱스로 고정 매핑
// small → LOADTEST_CI_MAKER_001 (makers[0])
// medium → LOADTEST_CI_MAKER_002 (makers[1])
// large  → LOADTEST_CI_MAKER_003 (makers[2])
const DRAFT_SIZE_MAKER_INDEX = { small: 0, medium: 1, large: 2 };

export default function () {
  const makerIdx = DRAFT_SIZE_MAKER_INDEX[DRAFT_SIZE] ?? 0;
  const token = getMakerTokenByIndex(makerIdx);
  const headers = authHeaders(token);

  const draftId = getDraftId(DRAFT_SIZE, vu.idInTest);
  const payload = getDraftPayload();

  // autosave 패턴: 매 이터레이션마다 전체 payload 를 PATCH
  const res = patch(
    `/api/v1/test-drafts/${draftId}`,
    payload,
    headers,
    { scenario: 'draft_repeat', size: DRAFT_SIZE, draftId: String(draftId) }
  );

  if (checkStatus(res, 200, `draft_repeat[${DRAFT_SIZE}]`)) {
    draftUpdateSuccess.add(1, { size: DRAFT_SIZE });
  } else {
    draftUpdateFailure.add(1, { size: DRAFT_SIZE, status: String(res.status) });
    if (res.status >= 500) {
      integrityFailures.add(1, { reason: '5xx', size: DRAFT_SIZE });
    }
  }

  sleep(2 + Math.random() * 3);
}
