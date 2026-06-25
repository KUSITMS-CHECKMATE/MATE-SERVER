/**
 * 응답 제출
 * POST /api/v1/tests/{testId}/answers
 *
 * VU당 1회만 제출 (중복 참여 방지). 1000 VU = 1000명 토큰 풀 필요.
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import { BASE_URL, getUserToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

// TEST_IDS, Q_IDS 는 같은 순서로 매핑 (testIds[N] 의 질문이 qIds[N])
const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");
const qIds = (__ENV.Q_IDS || __ENV.Q_SUBJECTIVE || "1").split(",");

const latency = new Trend("answer_latency", true);
const successRate = new Rate("answer_success");
const successCount = new Counter("answer_success_count");

export const options = {
  scenarios: rampScenario("answer_submit"),
  thresholds: {
    answer_latency: ["p(95)<3000"],
    answer_success: ["rate>0.9"],
  },
};

export default function () {
  if (__ITER > 0) {
    sleep(60);
    return;
  }

  const idx = (__VU - 1) % testIds.length;
  const testId = testIds[idx];
  const qId = qIds[idx];
  const token = getUserToken();

  const payload = JSON.stringify({
    answers: [
      {
        type: "SUBJECTIVE",
        questionId: Number(qId),
        text: `k6 부하테스트 응답 VU=${__VU}`,
      },
    ],
  });

  const res = http.post(
    `${BASE_URL}/api/v1/tests/${testId}/answers`,
    payload,
    {
      headers: {
        Authorization: `Bearer ${token.accessToken}`,
        "Content-Type": "application/json",
      },
      timeout: "30s",
    }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 201": (r) => r.status === 201,
    "has participationId": (r) => {
      try { return JSON.parse(r.body).data.participationId !== undefined; }
      catch { return false; }
    },
  });

  successRate.add(ok);
  if (ok) successCount.add(1);

  if (!ok) {
    console.error(`VU=${__VU} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }

  sleep(1);
}
