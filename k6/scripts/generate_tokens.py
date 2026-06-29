#!/usr/bin/env python3
"""
부하테스트용 JWT 토큰 생성 스크립트

사전 조건:
  1. kubectl port-forward 로 DB 접속 (127.0.0.1:15432)
  2. k8s에서 JWT_SECRET 추출:
       kubectl get secret mate-app-secrets -n <namespace> \
         -o jsonpath='{.data.JWT_SECRET}' | base64 -d
  3. sql/seed/accounts.sql 실행 완료 (LOADTEST 계정이 DB에 있어야 함)

사용법:
  export JWT_SECRET="<추출한 시크릿>"
  python3 k6/scripts/generate_tokens.py

  또는 직접 인자로:
  python3 k6/scripts/generate_tokens.py --secret "<시크릿>" --hours 8

출력:
  k6/data/tokens.json  ← REPLACE_ME 값이 실제 토큰으로 교체됨
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import time
from pathlib import Path


# JWT 생성
def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def make_jwt(user_id: int, role: str, secret: str, hours: int = 8) -> str:
    """JwtProvider.generateToken 과 동일한 구조의 HS256 토큰 생성."""
    now = int(time.time())
    header = _b64url(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    payload = _b64url(json.dumps({
        "sub": str(user_id),
        "role": role,
        "tokenType": "ACCESS",
        "iat": now,
        "exp": now + hours * 3600,
    }, separators=(",", ":")).encode())

    signing_input = f"{header}.{payload}"
    sig = _b64url(
        hmac.new(secret.encode("utf-8"), signing_input.encode("utf-8"), hashlib.sha256).digest()
    )
    return f"{signing_input}.{sig}"


# DB 조회
def query_psql(host: str, port: str, dbname: str, user: str, password: str, sql: str) -> list[str]:
    env = {**os.environ, "PGPASSWORD": password}
    conn_str = f"host={host} port={port} dbname={dbname} user={user} sslmode=require"
    result = subprocess.run(
        ["psql", conn_str, "-t", "-A", "-F|", "-c", sql],
        capture_output=True, text=True, env=env
    )
    if result.returncode != 0:
        print(f"[DB 오류]\n{result.stderr}", file=sys.stderr)
        sys.exit(1)
    return [line.strip() for line in result.stdout.strip().splitlines() if line.strip()]


def parse_rows(rows: list[str]) -> list[dict]:
    """psql -A -F| 출력 → [{"id": int, "ci": str, "role": str}]"""
    result = []
    for row in rows:
        parts = [p.strip() for p in row.split("|")]
        if len(parts) >= 3:
            try:
                result.append({"id": int(parts[0]), "ci": parts[1], "role": parts[2]})
            except ValueError:
                pass  # 헤더 행 등 skip
    return result


def main():
    p = argparse.ArgumentParser(description="LOADTEST 계정 JWT 토큰 생성")
    p.add_argument("--host",     default="127.0.0.1",         help="DB host")
    p.add_argument("--port",     default="15432",              help="DB port")
    p.add_argument("--dbname",   default="mate",               help="DB name")
    p.add_argument("--user",     default="mateadmin",          help="DB user")
    p.add_argument("--password", default="kusitmsMate1234!",   help="DB password")
    p.add_argument("--secret",   default=os.environ.get("JWT_SECRET", ""),
                   help="JWT 서명 시크릿 (또는 JWT_SECRET 환경변수)")
    p.add_argument("--hours",    type=int, default=8,
                   help="토큰 유효 시간 (기본: 8시간 — 테스트 총 소요 시간보다 넉넉히)")
    args = p.parse_args()

    if not args.secret:
        print(
            "오류: JWT_SECRET 이 없습니다.\n"
            "  export JWT_SECRET=$(kubectl get secret mate-app-secrets -n <namespace> \\\n"
            "    -o jsonpath='{.data.JWT_SECRET}' | base64 -d)\n"
            "  python3 k6/scripts/generate_tokens.py",
            file=sys.stderr
        )
        sys.exit(1)

    db = dict(host=args.host, port=args.port, dbname=args.dbname,
              user=args.user, password=args.password)

    print("=== LOADTEST 계정 조회 ===")
    user_rows  = query_psql(**db, sql="SELECT id, ci, role FROM users WHERE ci LIKE 'LOADTEST_CI_USER_%'  ORDER BY ci;")
    maker_rows = query_psql(**db, sql="SELECT id, ci, role FROM users WHERE ci LIKE 'LOADTEST_CI_MAKER_%' ORDER BY ci;")

    users  = parse_rows(user_rows)
    makers = parse_rows(maker_rows)

    if not users or not makers:
        print("⚠️  LOADTEST 계정이 없습니다. sql/seed/accounts.sql 을 먼저 실행하세요.", file=sys.stderr)
        sys.exit(1)

    print(f"  USER  {len(users):3d}명 조회 완료")
    print(f"  MAKER {len(makers):3d}명 조회 완료")

    exp_ts = int(time.time()) + args.hours * 3600
    exp_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(exp_ts))
    print(f"\n=== JWT 생성 (유효 기간: {args.hours}시간 → {exp_str} 까지) ===")

    tokens_json: dict = {"users": [], "makers": []}

    for u in users:
        token = make_jwt(u["id"], u["role"], args.secret, args.hours)
        tokens_json["users"].append({
            "userId":      u["id"],
            "ci":          u["ci"],
            "accessToken": token,
        })

    for m in makers:
        token = make_jwt(m["id"], m["role"], args.secret, args.hours)
        tokens_json["makers"].append({
            "userId":      m["id"],
            "ci":          m["ci"],
            "accessToken": token,
        })

    out_path = Path(__file__).parent.parent / "data" / "tokens.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(tokens_json, f, indent=2, ensure_ascii=False)

    print(f"\n✅ 완료 → {out_path}")
    print(f"   users:  {len(tokens_json['users'])}개 토큰")
    print(f"   makers: {len(tokens_json['makers'])}개 토큰")
    print(f"\n⚠️  테스트 시작 전 {args.hours}시간 이내에 이 스크립트를 실행하세요.")


if __name__ == "__main__":
    main()
