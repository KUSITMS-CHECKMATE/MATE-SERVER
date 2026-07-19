# MATE-SERVER Claude 가이드

## 초기 설정 (최초 1회)

### 1. GitHub Personal Access Token 발급
1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
2. 권한: `Issues (Read & Write)`, `Pull requests (Read & Write)`, `Contents (Read)`
3. 발급 후 `~/.zshrc` 또는 `~/.bashrc`에 추가:
```bash
export GITHUB_TOKEN=ghp_여기에_토큰_입력
```
4. `source ~/.zshrc` 로 적용

### 2. Git Hook 활성화
```bash
git config core.hooksPath .githooks
```

### 3. GitHub CLI 설치 (없는 경우)
```bash
brew install gh
gh auth login
```

---

## GitHub Issue / PR 생성

이슈는 /issue, PR은 /pr 커맨드를 사용한다.
상세 규칙은 .claude/commands/issue.md, pr.md 참조.

---

## 커밋 메시지 컨벤션

```
<type>: <한글 설명> (#이슈번호)
```

타입은 브랜치·이슈·PR과 동일한 체계를 사용한다: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

예시

```
feat: 토스 unlink 처리 시 프로모션 리워드와 사용자 식별정보 익명화 연동 (#255)
fix: unlink 시 프로모션 리워드 벌크 쿼리로 인한 사용자/계정 익명화 유실 수정 (#255)
```

---

## 레포지토리 정보

- **GitHub**: `KUSITMS-CHECKMATE/MATE-SERVER`
- **기본 브랜치**: `dev`
- **배포 브랜치**: `feat/ci`
