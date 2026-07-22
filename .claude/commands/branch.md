# 브랜치 생성 커맨드

## 사용법

```
/branch <이슈번호> <설명>
```

사용 예시

```
/branch 12 detail-concurrency
```

## 동작 순서

1. `$ARGUMENTS` 파싱을 통한 이슈번호와 설명 추출
2. 커밋 타입 선택 질의
3. 브랜치명 조합 및 생성 여부 확인 요청
4. 확인 후 브랜치 생성 명령 실행
5. 생성된 브랜치명 출력

## 브랜치 생성 명령

| 순서 | 명령 | 목적 |
|---|---|---|
| 1 | `git fetch origin dev` | 최신 dev 동기화 |
| 2 | `git checkout -b <브랜치명> origin/dev` | 로컬 브랜치 생성 |
| 3 | `git push -u origin <브랜치명>` | 원격 브랜치 생성 및 추적 설정 |

## 브랜치 명명 규칙

```
<type>/#<이슈번호>-<설명>
```

### 타입 선택 기준

| 타입 | 사용 시점 |
|---|---|
| `feat` | 신규 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서, 주석 변경 |
| `test` | 테스트 코드 추가 및 수정 |
| `chore` | 빌드, 설정, 의존성 등 기타 변경 |

### 명명 예시

```
feat/#12-detail-concurrency
fix/#9-headless-backoff
refactor/#11-csv-columns
```

## 실행 명령 예시

```bash
git fetch origin dev
git checkout -b feat/#12-detail-concurrency origin/dev
git push -u origin feat/#12-detail-concurrency
```
