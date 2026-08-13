# DITTO BackEnd

중국·일본·미국 관광객이 국가별 K-컬처 트렌드를 탐색하고, AI로 맞춤 코스를 만든 뒤 코스 커스텀·모바일 실내 길찾기·여행자 커뮤니티로 경험을 이어가는 관광 플랫폼 DITTO의 백엔드입니다.

현재 문서는 **백엔드 초기 개발 환경 안내서**입니다. 비즈니스 로직이 완성되면 최종 서비스 설명, 도메인 모델, 아키텍처 및 배포 안내를 포함한 문서로 교체합니다.

## 핵심 사용자 흐름

```text
국가별 트렌드 탐색
→ AI 맞춤 코스 생성
→ 사용자 코스 커스텀
→ 모바일 실내 길찾기
→ 여행자 커뮤니티 공유
```

현재 단계에서는 개별 비즈니스 로직 구현보다 **공통 응답 포맷, 전역 예외 처리, 프로젝트 구조, 보안 골격, 코딩 컨벤션**을 먼저 탄탄하게 구축합니다. 이 기반 위에서 각 도메인 API를 일관된 형태로 얹는 것이 목표입니다.

## 기술 스택

| 영역 | 기술 | 책임 |
| --- | --- | --- |
| Language | Java 17 (JDK 17) | 애플리케이션 코드 |
| Framework | Spring Boot 3.3.x | 애플리케이션 구동, 자동 설정 |
| Web | Spring Web (MVC) | REST API, 요청/응답 처리 |
| Persistence | MyBatis | SQL 매핑 기반 영속화 |
| AI | Spring AI (AWS Bedrock) | LLM·임베딩, RAG |
| Security | Spring Security (세션 기반) | 인증·인가, 세션 로그인 |
| Validation | Bean Validation | 요청 DTO 유효성 검증 |
| Docs | SpringDoc OpenAPI 3 | Swagger UI, API 문서 자동화 |
| Convenience | Lombok | 보일러플레이트 제거 |
| Build | Gradle | 빌드, 의존성 관리 |
| Database | Oracle (메인) / PostgreSQL + pgvector (RAG) | 관계형 데이터 · 벡터 저장 |

설치된 정확한 버전은 [build.gradle](./build.gradle)을 기준으로 합니다.

## 공통 응답 포맷

모든 API는 성공·실패와 관계없이 동일한 최상위 필드(`success`, `code`, `message`)를 갖는 JSON을 반환합니다. 성공 응답은 제네릭 `ApiResponse<T>`로 감싸고, 실패 응답은 `ErrorResponse`로 반환합니다.

### `ApiResponse<T>`

```java
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청이 정상 처리되었습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).code("SUCCESS").message(message).data(data).build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true).code("SUCCESS").message("요청이 정상 처리되었습니다.").build();
    }
}
```

컨트롤러에서는 이렇게 사용합니다.

```java
@GetMapping("/{courseId}")
public ApiResponse<CourseDetailResponse> getCourse(@PathVariable Long courseId) {
    return ApiResponse.success(courseService.getCourse(courseId));
}
```

