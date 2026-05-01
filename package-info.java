/**
 * {@code art-market-place} 루트 패키지 문서.
 *
 * <h2>레포지토리 구조</h2>
 *
 * <ul>
 *   <li>{@code AGENTS.md}: Codex 작업 규칙과 문서 읽기 순서.</li>
 *   <li>{@code docs/}: 제품, 아키텍처, 의사결정, 프론트엔드, 디자인 문서.
 *     <ul>
 *       <li>{@code PRD.md}: 제품 목표와 범위.</li>
 *       <li>{@code ARCHITECTURE.md}: 시스템 경계, 모듈, 주요 데이터 흐름.</li>
 *       <li>{@code ADR.md}: 기술적 의사결정과 근거.</li>
 *       <li>{@code FRONTEND.md}: 프론트엔드 구조와 규칙.</li>
 *       <li>{@code DESIGN.md}: 디자인 가이드.</li>
 *     </ul>
 *   </li>
 *   <li>{@code .codex/}: Codex/Harness 설정과 로컬 스킬.
 *     <ul>
 *       <li>{@code settings.json}: 검증 명령과 위험 명령 패턴.</li>
 *       <li>{@code harness/}: Harness phase/step 실행 워크플로우.</li>
 *       <li>{@code review/}: 규칙 기반 변경사항 리뷰.</li>
 *     </ul>
 *   </li>
 *   <li>{@code scripts/execute.py}: 상태 관리를 포함한 Harness phase 순차 실행기.</li>
 *   <li>{@code scripts/test_execute.py}: {@code execute.py} 자동 검증 테스트.</li>
 *   <li>{@code front/}: Next.js 프론트엔드 애플리케이션.</li>
 *   <li>{@code src/}: Spring 백엔드 애플리케이션 코드.</li>
 * </ul>
 */
/*
 * ## 레포지토리 구조
 *
 * ```text
 * art-market-place
 * |-- AGENTS.md
 * |   `-- Codex 작업 규칙과 문서 읽기 순서
 * |-- docs/
 * |   |-- PRD.md          : 제품 목표와 범위
 * |   |-- ARCHITECTURE.md : 시스템 경계, 모듈, 주요 데이터 흐름
 * |   |-- ADR.md          : 기술적 의사결정과 근거
 * |   |-- FRONTEND.md     : 프론트엔드 구조와 규칙
 * |   `-- DESIGN.md       : 디자인 가이드
 * |-- .codex/
 * |   |-- settings.json   : Codex/Harness용 검증 명령과 위험 명령 패턴
 * |   `-- skills/
 * |       |-- harness/    : Harness phase/step 실행 워크플로우
 * |       `-- review/     : 규칙 기반 변경사항 리뷰
 * |-- scripts/
 * |   |-- execute.py      : Harness phase 순차 실행기
 * |   `-- test_execute.py : execute.py 자동 검증 테스트
 * |-- front/              : Next.js 프론트엔드 애플리케이션
 * `-- src/                : Spring 백엔드 애플리케이션 코드
 * ```
 */
package com.woobeee.artmarketplace;
