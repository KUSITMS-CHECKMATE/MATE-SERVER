#!/bin/bash

echo "=============================="
echo " MATE-SERVER 개발 환경 설정"
echo "=============================="
echo ""

FAILED=0

# 1. gh CLI 확인
echo "[1/4] gh CLI 확인 중..."
if ! command -v gh &>/dev/null; then
  echo "  ✗ gh CLI가 없습니다. 설치해주세요:"
  echo "    brew install gh"
  FAILED=1
else
  echo "  ✓ gh CLI 설치됨"
fi

# 2. claude CLI 확인
echo "[2/4] claude CLI 확인 중..."
if ! command -v claude &>/dev/null; then
  echo "  ✗ claude CLI가 없습니다. 설치해주세요:"
  echo "    npm install -g @anthropic-ai/claude-code"
  FAILED=1
else
  echo "  ✓ claude CLI 설치됨"
fi

# 3. GITHUB_TOKEN 확인
echo "[3/4] GITHUB_TOKEN 확인 중..."
if [[ -z "$GITHUB_TOKEN" ]]; then
  echo "  ✗ GITHUB_TOKEN이 설정되어 있지 않습니다."
  echo "    1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens"
  echo "    2. 권한: Issues (Read & Write), Pull requests (Read & Write), Contents (Read)"
  echo "    3. 아래를 ~/.zshrc 또는 ~/.bashrc에 추가 후 source 적용:"
  echo "       export GITHUB_TOKEN=ghp_여기에_토큰_입력"
  FAILED=1
else
  echo "  ✓ GITHUB_TOKEN 설정됨"
fi

# 4. git hook 경로 설정
echo "[4/4] git hook 경로 설정 중..."
git config core.hooksPath .githooks
if [[ $? -eq 0 ]]; then
  echo "  ✓ core.hooksPath = .githooks 설정 완료"
else
  echo "  ✗ git config 설정 실패"
  FAILED=1
fi

echo ""
if [[ $FAILED -eq 1 ]]; then
  echo "  위 항목을 해결한 후 다시 ./setup.sh 를 실행해주세요."
  exit 1
else
  echo "  모든 설정 완료! Claude에게 PR 생성을 요청하여 작업을 진행할 수 있습니다."
fi
