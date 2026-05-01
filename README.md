# art-market-place

## 소개

`art-market-place`는 예술 작품과 콘텐츠를 중심으로 회원, 상품, 기술 블로그 기능을 제공하는 웹 서비스입니다.

## 레포지토리 구조

```text
art-market-place
|-- AGENTS.md
|   `-- Codex 작업 규칙과 문서 읽기 순서
|-- docs/
|   |-- PRD.md          : 제품 목표와 범위
|   |-- ARCHITECTURE.md : 시스템 경계, 모듈, 주요 데이터 흐름
|   |-- ADR.md          : 기술적 의사결정과 근거
|   |-- FRONTEND.md     : 프론트엔드 구조와 규칙
|   `-- DESIGN.md       : 디자인 가이드
|-- .codex/
|   |-- settings.json   : Codex/Harness용 검증 명령과 위험 명령 패턴
|   `-- skills/
|       |-- harness/    : Harness phase/step 실행 워크플로우
|       `-- review/     : 규칙 기반 변경사항 리뷰
|-- scripts/
|   |-- execute.py      : Harness phase 순차 실행기
|   `-- test_execute.py : execute.py 자동 검증 테스트
|-- front/              : Next.js 프론트엔드 애플리케이션
`-- src/                : Spring 백엔드 애플리케이션 코드
```

## 주요 기술

- 백엔드: Spring Boot 4, Java 25, Maven
- 프론트엔드: Next.js 14, React 18, TypeScript, Tailwind CSS
- 데이터 저장소: PostgreSQL, Redis
- 파일 저장소: S3 호환 스토리지 또는 MinIO
- 인증: Google OAuth, access/refresh token

## 기본 검증

```bash
./mvnw test -Dspring.jpa.hibernate.ddl-auto=validate -Dspring.jpa.properties.hibernate.hbm2ddl.auto=validate
cd front && npm run build
```

Harness 실행기 자체 검증:

```bash
.venv/bin/python -m pytest scripts/test_execute.py -q
```

## 문서

작업 전에는 다음 문서를 먼저 확인합니다.

1. `docs/PRD.md`
2. `docs/ARCHITECTURE.md`
3. `docs/ADR.md`
4. `docs/FRONTEND.md`
5. `docs/DESIGN.md`
