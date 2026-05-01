# FRONTEND

프론트엔드는 `front/` 하위 Next.js 14 애플리케이션이다.

## 기술 스택

- Next.js 14 App Router
- React 18
- TypeScript
- Tailwind CSS
- Radix UI 기반 컴포넌트
- lucide-react 아이콘

## 디렉터리 구조

```text
front/
|-- app/                 라우트와 전역 레이아웃
|-- components/          화면 컴포넌트
|-- components/ui/       공통 UI primitive
|-- lib/                 API, 타입, 에러 처리, 유틸
|-- public/              정적 리소스
`-- package.json         프론트 빌드/실행 스크립트
```

## 주요 라우트

- `/`: 메인 페이지
- `/blog`: 기술블로그 블로그 목록
- `/blog/[postId]`: 블로그 상세
- `/login`: 로그인
- `/logout`: 로그아웃
- `/signup`: 회원가입

## API 호출 규칙

- API 호출 공통 로직은 `front/lib/api.ts`에 둔다.
- 응답 타입은 `front/lib/types.ts`에 정의한다.
- access token은 현재 `localStorage`에 저장한다.
- 요청에는 언어 정보(`ko-KR`, `en-US`)를 포함한다.
- 인증 만료 시 refresh를 시도하고 실패하면 토큰을 제거한다.

## 컴포넌트 규칙

- 페이지는 `app/`에 두고, 재사용 가능한 화면 단위는 `components/`에 둔다.
- 버튼, 입력, 다이얼로그, 드롭다운 등은 `components/ui/`의 기존 컴포넌트를 우선 사용한다.
- 새 아이콘은 가능하면 `lucide-react`를 사용한다.
- UI 텍스트는 사용자가 수행할 행동을 기준으로 짧게 작성한다.

## 스타일 규칙

- Tailwind utility를 우선 사용한다.
- 공통 스타일이 반복되면 작은 컴포넌트로 분리한다.
- 과도한 카드 중첩과 장식성 레이아웃을 피한다.
- 블로그/상품 화면은 콘텐츠 탐색과 가독성을 우선한다.

## 빌드와 검증

```bash
cd front
npm run build
```

현재 `npm run lint`는 ESLint 초기 설정 프롬프트가 뜰 수 있으므로 자동 검증에는 포함하지 않는다. ESLint 설정을 도입한 뒤 자동 검증에 추가한다.

## 알려진 주의사항

- `next/font`가 Google Fonts를 조회하므로 네트워크가 막힌 환경에서는 `npm run build`가 실패할 수 있다.
- `API_BASE_URL`은 현재 코드에서 `https://woobeee.com`으로 고정되어 있다. 환경별 전환이 필요하면 `NEXT_PUBLIC_API_BASE_URL` 기반으로 되돌린다.
