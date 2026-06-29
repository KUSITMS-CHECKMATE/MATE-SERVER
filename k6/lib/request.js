/**
 * HTTP 요청 공통 래퍼
 * - BASE_URL: K6_BASE_URL 환경변수로 주입 (기본값: https://api.kusitms-mate.cloud)
 * - 모든 요청에 공통 태그를 추가합니다.
 * - check 결과를 반환해 호출 측에서 추가 검증 가능합니다.
 */

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = (__ENV.K6_BASE_URL || 'https://api.kusitms-mate.cloud').replace(/\/$/, '');

/**
 * GET 요청
 * @param {string} path        - API 경로 (예: /api/v1/tests/123)
 * @param {Object} headers     - 요청 헤더
 * @param {Object} [tags={}]   - k6 태그 (결과 필터링/분류용)
 * @returns {Response}
 */
export function get(path, headers, tags = {}) {
  return http.get(`${BASE_URL}${path}`, {
    headers,
    tags,
  });
}

/**
 * POST 요청
 * @param {string} path
 * @param {Object} body       - JSON 직렬화할 body 객체
 * @param {Object} headers
 * @param {Object} [tags={}]
 * @returns {Response}
 */
export function post(path, body, headers, tags = {}) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
    headers,
    tags,
  });
}

/**
 * PATCH 요청
 */
export function patch(path, body, headers, tags = {}) {
  return http.patch(`${BASE_URL}${path}`, JSON.stringify(body), {
    headers,
    tags,
  });
}

/**
 * DELETE 요청
 */
export function del(path, headers, tags = {}) {
  return http.del(`${BASE_URL}${path}`, null, {
    headers,
    tags,
  });
}

/**
 * 공통 상태 코드 체크
 * @param {Response} res
 * @param {number} expectedStatus
 * @param {string} label
 * @returns {boolean} check 통과 여부
 */
export function checkStatus(res, expectedStatus, label) {
  return check(res, {
    [`${label} - status ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
}

/**
 * 2xx 성공 여부 체크
 */
export function checkOk(res, label) {
  return check(res, {
    [`${label} - 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
}

/**
 * 응답 body 크기 반환 (bytes)
 */
export function bodySize(res) {
  return res.body ? res.body.length : 0;
}
