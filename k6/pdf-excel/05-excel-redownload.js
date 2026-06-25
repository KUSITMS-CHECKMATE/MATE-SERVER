/**
 * Excel 재다운로드
 * GET /api/v1/tests/{testId}/report/excel
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, TEST_ID, authHeaders, getMakerToken } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const latency = new Trend("excel_dl_latency", true);
const successRate = new Rate("excel_dl_success");

export const options = {
  scenarios: rampScenario("excel_redownload"),
  thresholds: {
    excel_dl_latency: ["p(95)<5000"],
    excel_dl_success: ["rate>0.99"],
  },
};

export default function () {
  const res = http.get(
    `${BASE_URL}/api/v1/tests/${TEST_ID}/report/excel`,
    {
      headers: authHeaders(getMakerToken()),
      responseType: "binary",
      timeout: "30s",
    }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "body not empty": (r) => r.body && r.body.byteLength > 0,
  });

  successRate.add(ok);
  if (!ok) {
    console.error(`VU=${__VU} FAIL status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