### 성공 응답 예시

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상 처리되었습니다.",
  "data": {
    "courseId": 12,
    "title": "성수동 K-뷰티 코스",
    "placeCount": 5
  }
}
```

### 실패 응답 예시

```json
{
  "success": false,
  "code": "CR001",
  "message": "코스를 찾을 수 없습니다."
}
```

검증 실패(400)처럼 필드별 상세가 있는 경우:

```json
{
  "success": false,
  "code": "C001",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    { "field": "email", "value": "abc", "reason": "올바른 이메일 형식이 아닙니다." }
  ]
}
```

## 공통 예외 처리

> 현재 단계에서는 도메인 로직보다 **예외를 한 곳에서 일관되게 처리하는 골격**을 먼저 구축합니다. 모든 예외는 `@RestControllerAdvice` 한 곳으로 모여 위의 실패 응답 포맷으로 변환됩니다.

구조는 다음 4개로 나뉩니다.

```text
ErrorCode(enum)  →  에러의 단일 정의(HTTP status + 비즈니스 코드 + 메시지)
BusinessException →  서비스 로직이 던지는 예외 (ErrorCode를 감쌈)
GlobalExceptionHandler → 모든 예외를 잡아 ErrorResponse로 변환
ErrorResponse    →  실패 응답 본문 DTO
```

### `ErrorCode` (enum)

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "요청 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C006", "필수 요청 파라미터가 누락되었습니다."),

    // Auth / Security
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "세션이 만료되었습니다. 다시 로그인해 주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),

    // Course
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "코스를 찾을 수 없습니다."),
    NOT_COURSE_OWNER(HttpStatus.FORBIDDEN, "CR002", "코스에 대한 권한이 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR003", "장소를 찾을 수 없습니다."),
    DUPLICATE_PLACE_IN_COURSE(HttpStatus.BAD_REQUEST, "CR004", "코스에 같은 장소가 중복되어 있습니다."),

    // Community
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "댓글을 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "CM003", "이미 좋아요한 코스입니다."),

    // Navigation / Mobile
    MAP_MANIFEST_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "지도 매니페스트를 찾을 수 없습니다."),
    INVALID_ACCESS_CODE(HttpStatus.BAD_REQUEST, "N002", "유효하지 않은 접속 코드입니다."),

    // External (AI / OCR)
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "E001", "AI 서비스 처리 중 오류가 발생했습니다."),
    OCR_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "E002", "OCR 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### `BusinessException` 계층

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

도메인별로 세분화가 필요하면 이 클래스를 상속합니다.

```java
public class CourseNotFoundException extends BusinessException {
    public CourseNotFoundException() {
        super(ErrorCode.COURSE_NOT_FOUND);
    }
}
```

서비스 로직에서는 다음처럼 던집니다.

```java
Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
```

### `ErrorResponse` DTO

```java
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> errors;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .success(false).code(errorCode.getCode()).message(errorCode.getMessage()).build();
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return ErrorResponse.builder()
                .success(false).code(errorCode.getCode()).message(errorCode.getMessage())
                .errors(FieldErrorDetail.from(bindingResult)).build();
    }

    @Getter @Builder
    public static class FieldErrorDetail {
        private final String field;
        private final String value;
        private final String reason;
        // from(BindingResult) 구현 생략 — 실제 파일 참고
    }
}
```

### `GlobalExceptionHandler` (@RestControllerAdvice)

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[UnhandledException]", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

### 처리 대상 예외 목록

| 예외 | 처리 상태 | 매핑 `ErrorCode` | 설명 |
| --- | --- | --- | --- |
| `BusinessException` | 각 코드의 status | `ErrorCode` 그대로 | 서비스 로직 정의 예외 |
| `MethodArgumentNotValidException` | 400 | `INVALID_INPUT_VALUE` | `@Valid @RequestBody` 검증 실패 |
| `BindException` | 400 | `INVALID_INPUT_VALUE` | `@ModelAttribute` 바인딩 검증 실패 |
| `MethodArgumentTypeMismatchException` | 400 | `INVALID_TYPE_VALUE` | 파라미터 타입 불일치 |
| `MissingServletRequestParameterException` | 400 | `MISSING_REQUEST_PARAMETER` | 필수 파라미터 누락 |
| `HttpMessageNotReadableException` | 400 | `INVALID_INPUT_VALUE` | 잘못된 JSON 본문 |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` | 미지원 HTTP 메서드 |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` | 인가 실패(Security) |
| `Exception` | 500 | `INTERNAL_SERVER_ERROR` | 그 외 처리되지 않은 모든 예외 |

전체 코드는 [global/exception](./src/main/java/com/ditto/global/exception)에서 확인할 수 있습니다.

## 프로젝트 구조

도메인형 패키지 구조(Package by Feature)를 따릅니다. 각 비즈니스 도메인(`auth`, `user`, `course`, `community`, `news`, `navigation`, `mobile`, `admin`, `aicourse`, `country`) 이하에 `controller`, `service`, `repository`, `domain`, `dto`를 두고, 도메인 간 공통 관심사는 `global`, 설정은 `config`, 보안은 `security`에 둡니다.

```text
Ditto-BackEnd/
├── build.gradle                 # 의존성, 빌드 설정
├── settings.gradle
├── README.md
└── src/
    ├── main/
    │   ├── java/com/ditto/
    │   │   ├── DittoApplication.java      # 진입점 (TimeZone=Asia/Seoul 설정)
    │   │   │
    │   │   ├── auth/                     # 인증 도메인 (회원가입·로그인·로그아웃·세션)
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   └── dto/ (request/·response/)
    │   │   │
    │   │   ├── user/                     # 사용자 도메인 (내 정보·설정)
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── aicourse/                 # AI 코스 추천 도메인
    │   │   │   ├── controller/  service/  dto/
    │   │   │
    │   │   ├── course/                   # 코스 도메인 (내 코스 + 공개 코스)
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── community/                # 커뮤니티 도메인 (게시글·댓글·좋아요·북마크)
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── news/                     # 뉴스피드 도메인
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── navigation/               # 실내 내비게이션 도메인
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── mobile/                   # 모바일 접속 코드 도메인
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── admin/                    # 관리자 도메인
    │   │   │   ├── controller/  service/  repository/  domain/  dto/
    │   │   │
    │   │   ├── country/                  # 국가 정보 도메인
    │   │   │   └── repository/
    │   │   │
    │   │   ├── global/                   # 전역 공통 관심사
    │   │   │   ├── common/
    │   │   │   │   └── response/ApiResponse.java
    │   │   │   └── exception/
    │   │   │       ├── ErrorCode.java
    │   │   │       ├── BusinessException.java
    │   │   │       ├── ErrorResponse.java
    │   │   │       └── GlobalExceptionHandler.java
    │   │   │
    │   │   ├── config/                   # 스프링 설정
    │   │   │   ├── SwaggerConfig.java
    │   │   │   ├── CorsConfig.java
    │   │   │   └── persistence/           # 이중 DataSource(Oracle/Postgres) + MyBatis 설정
    │   │   │
    │   │   └── security/                 # 인증·인가 (세션 기반)
    │   │       ├── SecurityConfig.java
    │   │       ├── CustomUserDetailsService.java # (예정) 사용자 조회
    │   │       └── CustomUserDetails.java        # (예정) 인증 주체
    │   │
    │   └── resources/
    │       ├── application.yml           # 공통 설정 + 프로파일 지정
    │       ├── application-local.yml     # 로컬 DB/세션/로깅/Spring AI
    │       └── mapper/                   # MyBatis 매퍼 XML
    └── test/
        └── java/com/ditto/
