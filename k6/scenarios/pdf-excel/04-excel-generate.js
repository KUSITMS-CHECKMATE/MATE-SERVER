/**
 * Excel 최초 생성
 * GET /api/v1/tests/{testId}/report/excel
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";
import { BASE_URL, authHeaders, getMakerTokenByIndex } from "../../config.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("excel_gen_latency", true);
const failed = new Counter("excel_gen_failed");

export const options = {
  scenarios: {
    excel_generate: {
      executor: "per-vu-iterations",
      vus: Math.min(testIds.length, 100),
      iterations: 1,
      maxDuration: "120s",
    },
  },
  thresholds: {
    excel_gen_latency: ["p(95)<60000"],
    excel_gen_failed: ["count<1"],
  },
};

export default function () {
  const testId = testIds[__VU - 1] || testIds[0];
  const makerIndex = __VU - 1;

  const res = http.get(
    `${BASE_URL}/api/v1/tests/${testId}/report/excel`,
    {
      headers: authHeaders(getMakerTokenByIndex(makerIndex)),
      responseType: "binary",
      timeout: "120s",
    }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "content-type xlsx": (r) => (r.headers["Content-Type"] || "").includes("spreadsheetml"),
    "body not empty": (r) => r.body && r.body.byteLength > 0,
  });

  if (!ok) {
    failed.add(1);
    console.error(`VU=${__VU} FAIL status=${res.status} duration=${res.timings.duration}ms`);
  } else {
    console.log(`VU=${__VU} OK duration=${res.timings.duration}ms size=${res.body.byteLength}B`);
  }

  sleep(1);
}
