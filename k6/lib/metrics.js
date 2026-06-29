/**
 * 시나리오 전반에서 사용하는 커스텀 k6 메트릭 정의합니다.
 */

import { Trend, Counter, Rate } from 'k6/metrics';


// 응답 body 크기 추적
/** GET /api/v1/tests 응답 크기 */
export const listBodySize = new Trend('list_body_size_bytes');

/** GET /api/v1/tests/{testId} 응답 크기 */
export const detailBodySize = new Trend('detail_body_size_bytes');

/** GET /api/v1/tests/{testId}/questions 응답 크기 */
export const questionsBodySize = new Trend('questions_body_size_bytes');


// 정합성 실패 카운터
/** 예상 성공인데 실패한 요청 수 */
export const integrityFailures = new Counter('integrity_failures_total');

/** 예상 실패인데 성공한 요청 수 (ex: 중복 제출이 201 반환된 경우) */
export const unexpectedSuccess = new Counter('unexpected_success_total');


// 트래픽 유형별 성공률
/** hot traffic 성공률 */
export const hotSuccessRate = new Rate('hot_success_rate');

/** distributed traffic 성공률 */
export const distributedSuccessRate = new Rate('distributed_success_rate');


// 응답 제출 결과
/** 응답 제출 성공 수 */
export const answerSubmitSuccess = new Counter('answer_submit_success_total');

/** 응답 제출 실패 수 (정원 초과, 중복, validation 등) */
export const answerSubmitFailure = new Counter('answer_submit_failure_total');


// Hot like / unlike 카운터
export const likeSuccess = new Counter('like_success_total');
export const unlikeSuccess = new Counter('unlike_success_total');


// Draft 수정
export const draftUpdateSuccess = new Counter('draft_update_success_total');
export const draftUpdateFailure = new Counter('draft_update_failure_total');
