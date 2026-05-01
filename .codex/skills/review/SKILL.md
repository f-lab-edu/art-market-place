---
name: review
description: Use this skill to review repository changes against project rules, architecture docs, ADR decisions, tests, and buildability, especially for Harness-style review requests.
metadata:
  short-description: Review changes with project guardrails
---

# Review

이 프로젝트의 변경 사항을 리뷰하라.

## 준비

먼저 다음 문서들을 읽어라:

- `/AGENTS.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`

`CLAUDE.md`가 있으면 함께 읽어라.

그런 다음 변경된 파일들을 확인한다:

```bash
git status --short
git diff --stat
git diff
```

필요하면 staged 변경은 `git diff --cached`로 별도 확인한다.

## 체크리스트

1. **아키텍처 준수**: `docs/ARCHITECTURE.md`에 정의된 디렉토리 구조와 모듈 경계를 따르고 있는가?
2. **기술 스택 준수**: `docs/ADR.md`에 정의된 기술 선택을 벗어나지 않았는가?
3. **테스트 존재**: 새로운 기능 또는 위험한 변경에 대한 테스트가 작성되어 있는가?
4. **CRITICAL 규칙**: `AGENTS.md` 또는 `CLAUDE.md`의 필수 규칙을 위반하지 않았는가?
5. **빌드 가능**: 프로젝트에 맞는 빌드/테스트 명령어가 에러 없이 통과하는가?

## 출력 방식

Codex의 코드 리뷰 기본 형식을 따른다:

- 발견사항을 먼저, 심각도 순으로 제시한다.
- 각 발견사항은 파일/라인 근거를 포함한다.
- 문제가 없으면 명확히 "발견한 문제 없음"이라고 말한다.
- 남은 테스트 공백이나 확인하지 못한 검증은 별도로 적는다.
- 위반 사항이 있으면 수정 방안을 구체적으로 제시한다.

마지막에 체크리스트 요약을 붙인다:

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 아키텍처 준수 | ✅/❌ | {상세} |
| 기술 스택 준수 | ✅/❌ | {상세} |
| 테스트 존재 | ✅/❌ | {상세} |
| CRITICAL 규칙 | ✅/❌ | {상세} |
| 빌드 가능 | ✅/❌ | {상세} |
