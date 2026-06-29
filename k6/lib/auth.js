/**
 * 토큰 풀에서 VU별 토큰을 할당하고 공통 인증 헤더를 반환합니다.
 *
 * data/tokens.json 구조
 * {
 *   "users":  [ { "userId": 1, "accessToken": "eyJ..." }, ... ],
 *   "makers": [ { "userId": 101, "accessToken": "eyJ..." }, ... ]
 * }
 *
 * 준비 방법
 *   1. 테스트 계정으로 로그인 → access token 수집
 *   2. tokens.json 에 배열로 저장
 *   3. users: 일반 사용자 (조회, 응답 제출, like)
 *      makers: 메이커 계정 (draft 수정, report 조회)
 */

import { SharedArray } from 'k6/data';
import { vu } from 'k6/execution';

const userTokens = new SharedArray('userTokens', function () {
  return JSON.parse(open('../data/tokens.json')).users;
});

const makerTokens = new SharedArray('makerTokens', function () {
  return JSON.parse(open('../data/tokens.json')).makers;
});

/**
 * 일반 사용자 토큰 반환 (VU index 기반 순환)
 * 동일 VU가 항상 동일 사용자 토큰을 사용 — 조회/like 시나리오에 적합
 */
export function getUserToken() {
  return userTokens[vu.idInTest % userTokens.length];
}

/**
 * 이터레이션별 사용자 토큰 반환 (이터레이션 번호 기반 순환)
 * 매 이터레이션마다 다른 사용자로 순환 — 경합 시나리오처럼
 * 다양한 사용자가 동시에 요청해야 할 때 사용
 */
export function getUserTokenByIteration() {
  return userTokens[vu.iterationInScenario % userTokens.length];
}

/**
 * 메이커 토큰 반환 (VU index 기반 순환)
 */
export function getMakerToken() {
  return makerTokens[vu.idInTest % makerTokens.length];
}

/**
 * 인덱스로 특정 메이커 토큰 반환
 * draft 시나리오처럼 size별로 maker가 고정된 경우 사용
 * @param {number} index
 */
export function getMakerTokenByIndex(index) {
  return makerTokens[index % makerTokens.length];
}

/**
 * Authorization 헤더 반환
 * @param {{ accessToken: string }} token
 */
export function authHeaders(token) {
  return {
    'Authorization': `Bearer ${token.accessToken}`,
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
}
