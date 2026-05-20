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
| `MbtiService` | `domain/mbti/service` | MBTI 목록 조회 비즈니스 로직 (읽기 전용, findAll 위임) |
| `MbtiController` | `domain/mbti/controller` | MBTI 메타데이터 API 컨트롤러 (GET /api/mbti, 인증 불필요) |
| `JpaConfig` | `global/config` | QueryDSL JPAQueryFactory 빈 등록 |
| `PostTypeRepository` | `domain/post/repository` | 게시판 유형 조회 레포지토리 (code 기준 단건 조회) |
| `PostRepositoryCustom` | `domain/post/repository` | 게시글 목록 QueryDSL 커스텀 조회 인터페이스 |
| `PostRepositoryImpl` | `domain/post/repository` | 게시글 목록 QueryDSL 구현체 (유형·정렬·MBTI 필터 조합) |
| `PostRepository` | `domain/post/repository` | 게시글 기본 CRUD + viewCount 원자적 증가 (@Modifying) |
| `PostCreateRequest` | `domain/post/dto` | 게시글 작성 요청 DTO (제목·content·유형·비회원 비밀번호) |
| `PostUpdateRequest` | `domain/post/dto` | 게시글 수정 요청 DTO (제목·content·비회원 비밀번호) |
| `PostDeleteRequest` | `domain/post/dto` | 게시글 삭제 요청 DTO (비회원 비밀번호) |
| `PostSummaryResponse` | `domain/post/dto` | 게시글 목록 응답 DTO (content 제외, 작성자 MBTI 배지 포함) |
| `PostDetailResponse` | `domain/post/dto` | 게시글 상세 응답 DTO (content 포함, isOwner·isGuest 포함) |
| `PostService` | `domain/post/service` | 게시글 도메인 비즈니스 로직 (목록·단건 조회, 작성·수정·삭제, 회원/비회원 분기) |
| `PostController` | `domain/post/controller` | 게시글 API 컨트롤러 (GET·POST /api/posts, GET·PATCH·DELETE /api/posts/{id}) |
