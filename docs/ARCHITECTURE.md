# ARCHITECTURE

`art-market-place`는 Spring Boot 백엔드와 Next.js 프론트엔드를 같은 레포지토리에 둔 단일 저장소 구조다.

## 상위 구조

```text
art-market-place
|-- src/                  Spring Boot 백엔드
|-- front/                Next.js 프론트엔드
|-- docs/                 제품/아키텍처/기술결정 문서
|-- scripts/              Harness 실행기와 검증 테스트
|-- .codex/               Codex 스킬과 Harness 검증 설정
|-- .docker-compose/      로컬 PostgreSQL, Redis, Kafka, MinIO 구성
`-- pom.xml               백엔드 Maven 설정
```

## 백엔드 모듈

### `auth`

회원가입, 로그인, Google OAuth callback, 토큰 발급/재발급을 담당한다.

- Controller: `AuthController`, `TokenGenerateController`
- Service: `AuthService`, `TokenService`, Google OAuth 관련 client/store
- Entity: `Buyer`, `Seller`, `Address`, `MemberType`
- Token: `TokenGenerator`, `TokenStore`, Redis 기반 구현

### `product`

상품 등록과 상품 이미지 업로드/복구를 담당한다.

- Controller: `ProductController`
- Service: `ProductService`, `ProductImageStorageService`
- Entity: `Product`, `ProductImage`, `ProductTag`, `Tag`
- Event: 이미지 등록 후 스토리지 이동 처리를 위한 이벤트 리스너
- Storage: S3 호환 스토리지 설정과 Presigned URL 생성

### `blog`

게시글, 카테고리, 댓글, 좋아요 기능을 담당한다.

- Controller: `PostController`, `CategoryController`, `CommentController`, `LikeController`
- Service: 각 도메인별 interface/implementation 분리
- Entity: `Post`, `Category`, `Comment`, `Like`
- Support: 페이징, Redis 지원, 업로드 진행 스트림

### `_common`

공통 요청 처리와 필터를 둔다.

- `AccessTokenLoginIdHeaderFilter`: access token 기반 loginId 헤더 주입
- `MutableHttpServletRequest`: 요청 헤더 조작을 위한 wrapper

## 데이터 저장소

- PostgreSQL: JPA 영속성 저장소
- Redis: 토큰, OAuth state, 세션/캐시성 데이터
- S3/MinIO: 상품 이미지와 블로그 첨부 파일 저장소

## 주요 흐름

### 인증 흐름

1. 클라이언트가 회원가입 또는 로그인 authorization 생성을 요청한다.
2. 서버가 Google authorization URL과 state를 발급한다.
3. Google callback에서 code/state를 검증한다.
4. 서버가 회원 정보를 확인하고 access/refresh token을 발급한다.
5. refresh token은 device, IP 정보와 함께 저장소에서 검증한다.

### 상품 이미지 흐름

1. 클라이언트가 이미지 `contentType`으로 업로드용 Presigned URL을 요청한다.
   단건 발급 또는 대표/썸네일/상세 이미지 묶음 발급을 사용할 수 있으며, 서버는 허용된 이미지 타입을 검증하고 UUID 기반 temp object key를 생성한다.
2. 클라이언트가 temp 경로에 직접 업로드한다.
3. 상품 등록 시 대표 이미지, 썸네일 이미지 목록, 상세 이미지 목록의 temp image key를 함께 전달한다.
   서버는 temp object 존재 여부를 확인한 뒤 상품을 저장한다.
4. 트랜잭션 커밋 이후 이미지가 상품 경로로 이동된다.
5. 실패한 이미지 이동은 상품을 `IMAGE_FAILED`로 표시하고 상품 이미지 row를 제거한 뒤 복구 API로 재시도한다.

### 상품 조회 흐름

1. 상품 목록 API는 이미지 이동이 끝난 `active=true` 상품만 최신순으로 조회한다.
2. 태그/작가 필터처럼 동적 조회 조건이 필요한 SQL 접근은 QueryDSL 커스텀 레포지토리로 처리한다.
3. 태그 필터는 태그명 완전 일치, 작가 필터는 판매자 닉네임 부분 일치로 처리한다.
4. 상품 엔티티 간 JPA 연관관계는 추가하지 않고 기존 `productId`, `tagId`, `sellerId` 식별자 기반 연결을 유지한다.

### 블로그 흐름

1. 게시글은 카테고리, 검색어, 페이징 조건으로 조회한다.
2. 상세 조회는 locale과 로그인 정보를 반영한다.
3. 댓글은 댓글/대댓글 구조를 지원한다.
4. 좋아요는 로그인 사용자 기준으로 등록/취소한다.

## 검증과 안전장치

- `spring.jpa.properties.hibernate.hbm2ddl.auto=validate`를 기본으로 사용한다.
- Harness 실행 시 `.codex/settings.json`의 validation 명령을 강제 실행한다.
- `SchemaValidationTest`는 JPA 매핑과 실제 PostgreSQL 스키마 불일치를 감지하기 위한 전용 테스트다.
- 엔티티 변경은 DB 스키마 변경으로 간주하며, 문서와 마이그레이션 계획을 함께 갱신해야 한다.
