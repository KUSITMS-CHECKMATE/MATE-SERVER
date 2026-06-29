/**
 * 응답 제출 실패
 * ──────────────────────────────────────────────────────────────────
 * 목적: 실패 케이스에서 부분 저장 없음, 응답 일관성 확인
 * API:  POST /api/v1/tests/{testId}/answers
 * 패턴: controlled failure (낮은 동시성, K6_FAILURE_TYPE 으로 케이스 선택)
 *       duplicate — 이미 응답한 사용자 재제출 (400, PARTICIPATION_003)
 *       capacity  — 정원 초과 테스트 제출 (400, PARTICIPATION_004)
 *       invalid   — 잘못된 payload 제출 (400)
 * 비교 목표:
 *   - 모든 실패가 일관된 에러 코드 반환
 *   - 부분 저장 없음 (assertions SQL 로 확인)
 *   - 5xx 없음 (실패가 서버 오류로 이어지지 않음)
 */

import { sleep } from 'k6';
import { getUserToken, authHeaders } from '../../lib/auth.js';
import { getAnswersFailureTestId } from '../../lib/data.js';
import { post, checkStatus } from '../../lib/request.js';
import { unexpectedSuccess, integrityFailures } from '../../lib/metrics.js';

const SCENARIO = __ENV.K6_SCENARIO || 'smoke';
const FAILURE_TYPE = __ENV.K6_FAILURE_TYPE || 'duplicate';

// 실패 케이스별 payload
const FAILURE_PAYLOADS = {
  duplicate: {
    answers: [
      { type: 'SCALE', questionId: 0, value: 3 },
    ],
    expectedStatus: 400,
    expectedCode: 'PARTICIPATION_003',  // 이미 참여한 테스트 → 400
    description: '중복 제출 실패',
  },
  capacity: {
    answers: [
      { type: 'SCALE', questionId: 0, value: 3 },
    ],
    expectedStatus: 400,
    expectedCode: 'PARTICIPATION_004',  // 테스트 참여 인원 마감 → 400
    description: '정원 초과 실패',
  },
  invalid: {
    // value 필드 누락 → @NotNull 검증 실패 → 400
    answers: [
      { type: 'SCALE', questionId: 0 },
    ],
    expectedStatus: 400,
    expectedCode: null,
    description: 'invalid payload 실패',
  },
};

const profiles = {
  smoke: {
    executor: 'constant-vus',
    vus: 2,
    duration: '3m',
  },
  load: {
    executor: 'constant-vus',
    vus: 5,
    duration: '5m',
  },
};

export const options = {
  scenarios: {
    answers_failure: profiles[SCENARIO],
  },
  thresholds: {
    // 예상치 못한 성공(부분 저장 가능성) 0건
    'unexpected_success_total': ['count==0'],
    // 정합성 오류 0
    'integrity_failures_total': ['count==0'],
    // 5xx는 없어야 함 — http_req_failed는 이 시나리오에서 항상 100%이므로 검증 제외
  },
};

export default function () {
  const token = getUserToken();
  const headers = authHeaders(token);

  const caseConfig = FAILURE_PAYLOADS[FAILURE_TYPE];
  const testId = getAnswersFailureTestId(
    FAILURE_TYPE === 'duplicate' ? 'duplicate' :
    FAILURE_TYPE === 'capacity' ? 'capacityExceeded' :
    'duplicate'  // invalid 는 아무 testId 사용 가능
  );

  const res = post(
    `/api/v1/tests/${testId}/answers`,
    { answers: caseConfig.answers },
    headers,
    {
      scenario: 'answers_failure',
      failure_type: FAILURE_TYPE,
      expected_failure: 'true',  // 모든 요청이 실패해야 함
    }
  );

  // 예상 상태코드 확인
  const isExpectedStatus = res.status === caseConfig.expectedStatus;

  if (res.status >= 200 && res.status < 300) {
    // 성공 응답 → 부분 저장 가능성
    unexpectedSuccess.add(1, { failure_type: FAILURE_TYPE });
    integrityFailures.add(1, { reason: 'unexpected_success', failure_type: FAILURE_TYPE });
  } else if (!isExpectedStatus) {
    // 예상과 다른 상태코드
    integrityFailures.add(1, {
      reason: 'unexpected_status',
      failure_type: FAILURE_TYPE,
      actual_status: String(res.status),
    });
  } else if (caseConfig.expectedCode) {
    // 상태코드 맞다면 에러 코드도 확인
    try {
      const body = res.json();
      if (body && body.code !== caseConfig.expectedCode) {
        integrityFailures.add(1, {
          reason: 'unexpected_error_code',
          failure_type: FAILURE_TYPE,
          expected_code: caseConfig.expectedCode,
          actual_code: String(body.code || ''),
        });
      }
    } catch (_) {
      integrityFailures.add(1, { reason: 'parse_error', failure_type: FAILURE_TYPE });
    }
  }

  sleep(1 + Math.random() * 1);
}
