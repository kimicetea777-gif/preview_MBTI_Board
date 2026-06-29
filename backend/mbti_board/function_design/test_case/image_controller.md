# ImageController 테스트 케이스

**대상 클래스**: `ImageControllerTest`
**테스트 방식**: `@WebMvcTest` (컨트롤러 슬라이스, Security 필터 제외)
> Rate Limit(`@RateLimit` AOP)은 `@WebMvcTest` 슬라이스에서 로드되지 않으므로 별도 통합 테스트 필요

---

### POST /api/images/upload

| ID      | 케이스                              | 기대 결과                        |
|---------|-------------------------------------|----------------------------------|
| IMG-1-1 | 유효한 키 + PNG 파일                | 200 OK, `data.url` 반환          |
| IMG-1-2 | X-Internal-Key 헤더 누락            | 403 Forbidden                    |
| IMG-1-3 | 잘못된 X-Internal-Key               | 403 Forbidden                    |
| IMG-1-4 | 지원하지 않는 이미지 형식 (gif)     | 400 Bad Request                  |
| IMG-1-5 | 파일 크기 10MB 초과                 | 400 Bad Request                  |
| IMG-1-6 | R2 업로드 오류 (서비스 예외)        | 500 Internal Server Error        |

---

### DELETE /api/images

| ID      | 케이스                              | 기대 결과                        |
|---------|-------------------------------------|----------------------------------|
| IMG-2-1 | 유효한 키 + URL 1개                 | 204 No Content                   |
| IMG-2-2 | 유효한 키 + URL 최대 개수 (3개)     | 204 No Content                   |
| IMG-2-3 | X-Internal-Key 헤더 누락            | 403 Forbidden                    |
| IMG-2-4 | 잘못된 X-Internal-Key               | 403 Forbidden                    |
| IMG-2-5 | urls 빈 목록 (`@NotEmpty` 위반)     | 400 Bad Request                  |
| IMG-2-6 | urls 최대 개수 초과 (`@Size` 위반)  | 400 Bad Request                  |
| IMG-2-7 | urls 필드 자체 누락 (`{}` 전송)     | 400 Bad Request                  |
