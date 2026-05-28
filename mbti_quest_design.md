# MBTI Test Feature Specification

## 1. 문항 및 답변 구조

- **총 문항 수**: 총 32문항 (4개 지표 각 8문항 배치)
- **진행률 표시**: 사용자가 문항을 넘길 때마다 상단에 진행 상태바(Progress Bar) 노출 필수
- **답변 형식**: 5지 선다형 구조로 선택지에 따라 점수를 차등 부여합니다.

| 선택지      | 부여 점수 (`selectionScore`) |
| :---------- | :--------------------------- |
| 매우 그렇다 | `+2`                         |
| 그렇다      | `+1`                         |
| 보통이다    | `0`                          |
| 아니다      | `-1`                         |
| 매우 아니다 | `-2`                         |

---

## 2. 역채점 문항 설계 (Abuse 방지)

사용자가 생각 없이 한 줄로 찍는 것을 방지하기 위해 일반 문항과 역채점 문항을 50:50 비율로 혼합합니다.

- **일반 문항**: 긍정 답변(그렇다)이 앞자리 지표(E, S, T, J)를 향하는 질문
- **역채점 문항**: 긍정 답변(그렇다)이 뒷자리 지표(I, N, F, P)를 향하는 질문

## 3. 점수 계산 알고리즘

사용자가 선택한 답변의 가중치를 `selectionScore`(`2`, `1`, `0`, `-1`, `-2`)라고 할 때, 각 지표별 최종 점수 합산 및 판정 규칙은 다음과 같습니다.

### 3.1 문항별 점수 계산 규칙

문항의 역채점 여부(`isReverse`) 플래그에 따라 점수를 누적합니다.

- **일반 문항 (`isReverse: false`)**  
  선택 점수를 그대로 해당 지표에 더합니다.
  $$\text{지표 점수} += \text{selectionScore}$$

- **역채점 문항 (`isReverse: true`)**  
  선택 점수의 부호를 반대로 뒤집어서 해당 지표에 더합니다.
  $$\text{지표 점수} += (\text{selectionScore} \times -1)$$

> **지표별 점수 범위: -16 ~ +16** (카테고리당 8문항 × 최대 ±2점)  
> DB `mbti_test_history` 의 `*_score` 컬럼 CHECK 제약과 일치.

---

### 3.2 최종 MBTI 성향 판정 기준

4개 지표별로 누적된 최종 합산 점수의 부호(양수/음수)를 판별하여 4글자의 MBTI 코드를 조합합니다.  
_(※ 합산 결과가 정확히 `0`점일 경우에는 앞글자(E, S, T, J)를 기본값으로 채택합니다.)_

#### 🪐 E (외향) vs I (내향)

- $\text{EI 점수} \ge 0 \longrightarrow \mathbf{E}$
- $\text{EI 점수} < 0 \longrightarrow \mathbf{I}$

#### 🪐 S (감각) vs N (직관)

- $\text{SN 점수} \ge 0 \longrightarrow \mathbf{S}$
- $\text{SN 점수} < 0 \longrightarrow \mathbf{N}$

#### 🪐 T (사고) vs F (감정)

- $\text{TF 점수} \ge 0 \longrightarrow \mathbf{T}$
- $\text{TF 점수} < 0 \longrightarrow \mathbf{F}$

#### 🪐 J (판단) vs P (인식)

- $\text{JP 점수} \ge 0 \longrightarrow \mathbf{J}$
- $\text{JP 점수} < 0 \longrightarrow \mathbf{P}$

---

## 4. DB 테이블 설계

### 4.1 mbti_questions 테이블

```sql
mbti_questions (
  id           BIGSERIAL PRIMARY KEY,
  category     VARCHAR(2)   NOT NULL,         -- EI, SN, TF, JP
  title        TEXT NOT NULL,
  is_reverse   BOOLEAN      NOT NULL,
  test_version VARCHAR(10)  DEFAULT 'v1.0' NOT NULL
)

seed 데이터 파일 생성 완료 (참고: backend\mbti_board\src\main\resources\db\migration\V4_seed_mbti_questions.sql)
```

카테고리당 문항 수는 최소 16개 이상 준비하여 재검사 시 다른 문항이 노출되도록 한다.

### 4.2 문항 조회 쿼리

카테고리별로 균등하게 8개씩 랜덤 추출하는 단일 쿼리 (DB 요청 1회):

```sql
(SELECT * FROM mbti_questions WHERE category = 'EI' AND test_version = 'v1.0' ORDER BY RANDOM() LIMIT 8)
UNION ALL
(SELECT * FROM mbti_questions WHERE category = 'SN' AND test_version = 'v1.0' ORDER BY RANDOM() LIMIT 8)
UNION ALL
(SELECT * FROM mbti_questions WHERE category = 'TF' AND test_version = 'v1.0' ORDER BY RANDOM() LIMIT 8)
UNION ALL
(SELECT * FROM mbti_questions WHERE category = 'JP' AND test_version = 'v1.0' ORDER BY RANDOM() LIMIT 8)
```

조회 후 서비스 레이어에서 `Collections.shuffle()`을 적용해 카테고리 순서를 뒤섞어 사용자에게 노출한다.

### 4.3 재검사 문항 전략

이전 문항 추적 없이 단순 랜덤 방식을 사용한다. 카테고리당 문항 수를 충분히 확보(16개 이상)하면 재검사 시 자연스럽게 다른 문항이 노출될 확률이 높아진다. `mbti_test_history` 테이블 구조 변경 없이 그대로 사용한다.

---

## 5. mbti_metadata 스키마 변경

### 5.1 변경 내용

| 컬럼          | 변경 전         | 변경 후                      |
| ------------- | --------------- | ---------------------------- |
| `summary`     | 없음            | `VARCHAR(100) NOT NULL` 추가 |
| `description` | `TEXT NOT NULL` | `JSONB NOT NULL` 으로 변경   |

### 5.2 용도

- `summary`: 게시글/댓글의 MBTI 배지 Hover·Click 시 표시하는 한 줄 소개 (모달)
- `description`: MBTI 소개 페이지에서 표시하는 구조화된 상세 설명

### 5.3 description JSONB 구조

```json
{
  "overview": "전략적 사고와 강한 의지로 목표를 달성하는 전략가",
  "traits": ["전략적", "독립적", "결단력"],
  "strengths": ["목표 지향적", "효율적인 계획 수립"],
  "weaknesses": ["감정 표현 어려움", "타인에게 냉담할 수 있음"],
  "compatible": ["ENFP", "ENTP"]
}
```

