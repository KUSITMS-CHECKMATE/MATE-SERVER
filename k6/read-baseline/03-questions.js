/**
 * 질문 전체 조회
 * GET /api/v1/tests/{testId}/questions
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, authHeaders, getUserToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("questions_latency", true);
const bodySize = new Trend("questions_body_bytes");
const successRate = new Rate("questions_success");

export const options = {
  scenarios: rampScenario("questions"),
  thresholds: {
    questions_latency: ["p(50)<500", "p(95)<2000", "p(99)<5000"],
    questions_success: ["rate>0.99"],
  },
};

export default function () {
  const testId = testIds[__VU % testIds.length];

  const res = http.get(`${BASE_URL}/api/v1/tests/${testId}/questions`, {
    headers: authHeaders(getUserToken()),
    timeout: "15s",
  });

  latency.add(res.timings.duration);
  bodySize.add(res.body ? res.body.length : 0);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "has questions": (r) => {
      try { return Array.isArray(JSON.parse(r.body).data.questions); }
      catch { return false; }
    },
  });

  successRate.add(ok);
  if (!ok) {
    console.error(`VU=${__VU} status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
