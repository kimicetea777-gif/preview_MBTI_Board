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

### Infrastructure & DevOps

- **Virtualization**: Docker & Docker Compose
- **Operating System**: Ubuntu
- **Networking**: Cloudflare Tunnel
- **Monitoring**: Prometheus + Grafana

## 설계 문서

- PRD (제품 요구사항): `../../prd.md`
- DB 스키마 전체 명세: `../../database_schema.md`
- 기능별 설계: `./function_design/` (기능 개발 전 반드시 확인)
  - `fn_security.md` — JWT/Redis 인증 흐름, 키 설계

---
