# k6 토큰 발급 가이드

## 전체 흐름

```
터미널 A: kubectl port-forward (계속 유지)
터미널 B: Spring Boot 로컬 서버 실행 (계속 유지)
터미널 C: seed-accounts.sql 실행 → issue-tokens.mjs 실행
결과: k6/data/tokens.json 생성
```

---

## Step 1 — k8s → Azure 서비스 터널 (터미널 A·B, 계속 유지)

PostgreSQL과 Redis는 Azure 관리형 서비스 (private endpoint)라 k8s 서비스가 아님.
socat 터널 pod을 k8s에 띄워서 로컬 → k8s pod → Azure 서비스로 연결한다.

### 1-1. hostname 확인

```bash
# PostgreSQL — jdbc:postgresql://<host>:5432/... 에서 host 부분 복사
kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.DB_URL}' | base64 -d

# Redis hostname
kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.REDIS_HOST}' | base64 -d

# Redis password (.env에 필요)
kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d

# DB password (.env에 필요)
kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.DB_PASSWORD}' | base64 -d

# JWT_SECRET (.env에 필요)
kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.JWT_SECRET}' | base64 -d
```

### 1-2. socat 터널 pod 실행

```bash
PG_HOST=<1-1에서 나온 postgres 호스트>
REDIS_HOST=<1-1에서 나온 redis 호스트>

kubectl run pg-tunnel -n mate \
  --image=alpine/socat --restart=Never -- \
  socat TCP-LISTEN:5432,fork TCP:${PG_HOST}:5432

# Azure Redis는 SSL 포트 6380 사용
kubectl run redis-tunnel -n mate \
  --image=alpine/socat --restart=Never -- \
  socat TCP-LISTEN:6380,fork TCP:${REDIS_HOST}:6380
```

### 1-3. 로컬로 포트포워딩

```bash
# 터미널 A
kubectl port-forward pod/pg-tunnel 15432:5432 -n mate

# 터미널 B
kubectl port-forward pod/redis-tunnel 16379:6380 -n mate
```

연결 확인:
```bash
psql "host=127.0.0.1 port=15432 dbname=mate user=mateadmin sslmode=require"
```

---

## Step 2 — seed-accounts.sql 실행 (최초 1회)

운영 DB에 k6 전용 테스트 계정 1100명을 삽입한다.

**A. Step 1 포트포워딩 연결 후 로컬에서 실행:**
```bash
psql "host=127.0.0.1 port=15432 dbname=mate user=mateadmin sslmode=require" \
  -f k6/token/pdf-excel.sql
```

**B. 포트포워딩 없이 k8s 안에서 직접 실행 (더 간단):**
```bash
PG_URL=$(kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.DB_URL}' | base64 -d | sed 's/jdbc://')
PG_PASS=$(kubectl get secret mate-app-secrets -n mate \
  -o jsonpath='{.data.DB_PASSWORD}' | base64 -d)

kubectl run psql-tmp --rm -it --image=postgres:16 -n mate --restart=Never \
  --env="PGPASSWORD=${PG_PASS}" \
  -- psql "$PG_URL" -f /dev/stdin < k6/token/pdf-excel.sql
```

생성되는 계정:
- `K6_CI_USER_0001` ~ `K6_CI_USER_1000` (USER 역할) 1000명
- `K6_CI_MAKER_001` ~ `K6_CI_MAKER_100` (USER 역할) 100명

`ON CONFLICT DO NOTHING` — 여러 번 실행해도 안전.

결과 확인:
```sql
SELECT role, COUNT(*) FROM users WHERE ci LIKE 'K6_CI_%' GROUP BY role;
```

---

## Step 3 — .env 설정 + 로컬 서버 실행 (터미널 B, 계속 유지)

### 3-1. .env 파일 생성

```bash
cp k6/token/env.example k6/token/.env
```

### 3-2. .env 값 채우기

```
DB_URL=jdbc:postgresql://127.0.0.1:15432/mate?sslmode=require
DB_USERNAME=mateadmin
DB_PASSWORD=<운영 DB 비밀번호>

REDIS_HOST=127.0.0.1
REDIS_PORT=16379
REDIS_PASSWORD=<운영 Redis 비밀번호>
REDIS_SSL_ENABLED=true

JWT_SECRET=<Azure Key Vault의 jwt-secret 값>
```

> **JWT_SECRET 위치:** Azure Portal → Key Vault → Secrets → `jwt-secret`

### 3-3. 로컬 서버 실행

```bash
./k6/token/run-local-server.sh
```

서버 기동 확인:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"} 나오면 OK
```

**핵심:** 로컬 서버가 운영과 동일한 JWT_SECRET으로 기동되므로, 여기서 발급한 토큰이 운영 API(`api.kusitms-mate.cloud`)에서도 유효하다.

---

## Step 4 — 토큰 발급 (터미널 C)

1100명 계정의 토큰을 `k6/data/tokens.json`으로 저장한다.

```bash
node k6/token/issue-tokens.mjs
```

내부 동작:
1. 운영 DB에서 `K6_CI_USER_*`, `K6_CI_MAKER_*` 계정의 user ID 조회
2. `POST http://localhost:8080/api/v1/auth/local-test-token?userId={id}&includeRefresh=true` 20개 동시 호출
3. 응답 토큰을 `k6/data/tokens.json`에 저장

생성 파일 구조:
```json
{
  "users": [
    { "userId": 101, "ci": "K6_CI_USER_0001", "accessToken": "eyJ...", "refreshToken": "..." }
  ],
  "makers": [
    { "userId": 1101, "ci": "K6_CI_MAKER_001", "accessToken": "eyJ...", "refreshToken": "..." }
  ]
}
```

---

## k6 실행

`tokens.json` 생성 후 바로 k6 스크립트를 실행할 수 있다. `k6/lib/auth.js`가 VU별로 토큰을 자동 분배한다.

| 함수 | 사용 스크립트 | 분배 방식 |
|------|-------------|---------|
| `getUserToken()` | read-baseline, like-toggle 등 | `VU % 1000` |
| `getMakerToken()` | report-json 등 maker 전용 | `VU % 100` |
| `getUserTokenByIteration()` | like-toggle (iteration 기준) | `iteration % 1000` |
| `getMakerTokenByIndex(n)` | pdf/excel 생성 (VU 직접 지정) | 인덱스 직접 |
| `getRefreshToken()` | auth-reissue | `VU % 1000` |

실행 예시:
```bash
# like-toggle: 각 VU가 서로 다른 user 토큰 사용
k6 run k6/scenarios/write-baseline/03-like-toggle.js \
  -e BASE_URL=https://api.kusitms-mate.cloud \
  -e TEST_ID=<테스트ID>

# pdf 생성: TEST_IDS 개수만큼 VU, 각각 다른 maker 토큰
k6 run k6/scenarios/pdf-excel/02-pdf-generate.js \
  -e BASE_URL=https://api.kusitms-mate.cloud \
  -e TEST_IDS=1001,1002,1003,...

# smoke 테스트 (10명으로 빠른 확인)
k6 run k6/scenarios/read-baseline/01-test-list.js \
  -e BASE_URL=https://api.kusitms-mate.cloud \
  -e K6_PROFILE=smoke
```

---

## 계정 정리

테스트 완료 후 계정을 삭제하려면:
```bash
psql "host=127.0.0.1 port=15432 dbname=mate user=mateadmin sslmode=require" \
  -f k6/token/pdf-excel.sql
```
