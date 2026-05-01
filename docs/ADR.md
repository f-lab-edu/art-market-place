# ADR

이 문서는 현재 코드 기준의 주요 기술 선택과 이유를 기록한다.

## ADR-001. 백엔드는 Spring Boot 4와 Java 25를 사용한다

- 상태: 적용
- 이유: MVC, JPA, Validation, Redis, 테스트 자동설정 등 서버 애플리케이션에 필요한 기능을 통합적으로 제공한다.
- 영향: Java 25 런타임과 Maven 빌드 환경이 필요하다.

## ADR-002. 백엔드는 도메인 패키지 단위로 나눈다

- 상태: 적용
- 결정: `auth`, `product`, `blog`, `_common` 패키지를 기준으로 controller/service/entity/repository를 둔다.
- 이유: 기능 경계를 코드 구조에서 바로 파악할 수 있고, Harness step을 모듈 단위로 쪼개기 쉽다.
- 주의: 공통 유틸을 무분별하게 `_common`으로 옮기지 않는다. 여러 도메인이 실제로 공유할 때만 공통화한다.

## ADR-003. 영속성은 PostgreSQL + Spring Data JPA를 사용한다

- 상태: 적용
- 이유: 관계형 도메인 모델과 트랜잭션 처리가 핵심이며, 엔티티 기반 개발 생산성이 높다.
- 운영 규칙: Hibernate DDL 자동 생성은 사용하지 않고 validate를 기본으로 한다.
- 검증: `SchemaValidationTest`와 Harness validation 명령으로 엔티티/스키마 불일치를 실패 처리한다.

## ADR-004. 토큰과 OAuth state는 Redis에 저장한다

- 상태: 적용
- 이유: refresh token, authorization state는 TTL과 빠른 조회가 필요하다.
- 영향: 로컬/테스트 환경에서 Redis 의존성이 생긴다.
- 주의: Redis 장애 시 인증 흐름이 실패할 수 있으므로 에러 메시지를 명확히 유지한다.

## ADR-005. 이미지 업로드는 S3 호환 스토리지와 Presigned URL을 사용한다

- 상태: 적용
- 이유: 대용량 파일을 애플리케이션 서버로 직접 중계하지 않고 클라이언트가 스토리지로 업로드하게 한다.
- 구현: `ProductImageStorageService`, `StorageProperties`, `StorageConfig`
- 로컬: MinIO를 S3 호환 스토리지로 사용할 수 있다.

## ADR-006. 프론트엔드는 Next.js 14와 React 18을 사용한다

- 상태: 적용
- 이유: App Router 기반 라우팅, 서버/클라이언트 컴포넌트 분리, 정적/동적 페이지 구성이 가능하다.
- 위치: `front/`
- 주의: 현재 `next/font`가 Google Fonts를 조회하므로 네트워크가 막힌 환경에서는 빌드 실패가 발생할 수 있다.

## ADR-007. UI 컴포넌트는 Radix 기반 컴포넌트와 Tailwind 스타일을 사용한다

- 상태: 적용
- 이유: 접근성 있는 primitive와 일관된 스타일 합성이 가능하다.
- 위치: `front/components/ui/`
- 규칙: 새 UI는 기존 컴포넌트 조합을 우선 사용한다.

## ADR-008. Codex/Harness 기반 실행 계획을 사용한다

- 상태: 적용
- 이유: 큰 작업을 phase/step 단위로 나눠 독립 실행하고, 검증 명령을 자동으로 강제할 수 있다.
- 위치:
  - `.codex/skills/harness`
  - `.codex/skills/review`
  - `.codex/settings.json`
  - `scripts/execute.py`
- 규칙: Harness step 완료는 Codex 응답이 아니라 validation 명령 통과로 확정한다.