```

레이어 및 구조 규칙:

- 각 비즈니스 기능은 해당 도메인 패키지(`com.ditto.<domain>`) 하위에 작성합니다.
- **controller**: HTTP 요청/응답 변환만 담당합니다. 비즈니스 로직을 넣지 않고, 반환은 항상 `ApiResponse<T>`로 감쌉니다.
- **service**: 실제 로직과 트랜잭션 경계입니다. 도메인 예외(`BusinessException`)를 던집니다.
- **repository**: `@Mapper` 인터페이스만 둡니다. SQL은 `resources/mapper/**/*.xml`(또는 애너테이션)에 둡니다.
- **domain**: MyBatis가 매핑하는 도메인 객체/엔티티/이넘. `dto`와 절대 섞지 않으며, 컨트롤러 응답으로 직접 반환하지 않습니다.
- **dto**: 요청/응답 DTO. `request`와 `response` subpackage로 분리합니다.
- **global / config / security**: 도메인에 종속되지 않는 공통 코드입니다.

## API 명세

- **Base URL**: `/api/v1`
- 모든 응답은 [공통 응답 포맷](#공통-응답-포맷)을 따릅니다.
- **인증** 열: `X` = 인증 불필요(공개), `O` = 로그인(세션) 필요, `ADMIN` = 관리자 권한 필요.

> 현재 단계에서는 엔드포인트 **계약(Method·경로·인증 정책)**을 먼저 확정합니다. 실제 요청/응답 스키마는 도메인 구현과 함께 Swagger에 채웁니다.

### 인증 (Auth) — `/api/v1/auth`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 회원가입 | `POST` | `/api/v1/auth/signup` | X |
| 로그인 | `POST` | `/api/v1/auth/login` | X |
| 로그아웃 | `POST` | `/api/v1/auth/logout` | O |
| 세션 로그인 상태 확인 | `GET` | `/api/v1/auth/me` | O |

### 사용자 (User) — `/api/v1/users`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 내 정보 조회 | `GET` | `/api/v1/users/me` | O |
| 국가·언어 설정 변경 | `PATCH` | `/api/v1/users/me/preferences` | O |

### AI 코스 (AI Course) — `/api/v1/ai`

| 기능 | Method | Endpoint | 인증 | 상태 |
| --- | --- | --- | --- | --- |
| AI 코스 추천 대화 · 맞춤 생성 · 재추천 | `POST` | `/api/v1/ai/course-recommendations/chat` | O | 구현됨 |

**엔드포인트는 하나입니다.** 맞춤 생성·대화로 다듬기·재추천이 전부 같은 호출입니다.
차이는 `sessionId`를 싣느냐뿐입니다.

| 하고 싶은 것 | 보내는 값 |
| --- | --- |
| 맞춤 코스 생성 (첫 요청) | `sessionId` 없이 `message`만 |
| 대화로 다듬기 | 받은 `sessionId` + `"좀 더 저렴한 걸로"` |
| 재추천 | 받은 `sessionId` + `"다시 짜줘"` |

```
요청  {"sessionId": "Op3uskz8Gpo"|생략, "message": "카리나가 좋아하는 브랜드 구경하고 밥도 먹고 싶어"}
응답  data: {"sessionId": "Op3uskz8Gpo", "reply": "...", "turn": 1,
             "places": [{"navigationKey": "1F_STORE_0035",
                         "placeName": "프라다", "reason": "카리나가 2024년부터 ..."}]}
