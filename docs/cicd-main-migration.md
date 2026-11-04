# CI/CD 정리: PR 테스트 · main 단일 배포 · 버전 릴리스 (#358)

## 최종 흐름

```
기능 브랜치 ──PR(ci-test)──▶ dev ──PR(ci-test)──▶ main ──push──▶ ci-acr: 이미지 빌드·푸시 + newTag 커밋 ──▶ ArgoCD 배포
                                                   └─ vX.Y.Z 태그 ──▶ release: 이미지 버전 태그 + GitHub Release
```

| 워크플로 | 트리거 | 하는 일 |
|---|---|---|
| `ci-test.yml` | `dev`, `main` 대상 PR | `./gradlew test` (실패 시 테스트 리포트 업로드) |
| `ci-acr.yml` | `main`(전환 기간에는 `feat/ci`도) push | 이미지 빌드·ACR 푸시, `deploy/apps/mate/kustomization.yaml` `newTag` 커밋 |
| `release.yml` | `vX.Y.Z` 태그 push | 태그 커밋의 `newTag` 이미지에 버전 태그 추가, GitHub Release 생성 |

## 이 PR에 포함된 것 / 포함하지 않은 것

- 포함: `ci-test.yml` 신규, `ci-acr.yml` 트리거에 `main` 추가·PR 트리거 제거·푸시 토큰 교체 가능, `release.yml` 신규, 이 문서
- **포함하지 않음: ArgoCD `targetRevision` 변경.** 이 변경이 dev를 거쳐 `feat/ci`에 먼저 들어가면 ArgoCD가 아직 오래된 `main`(deploy/ 폴더 없음)을 바라보게 되어 리소스가 삭제될 수 있다. 아래 전환 절차 3단계에서 별도로 반영한다.

## 전환 절차 (운영 영향 있음, 순서대로)

### 0. 사전 준비
- [ ] 이 PR을 dev에 머지하고, 평소처럼 dev → `feat/ci`로 반영해 `ci-acr.yml` 변경을 운영 경로에 올린다.
- [ ] Azure 포털 > 앱 등록(`AZURE_CLIENT_ID` 앱) > 인증서 및 비밀 > 페더레이션 자격 증명에 추가
  - `repo:KUSITMS-CHECKMATE/MATE-SERVER:ref:refs/heads/main` (main push 배포용)
  - `repo:KUSITMS-CHECKMATE/MATE-SERVER:ref:refs/tags/v*` 는 와일드카드가 안 되므로, 릴리스 태그용은 **environment 방식**(예: `environment:release`)으로 만들고 `release.yml` job에 `environment: release`를 추가하거나, 태그마다 subject를 추가한다. (팀 결정 필요)

### 1. main을 feat/ci와 같게 맞추기
현재 `main`은 2026-04 초기 커밋이라 `deploy/`, `infra/`가 없다.
```bash
git fetch origin main feat/ci
git merge-base --is-ancestor origin/main origin/feat/ci && echo "fast-forward 가능"
git push origin origin/feat/ci:refs/heads/main     # main을 feat/ci 커밋으로 이동
```
fast-forward가 불가하면(main에만 있는 커밋이 있으면) 내용 확인 후 `feat/ci`를 main에 머지한다.

### 2. 이미지 태그 확인
⚠️ `dev`의 `kustomization.yaml`에는 오래된 태그(예: `gha-72`/`gha-58`)가 남아 있다. dev를 main에 머지할 때 이 값이 main(운영 태그, 예: `gha-107`/`gha-104`)을 덮어쓰면 **오래된 이미지로 롤백**된다.
- [ ] main의 `newTag`가 현재 운영 파드 이미지와 같은지 확인: `kubectl -n mate get deploy -o jsonpath='{..image}'`
- [ ] 전환 후 dev → main PR에서 `kustomization.yaml`의 `newTag` 변경이 보이면 main 쪽 값을 유지한다. (근본 해결: dev의 `newTag`를 main과 맞추거나, 이미지 태그를 dev에서 관리하지 않음)

### 3. ArgoCD를 main으로
```bash
# main 기준 브랜치에서
sed -i 's/targetRevision: feat\/ci/targetRevision: main/' \
  deploy/argocd/root-application.yaml \
  deploy/argocd/applications/{platform,mate,monitoring,kube-prometheus-stack,loki-stack}-application.yaml
grep -rn 'targetRevision: feat/ci' deploy/argocd || echo "6곳 모두 변경됨"
```
- [ ] 변경을 main에 반영한 뒤, root 앱은 자기 자신을 동기화하지 않으므로 직접 적용: `kubectl apply -f deploy/argocd/root-application.yaml`
- [ ] ArgoCD에서 모든 앱 `Synced / Healthy` 확인

### 4. CI 배포 브랜치 정리
- [ ] main에 테스트 커밋을 머지해 `ci-acr.yml`이 main에서 빌드·푸시·`newTag` 커밋까지 되는지 확인
- [ ] `ci-acr.yml`의 `branches: [main, feat/ci]`에서 `feat/ci` 제거

### 5. 브랜치 보호
- [ ] `dev`: Require status checks → `CI - test / test` (머지 필수 조건으로 걸기로 한 경우)
- [ ] `main`: PR 필수 + `CI - test / test` 필수
- [ ] main 보호 후 CI의 `newTag` 커밋이 막히므로, 우회 권한이 있는 GitHub App 토큰 또는 deploy key를 `CD_PUSH_TOKEN` 시크릿으로 등록 (ruleset bypass list에 해당 App 추가)

### 6. feat/ci 은퇴
- [ ] 운영 배포가 main에서 정상 동작하는 것을 확인한 뒤 `feat/ci` 삭제
- [ ] `CLAUDE.md`의 `배포 브랜치: feat/ci` → `main`, 브랜치 흐름 `기능 → dev → main` 반영

## 버전 릴리스

- 버전 규칙(SemVer): 호환 깨지는 API 변경 → Major, 호환되는 기능 추가 → Minor, 버그 수정 → Patch
- 첫 버전: 전환 직후 운영 상태를 `v1.0.0`으로 (팀 결정 필요)
- 태그는 main 커밋에만 붙인다 (`release.yml`이 main에 없는 커밋이면 실패시킨다)

```bash
git checkout main && git pull
git tag -a v1.0.0 -m "v1.0.0"
git push origin v1.0.0
```
또는 GitHub > Releases > Draft a new release 에서 태그 `v1.0.0`, target `main`으로 생성.

- 롤백: `deploy/apps/mate/kustomization.yaml`의 `newTag`를 이전 버전 태그(예: `v1.0.0`)로 바꿔 main에 반영 → ArgoCD가 해당 이미지로 배포
- GHCR 병행 여부: 배포는 ACR로 충분. GitHub Packages 노출이 필요하면 추가 (public 레포이므로 이미지 공개 여부 먼저 결정)
