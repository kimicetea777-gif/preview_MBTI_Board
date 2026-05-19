| 클래스 | 경로 | 설명 |
|---|---|---|
| `DebateSummaryResponse` | `domain/post/dto` | 토론 게시글 요약 응답 DTO (찬반 레이블, 카운트, 마감 여부) |
| `MyProfileResponse` | `domain/user/dto` | 내 프로필 조회 응답 DTO (이메일·MBTI·역할·소셜여부 포함) |
| `UserProfileResponse` | `domain/user/dto` | 타인 공개 프로필 응답 DTO (민감 정보 제외) |
| `UpdateProfileRequest` | `domain/user/dto` | 프로필 수정 요청 DTO (닉네임·이미지·MBTI 선택적 수정) |
| `ChangePasswordRequest` | `domain/user/dto` | 비밀번호 변경 요청 DTO (현재·새 비밀번호) |
| `UserService` | `domain/user/service` | 유저 프로필 비즈니스 로직 (조회·수정·탈퇴·비밀번호 변경) |
| `UserController` | `domain/user/controller` | 유저 프로필 API 컨트롤러 (GET·PATCH /me, GET /{id}, DELETE /me, PATCH /me/password) |