```

**결과물은 장소마다의 `navigationKey`와 `reason`입니다.** 클라이언트는 `navigationKey`로
실내지도에서 장소를 찾습니다. 추천 코스는 DB에 저장하지 않으므로 Oracle `place_id`는
싣지 않습니다. 조건에 맞는 장소가 없으면 `places`는 빈 배열입니다.

추천 로직은 이 서버에 없습니다. 외부 **AI 엔진**(현재 로컬 파이썬 서비스, 이후 AWS Lambda)을
HTTP로 호출하고 응답을 `ApiResponse`로 감싸 돌려줄 뿐입니다. 엔진을 옮길 때 바뀌는 것은
`ditto.ai-engine.base-url` 한 줄이고 자바 코드는 그대로입니다.

엔진 장애·타임아웃은 `E001`(502)로 변환됩니다. 한 턴에 **수십 초**가 걸리므로
클라이언트 타임아웃을 넉넉히 잡아야 합니다 (실측 40~45초).

> 엔진으로 보내는 본문은 반드시 `Content-Length`가 붙어야 합니다. 청크 전송으로 보내면
> 엔진 쪽 `http.server`가 본문을 0바이트로 읽어 **에러 없이 빈 메시지**를 처리합니다.
> `AiEngineClient`가 본문을 `byte[]`로 직렬화하는 이유입니다. AWS API Gateway도
> 청크 전송을 받지 않으므로 Lambda 이전 후에도 동일합니다.

### 내 코스 (My Course) — `/api/v1/courses`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 내 코스 생성·저장 | `POST` | `/api/v1/courses` | O |

수동 모드 「빈 코스로 시작하기」는 `name`·`placeIds` 없이 호출하면 된다. `placeIds`를 넘기면 DB `place` 테이블에 있는 ID만 담는다.

요청 예시:

```json
{
  "name": "나의 더현대 코스",
  "description": "오후 반나절 코스",
  "placeIds": [11, 22, 33]
}
```

빈 코스:

```json
{}
```

성공 응답:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "courseId": 100,
    "name": "나의 더현대 코스",
    "places": [
      { "placeId": 11, "order": 1 },
      { "placeId": 22, "order": 2 },
      { "placeId": 33, "order": 3 }
    ]
  }
}
```
| 내 코스 목록 조회 | `GET` | `/api/v1/courses/my` | O |
| 내 코스 정보·방문 순서 수정 | `PUT` | `/api/v1/courses/{courseId}` | O |
| 내 코스 삭제 | `DELETE` | `/api/v1/courses/{courseId}` | O |
| 내 코스에 장소 추가 | `POST` | `/api/users/me/courses/{courseId}/places` | O |
| 내 코스에서 장소 삭제 | `DELETE` | `/api/v1/courses/{courseId}/places/{placeId}` | O |
| 공개 코스를 내 코스로 복사 | `POST` | `/api/v1/courses/{courseId}/copy` | O |

내 코스에 장소 추가 요청:

```json
{
  "placeId": 44,
  "position": 2
}
```

