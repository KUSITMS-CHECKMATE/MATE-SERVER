/**
 * 테스트 자산(testId, draftId 등)을 로드하고 반환합니다.
 *
 * data/test-ids.json 구조
 * {
 *   "read": {
 *     "hotTestId": 123,
 *     "distributedPool": [124, 125, ..., 133]
 *   },
 *   "questions": {
 *     "hotTestId": 123,
 *     "distributedPool": [124, 125, 126],
 *     "setsA": [7문항 testId, ...],
 *     "setsB": [28문항 testId, ...],
 *     "setsC": [98문항 testId, ...]
 *   },
 *   "answers": {
 *     "contention": {
 *       "hotTestId": 200,
 *       "distributedPool": [201, 202, 203]
 *     },
 *     "failure": {
 *       "duplicateTestId": 210,
 *       "capacityExceededTestId": 211
 *     }
 *   },
 *   "likes": {
 *     "hotTestId": 300,
 *     "distributedPool": [301, 302, 303]
 *   },
 *   "drafts": {
 *     "small":  [401, 402, 403, 404, 405],
 *     "medium": [411, 412, 413, 414, 415],
 *     "large":  [421, 422, 423, 424, 425]
 *   }
 * }
 */

import { SharedArray } from 'k6/data';

const assetData = new SharedArray('testIds', function () {
  return [JSON.parse(open('../data/test-ids.json'))];
});

function assets() {
  return assetData[0];
}


// Hot test 상세 조회
/** 단일 hot testId 반환 — 80% 트래픽이 여기에 집중 */
export function getHotReadTestId() {
  return assets().read.hotTestId;
}

/** distributed pool에서 무작위 testId 반환 — 나머지 20% 트래픽 */
export function getDistributedReadTestId() {
  const pool = assets().read.distributedPool;
  return pool[Math.floor(Math.random() * pool.length)];
}

/** 80% hot / 20% distributed 비율로 testId 선택 */
export function pickReadTestId() {
  return Math.random() < 0.8
    ? getHotReadTestId()
    : getDistributedReadTestId();
}


// Hot test 질문 조회
/** 질문 조회용 hot testId 반환 — 기본 hot/distributed 모드에서 사용 */
export function getHotQuestionsTestId() {
  return assets().questions.hotTestId;
}

/** 질문 조회용 distributed testId 반환 — 기본 hot/distributed 모드에서 사용 */
export function getDistributedQuestionsTestId() {
  const pool = assets().questions.distributedPool;
  return pool[Math.floor(Math.random() * pool.length)];
}

/** 80% hot / 20% distributed 비율로 질문 조회용 testId 선택 */
export function pickQuestionsTestId() {
  return Math.random() < 0.8
    ? getHotQuestionsTestId()
    : getDistributedQuestionsTestId();
}

/**
 * 질문 수 세트별 testId 반환 — K6_QUESTIONS_SET=A/B/C 모드에서 사용
 * A: 7문항 / B: 28문항 / C: 98문항 세트별 latency 비교용 (유형별 1/4/14개 균등 배분)
 * @param {'A'|'B'|'C'} variant
 */
export function getQuestionsSetTestId(variant) {
  const key = `sets${String(variant).toUpperCase()}`;
  const set = assets().questions[key];
  if (!set || set.length === 0) {
    throw new Error(`Questions set for variant '${variant}' (key: ${key}) is empty or not found in test-ids.json`);
  }
  return set[Math.floor(Math.random() * set.length)];
}


// 응답 제출 경합
/**
 * 경합용 testId 반환 — 70% hot / 30% distributed
 * ※ write-answers-contention.js는 경합 극대화를 위해 hotTestId를 직접 사용하며
 *    이 함수는 분산 경합 테스트 확장 시 활용 가능
 */
export function getAnswersContentionTestId() {
  return Math.random() < 0.7
    ? assets().answers.contention.hotTestId
    : (() => {
        const pool = assets().answers.contention.distributedPool;
        return pool[Math.floor(Math.random() * pool.length)];
      })();
}


// 응답 제출 실패
/**
 * 실패 케이스별 testId 반환
 * @param {'duplicate'|'capacityExceeded'} type
 */
export function getAnswersFailureTestId(type) {
  return assets().answers.failure[type + 'TestId'];
}


// Hot like/unlike churn
/** 80% hot / 20% distributed 비율로 like 대상 testId 선택 */
export function getLikeTestId() {
  const likes = assets().likes;
  return Math.random() < 0.8
    ? likes.hotTestId
    : likes.distributedPool[Math.floor(Math.random() * likes.distributedPool.length)];
}


// Draft 수정 반복
/**
 * payload 크기별 draftId 반환 — VU별로 다른 draftId를 순환 할당
 * @param {'small'|'medium'|'large'} size - K6_DRAFT_SIZE 값
 * @param {number} vuIndex - vu.idInTest 값
 */
export function getDraftId(size, vuIndex) {
  const normalizedSize = String(size).toLowerCase();
  const pool = assets().drafts[normalizedSize];
  if (!pool || pool.length === 0) {
    throw new Error(`Draft pool for size '${size}' (key: ${normalizedSize}) is empty or not found in test-ids.json`);
  }
  return pool[(vuIndex || 0) % pool.length];
}
