/**
 * like / unlike 토글
 * POST /api/v1/tests/{testId}/likes → DELETE
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";
import { BASE_URL, TEST_ID, authHeaders, getUserTokenByIteration } from "../config.js";
import { rampScenario } from "../lib/profiles.js";

const likeLatency = new Trend("like_latency", true);
const unlikeLatency = new Trend("unlike_latency", true);
const successRate = new Rate("like_toggle_success");

export const options = {
  scenarios: rampScenario("like_toggle"),
  thresholds: {
    like_latency: ["p(95)<2000"],
    unlike_latency: ["p(95)<2000"],
    like_toggle_success: ["rate>0.99"],
  },
};

export default function () {
  const headers = authHeaders(getUserTokenByIteration());
  const url = `${BASE_URL}/api/v1/tests/${TEST_ID}/likes`;

  const likeRes = http.post(url, null, { headers, timeout: "10s" });
  likeLatency.add(likeRes.timings.duration);

  const likeOk = check(likeRes, {
    "like status 200": (r) => r.status === 200,
    "isLiked true": (r) => {
      try { return JSON.parse(r.body).data.isLiked === true; }
      catch { return false; }
    },
  });

  sleep(0.5);

  const unlikeRes = http.del(url, null, { headers, timeout: "10s" });
  unlikeLatency.add(unlikeRes.timings.duration);

  const unlikeOk = check(unlikeRes, {
    "unlike status 200": (r) => r.status === 200,
    "isLiked false": (r) => {
      try { return JSON.parse(r.body).data.isLiked === false; }
      catch { return false; }
    },
  });

  successRate.add(likeOk && unlikeOk);

  if (!likeOk || !unlikeOk) {
    console.error(`VU=${__VU} like=${likeRes.status} unlike=${unlikeRes.status}`);
  }

  sleep(1);
}