성공 응답:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "courseId": 100,
    "placeId": 44,
    "position": 2
  }
}
```

### 공개 코스 · 커뮤니티 (Public Course & Community) — `/api/v1/courses/public`, `/api/v1/community`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 공개 코스 목록 조회 | `GET` | `/api/v1/courses/public` | X |
| 공개 코스 상세 조회 | `GET` | `/api/v1/courses/public/{courseId}` | X |
| 코스 상세·방문 장소 조회 | `GET` | `/api/v1/courses/public/{courseId}/places` | X |
| TOP 코스 목록 조회 | `GET` | `/api/v1/courses/public/top` | X |
| 국가별 인기 코스 조회 | `GET` | `/api/v1/courses/public/popular?country={code}` | X |
| 테마별 코스 조회 | `GET` | `/api/v1/courses/public/themes/{theme}` | X |
| 공개 코스 좋아요 등록 | `POST` | `/api/v1/courses/public/{courseId}/likes` | O |
| 공개 코스 좋아요 취소 | `DELETE` | `/api/v1/courses/public/{courseId}/likes` | O |
| 공개 코스 북마크 등록 | `POST` | `/api/v1/courses/public/{courseId}/bookmarks` | O |
| 공개 코스 북마크 취소 | `DELETE` | `/api/v1/courses/public/{courseId}/bookmarks` | O |
| 내 북마크 목록 조회 | `GET` | `/api/v1/users/me/bookmarks` | O |
| 코스 게시글 작성 | `POST` | `/api/v1/community/posts` | O |
| 코스 게시글 수정 | `PUT` | `/api/v1/community/posts/{postId}` | O |
| 코스 게시글 삭제 | `DELETE` | `/api/v1/community/posts/{postId}` | O |
| 댓글·답글 작성 | `POST` | `/api/v1/community/posts/{postId}/comments` | O |
| 댓글 수정 | `PUT` | `/api/v1/community/comments/{commentId}` | O |
| 댓글 삭제 | `DELETE` | `/api/v1/community/comments/{commentId}` | O |

> 답글은 댓글 작성과 동일 엔드포인트에 `parentId`를 담아 처리합니다.

### 뉴스피드 (News) — `/api/v1/news`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 뉴스피드 목록 조회 | `GET` | `/api/v1/news` | X |
| 뉴스피드 상세 조회 | `GET` | `/api/v1/news/{newsId}` | X |
| 뉴스피드 작성 | `POST` | `/api/v1/news` | ADMIN |
| 뉴스피드 수정 | `PUT` | `/api/v1/news/{newsId}` | ADMIN |
| 뉴스피드 삭제 | `DELETE` | `/api/v1/news/{newsId}` | ADMIN |

### 실내 내비게이션 · 모바일 (Navigation & Mobile) — `/api/v1/navigation`, `/api/v1/mobile`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 길찾기 가능 장소 목록 조회 | `GET` | `/api/v1/places/navigation` | X |
| 장소 길찾기 식별자 조회 | `GET` | `/api/v1/places/{placeId}/navigation` | X |
| 지도 매니페스트 조회 | `GET` | `/api/v1/navigation/maps/{mapId}/manifest` | X |
| 층별 내비게이션 데이터 조회 | `GET` | `/api/v1/navigation/maps/{mapId}/floors/{floor}` | X |
| 코스 이동 경로 계산 | `POST` | `/api/v1/navigation/courses/{courseId}/route` | O |
| 현재 위치 확인·경로 시작점 설정 | `POST` | `/api/v1/navigation/location` | O |
| OCR 현재 위치 인식 | `POST` | `/api/v1/navigation/location/ocr` | O |
| 장소 방문 완료·코스 진행률 조회 | `POST` | `/api/v1/navigation/courses/{courseId}/progress` | O |
| 모바일 접속 코드 발급 | `POST` | `/api/v1/mobile/access-codes` | O |
| 접속 코드 검증·코스 불러오기 | `POST` | `/api/v1/mobile/access-codes/verify` | X |

### 관리자 (Admin) — `/api/v1/admin`

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 관리자 국가 등록 | `POST` | `/api/v1/admin/countries` | ADMIN |
| 관리자 국가 목록 조회 | `GET` | `/api/v1/admin/countries` | ADMIN |
| 관리자 국가 수정·비활성화 | `PATCH` | `/api/v1/admin/countries/{countryId}` | ADMIN |
| 관리자 브랜드 등록 | `POST` | `/api/v1/admin/brands` | ADMIN |
| 관리자 브랜드 목록 조회 | `GET` | `/api/v1/admin/brands` | ADMIN |
| 관리자 브랜드 수정·비활성화 | `PATCH` | `/api/v1/admin/brands/{brandId}` | ADMIN |
| 셀럽 연관 브랜드 관리 | `PUT` | `/api/v1/admin/celebs/{celebId}/brands` | ADMIN |
| 관리자 키워드 등록 | `POST` | `/api/v1/admin/keywords` | ADMIN |
| 관리자 키워드 목록 조회 | `GET` | `/api/v1/admin/keywords` | ADMIN |
| 관리자 키워드 수정·비활성화 | `PATCH` | `/api/v1/admin/keywords/{keywordId}` | ADMIN |
| 기본 추천 코스 등록 | `POST` | `/api/v1/admin/recommend-courses` | ADMIN |
| 기본 추천 코스 수정 | `PUT` | `/api/v1/admin/recommend-courses/{courseId}` | ADMIN |
| 기본 추천 코스 삭제 | `DELETE` | `/api/v1/admin/recommend-courses/{courseId}` | ADMIN |
| 트렌드 순위 관리 | `PUT` | `/api/v1/admin/trends/rankings` | ADMIN |
| AI 코스·챗봇 로그 조회 | `GET` | `/api/v1/admin/logs/ai` | ADMIN |
| 사이트맵 조회 | `GET` | `/api/v1/admin/sitemap` | ADMIN |
| 검색 유입 콘텐츠 조회 | `GET` | `/api/v1/admin/seo/contents` | ADMIN |

## 인증/인가 기본 골격

**세션(Session) 기반 인증**을 사용합니다. JWT는 사용하지 않습니다.

- 로그인 성공 시 서버가 `HttpSession`을 생성하고, 브라우저에는 `JSESSIONID` 쿠키가 내려갑니다.
- 이후 요청은 이 세션 쿠키로 인증되며, 인증 정보는 서버의 `SecurityContext`(세션)에 보관됩니다.
- "세션 로그인 상태 확인"은 현재 세션의 인증 여부로 판단합니다(`GET /auth/me`).
- 로그아웃 시 세션을 무효화하고 `JSESSIONID` 쿠키를 삭제합니다.
- 세션 만료는 `server.servlet.session.timeout`(기본 30분)으로 관리합니다. 별도의 토큰 재발급 흐름이 없습니다.

> 세션은 기본적으로 서버 메모리에 저장됩니다. 서버를 여러 대로 확장하면 세션 공유가 필요하므로, 그 시점에 Spring Session(Redis 등) 도입을 검토합니다. 관련 의존성은 [build.gradle](./build.gradle)에 주석으로 준비돼 있습니다.

### SecurityFilterChain 방향

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())               // SPA(별도 오리진) + SameSite 쿠키로 시작
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())          // 로그인은 커스텀 /auth/login 에서 처리
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation(fixation -> fixation.changeSessionId())  // 세션 고정 공격 방지
            .maximumSessions(1))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**", "/api/v1/courses/public/**",
                             "/api/v1/news/**",
                             "/swagger-ui.html", "/swagger-ui.html/**", "/swagger-ui/**",
                             "/v3/api-docs", "/v3/api-docs/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .logout(logout -> logout
            .logoutUrl("/api/v1/auth/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"));

    return http.build();
}
```

