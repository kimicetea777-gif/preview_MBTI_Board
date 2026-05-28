# Phase 6 개발 계획 상세

---

## 6-2. 검사 결과 제출

### 엔드포인트

`POST /api/mbti/test/submit` — 회원 전용 (JWT 인증 필수)

---

### 점수 계산 주체

| 단계 | 주체 | 내용 |
|------|------|------|
| 문항별 점수 계산 | **프론트엔드** | selectionScore × isReverse 반전 적용 후 지표별 누적 |
| 4개 지표 점수 전송 | **프론트엔드 → 서버** | eiScore, snScore, tfScore, jpScore |
| MBTI 결과 판정 | **서버** | 전송받은 점수 부호로 MBTI 재도출 (클라이언트 조작 방어) |

> 클라이언트가 최종 MBTI 문자열을 직접 전송하지 않는다.  
> 서버가 점수로부터 독립적으로 재도출하여 저장한다.

---

### Request DTO — `MbtiTestSubmitRequest`

```json
{
  "eiScore": 10,
  "snScore": -4,
  "tfScore":  6,
  "jpScore":  -8
}
```

| 필드 | 타입 | 검증 |
|------|------|------|
| `eiScore` | `short` | -16 ~ 16 |
| `snScore` | `short` | -16 ~ 16 |
| `tfScore` | `short` | -16 ~ 16 |
| `jpScore` | `short` | -16 ~ 16 |

Bean Validation: `@Min(-16) @Max(16)`

---

### 서버 처리 흐름

```
1. JWT → 회원 조회
2. 점수 범위 검증 (-16 ~ 16)
3. 점수 부호로 MBTI 판정
     eiScore >= 0 → E,  < 0 → I
     snScore >= 0 → S,  < 0 → N
     tfScore >= 0 → T,  < 0 → F
     jpScore >= 0 → J,  < 0 → P
4. MbtiMetadata 조회 (판정된 MBTI id)
5. MbtiTestHistory 저장
6. users.mbti_id = 판정 MBTI, mbti_verified = TESTED 업데이트
7. 결과 반환
```

---

### Response DTO — `MbtiTestResultResponse`

```json
{
  "mbtiId":   "INFP",
  "eiScore":  -4,
  "snScore":  -6,
  "tfScore":  -10,
  "jpScore":  -8
}
```

---

### 구현 목록

| 항목 | 클래스 | 비고 |
|------|--------|------|
| Request DTO | `MbtiTestSubmitRequest` | Bean Validation 포함 |
| Response DTO | `MbtiTestResultResponse` | |
| 판정 로직 | `MbtiService.submitTest()` | 트랜잭션 쓰기 |
| 이력 저장 | `MbtiTestHistoryRepository` | JpaRepository |
| 유저 업데이트 | `UserRepository` | `@Modifying` — mbti_id·mbti_verified 갱신 |
| 컨트롤러 | `MbtiController.submitTest()` | `@PreAuthorize` or SecurityConfig authenticated |

---

### SecurityConfig 처리

`POST /api/mbti/test/submit`은 현재 `anyRequest().authenticated()`에 위임되어 있어 별도 추가 불필요.
