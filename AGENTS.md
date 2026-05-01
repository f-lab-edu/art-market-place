# AGENTS

이 문서는 Codex와 기타 코딩 에이전트가 `art-market-place` 레포지토리에서 작업할 때 따라야 할 기준을 정의한다.

## 목적

- 제품 의도, 아키텍처, 기술 결정, 프론트엔드/디자인 기준을 먼저 파악하게 한다.
- 구현 변경이 문서, 테스트, Harness 실행 계획과 함께 관리되도록 한다.
- ERD나 주요 아키텍처 변경이 검증 없이 섞이지 않도록 한다.

## 문서 읽기 순서

1. `docs/PRD.md`
2. `docs/ARCHITECTURE.md`
3. `docs/ADR.md`
4. `docs/FRONTEND.md`
5. `docs/DESIGN.md`
6. 관련 코드와 테스트

## 프로젝트 개요

- 백엔드: Spring Boot 4, Java 25, Maven 단일 모듈 애플리케이션
- 프론트엔드: `front/` 하위 Next.js 14 애플리케이션
- 주요 도메인: 인증/회원, 상품 등록과 이미지 업로드, 기술 블로그 게시글/카테고리/댓글/좋아요
- 인프라: PostgreSQL, Redis, S3 호환 스토리지(MinIO/S3), Google OAuth

## 작업 규칙

- 동작이나 구조가 바뀌면 관련 문서를 함께 갱신한다.
- 도메인별 패키지 경계를 우선 유지한다: `auth`, `product`, `blog`, `_common`.
- 엔티티 변경은 ERD 변경으로 간주한다. Harness 검증에서 Hibernate `hbm2ddl.auto=validate`가 통과해야 한다.
- 위험 명령(`rm -rf`, `git reset --hard`, `git push --force`, destructive SQL)은 사용자 명시 승인 없이 실행하지 않는다.
- 프론트엔드는 `front/` 내부 규칙과 기존 컴포넌트 체계를 따른다.
- 테스트 추가/수정은 변경 범위에 맞게 최소한으로 하되, 서비스 로직과 API 계약 변경에는 테스트를 우선 고려한다.

## Harness / Codex 운영

- 로컬 스킬은 `.codex/skills/` 아래에 둔다.
- Harness 실행기는 `scripts/execute.py`를 사용한다.
- Harness 검증 명령은 `.codex/settings.json`의 `validation.commands`에 둔다.
- Harness step이 `completed`로 표시되어도 검증 명령이 실패하면 완료 처리하지 않는다.
- 실행기 자체 검증은 다음 명령을 사용한다.

```bash
.venv/bin/python -m pytest scripts/test_execute.py -q
```

## 기본 검증 명령

```bash
./mvnw test -Dspring.jpa.hibernate.ddl-auto=validate -Dspring.jpa.properties.hibernate.hbm2ddl.auto=validate
cd front && npm run build
```

현재 프론트 빌드는 Google Fonts 네트워크 접근에 영향을 받을 수 있다.