로그인은 `formLogin`을 끄고 `AuthService`에서 직접 인증한 뒤 세션에 저장합니다.

```java
// AuthService.login(...) 요지
UsernamePasswordAuthenticationToken authRequest =
        new UsernamePasswordAuthenticationToken(email, password);
Authentication authentication = authenticationManager.authenticate(authRequest);

SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
// HttpSession 에 SecurityContext 저장 → 이후 요청에서 JSESSIONID 로 인증 유지
securityContextRepository.saveContext(context, request, response);
```

구현 순서(예정):

1. `CustomUserDetailsService` — 이메일로 사용자 조회
2. `CustomUserDetails` — 인증 주체(권한 포함)
3. `AuthService` — 위와 같이 인증 후 `SecurityContext`를 세션에 저장
4. 로그아웃은 위 `SecurityConfig`의 `logout` 설정으로 처리

### CORS와 세션 쿠키

세션 쿠키를 크로스 오리진으로 주고받으므로 프론트·백엔드 양쪽 설정이 맞아야 합니다.

- 백엔드: `setAllowCredentials(true)`, 허용 오리진에 프론트 주소 명시(`*` 불가).
- 프론트엔드: 요청 시 자격 증명 포함(Axios `withCredentials: true`).
- 쿠키: `HttpOnly`, `SameSite=Lax`(운영 HTTPS에서는 `Secure=true`).

### 프론트엔드 온보딩 흐름과의 정합성

프론트엔드는 현재 실제 인증 없이 화면 전환만 하는 임시 온보딩(`/signup → /country → /persona → /`, `/login → /`)을 사용합니다. 백엔드는 이 흐름을 다음과 같이 실제 API로 대응시킵니다.

- 회원가입(`POST /auth/signup`) 성공 → 국가·언어 설정(`PATCH /users/me/preferences`)
- 로그인(`POST /auth/login`) 성공 → 세션 생성(`JSESSIONID` 발급)
- 온보딩에서 선택한 국가·언어·페르소나는 사용자 설정 API로 저장

프론트엔드 인증이 임시 단계인 동안, 백엔드는 공개(`X`) 엔드포인트만으로도 화면이 동작하도록 공개 범위를 위 표대로 유지합니다.

## 코딩 컨벤션

