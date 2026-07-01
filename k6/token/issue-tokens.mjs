#!/usr/bin/env node
/**
 * 운영 API용 k6 토큰 발급
 *
 * #236 방식(오프라인 JWT 생성)과 다름:
 *   로컬 Spring Boot(local profile)의 /local-test-token API를 호출해
 *   서버 JwtProvider가 서명한 토큰을 받습니다.
 *
 * 사전 조건:
 *   1. seed-accounts.sql 을 운영 DB에 실행
 *   2. k6/token/.env 에 운영 JWT_SECRET, DB, Redis 설정
 *   3. 로컬 서버 실행:
 *        set -a && source k6/token/.env && set +a
 *        ./gradlew bootRun --args='--spring.profiles.active=local'
 *   4. DB/Redis port-forward 연결
 *
 * 사용:
 *   node k6/token/issue-tokens.mjs
 *   → k6/data/tokens.json 생성
 */
import { execSync } from "node:child_process";
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dir = dirname(fileURLToPath(import.meta.url));
const root = join(__dir, "..");
const envPath = join(__dir, ".env");

function loadEnvFile(path) {
  if (!existsSync(path)) return;
  for (const line of readFileSync(path, "utf8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    const val = trimmed.slice(eq + 1).trim();
    if (!process.env[key]) process.env[key] = val;
  }
}

loadEnvFile(envPath);

const LOCAL_API = process.env.LOCAL_API || "http://localhost:8080";
const INCLUDE_REFRESH = (process.env.INCLUDE_REFRESH || "true") === "true";
const CONCURRENCY = Number(process.env.TOKEN_CONCURRENCY || "20");

const pg = {
  host: process.env.PGHOST || "127.0.0.1",
  port: process.env.PGPORT || "15432",
  db: process.env.PGDATABASE || "mate",
  user: process.env.PGUSER || "mateadmin",
  password: process.env.PGPASSWORD || process.env.DB_PASSWORD || "",
};

function queryUsers(pattern) {
  const sql = `SELECT id, ci, role FROM users WHERE ci LIKE '${pattern}' ORDER BY ci;`;
  const conn = `host=${pg.host} port=${pg.port} dbname=${pg.db} user=${pg.user} sslmode=require`;
  const out = execSync(`psql "${conn}" -t -A -F'|' -c "${sql}"`, {
    env: { ...process.env, PGPASSWORD: pg.password },
    encoding: "utf8",
  });
  return out
    .trim()
    .split("\n")
    .filter(Boolean)
    .map((line) => {
      const [id, ci, role] = line.split("|");
      return { id: Number(id), ci, role };
    });
}

async function issueToken(userId) {
  const url = new URL(`${LOCAL_API}/api/v1/auth/local-test-token`);
  url.searchParams.set("userId", String(userId));
  if (INCLUDE_REFRESH) url.searchParams.set("includeRefresh", "true");

  const res = await fetch(url, { method: "POST" });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`userId=${userId} status=${res.status} body=${body.slice(0, 200)}`);
  }
  const json = await res.json();
  const data = json.data;
  return {
    userId: data.userId,
    ci: null,
    role: data.role,
    accessToken: data.accessToken,
    refreshToken: data.refreshToken ?? null,
  };
}

async function issueAll(users, label) {
  const results = [];
  let done = 0;
  const queue = [...users];

  async function worker() {
    while (queue.length) {
      const user = queue.shift();
      const entry = await issueToken(user.id);
      entry.ci = user.ci;
      results.push(entry);
      done++;
      if (done % 50 === 0 || done === users.length) {
        process.stdout.write(`\r  ${label}: ${done}/${users.length}`);
      }
    }
  }

  await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()));
  process.stdout.write("\n");
  return results.sort((a, b) => a.ci.localeCompare(b.ci));
}

async function main() {
  console.log("=== k6 토큰 발급 (local-test-token API) ===");
  console.log(`  LOCAL_API: ${LOCAL_API}`);
  console.log(`  includeRefresh: ${INCLUDE_REFRESH}`);

  try {
    const health = await fetch(`${LOCAL_API}/actuator/health`);
    // Redis DOWN 등으로 overall 503이어도 access token 발급 API는 동작할 수 있음
    if (!health.ok && health.status !== 503) {
      throw new Error(`health check failed: ${health.status}`);
    }
    if (health.status === 503) {
      console.warn("  ⚠️  actuator health=503 (Redis 등) — access token 발급은 계속 시도합니다.");
    }
  } catch (e) {
    if (e.cause?.code === "ECONNREFUSED" || e.message?.includes("fetch failed")) {
      console.error(
        "\n로컬 서버가 떠 있지 않습니다.\n" +
          "  ./k6/token/run-local-server.sh\n"
      );
      process.exit(1);
    }
    throw e;
  }

  console.log("\n=== DB 계정 조회 ===");
  const users = queryUsers("K6_CI_USER_%");
  const makers = queryUsers("K6_CI_MAKER_%");

  if (!users.length) {
    console.error("K6_CI_USER_* 계정이 없습니다. seed-accounts.sql 을 먼저 실행하세요.");
    process.exit(1);
  }

  console.log(`  users:  ${users.length}명`);
  console.log(`  makers: ${makers.length}명`);

  console.log("\n=== 토큰 발급 ===");
  const tokens = {
    users: await issueAll(users, "USER"),
    makers: makers.length ? await issueAll(makers, "MAKER") : [],
    issuedAt: new Date().toISOString(),
    targetApi: process.env.TARGET_API || "https://api.kusitms-mate.cloud",
  };

  const outDir = join(root, "data");
  mkdirSync(outDir, { recursive: true });
  const outPath = join(outDir, "tokens.json");
  writeFileSync(outPath, JSON.stringify(tokens, null, 2), "utf8");

  console.log(`\n✅ ${outPath}`);
  console.log(`   users:  ${tokens.users.length} access tokens`);
  console.log(`   makers: ${tokens.makers.length} access tokens`);
  if (INCLUDE_REFRESH) {
    console.log("   refresh tokens → 운영 Redis에 저장됨 (reissue 테스트용)");
  }
  console.log("\n다음: k6 run k6/scenarios/read-baseline/01-test-list.js");
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
