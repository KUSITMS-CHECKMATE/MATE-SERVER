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

## GitHub Issue 생성

별도 명령어 없이 자연어로 말하면 돼:

> "로그인 시 500 에러 발생하는 버그 이슈 만들어줘"
> "Excel 다운로드 기능 추가 feature 이슈 만들어줘"

Claude가 자동으로 이슈 템플릿에 맞게 생성해줘.

### 이슈 유형별 라벨
| 유형 | 라벨 | 제목 prefix |
|------|------|-------------|
| 버그 | `fix` | `fix: ` |
| 기능 | `feat` | `feat: ` |
| 작업 | - | `chore:` / `refactor:` 등 |

---

## PR 생성 요청

**기능 브랜치에서 작업 완료 후 Claude에게 PR 생성을 요청할 수 있습니다.**

> "현재 브랜치 기준으로 dev 브랜치로의 PR 생성해줘"

- base 브랜치: `dev`

---

## 레포지토리 정보

- **GitHub**: `KUSITMS-CHECKMATE/MATE-SERVER`
- **기본 브랜치**: `dev`
- **배포 브랜치**: `feat/ci`