### 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 패키지 | 소문자 | `com.ditto.course` |
| 클래스 | PascalCase | `CourseService` |
| 메서드·변수 | camelCase | `getMyCourses()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PLACE_COUNT` |
| 엔티티 | 단수 명사 | `Course`, `Place` |
| 컨트롤러 | `~Controller` | `CourseController` |
| 서비스 | `~Service` | `CourseService` |
| 리포지토리 | `~Repository` | `CourseRepository` |
| 테이블·컬럼 | snake_case | `course_place` |

### Request / Response DTO 분리

- 요청과 응답 DTO를 **반드시 분리**하고, 엔티티를 컨트롤러 입출력에 직접 노출하지 않습니다.
- 요청 DTO는 `record` + Bean Validation을 기본으로 합니다.

```java
public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 30) String password,
        @NotBlank String nickname
) {}

public record CourseDetailResponse(
        Long courseId,
        String title,
        int placeCount
) {
    public static CourseDetailResponse from(Course course) {
        return new CourseDetailResponse(course.getId(), course.getTitle(), course.getPlaceCount());
    }
}
```

### Service 트랜잭션 처리

- 클래스에 `@Transactional(readOnly = true)`를 기본으로 두고, 쓰기 메서드에만 `@Transactional`을 붙입니다.
- 트랜잭션 경계는 **서비스 계층**에 둡니다(컨트롤러·리포지토리 아님).

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseMapper courseMapper;

    public CourseDetailResponse getCourse(Long courseId) {
        Course course = courseMapper.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        return CourseDetailResponse.from(course);
    }

    @Transactional
    public Long create(Long userId, CourseCreateRequest request) {
        // 쓰기 로직
    }
}
```

기타 규칙:

- 컨트롤러 반환은 항상 `ApiResponse<T>`.
- 예외는 `BusinessException` + `ErrorCode`로만 던집니다(임의 `RuntimeException` 금지).
- 도메인 객체는 `@Setter`를 열지 않고 의미 있는 도메인 메서드로 상태를 변경합니다.
- `Optional`은 반환에만 쓰고 필드로 두지 않습니다.

## 커밋 컨벤션 & Git Flow

> 프론트엔드 저장소와 **완전히 동일한 규칙**을 사용합니다.

### 🦴 Commit Convention

커밋 메시지는 아래 형식으로 작성합니다.

```
<타입>:<내용>

ex) feat: 로그인 기능 추가
```

| 타입 | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 등 |
| `refactor` | 코드 리팩토링 (기능 변화 없음) |
| `test` | 테스트 코드 추가 또는 수정 |
| `chore` | 기타 변경사항 (빌드 설정, 패키지 등) |

### 🌙 Git Flow 브랜치 전략

| 브랜치 이름 | 용도 |
| --- | --- |
| `main` | 배포(Release)가 이루어지는 안정적인 코드 |
| `dev` | 다음 릴리스를 준비하는 개발 브랜치 |

브랜치 네이밍:

```
<타입>/<이슈번호>

ex) feat/#23
```

| 타입 | 설명 |
| --- | --- |
| `feat` | 새로운 기능 작업 |
| `fix` | 버그 수정 작업 |
| `hotfix` | 급한 수정 작업 (배포 후 등) |
| `refactor` | 코드 리팩토링 |
| `docs` | 문서 작업 |
| `chore` | 기타 작업 (설정, 패키지 등) |

### ☸️ 개발 프로세스

1. **기능 개발 시작** — `dev` 브랜치에서 새로운 `feat` 브랜치를 생성합니다.
2. **기능 개발 및 커밋** — `feat` 브랜치에서 기능을 완성하고 커밋합니다.
3. **코드 리뷰 및 병합** — `feat` → `dev`로 PR을 생성해 리뷰 후 병합합니다.
4. **테스트** — `dev` 브랜치에서 배포 전 최종 동작을 검증합니다.
5. **배포** — 테스트 완료 후 `dev`를 `main`에 병합해 배포합니다.

## 시작하기

### 요구 환경

- JDK 17 (Temurin 등 배포판 권장)
- Gradle (프로젝트의 Gradle Wrapper `./gradlew` 사용 권장)
- Oracle (메인 DB) + PostgreSQL(pgvector 확장, RAG용) 로컬 실행
- AWS 자격증명 (Spring AI Bedrock 사용 시 — IAM 역할 또는 기본 자격증명 체인)

### 설치 및 실행

```bash
git clone https://github.com/HDF-final/Ditto-BackEnd.git
cd Ditto-BackEnd

