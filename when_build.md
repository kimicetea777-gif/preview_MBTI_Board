# 배포 가이드 — 운영 서버(미니PC, Ubuntu)

`infra.md`의 GMKtec 미니PC(RAM 8GB)에 배포하는 절차. Docker 이미지 빌드나 systemd 유닛, CI/CD는 아직 없어서 아래 단계를 수동으로 밟아야 한다.

## 1. 서버 사전 준비

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git

# Java 21
sudo apt install -y openjdk-21-jdk

# Node.js (LTS, Next.js 16 요구사항 확인 후 버전 조정)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

## 2. 코드 배포 — git과 비밀정보는 분리 이동

- 저장소는 `git clone`으로 받으면 되지만, `.gitignore`에 `.env`가 제외되어 있어 **`.env`와 `frontend/mbti_board/.env.local`은 git에 올라가지 않는다.**
- 이 두 파일은 `scp` 등 보안 채널로 직접 옮길 것 (Slack/이메일 등 평문 채널 금지) — JWT 시크릿, Google OAuth secret, R2 키, 메일 앱 비밀번호가 그대로 들어있음.
- 신뢰 수준이 다른 새 환경이라면 `JWT_SECRET`, `INTERNAL_API_KEY`, `RETENTION_ENCRYPTION_*` 등은 이 기회에 재발급 권장.
  ```bash
  openssl rand -base64 42
  ```

## 3. 인프라 컨테이너 (Postgres/Redis)

```bash
docker compose up -d
```

`.env`의 `POSTGRES_*`, `REDIS_PORT` 값 그대로 사용 (`docker-compose.yml`).

## 4. 백엔드 빌드/실행

```bash
cd backend/mbti_board
./gradlew clean build -x test
# 산출물: build/libs/kimicetea-0.0.1-SNAPSHOT.jar

sudo mkdir -p /var/log/mbti-board
sudo chown $USER:$USER /var/log/mbti-board   # application-prod.yaml 로그 경로 권한

java -Xmx2g -jar build/libs/kimicetea-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

- `-Xmx2g`: `infra.md` 스펙 변경(RAM 8GB → 16GB) 반영. Postgres/Redis(Docker) + Node.js(프론트) 여유분을 감안한 권장값.

- `-Xmx512m`: RAM 8GB 제약으로 인한 권장값 (backend/mbti_board/CLAUDE.md).
- Flyway는 `flyway.enabled: true`라 기동 시 자동 마이그레이션됨.
- `prod` 프로필은 로그 경로만 변경 (`application-prod.yaml`). `APP_COOKIE_SECURE` 기본값이 이미 `true`라 별도 설정 불필요.

## 5. 프론트엔드 빌드/실행

```bash
cd frontend/mbti_board
npm ci
npm run build
npm run start   # 기본 3000 포트
```

`.env.local`의 `BACKEND_INTERNAL_URL` 확인 — 같은 서버에서 백엔드 jar를 직접 실행한다면 `http://localhost:8080` 그대로 유지.

## 6. 상시 구동 — systemd 유닛 필요

현재 터미널을 닫으면 프로세스가 죽는다. `/etc/systemd/system/mbti-backend.service`, `mbti-frontend.service`를 만들어 `systemctl enable --now`로 등록할 것. (아직 미작성 — 필요 시 별도 작성)

## 7. 외부 노출 — Cloudflare Tunnel

CLAUDE.md에 "Networking: Cloudflare Tunnel"로 명시되어 있으나 저장소에 `cloudflared` 설정 파일은 없음. 신규 구성 필요:

```bash
cloudflared tunnel login
cloudflared tunnel create mbti-board
# ~/.cloudflared/config.yml 에 ingress 규칙 추가: thebluebutter.com → localhost:3000
cloudflared tunnel route dns mbti-board thebluebutter.com
sudo cloudflared service install
```

포트를 직접 열 필요 없음 — Tunnel이 아웃바운드로 연결하므로 방화벽/공유기 포트포워딩 불필요.

## 참고 — 아직 구성되지 않은 것

- Prometheus/Grafana: CLAUDE.md에 목표 스택으로만 명시, 실제 설정 파일 없음.
- systemd 유닛, `cloudflared` config: 미작성.
- 백엔드/프론트엔드 Dockerfile: 없음 (DB/Redis용 `docker-compose.yml`만 존재).
