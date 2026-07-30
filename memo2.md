# MBTI Board 백업 스크립트 /scripts/backup.sh

## 사용법:

./scripts/backup.sh <백업 대상 디렉토리>

./scripts/backup.sh /run/media/user/sub/MBTI_Board/backup

## 실행 파일 권한 업데이트

chmod +x 안 돼 있어서 그래요 — Windows에서 scp로 옮기면 실행 권한 비트가 안 붙어서 나는 흔한 에러입니다.
chmod +x /home/user/apps/MBTI_Board/scripts/backup.sh
sudo ./scripts/backup.sh /run/media/user/sub/MBTI_Board/backup

## 복원 법

# 1. 백엔드 정지 (DB 커넥션 끊기)

docker compose stop backend

# 2. (선택, 더 안전) 혹시 남아있는 커넥션 강제 종료

docker exec -i mbti_postgres sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '"'"'$POSTGRES_DB'"'"' AND pid <> pg_backend_pid();"'

# 3. 복원

docker exec -i mbti_postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean' \
 < /run/media/user/sub/MBTI_Board/backup/mbti-board/20260724_222641/postgres.dump

# 4. 백엔드 재시작

docker compose start backend

---

# 실시간 로그

docker exec -it mbti_backend tail -f /var/log/mbti-board/app.log

---

# 파일 이동 및 연결

- 서버 연결: ssh mbti-server
- 서버 연결 + DB포트 개방 : ssh -L 5432:localhost:5432 mbti-server
- netdata 연결 + 포트 개방 : ssh -L 19999:localhost:19999 mbti-server
  -> 터널링 후 브라우저에서 http://localhost:19999 접속

파일 이동: scp mbti-backend.tar mbti-frontend.tar mbti-server:~/apps/MBTI_Board/
폴더 이동(무거우면 압축 후): scp -r mbti-backend/ mbti-frontend/ mbti-server:~/apps/MBTI_Board/

---

# 배포 가이드 — 운영 서버(미니PC, Ubuntu)

```bash
# 개발 PC
docker compose build backend frontend
docker save -o mbti-backend.tar mbti-backend:latest
docker save -o mbti-frontend.tar mbti-frontend:latest
# → mbti-backend.tar, mbti-frontend.tar를 USB로 서버에 복사 (docker-compose.yml, .env, .env_next는 변경 없으면 그대로 둠)

# 서버
docker load -i mbti-backend.tar
docker load -i mbti-frontend.tar
docker compose up -d backend frontend   # 변경된 이미지로 컨테이너만 교체, postgres/redis는 그대로 유지

# 로그 확인
docker compose logs -f backend
docker compose logs -f frontend
```

`docker-compose.yml`의 `backend`/`frontend` 서비스에 `image: mbti-backend:latest` / `image: mbti-frontend:latest`가 지정돼 있어서, `docker compose build`로 빌드하면 이 이름으로 태깅된다.

#

## 2. 배포 패키지 구성 — USB/보안 채널로 4종류 파일만 전달

`docker-compose.yml`
`mbti-backend.tar`, `mbti-frontend.tar`
`.env` : `backend`가 참조
`.env_next` : `frontend`가 참조
