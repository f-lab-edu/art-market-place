# PRD

`art-market-place`는 예술 작품과 콘텐츠를 중심으로 회원, 상품, 그리고 기술 블로그 기능을 제공하는 웹 서비스다.

## 목표

- 구매자와 판매자가 Google OAuth 기반 흐름으로 가입/로그인할 수 있다.
- 판매자는 상품 정보를 등록하고 이미지를 S3 호환 스토리지에 업로드할 수 있다.
- 사용자는 블로그 게시글을 조회하고, 댓글과 좋아요로 상호작용할 수 있다.
- 프론트엔드는 게시글 목록/상세, 로그인/회원가입, 기본 사용자 메뉴를 제공한다.

## 사용자

- 비회원: 게시글과 일부 공개 콘텐츠를 조회한다.
- 구매자: 로그인 후 콘텐츠 상호작용과 구매자 기능을 이용한다.
- 판매자: 로그인 후 상품 등록과 판매자 기능을 이용한다.
- 운영/개발자: 문서와 Harness 계획을 기준으로 변경을 검증한다.

## 핵심 기능

### 인증/회원

- 구매자 회원가입 authorization 시작: `POST /api/auth/signup/buyers`
- 판매자 회원가입 authorization 시작: `POST /api/auth/signup/sellers`
- 로그인 authorization 시작: `POST /api/auth/login`
- Google callback 처리: `POST /api/auth/callback-google`
- 토큰 발급: `POST /api/auth/access-tokens`
- 토큰 재발급: `POST /api/auth/refresh-tokens`

### 상품

- 상품 이미지 Presigned URL 발급: `POST /api/products/images`
- 상품 등록: `POST /api/products`
- 상품 전체 조회: `GET /api/products`
- 상품 태그/작가 필터 조회: `GET /api/products/filter?tag={tagName}&artist={sellerNickname}`
- 상품 이미지 수동 복구: `POST /api/products/{productId}/images`

### 블로그

- 게시글 목록 조회: `GET /api/back/posts`
- 게시글 상세 조회: `GET /api/back/posts/{postId}`
- 게시글 저장/삭제: `POST`, `DELETE /api/back/posts`
- 카테고리 조회/생성/삭제: `/api/back/categories`
- 댓글 조회/생성/삭제: `/api/back/comments`
- 좋아요 등록/취소: `/api/back/likes`

## 비기능 요구사항

- DB 스키마는 Hibernate validate로 검증한다.
- 이미지 업로드는 서버 직접 업로드보다 Presigned URL 기반 직접 업로드를 우선한다.
- 토큰과 OAuth state는 Redis 기반 저장소를 사용한다.
- API 응답은 도메인별 `ApiResponse` 형식을 유지한다.
- 변경 사항은 Maven 테스트와 프론트 빌드로 검증한다.

## 현재 제약

- 로컬 검증은 PostgreSQL, Redis, MinIO 등 외부 서비스 상태에 의존할 수 있다.
- 프론트 빌드는 `next/font`가 Google Fonts를 조회하므로 네트워크 상태에 영향을 받는다.
- 일부 테스트는 컨텍스트 구성 미비나 외부 인프라 부재로 실패할 수 있으며, 이 경우 원인을 문서화하고 수정한다.
