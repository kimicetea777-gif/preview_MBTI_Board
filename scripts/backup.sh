#!/usr/bin/env bash
# ===================================================
# MBTI Board 백업 스크립트
#
# 사용법:
#   ./scripts/backup.sh <백업 대상 디렉토리>
#   예) ./scripts/backup.sh /mnt/usb-backup
#
# 백업 대상:
#   1. postgres_data — pg_dump 논리적 덤프 (컨테이너가 떠있는 상태로도 안전)
#   2. env 파일들     — git에 없는 유일한 원본 (JWT_SECRET, R2 키, DB 비밀번호 등)
#   3. backend_logs   — 로그 볼륨
# redis_data는 세션/rate-limit 캐시성 데이터라 백업 대상에서 제외.
# 이미지는 Cloudflare R2에 있어 서버 백업 대상이 아님.
#
# 주의: env 파일은 시크릿을 그대로 담고 있으므로, 이 스크립트가 만든
#       백업을 옮기는 이동식 디스크는 반드시 전체 암호화(BitLocker/LUKS 등)해서 사용할 것.
#
# 하는 일:
#
# mbti_postgres 컨테이너에 docker exec로 들어가서 pg_dump로 논리적 덤프 (컨테이너 자체 환경변수 POSTGRES_USER/POSTGRES_DB를 그대로 사용하므로 별도 설정 불필요)
# .env, .env_next, .env_me, backend/mbti_board/.env, 프론트엔드 쪽 .env.* 파일들을 존재하는 것만 모아서 env.tar.gz로 압축
# backend_logs 볼륨을 logs.tar.gz로 압축
# <백업경로>/mbti-board/2026xxxx_xxxxxx/ 형태로 타임스탬프 폴더에 저장
# 최근 14개 백업만 남기고 오래된 건 자동 삭제 (디스크 용량 관리)
#
# 복원 예시 (Postgres) — 주의: --clean은 복원 전에 기존 데이터를 지우므로 되돌릴 수 없음
#   docker exec -i mbti_postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean' < postgres.dump
#
# ls -1 /mnt/usb-backup/mbti-board/
# # 20260710_030000
# # 20260711_030000
# # 20260712_030000   ← 예: 이 날짜 복원하고 싶다면
# 원하는 시점의 폴더를 골라서 그 안의 postgres.dump를 restore 명령에 지정하면 됩니다:
#
# docker exec -i mbti_postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean' \
#   < /mnt/usb-backup/mbti-board/20260712_030000/postgres.dump
# ===================================================

set -euo pipefail

BACKUP_ROOT="${1:?사용법: $0 <백업 대상 디렉토리 (예: 이동식 디스크 마운트 경로)>}"
RETENTION_COUNT=14   # 최근 N개 백업만 보관, 나머지는 자동 삭제

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/mbti-board/${TIMESTAMP}"

mkdir -p "$BACKUP_DIR"
echo "[backup] 대상 디렉토리: $BACKUP_DIR"

# 1. Postgres — 컨테이너 안의 POSTGRES_USER/POSTGRES_DB 환경변수를 그대로 사용
echo "[backup] Postgres 덤프 중..."
docker exec mbti_postgres sh -c 'pg_dump -U "$POSTGRES_USER" -F c -d "$POSTGRES_DB"' > "${BACKUP_DIR}/postgres.dump"

# 2. 시크릿 env 파일 — 존재하는 것만 복사
echo "[backup] env 파일 백업 중..."
ENV_STAGING="${BACKUP_DIR}/env"
mkdir -p "$ENV_STAGING"
for f in \
    "$REPO_ROOT/.env" \
    "$REPO_ROOT/.env_next" \
    "$REPO_ROOT/.env_me" \
    "$REPO_ROOT/backend/mbti_board/.env" \
    "$REPO_ROOT/frontend/mbti_board/.env.local" \
    "$REPO_ROOT/frontend/mbti_board/.env.production" \
    "$REPO_ROOT/frontend/mbti_board/.env.development" \
    "$REPO_ROOT/frontend/mbti_board/.env.development.local"
do
    [ -f "$f" ] && cp "$f" "$ENV_STAGING/$(basename "$f")"
done
tar czf "${BACKUP_DIR}/env.tar.gz" -C "$ENV_STAGING" .
rm -rf "$ENV_STAGING"

# 3. 백엔드 로그 볼륨
echo "[backup] 로그 백업 중..."
docker run --rm \
    -v backend_logs:/volume:ro \
    -v "${BACKUP_DIR}":/backup \
    alpine tar czf /backup/logs.tar.gz -C /volume .

echo "[backup] 완료: $BACKUP_DIR"

# 4. 오래된 백업 정리 (최근 RETENTION_COUNT개만 유지)
echo "[backup] 오래된 백업 정리 (최근 ${RETENTION_COUNT}개 유지)..."
ls -1dt "${BACKUP_ROOT}/mbti-board"/*/ 2>/dev/null | tail -n +"$((RETENTION_COUNT + 1))" | xargs -r rm -rf

echo "[backup] 모든 작업 완료."
