/**
 * 전체 테스트 목록 조회
 * GET /api/v1/tests
 *
 * 토큰: k6/data/tokens.json (k6/token/issue-tokens.mjs 로 발급)
 * VU: K6_PROFILE=load 기준 최대 1000
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, authHeaders, getUserToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const latency = new Trend("test_list_latency", true);
const bodySize = new Trend("test_list_body_bytes");
const successRate = new Rate("test_list_success");

export const options = {
  scenarios: rampScenario("test_list"),
  thresholds: {
    test_list_latency: ["p(50)<500", "p(95)<2000", "p(99)<5000"],
    test_list_success: ["rate>0.99"],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/tests`, {
    headers: authHeaders(getUserToken()),
    timeout: "15s",
  });

  latency.add(res.timings.duration);
  bodySize.add(res.body ? res.body.length : 0);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "has data": (r) => {
      try { return JSON.parse(r.body).data !== undefined; }
      catch { return false; }
    },
  });

  successRate.add(ok);
  if (!ok) {
    console.error(`VU=${__VU} status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
