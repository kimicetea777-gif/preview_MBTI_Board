@AGENTS.md

# MBTI Board - Claude Code Guide

## 프로젝트 개요

MBTI 기반 커뮤니티 플랫폼. 자유글·투표·토론 게시판을 제공하며 회원/비회원 모두 이용 가능.

## 기술 스택

### Frontend

- **Framework**: Next.js 14+ (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State & Data Fetching**: TanStack Query (React Query)
- **Internationalization**: next-intl
- **Components** : RichContentEditor (Tiptap)

### Backend

- **Core**: Java 21 + Spring Boot 3.3.5
- **Database Access**: Spring Data JPA + QueryDSL
- **Security**: Spring Security & JWT
- **Cache & Session**: Redis (Refresh Token (Multi-device) & Blacklist)
- **Documentation**: Swagger/OpenAPI 3.0
- **Build Tool**: Gradle

- **Entity & DTO Mapper lib**: MapStruct

### Database & Storage

- **Main Database**: PostgreSQL(Local)
- **In-Memory Storage**: Redis

### Infrastructure & DevOps

- **Virtualization**: Docker & Docker Compose
- **Operating System**: Ubuntu
- **Networking**: Cloudflare Tunnel
- **Monitoring**: Prometheus + Grafana

## 설계 문서

- PRD (제품 요구사항): `../../prd.md`
- DB 스키마 전체 명세: `../../database_schema.md`
- 기능별 설계: `./function_design/` (기능 개발 전 반드시 확인)
  - `security_steps.md` — JWT/Redis 인증 흐름, 키 설계

- (Backend) 클래스 **생성** 시 : 한 줄 요약해서 `backend/mbti_board/function_design/class_descript.md`에 작성

- (Frontend) 컴포넌트 **생성** 시 : 한 줄 요약해서 `frontend/mbti_board/function_design/componet_descript.md`에 작성

- (Frontend) URL 경로 하드코딩 금지 — 반드시 `@frontend/mbti_board/lib/routes.ts`의 `ROUTES` 상수를 참조
- (Frontend) API 엔드포인트 하드코딩 금지 — 반드시 `@frontend/mbti_board/lib/apiEndpoints.ts`의 `API` 상수를 참조

- (Frontend) 본문은 <article>, <section>, <p> 태그를 사용하고, 메뉴는 <nav>, 하단 정보는 <footer> 같은 의미 있는 태그 적극 사용

---
