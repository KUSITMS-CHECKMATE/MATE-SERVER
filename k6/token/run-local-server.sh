#!/usr/bin/env bash
# 운영 DB·Redis·JWT_SECRET 에 맞춘 로컬 서버 (토큰 발급 전용)
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"

if [[ ! -f "$DIR/.env" ]]; then
  echo "먼저: cp k6/token/env.example k6/token/.env 후 값을 채우세요"
  exit 1
fi

set -a
# shellcheck disable=SC1091
source "$DIR/.env"
set +a

: "${JWT_SECRET:?JWT_SECRET 이 .env 에 없습니다}"
: "${DB_URL:?DB_URL 이 .env 에 없습니다}"

cd "$ROOT"

exec ./gradlew bootRun --args="\
--spring.profiles.active=local \
--spring.datasource.url=${DB_URL} \
--spring.datasource.username=${DB_USERNAME:-mateadmin} \
--spring.datasource.password=${DB_PASSWORD:-} \
--jwt.secret=${JWT_SECRET} \
--spring.data.redis.host=${REDIS_HOST:-127.0.0.1} \
--spring.data.redis.port=${REDIS_PORT:-6379} \
--spring.data.redis.password=${REDIS_PASSWORD:-} \
--spring.data.redis.ssl.enabled=${REDIS_SSL_ENABLED:-false}"
