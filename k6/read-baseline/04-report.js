/**
 * Report 조회
 * GET /api/v1/tests/{testId}/report
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, authHeaders, getMakerToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("report_latency", true);
const bodySize = new Trend("report_body_bytes");
const successRate = new Rate("report_success");

export const options = {
  scenarios: rampScenario("report"),
  thresholds: {
    report_latency: ["p(50)<500", "p(95)<3000", "p(99)<5000"],
    report_success: ["rate>0.99"],
  },
};

export default function () {
  const testId = testIds[__VU % testIds.length];

  const res = http.get(`${BASE_URL}/api/v1/tests/${testId}/report`, {
    headers: authHeaders(getMakerToken()),
    timeout: "15s",
  });

  latency.add(res.timings.duration);
  bodySize.add(res.body ? res.body.length : 0);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "has reportStatus": (r) => {
      try { return JSON.parse(r.body).data.reportStatus !== undefined; }
      catch { return false; }
    },
  });

  successRate.add(ok);

  if (ok && __ITER === 0) {
    try {
      const data = JSON.parse(res.body).data;
      console.log(
        `Report baseline: reportStatus=${data.reportStatus} questions=${data.questionCount} participants=${data.participantCount} bodySize=${res.body.length}B`
      );
    } catch { /* skip */ }
  }

  if (!ok) {
    console.error(`VU=${__VU} status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
