/**
 * 리포트 JSON 조회
 * GET /api/v1/tests/{testId}/report
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, authHeaders, getMakerToken, getMakerTokenByIndex } from "../../config.js";
import { rampScenario } from "../../lib/profiles.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("report_json_latency", true);
const successRate = new Rate("report_json_success");

export const options = {
  // K6_PROFILE=medium → 최대 100 VU / K6_PROFILE=smoke → 10 VU
  scenarios: rampScenario("report_json"),
  thresholds: {
    report_json_latency: ["p(95)<3000"],
    report_json_success: ["rate>0.99"],
  },
};

export default function () {
  // VU별로 서로 다른 testId + 해당 testId 소유 maker 토큰 사용
  // 전제: testIds[N] 은 makerTokens[N] 소유 (seed-report-data.md SQL로 maker_id 배분 필요)
  const idx = (__VU - 1) % testIds.length;
  const testId = testIds[idx];

  const res = http.get(
    `${BASE_URL}/api/v1/tests/${testId}/report`,
    { headers: authHeaders(getMakerTokenByIndex(idx)) }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "has reportStatus": (r) => {
      try { return JSON.parse(r.body).data.reportStatus !== undefined; }
      catch { return false; }
    },
  });

  successRate.add(ok);
  sleep(1);
}
