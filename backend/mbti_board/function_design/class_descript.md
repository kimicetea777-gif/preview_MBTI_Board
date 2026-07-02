| 클래스 | 경로 | 설명 |
|---|---|---|
| `AlertType` | `global/alert` | 이메일 알림 성격 분류 enum — JWT_TAMPER·UNHANDLED_ERROR·DB_QUERY_SLOW, 타입별 독립 flag 키로 사용 |
| `SlowQueryAspect` | `global/aop` | Repository 메서드 실행 시간 감시 AOP — 임계치(기본 1000ms) 초과 시 DB_QUERY_SLOW 알림 발송 |
| `AlertFlagRegistry` | `global/alert` | AlertType별 이메일 알림 활성화 플래그 레지스트리 — 발송 후 auto-mute, 서버 재시작 시 초기화 |
| `AlertMarkers` | `global/alert` | SLF4J Marker 상수 보관 — log.error(AlertMarkers.JWT_TAMPER, ...) 형태로 알림 타입 지정 |
| `ValidationConstants` | `global/common` | 비밀번호·전화번호 정책 등 공통 검증 상수 모음 (PASSWORD_REGEXP, PHONE_REGEXP 등) |
| `PhoneHashService` | `global/crypto` | 전화번호 HMAC-SHA256 해싱 서비스 — DB 조회 키용 결정적 해시 생성 (시크릿 키로 역추적 방지, 최소 32자 검증) |
| `RateLimitService` | `global/ratelimit` | Redis INCR 기반 Rate Limiting — IP별(check)·이메일별(checkByEmail) 두 방식 지원, 엔드포인트·최대 횟수·시간 창 지정 가능 (Cloudflare X-Forwarded-For 지원) |
| `RateLimit` | `global/ratelimit` | IP 기반 Rate Limit 선언 어노테이션 — endpoint·max·windowSecs 지정, RateLimitAspect가 메서드 진입 전 자동 처리 |
| `RateLimitAspect` | `global/ratelimit` | @RateLimit 어노테이션 AOP 처리기 — RequestContextHolder로 HttpServletRequest 획득 후 RateLimitService.check() 호출 |
| `RequestUtils` | `global/common/util` | HTTP 요청 유틸 — Cloudflare Tunnel 환경 클라이언트 IP 추출(extractClientIp), salt 기반 SHA-256 IP 해싱(hashIp) |
| `EmailUtils` | `global/common/util` | 이메일 관련 유틸 — @앞 2자만 노출하고 나머지 로컬 파트를 *** 처리(mask) |
| `OAuth2AuthenticationFailureHandler` | `global/security/oauth2` | OAuth2 인증 실패 시 에러 코드를 쿼리 파라미터로 담아 프론트 콜백 페이지로 리다이렉트 |
| `EmailVerifySendRequest` | `domain/auth/dto` | 이메일 인증 발송 요청 DTO (이메일 주소) |
| `EmailVerifyConfirmRequest` | `domain/auth/dto` | 이메일 인증 토큰 확인 요청 DTO (UUID 토큰) |
| `DebateSummaryResponse` | `domain/post/dto` | 토론 게시글 요약 응답 DTO (찬반 레이블, 카운트, 마감 여부) |
| `MyProfileResponse` | `domain/user/dto` | 내 프로필 조회 응답 DTO (이메일·MBTI·역할·소셜여부 포함) |
| `UserProfileResponse` | `domain/user/dto` | 타인 공개 프로필 응답 DTO (민감 정보 제외) |
| `UpdateProfileRequest` | `domain/user/dto` | 프로필 수정 요청 DTO (닉네임·이미지·MBTI 선택적 수정) |
| `ChangePasswordRequest` | `domain/user/dto` | 비밀번호 변경 요청 DTO (현재·새 비밀번호) |
| `UserService` | `domain/user/service` | 유저 프로필 비즈니스 로직 (조회·수정·탈퇴 OTP 발송·탈퇴·비밀번호 변경·전화번호 등록/삭제) |
| `UpdatePhoneRequest` | `domain/user/dto` | 전화번호 등록/변경 요청 DTO (HMAC 해싱 전 원본 전화번호, PHONE_REGEXP 검증) |
| `UserController` | `domain/user/controller` | 유저 프로필 API 컨트롤러 (GET·PATCH /me, GET /{id}, POST·DELETE /me/withdrawal-verify·/me, PATCH /me/password, PATCH·DELETE /me/phone) |
| `WithdrawalRequest` | `domain/user/dto` | 회원 탈퇴 요청 DTO (6자리 OTP 코드) |
| `EmailService` | `global/mail` | 이메일 발송 서비스 (탈퇴 OTP 발송) |
| `MbtiResponse` | `domain/mbti/dto` | MBTI 메타데이터 응답 DTO (id·title·colorCode·descript·personalities·bestMbti·badMbti 노출, 내부 타임스탬프 제외) |
| `MbtiService` | `domain/mbti/service` | MBTI 목록 및 검사 문항 비즈니스 로직 (읽기 전용, 문항 카테고리별 랜덤 추출 후 셔플) |
| `MbtiController` | `domain/mbti/controller` | MBTI 메타데이터 및 검사 API 컨트롤러 (GET /api/mbti, GET /api/mbti/{id}, GET /api/mbti/test/questions, 인증 불필요) |
| `MbtiQuestion` | `domain/mbti/entity` | MBTI 검사 문항 엔티티 (mbti_questions 테이블 매핑, 쓰기 없음) |
| `MbtiQuestionRepository` | `domain/mbti/repository` | MBTI 검사 문항 레포지토리 (UNION ALL 네이티브 쿼리로 카테고리당 8개 랜덤 추출) |
| `MbtiQuestionResponse` | `domain/mbti/dto` | MBTI 검사 문항 응답 DTO (id·category·title·isReverse) |
| `MbtiTestHistoryRepository` | `domain/mbti/repository` | MBTI 검사 이력 레포지토리 (userId 기준 최신순 조회, @EntityGraph로 N+1 방지) |
| `MbtiTestSubmitRequest` | `domain/mbti/dto` | MBTI 검사 결과 제출 요청 DTO (4개 지표 점수, -16~16 Bean Validation) |
| `MbtiTestResultResponse` | `domain/mbti/dto` | MBTI 검사 결과 응답 DTO (서버 판정 mbtiId + 4개 점수) |
| `MbtiTestHistoryResponse` | `domain/mbti/dto` | MBTI 검사 이력 응답 DTO (id·mbtiId·4개 점수·testVersion·createdAt) |
| `JpaConfig` | `global/config` | QueryDSL JPAQueryFactory 빈 등록 |
| `AsyncConfig` | `global/config` | @EnableAsync 활성화 + 비동기 예외 로깅 핸들러 — 가상 스레드 기반 메일 발송 비동기 처리 |
| `UserCleanupService` | `domain/user/service` | 탈퇴 유저(WITHDRAWN) 배치 삭제 스케줄러 — 매일 04:00 실행, findAllByState → deleteAll |
| `AdminUserStatsResponse` | `domain/user/dto` | 유저 통계 응답 DTO — totalUsers·inactiveCount·소셜/로컬 수·상태/역할/MBTI 분포·30일 가입 추이 |
| `AdminUserStatsService` | `domain/user/service` | 어드민 유저 통계 서비스 — computeAndCache()로 DB 집계 후 Redis 저장, getCachedStats()로 즉시 반환 |
| `AdminUserStatsScheduler` | `domain/user/service` | 어드민 유저 통계 사전 계산 스케줄러 — 10분마다 computeAndCache() 호출, triggerAsync()로 수동 갱신 지원 |
| `AdminPostStatsResponse` | `domain/post/dto` | 게시글 통계 응답 DTO — totalPosts·hiddenCount·타입 분포·MBTI별 작성 분포·30일 작성 추이 |
| `AdminPostStatsService` | `domain/post/service` | 어드민 게시글 통계 서비스 — computeAndCache()로 DB 집계 후 Redis 저장, getCachedStats()로 즉시 반환 |
| `AdminPostStatsScheduler` | `domain/post/service` | 어드민 게시글 통계 사전 계산 스케줄러 — 10분마다 computeAndCache() 호출, triggerAsync()로 수동 갱신 지원 |
| `AdminCommentStatsResponse` | `domain/comment/dto` | 댓글 통계 응답 DTO — totalComments·hiddenCount·좋아요/싫어요 합계·MBTI 분포·30일 작성 추이 |
| `AdminCommentStatsService` | `domain/comment/service` | 어드민 댓글 통계 서비스 — computeAndCache()로 DB 집계 후 Redis 저장, getCachedStats()로 즉시 반환 |
| `AdminCommentStatsScheduler` | `domain/comment/service` | 어드민 댓글 통계 사전 계산 스케줄러 — 10분마다 computeAndCache() 호출, triggerAsync()로 수동 갱신 지원 |
| `AdminMbtiTestStatsResponse` | `domain/mbti/dto` | MBTI 검사 통계 응답 DTO — totalTests·지표별 평균·결과 분포·재검사 빈도·30일 검사 추이 |
| `AdminMbtiTestStatsService` | `domain/mbti/service` | 어드민 MBTI 검사 통계 서비스 — computeAndCache()로 DB 집계 후 Redis 저장, getCachedStats()로 즉시 반환 |
| `AdminMbtiTestStatsScheduler` | `domain/mbti/service` | 어드민 MBTI 검사 통계 사전 계산 스케줄러 — 10분마다 computeAndCache() 호출, triggerAsync()로 수동 갱신 지원 |
| `AdminMbtiController` | `domain/mbti/controller` | 어드민 MBTI 검사 통계 API 컨트롤러 (GET /api/admin/mbti/stats — ADMIN 전용) |
| `PostRepositoryCustom` | `domain/post/repository` | 게시글 목록 QueryDSL 커스텀 조회 인터페이스 (findPosts·findAllPostsForAdmin·findMyPosts) |
| `PostRepositoryImpl` | `domain/post/repository` | 게시글 목록 QueryDSL 구현체 (유형·정렬·MBTI 필터 조합, 내 게시글 userId 필터 포함) |
| `PostRepository` | `domain/post/repository` | 게시글 기본 CRUD + viewCount 원자적 증가 (@Modifying) |
| `PostCreateRequest` | `domain/post/dto` | 자유글(FREE) 게시글 작성 요청 DTO (title·content·type·guestPassword·targetMbti — VOTE·DEBATE는 전용 엔드포인트 사용) |
| `DebatePostCreateRequest` | `domain/post/dto` | 토론(DEBATE) 게시글 작성 요청 DTO (POST /api/posts/debate 전용 — 회원 전용, title·content·targetMbti·endsAt·proLabel·conLabel) |
| `PostUpdateRequest` | `domain/post/dto` | 게시글 수정 요청 DTO (제목·content·비회원 비밀번호) |
| `PostDeleteRequest` | `domain/post/dto` | 게시글 삭제 요청 DTO (비회원 비밀번호) |
| `PostSummaryResponse` | `domain/post/dto` | 게시글 목록 응답 DTO (content 제외, 작성자 MBTI 배지 포함) |
| `PostDetailResponse` | `domain/post/dto` | 게시글 상세 응답 DTO (content 포함, isOwner·isGuest 포함) |
| `PostService` | `domain/post/service` | 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제, 회원/비회원 분기) |
| `PostController` | `domain/post/controller` | 게시글 API 컨트롤러 (GET·POST /api/posts, POST /api/posts/debate, POST /api/posts/vote, GET·PATCH·DELETE /api/posts/{id}, POST·DELETE /api/posts/debate/{id}/stance) |
| `DebateStanceHistoryId` | `domain/post/entity` | 토론 진영 투표 내역 복합 PK Embeddable — (user_id UUID, post_id BIGINT) |
| `DebateStanceHistory` | `domain/post/entity` | 토론 진영 투표 내역 엔티티 — 회원 1인당 1표, stance(1/-1) 변경 가능, DB 트리거로 pro/con_count 동기화 |
| `DebateStanceHistoryRepository` | `domain/post/repository` | 토론 진영 투표 레포지토리 — stance 단건 조회, ON CONFLICT upsert 네이티브 쿼리 포함 |
| `DebateStanceRequest` | `domain/post/dto` | 토론 진영 투표 요청 DTO (stance: Short, 1=찬성/-1=반대) |
| `PostSortType` | `domain/post` | 게시글 정렬 기준 Enum (LATEST·VIEWS·COMMENTS) |
| `CommentRepository` | `domain/comment/repository` | 댓글 기본 CRUD + 게시글별 visible 댓글 flat 조회 (findAllVisibleByPostId) |
| `CommentResponse` | `domain/comment/dto` | 댓글 응답 DTO — 루트 댓글은 replies 포함, 대댓글은 replies 빈 리스트 |
| `CommentService` | `domain/comment/service` | 댓글 도메인 비즈니스 로직 (목록 조회, 계층형 구조 조립) |
| `CommentController` | `domain/comment/controller` | 댓글 API 컨트롤러 (GET·POST /api/posts/{postId}/comments, PATCH·DELETE /{id}) |
| `CommentCreateRequest` | `domain/comment/dto` | 댓글 작성 요청 DTO (content·parentId·guestPassword·debateStance) |
| `CommentUpdateRequest` | `domain/comment/dto` | 댓글 수정 요청 DTO (content·guestPassword) |
| `GuestPasswordHelper` | `global/common/util` | 비회원 BCrypt 인코딩 정적 유틸 — PostService·CommentService 공유 |
| `MbtiMetadataSupport` | `domain/mbti` | NONE MBTI sentinel을 @PostConstruct로 1회 로드해 캐싱 — PostService·CommentService·CustomOAuth2UserService 공유 |
| `CommentReactionRepository` | `domain/comment/repository` | 댓글 리액션 CRUD — findByComment_IdAndUser_Id로 유저별 리액션 단건 조회 |
| `CommentReactionRequest` | `domain/comment/dto` | 댓글 리액션 요청 DTO (reactionType: 1=LIKE, -1=DISLIKE) |
| `CommentReactionResponse` | `domain/comment/dto` | 댓글 리액션 응답 DTO (commentId·myReactionType·likeCount·dislikeCount) |
| `CommentReactionService` | `domain/comment/service` | 댓글 리액션 토글·취소 비즈니스 로직 — EntityManager flush+refresh로 DB 트리거 반영 |
| `CommentReactionController` | `domain/comment/controller` | 댓글 리액션 API 컨트롤러 (POST·DELETE /api/comments/{id}/reactions, 회원 전용) |
| `MbtiQuestionResponse` | `domain/mbti/dto` | MBTI 검사 문항 응답 DTO (id·question·dimension·optionA·optionB — dimension 별 앞글자=A, 뒷글자=B 규칙) |
| `MbtiTestService` | `domain/mbti/service` | MBTI 검사 문항 제공 서비스 — 정적 20문항(EI/SN/TF/JP 각 5개)을 캐시하여 반환 |
| `MbtiTestController` | `domain/mbti/controller` | MBTI 검사 API 컨트롤러 (GET /api/mbti/test/questions — 인증 불필요) |
| `DebatePostRepository` | `domain/post/repository` | 토론 게시글 메타 레포지토리 — proCount/conCount는 DB 트리거 관리 |
| `VotePostRepository` | `domain/post/repository` | 투표 게시글 메타 레포지토리 — findWithOptionsById로 options JOIN FETCH 조회 |
| `VotePostOptionRepository` | `domain/post/repository` | 투표 선택지 레포지토리 — MBTI 중복 사전 검증용 existsByVotePost_IdAndMbtiTarget_Id |
| `VotePostHistoryRepository` | `domain/post/repository` | 유저 투표 내역 레포지토리 — findByVotePost_IdAndUser_Id로 1인 1표 UPSERT 지원 |
| `VotePostCreateRequest` | `domain/post/dto` | 투표 게시글 작성 요청 DTO (POST /api/posts/vote 전용, 회원 전용 — title·content·targetMbti·voteOptions·endsAt) |
| `VoteOptionRequest` | `domain/post/dto` | 투표 선택지 작성 요청 DTO (optionText 필수, mbtiTarget은 MBTI 투표 시에만) |
| `VoteOptionResponse` | `domain/post/dto` | 투표 선택지 응답 DTO (id·optionText·mbtiTargetId·voteCount) |
| `VoteResultResponse` | `domain/post/dto` | 투표 결과 응답 DTO — 선택지별 득표수 + 본인 선택 옵션 ID + 마감 여부 |
| `VoteRequest` | `domain/post/dto` | 투표 참여 요청 DTO (optionId 필수) |
| `VoteService` | `domain/post/service` | 투표 참여(castVote) 및 결과 조회(getVoteResult) 비즈니스 로직 — 1인 1표 UPSERT, 마감 검증 |
| `VoteController` | `domain/post/controller` | 투표 API 컨트롤러 (POST /api/posts/{postId}/vote, GET /api/posts/{postId}/vote/result) |
| `Notice` | `domain/notice/entity` | 공지사항 엔티티 — is_active·priority·starts_at/ends_at 기간 노출 제어 |
| `NoticeRepository` | `domain/notice/repository` | 공지사항 레포지토리 — findActive(now)로 현재 노출 가능한 공지 priority DESC 조회 |
| `NoticeResponse` | `domain/notice/dto` | 공개 공지사항 응답 DTO (id·title·content·createdAt, 관리 필드 미노출) |
| `AdminNoticeResponse` | `domain/notice/dto` | 어드민 공지사항 응답 DTO (isActive·priority·starts_at/ends_at 포함, ADMIN 전용) |
| `NoticeCreateRequest` | `domain/notice/dto` | 공지사항 생성 요청 DTO (title·content·isActive·priority·startsAt·endsAt) |
| `NoticeUpdateRequest` | `domain/notice/dto` | 공지사항 수정 요청 DTO (NoticeCreateRequest와 동일 필드, 전체 교체) |
| `NoticeService` | `domain/notice/service` | 공개 공지사항 서비스 — getActiveNotices()로 현재 노출 가능한 목록 반환 (읽기 전용) |
| `AdminNoticeService` | `domain/notice/service` | 어드민 공지사항 CRUD 서비스 — 전체 목록 조회·생성·수정·삭제 |
| `NoticeController` | `domain/notice/controller` | 공개 공지사항 API 컨트롤러 (GET /api/notices/active — 인증 불필요) |
| `AdminNoticeController` | `domain/notice/controller` | 어드민 공지사항 관리 API 컨트롤러 (GET·POST /api/admin/notices, PATCH·DELETE /api/admin/notices/{id}) |
| `AdminUserSummaryResponse` | `domain/user/dto` | 어드민 유저 목록 응답 DTO (이메일·역할·상태·MBTI 등 민감 정보 포함, ADMIN 전용) |
| `AdminUserStateRequest` | `domain/user/dto` | 어드민 유저 상태 변경 요청 DTO (state 필수) |
| `AdminUserRoleRequest` | `domain/user/dto` | 어드민 유저 역할 변경 요청 DTO (role 필수) |
| `UserSortField` | `domain/user/enums` | 어드민 유저 목록 정렬 기준 Enum (CREATED_AT·UPDATED_AT·LAST_LOGIN_AT) |
| `UserRepositoryCustom` | `domain/user/repository` | 유저 목록 QueryDSL 커스텀 조회 인터페이스 (이메일·상태·역할 필터 + sortBy·direction 정렬, 어드민 전용) |
| `UserRepositoryImpl` | `domain/user/repository` | 유저 목록 QueryDSL 구현체 (BooleanBuilder 필터 조합, 동적 정렬 OrderSpecifier, mbti fetchJoin으로 N+1 방지) |
| `AdminUserProfileRequest` | `domain/user/dto` | 어드민 유저 프로필 수정 요청 DTO (nickname nullable, clearProfileImage boolean) |
| `AdminUserService` | `domain/user/service` | 어드민 유저 관리 비즈니스 로직 (목록 조회·상태 변경·역할 변경·프로필 수정, 자기 자신 변경 금지) |
| `AdminCreateUserRequest` | `domain/user/dto` | 어드민 유저 직접 생성 요청 DTO (email·password·nickname·mbtiId·role·state — 이메일 인증 없이 생성) |
| `AdminUserController` | `domain/user/controller` | 어드민 유저 관리 API 컨트롤러 (POST /api/admin/users, GET /api/admin/users, PATCH /{id}/state, PATCH /{id}/role, PATCH /{id}/profile) |
| `AdminPostSummaryResponse` | `domain/post/dto` | 어드민 게시글 목록 응답 DTO (isVisible·typeCode 포함, 숨김 게시글 포함 ADMIN 전용) |
| `AdminPostVisibilityRequest` | `domain/post/dto` | 어드민 게시글 가시성 변경 요청 DTO (visible 필수, ADMIN 전용) |
| `AdminPostService` | `domain/post/service` | 어드민 게시글 관리 비즈니스 로직 (전체 목록 조회·숨김·복구, isVisible 필터 없음) |
| `AdminPostController` | `domain/post/controller` | 어드민 게시글 관리 API 컨트롤러 (GET /api/admin/posts, PATCH /{id}/visibility) |
| `AdminCommentSummaryResponse` | `domain/comment/dto` | 어드민 댓글 목록 응답 DTO (isVisible·postId·parentId 포함, 숨김 댓글 포함 ADMIN 전용) |
| `AdminCommentVisibilityRequest` | `domain/comment/dto` | 어드민 댓글 가시성 변경 요청 DTO (visible 필수, ADMIN 전용) |
| `AdminCommentService` | `domain/comment/service` | 어드민 댓글 관리 비즈니스 로직 (전체 목록 조회·숨김·복구, isVisible 필터 없음) |
| `AdminCommentController` | `domain/comment/controller` | 어드민 댓글 관리 API 컨트롤러 (GET /api/admin/comments, PATCH /{id}/visibility) |
| `CommentRepositoryCustom` | `domain/comment/repository` | 댓글 목록 QueryDSL 커스텀 조회 인터페이스 (어드민 전용 postId·visibility 필터) |
| `CommentRepositoryImpl` | `domain/comment/repository` | 댓글 목록 QueryDSL 구현체 (postId·isVisible BooleanBuilder 필터, authorMbti fetchJoin으로 N+1 방지) |
| `DeletedImageStatus` | `global/image/entity` | 이미지 삭제 추적 상태 Enum — PENDING(업로드 후 미저장), DELETE_REQUESTED(즉시 삭제 실패, 재시도 대기) |
| `DeletedImage` | `global/image/entity` | 삭제 예약된 R2 이미지 엔티티 — url·status·marked_at 저장, pending()/markedForCleanup() 팩토리 + markDeleteRequested() 제공 |
| `DeletedImageRepository` | `global/image/repository` | 삭제 예약 이미지 레포지토리 — status·marked_at 기준 배치 조회, URL 목록 조회·삭제 |
| `ImageCleanupService` | `global/image/service` | R2 이미지 삭제 예약·배치 정리 서비스 — registerPending/commitImages/cancelImages 제공, 매일 오전 3시 PENDING 24h 초과·DELETE_REQUESTED 재시도 처리 |
| `ImagePendingRequest` | `global/image/dto` | PENDING 이미지 등록 요청 DTO (POST /api/images/pending, Next.js 서버→백엔드 내부 전용) |
| `ImageCancelRequest` | `global/image/dto` | 이미지 취소 요청 DTO (DELETE /api/images, 작성 취소 시 업로드된 URL 목록 전달) |
| `R2Config` | `global/image/config` | Cloudflare R2 S3Client 빈 등록 — S3 호환 API, pathStyle 필수 |
| `ImageService` | `global/image/service` | R2 이미지 업로드 서비스 — 타입(jpg·png·gif·webp)·크기(5MB) 검증 후 UUID 파일명으로 저장 |
| `ImageController` | `global/image/controller` | 이미지 API 컨트롤러 (POST /upload 업로드, POST /pending PENDING 등록(내부 키 필수), DELETE / 취소) |
| `ImageUploadResponse` | `global/image/dto` | 이미지 업로드 응답 DTO — Tiptap Image src로 사용 가능한 공개 URL |
| `UserActivityStatsResponse` | `domain/user/dto` | 내 활동 통계 응답 DTO (작성 게시글 수·댓글 수·최근 검사 MBTI) |
| `FindEmailRequest` | `domain/auth/dto` | 이메일 찾기 요청 DTO (닉네임) |
| `FindEmailResponse` | `domain/auth/dto` | 이메일 찾기 응답 DTO (마스킹 이메일·소셜 전용 여부) |
| `PasswordResetRequestBody` | `domain/auth/dto` | 비밀번호 재설정 이메일 발송 요청 DTO (이메일) |
| `PasswordResetConfirmRequest` | `domain/auth/dto` | 비밀번호 재설정 확인 요청 DTO (토큰·새 비밀번호, @Size(min=8)) |
| `TiptapContentParser` | `global/common/util` | Tiptap JSONB content에서 텍스트 미리보기(extractTextPreview)·이미지 key(extractImageKeys) 추출 유틸리티 |
| `MyPostPreviewResponse` | `domain/post/dto` | 마이페이지 내 게시글 프리뷰 응답 DTO (id·typeCode·title·commentCount·createdAt, 최근 5개 전용) |
| `FunQuestion` | `domain/mbti/entity` | Fun 질문 엔티티 (fun_questions 테이블 매핑, options LazyLoading) |
| `FunQuestionOption` | `domain/mbti/entity` | Fun 질문 선택지 엔티티 (fun_question_options 테이블 매핑, ManyToOne FunQuestion) |
| `FunResponseCountId` | `domain/mbti/entity` | fun_response_counts 복합 PK Embeddable (option_id + mbti_id) |
| `FunResponseCount` | `domain/mbti/entity` | Fun 응답 집계 엔티티 (option×mbti 조합별 count, ON CONFLICT UPSERT로 원자적 증가) |
| `QuestionResponse` | `domain/mbti/dto` | MBTI/FUN 문항 응답 Sealed Interface — Jackson @JsonTypeInfo(type 필드)로 다형 직렬화 |
| `FunOptionResponse` | `domain/mbti/dto` | Fun 선택지 응답 DTO (id·optionText·displayOrder) |
| `FunQuestionResponse` | `domain/mbti/dto` | Fun 질문 응답 DTO (id·title·options 포함, QuestionResponse 구현체) |
| `FunResponseSubmitRequest` | `domain/mbti/dto` | Fun 응답 제출 요청 DTO (mbtiId 4자·optionIds 목록, 비인증 허용) |
| `FunStatRow` | `domain/mbti/dto` | Fun 통계 네이티브 쿼리 Projection Interface (questionId·optionId·mbtiId·count 7컬럼) |
| `FunStatsResponse` | `domain/mbti/dto` | Fun 질문 통계 응답 DTO (questionId·title·options별 totalCount·mbtiCounts 중첩 레코드) |
| `FunQuestionRepository` | `domain/mbti/repository` | Fun 질문 레포지토리 (JOIN FETCH options 쿼리, 통계 네이티브 쿼리) |
| `FunResponseCountRepository` | `domain/mbti/repository` | Fun 응답 집계 레포지토리 (ON CONFLICT DO UPDATE upsertCount 네이티브 쿼리) |
| `FunService` | `domain/mbti/service` | Fun 질문 비즈니스 로직 (랜덤 3개 추출, 응답 UPSERT, 통계 LinkedHashMap 조립) |
| `FunController` | `domain/mbti/controller` | Fun API 컨트롤러 (POST /api/mbti/fun/responses, GET /api/mbti/fun/stats — 인증 불필요) |
| `UserCleanupService` | `domain/user/service` | WITHDRAWN 유저 정기 삭제·법적 보존 로그 만료 삭제 스케줄러 — 04:00 유저 배치, 04:05 retention 배치 |
| `WithdrawalLog` | `domain/user/entity` | 탈퇴 이벤트 기록 엔티티 — PII 없이 withdrawn_at 타임스탬프만 보관, 탈퇴 추이 통계 전용 |
| `WithdrawalLogRepository` | `domain/user/repository` | 탈퇴 로그 레포지토리 — MM-DD 기준 일별 탈퇴 수 집계 네이티브 쿼리 (findTrendRaw) |
| `UserRetentionLog` | `domain/user/entity` | 탈퇴 유저 법적 보존 로그 엔티티 — email(AES 암호화)·phone_hash·탈퇴 IP를 12개월 보관 (전기통신사업법 §83의3) |
| `UserRetentionLogRepository` | `domain/user/repository` | 법적 보존 로그 레포지토리 — deleteAllByExpiresAtBefore로 만료 로그 일괄 삭제 |
| `RetentionEncryptionService` | `global/crypto` | 법적 보존 로그용 가역 암호화 서비스 — Spring Security Encryptors(AES-256/CBC), 환경변수 password·salt 주입 필수 |
| `AdminMbtiQuestionUpdateRequest` | `domain/mbti/dto` | 어드민 MBTI 문항 수정 요청 DTO (title·category·isReverse, testVersion 변경 불가) |
| `AdminMbtiQuestionService` | `domain/mbti/service` | 어드민 MBTI 문항 수정·노출 변경 서비스 — title·category·isReverse 수정, isVisible 토글, QUESTION_NOT_FOUND 예외 처리 |
| `AdminMbtiQuestionVisibilityRequest` | `domain/mbti/dto` | 어드민 MBTI 문항 노출 여부 변경 요청 DTO (visible 필수) |
| `AdminFunQuestionResponse` | `domain/mbti/dto` | 어드민 FUN 문항 응답 DTO (id·title·optionCount·isVisible·updatedAt, 공개 엔드포인트 사용 금지) |
| `AdminFunQuestionVisibilityRequest` | `domain/mbti/dto` | 어드민 FUN 문항 노출 여부 변경 요청 DTO (Boolean 박싱 타입, visible 필수) |
| `AdminFunQuestionService` | `domain/mbti/service` | 어드민 FUN 문항 전체 조회·노출 변경 서비스 — options JOIN FETCH, is_visible 무관 전체 반환 |
| `RequestIdFilter` | `global/common/filter` | 요청마다 8자리 UUID를 MDC requestId에 심어 로그 추적 가능하게 하는 서블릿 필터 — finally에서 반드시 제거 |
| `EmailAlertFlag` | `global/alert` | 이메일 에러 알림 전송 여부 static AtomicBoolean 플래그 — Logback은 Spring 컨텍스트 밖이므로 static 관리, 서버 재시작 시 true로 초기화 |
| `AlertingSmtpAppender` | `global/alert` | SMTPAppender 확장 — sendBuffer 오버라이드로 EmailAlertFlag mute 시 이메일 전송 건너뜀 |
| `AlertController` | `global/alert` | 이메일 에러 알림 제어 API (POST /api/admin/alerts/mute·/unmute, GET /api/admin/alerts/status — ADMIN 전용) |
| `InternalOnly` | `global/internal` | Next.js 서버 등 내부 호출 전용 엔드포인트 선언 어노테이션 — X-Internal-Key 헤더 검증을 InternalOnlyAspect가 자동 처리 |
| `InternalOnlyAspect` | `global/internal` | @InternalOnly 어노테이션 AOP 처리기 — RequestContextHolder로 HttpServletRequest 획득 후 X-Internal-Key 헤더 검증 |
| `InternalApiConstants` | `global/internal` | 내부 API 프로토콜 상수 모음 — INTERNAL_KEY_HEADER("X-Internal-Key") |
| `AuditEvent` | `global/audit` | 감사 로그 이벤트 유형 Enum — SIGNUP·OAUTH2_SIGNUP·LOGIN·OAUTH2_LOGIN·PASSWORD_CHANGE·PHONE_CHANGE·WITHDRAWAL |
| `AuditLog` | `global/audit` | 개인정보보호법 시행령 제30조 접속 기록 엔티티 — actor_id·event·ip·created_at 저장, of()/ofWithTarget() 팩토리 |
| `AuditLogRepository` | `global/audit` | 감사 로그 JPA 레포지토리 (audit_logs 테이블, 커스텀 쿼리 없음) |
| `AuditLogEvent` | `global/audit` | 감사 로그 기록 요청 Spring 이벤트 레코드 — event·actorId·actorRole·ip 전달 |
| `AuditLogService` | `global/audit` | 감사 로그 기록 진입점 — ApplicationEventPublisher로 AuditLogEvent 발행만 담당 |
| `AuditLogEventHandler` | `global/audit` | 감사 로그 비동기 저장 핸들러 — @TransactionalEventListener(AFTER_COMMIT)+@Async 조합으로 호출자 커밋 이후 별도 스레드에 저장 |
| `TermsPolicy` | `domain/user` | 이용약관/개인정보처리방침 현재 버전 상수 (CURRENT_TERMS_VERSION·CURRENT_PRIVACY_VERSION) — 재동의 필요 여부 판단 기준 |
| `PendingOAuth2SignupPrincipal` | `global/security/oauth2` | 약관 동의 전 신규 소셜 가입자용 임시 OAuth2User — User 없이 provider+프로필 정보만 보유, OAuth2AuthenticationSuccessHandler에서 분기 처리 |
| `OAuth2AccountService` | `global/security/oauth2` | 소셜 로그인 신규 계정 생성 서비스 — 약관 동의 완료 API에서만 호출, 이메일 검증은 CustomOAuth2UserService.loadUser()에서 선행 |
| `OAuth2SignupConfirmRequest` | `domain/auth/dto` | 소셜 로그인 신규 가입 약관 동의 완료 요청 DTO (signupToken·termsAgreed·privacyAgreed) |