# 로컬 DB 준비
# - Oracle: 스키마(ditto) 및 테이블 생성 (MyBatis는 자동 DDL 없음 — SQL 스크립트로 관리)
# - PostgreSQL(RAG): CREATE DATABASE ditto_rag; CREATE EXTENSION IF NOT EXISTS vector;

# 비밀 설정
cp .env.example .env   # Windows: copy .env.example .env

# 실행 (local 프로파일)
./gradlew bootRun
```

로컬 기동은 Swagger·도메인 API 확인용입니다. Bedrock 임베딩과 pgvector VectorStore 자동설정은 RAG가 붙기 전까지 `none`이라 AWS/PostgreSQL 없이 서버가 뜹니다. Oracle은 `.env`의 `ORACLE_*`로 연결합니다.

Windows PowerShell에서는 다음을 사용합니다.

```bash
.\gradlew.bat bootRun
```

빌드만 하려면:

```bash
./gradlew clean build
```

### Swagger UI

애플리케이션 실행 후 아래 주소에서 API 문서를 확인합니다.

| 항목 | 경로 |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health Check | http://localhost:8080/actuator/health |

## 환경 설정 예시

`application.yml`에 구조만 두고, DB 계정·비밀번호는 프로젝트 루트 `.env`에 둡니다. `.env`는 Git에 커밋하지 않습니다. 처음에는 `.env.example`을 복사합니다.

```bash
copy .env.example .env
```

### `.env`

```env
ORACLE_JDBC_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
ORACLE_USERNAME=DITTO
ORACLE_PASSWORD=CHANGE_ME

# Gemini API
GEMINI_API_KEY=CHANGE_ME

# AI 엔진 base URL (생략 시 http://127.0.0.1:8000)
AI_ENGINE_BASE_URL=http://127.0.0.1:8000

# AWS RDS PostgreSQL (RAG 계층)
PG_HOST=CHANGE_ME.rds.amazonaws.com
PG_USER=postgres
PG_PASSWORD=CHANGE_ME
PG_DATABASE=postgres
PG_SSLMODE=require
```

### `application.yml` (비밀 없음)

```yaml
spring:
  datasource:
    oracle:
      jdbc-url: ${ORACLE_JDBC_URL}
      username: ${ORACLE_USERNAME}
      password: ${ORACLE_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver

  ai:                       # Spring AI — AWS Bedrock (자격증명은 IAM 역할/기본 체인)
    # RAG 빈이 붙기 전까지 임베딩·pgvector 자동설정을 끈다.
    # (Titan + Cohere 빈이 동시에 뜨면 VectorStore 기동이 실패한다)
    model:
      embedding: none
    vectorstore:
      type: none
    bedrock:
      aws:
        region: ap-northeast-2

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.ditto
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    root: INFO
    com.ditto: DEBUG
    org.mybatis: DEBUG
```

## 기타 기본 설정

### CORS

프론트엔드 개발 서버(`http://localhost:3000`)를 허용합니다. 운영 도메인은 배포 시 추가합니다. 설정은 [config/CorsConfig.java](./src/main/java/com/ditto/config/CorsConfig.java)에서 관리하며 `SecurityConfig`가 이를 사용합니다.

```java
config.setAllowedOrigins(List.of("http://localhost:3000"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
config.setAllowCredentials(true);
```

### TimeZone (Asia/Seoul)

- 애플리케이션 기동 시 `DittoApplication`의 `@PostConstruct`에서 JVM 기본 시간대를 `Asia/Seoul`로 고정합니다.
- Jackson 직렬화 시간대도 `application.yml`의 `spring.jackson.time-zone: Asia/Seoul`로 통일합니다.
- 날짜는 타임스탬프가 아닌 ISO-8601 문자열로 직렬화합니다(`write-dates-as-timestamps: false`).

### Logging

- 애플리케이션 코드(`com.ditto`)는 `DEBUG`, 나머지는 `INFO`가 기본입니다.
- SQL 로그는 로컬에서만 `DEBUG`로 확인합니다.
- 로그는 `@Slf4j`(Lombok)를 사용하며 `System.out.println`을 사용하지 않습니다.

### Health Check

- Spring Boot Actuator로 `GET /actuator/health`를 노출합니다.
- 노출 엔드포인트는 `health`, `info`로 제한하고, 상세 정보는 인증된 사용자에게만 표시합니다(`show-details: when-authorized`).

---

현재 문서는 초기 개발 환경 기준이며, 도메인 구현이 진행되면 각 API의 상세 스키마와 아키텍처 설명을 함께 확장합니다.
