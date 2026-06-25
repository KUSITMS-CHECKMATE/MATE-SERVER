/**
 * 테스트 상세 조회
 * GET /api/v1/tests/{testId}
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, authHeaders, getUserToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("test_detail_latency", true);
const bodySize = new Trend("test_detail_body_bytes");
const successRate = new Rate("test_detail_success");

export const options = {
  scenarios: rampScenario("test_detail"),
  thresholds: {
    test_detail_latency: ["p(50)<300", "p(95)<1500", "p(99)<3000"],
    test_detail_success: ["rate>0.99"],
  },
};

export default function () {
  const testId = testIds[__VU % testIds.length];

  const res = http.get(`${BASE_URL}/api/v1/tests/${testId}`, {
    headers: authHeaders(getUserToken()),
    timeout: "15s",
  });

  latency.add(res.timings.duration);
  bodySize.add(res.body ? res.body.length : 0);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "has title": (r) => {
      try { return JSON.parse(r.body).data.title !== undefined; }
      catch { return false; }
    },
  });

  successRate.add(ok);
  if (!ok) {
    console.error(`VU=${__VU} status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
