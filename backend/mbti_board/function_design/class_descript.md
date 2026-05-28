| 클래스 | 경로 | 설명 |
|---|---|---|
| `OAuth2AuthenticationFailureHandler` | `global/security/oauth2` | OAuth2 인증 실패 시 에러 코드를 쿼리 파라미터로 담아 프론트 콜백 페이지로 리다이렉트 |
| `EmailVerifySendRequest` | `domain/auth/dto` | 이메일 인증 발송 요청 DTO (이메일 주소) |
| `EmailVerifyConfirmRequest` | `domain/auth/dto` | 이메일 인증 토큰 확인 요청 DTO (UUID 토큰) |
| `DebateSummaryResponse` | `domain/post/dto` | 토론 게시글 요약 응답 DTO (찬반 레이블, 카운트, 마감 여부) |
| `MyProfileResponse` | `domain/user/dto` | 내 프로필 조회 응답 DTO (이메일·MBTI·역할·소셜여부 포함) |
| `UserProfileResponse` | `domain/user/dto` | 타인 공개 프로필 응답 DTO (민감 정보 제외) |
| `UpdateProfileRequest` | `domain/user/dto` | 프로필 수정 요청 DTO (닉네임·이미지·MBTI 선택적 수정) |
| `ChangePasswordRequest` | `domain/user/dto` | 비밀번호 변경 요청 DTO (현재·새 비밀번호) |
| `UserService` | `domain/user/service` | 유저 프로필 비즈니스 로직 (조회·수정·탈퇴 OTP 발송·탈퇴·비밀번호 변경) |
| `UserController` | `domain/user/controller` | 유저 프로필 API 컨트롤러 (GET·PATCH /me, GET /{id}, POST·DELETE /me/withdrawal-verify·/me, PATCH /me/password) |
| `WithdrawalRequest` | `domain/user/dto` | 회원 탈퇴 요청 DTO (6자리 OTP 코드) |
| `EmailService` | `global/mail` | 이메일 발송 서비스 (탈퇴 OTP 발송) |
| `MbtiResponse` | `domain/mbti/dto` | MBTI 메타데이터 응답 DTO (id·colorCode·description 노출, 내부 타임스탬프 제외) |
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
| `CacheConfig` | `global/config` | 인메모리 캐시 설정 — 참조 테이블(postTypes 등) 전용 ConcurrentMapCacheManager |
| `PostTypeRepository` | `domain/post/repository` | 게시판 유형 조회 레포지토리 (code 기준 단건 조회) |
| `PostRepositoryCustom` | `domain/post/repository` | 게시글 목록 QueryDSL 커스텀 조회 인터페이스 |
| `PostRepositoryImpl` | `domain/post/repository` | 게시글 목록 QueryDSL 구현체 (유형·정렬·MBTI 필터 조합) |
| `PostRepository` | `domain/post/repository` | 게시글 기본 CRUD + viewCount 원자적 증가 (@Modifying) |
| `PostCreateRequest` | `domain/post/dto` | 자유글(FREE) 게시글 작성 요청 DTO (title·content·type·guestPassword·targetMbti — VOTE·DEBATE는 전용 엔드포인트 사용) |
| `DebatePostCreateRequest` | `domain/post/dto` | 토론(DEBATE) 게시글 작성 요청 DTO (POST /api/posts/debate 전용 — 회원 전용, title·content·targetMbti·endsAt·proLabel·conLabel) |
| `PostUpdateRequest` | `domain/post/dto` | 게시글 수정 요청 DTO (제목·content·비회원 비밀번호) |
| `PostDeleteRequest` | `domain/post/dto` | 게시글 삭제 요청 DTO (비회원 비밀번호) |
| `PostSummaryResponse` | `domain/post/dto` | 게시글 목록 응답 DTO (content 제외, 작성자 MBTI 배지 포함) |
| `PostDetailResponse` | `domain/post/dto` | 게시글 상세 응답 DTO (content 포함, isOwner·isGuest 포함) |
| `PostService` | `domain/post/service` | 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제, 회원/비회원 분기) |
| `PostController` | `domain/post/controller` | 게시글 API 컨트롤러 (GET·POST /api/posts, POST /api/posts/debate, POST /api/posts/vote, GET·PATCH·DELETE /api/posts/{id}) |
| `PostSortType` | `domain/post` | 게시글 정렬 기준 Enum (LATEST·VIEWS·COMMENTS) |
| `CommentRepository` | `domain/comment/repository` | 댓글 기본 CRUD + 게시글별 visible 댓글 flat 조회 (findAllVisibleByPostId) |
| `CommentResponse` | `domain/comment/dto` | 댓글 응답 DTO — 루트 댓글은 replies 포함, 대댓글은 replies 빈 리스트 |
| `CommentService` | `domain/comment/service` | 댓글 도메인 비즈니스 로직 (목록 조회, 계층형 구조 조립) |
| `CommentController` | `domain/comment/controller` | 댓글 API 컨트롤러 (GET·POST /api/posts/{postId}/comments, PATCH·DELETE /{id}) |
| `CommentCreateRequest` | `domain/comment/dto` | 댓글 작성 요청 DTO (content·parentId·guestPassword·debateStance) |
| `CommentUpdateRequest` | `domain/comment/dto` | 댓글 수정 요청 DTO (content·guestPassword) |
| `GuestPasswordHelper` | `global/common/util` | 비회원 BCrypt 인코딩 정적 유틸 — PostService·CommentService 공유 |
| `MbtiConstants` | `domain/mbti` | MBTI 도메인 상수 (NONE_MBTI_ID 등) |
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
| `AdminUserSummaryResponse` | `domain/user/dto` | 어드민 유저 목록 응답 DTO (이메일·역할·상태·MBTI 등 민감 정보 포함, ADMIN 전용) |
| `AdminUserStateRequest` | `domain/user/dto` | 어드민 유저 상태 변경 요청 DTO (state 필수) |
| `AdminUserRoleRequest` | `domain/user/dto` | 어드민 유저 역할 변경 요청 DTO (role 필수) |
| `UserRepositoryCustom` | `domain/user/repository` | 유저 목록 QueryDSL 커스텀 조회 인터페이스 (이메일·상태·역할 필터, 어드민 전용) |
| `UserRepositoryImpl` | `domain/user/repository` | 유저 목록 QueryDSL 구현체 (BooleanBuilder 필터 조합, mbti fetchJoin으로 N+1 방지) |
| `AdminUserService` | `domain/user/service` | 어드민 유저 관리 비즈니스 로직 (목록 조회·상태 변경·역할 변경, 자기 자신 변경 금지) |
| `AdminUserController` | `domain/user/controller` | 어드민 유저 관리 API 컨트롤러 (GET /api/admin/users, PATCH /{id}/state, PATCH /{id}/role) |
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
