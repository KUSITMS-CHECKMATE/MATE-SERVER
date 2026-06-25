/**
 * PDF 최초 생성
 * GET /api/v1/tests/{testId}/report/pdf
 *
 * per-vu-iterations: TEST_IDS 개수만큼 VU, 각 1회 생성
 * 사전: UPDATE test SET pdf_key = NULL WHERE id IN (...);
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";
import { BASE_URL, authHeaders, getMakerTokenByIndex } from "../config.js";

const testIds = (__ENV.TEST_IDS || __ENV.TEST_ID || "1").split(",");

const latency = new Trend("pdf_gen_latency", true);
const failed = new Counter("pdf_gen_failed");

export const options = {
  scenarios: {
    pdf_generate: {
      executor: "per-vu-iterations",
      vus: Math.min(testIds.length, 100),
      iterations: 1,
      maxDuration: "120s",
    },
  },
  thresholds: {
    pdf_gen_latency: ["p(95)<90000"],
    pdf_gen_failed: ["count<1"],
  },
};

export default function () {
  const testId = testIds[__VU - 1] || testIds[0];
  const makerIndex = __VU - 1;

  const res = http.get(
    `${BASE_URL}/api/v1/tests/${testId}/report/pdf`,
    {
      headers: authHeaders(getMakerTokenByIndex(makerIndex)),
      responseType: "binary",
      timeout: "120s",
    }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
    "content-type PDF": (r) => (r.headers["Content-Type"] || "").includes("application/pdf"),
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
