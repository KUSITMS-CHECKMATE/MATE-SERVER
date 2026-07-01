/**
 * Draft 수정
 * PATCH /api/v1/test-drafts/{draftId}
 *
 * 메이커 토큰 사용 (100명 풀 — VU별 순환)
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { vu } from "k6/execution";
import { BASE_URL, authHeaders, getMakerTokenByIndex } from "../../config.js";
import { rampScenario } from "../../lib/profiles.js";

const draftIds = (__ENV.DRAFT_IDS || __ENV.DRAFT_ID || "1").split(",");

const latency = new Trend("draft_update_latency", true);
const successRate = new Rate("draft_update_success");

const payloads = [
  { title: "간단 수정 A", goalPpl: 50 },
  {
    title: "중간 수정 B",
    description: "서비스 한줄 소개를 수정합니다",
    goalPpl: 100,
    reward: 500,
    categories: ["SHOPPING", "DAILY"],
  },
  {
    title: "대형 수정 C",
    description: "대형 페이로드 테스트용 서비스 한줄 소개입니다",
    serviceName: "대형테스트서비스",
    serviceDescription: "부하 테스트용 긴 설명",
    goalPpl: 200,
    reward: 1000,
    categories: ["SHOPPING", "DAILY", "AI"],
    closedAt: "2026-12-31",
    imageKeys: ["img/test1.jpg", "img/test2.jpg", "img/test3.jpg"],
  },
];

export const options = {
  scenarios: rampScenario("draft_update"),
  thresholds: {
    draft_update_latency: ["p(95)<3000", "p(99)<5000"],
    draft_update_success: ["rate>0.99"],
  },
};

export default function () {
  const idx = (vu.idInTest - 1) % draftIds.length;
  const draftId = draftIds[idx];
  const payload = payloads[Math.floor(Math.random() * payloads.length)];

  const res = http.patch(
    `${BASE_URL}/api/v1/test-drafts/${draftId}`,
    JSON.stringify(payload),
    {
      headers: authHeaders(getMakerTokenByIndex(idx)),
      timeout: "15s",
    }
  );

  latency.add(res.timings.duration);

  const ok = check(res, {
    "status 200": (r) => r.status === 200,
  });

  successRate.add(ok);
  if (!ok) {
    console.error(`VU=${vu.idInTest} status=${res.status} duration=${res.timings.duration}ms`);
  }

  sleep(1);
}
