# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

미니어처 백로그 추적 서비스 "SinTower - 죄악의 탑"의 **백엔드 API 서버**.
사용자가 미니어처 도색 백로그를 관리하고, 진행 상황을 공유할 수 있는 서비스.

## 서비스 아키텍처

Web과 API는 별도 서버로 분리:
- **Frontend (Web)**: Cloudflare Pages 배포 (별도 저장소)
- **Backend (API)**: 이 저장소 - Render (Docker) 배포

## 기술 스택

- Java 17
- Spring Boot 4.0.1
- Gradle (Groovy DSL)
- PostgreSQL (Neon)
- Object Storage: Cloudflare R2

## 빌드 및 실행 명령어

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "클래스명.메서드명"

# 애플리케이션 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build
```

## 주요 도메인

- **User**: 계정 및 인증 주체
- **Miniature**: 사용자 소유 백로그 단위 (공개 여부 포함)
- **BacklogItem**: 미니어처의 단계별 진행 상태
- **ProgressLog**: 진행 기록 (공개/비공개 설정 가능)
- **Image**: Object Storage key만 저장

## API 설계 참고

API 명세는 `doc/miniature_backlog_prd_and_api.md` 참조.

주요 엔드포인트:
- `POST /auth/login` - 로그인 (JWT 발급)
- `GET/POST /miniatures` - 백로그 목록/생성
- `PATCH /backlog-items/{id}` - 단계 상태 변경
- `POST /progress-logs` - 진행 로그 작성
- `GET /public/progress-logs` - 공개 게시판
- `POST /images/presign` - presigned URL 발급

## 인증/인가

- JWT 기반 인증 (HttpOnly 쿠키 방식)
- 로그인 시 `Set-Cookie` 헤더로 토큰 전달 (`access_token`)
- 인증이 필요한 API 호출 시 쿠키 자동 전송 (프론트엔드에서 `credentials: 'include'` 필요)
- Authorization 헤더 방식도 하위 호환성 유지
- 개인 백로그: 소유자만 접근
- 공개 로그: 전체 공개

### 인증 관련 엔드포인트

| 엔드포인트 | 설명 |
|------------|------|
| `POST /auth/login` | 로그인, 성공 시 `Set-Cookie`로 토큰 전달 |
| `POST /auth/refresh` | 쿠키의 토큰 갱신 |
| `POST /auth/logout` | 쿠키 삭제 (로그아웃) |
| `POST /auth/register` | 회원가입 |

## 이미지 처리

- 파일은 Cloudflare R2에 저장
- 백엔드는 presigned URL만 발급
- 파일 업로드 트래픽은 백엔드 미경유

## CORS 정책

- 허용 Origin: 프론트엔드 도메인, 개발용 localhost
- HttpOnly 쿠키 사용 → `Allow-Credentials: true`
- 프론트엔드에서 `credentials: 'include'` 옵션 필수

## 패키지명 참고

원래 패키지명 `com.rlaqjant.miniature-backlog-api`가 유효하지 않아 `com.rlaqjant.miniature_backlog_api` 사용.

---

## 코드 작성 가이드라인

### 패키지 구조

```
com.rlaqjant.miniature_backlog_api/
├── config/           # 설정 클래스 (Security, CORS, WebMvc)
├── common/
│   ├── dto/          # 공통 DTO (ApiResponse)
│   └── exception/    # 예외 처리 (ErrorCode, BusinessException, GlobalExceptionHandler)
├── security/
│   ├── jwt/          # JWT 관련 (JwtTokenProvider, JwtAuthenticationFilter, JwtCookieUtil)
│   ├── userdetails/  # UserDetails 구현
│   └── handler/      # 인증/인가 핸들러
├── auth/             # 인증 도메인
│   ├── controller/
│   ├── service/
│   └── dto/
├── user/             # 사용자 도메인
│   ├── domain/       # 엔티티
│   └── repository/
├── miniature/        # 미니어처 도메인 (Phase 3)
│   ├── domain/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
├── backlogitem/      # 백로그 항목 도메인 (Phase 4)
├── progresslog/      # 진행 로그 도메인 (Phase 5)
├── image/            # 이미지 도메인 (Phase 6)
└── health/           # 헬스체크
```

### 도메인 구현 패턴

각 도메인은 다음 순서로 구현:

1. **Entity** (`domain/`)
   - `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
   - `@PrePersist`, `@PreUpdate`로 시간 자동 관리
   - Lombok: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Builder`

2. **Repository** (`repository/`)
   - `JpaRepository<Entity, Long>` 상속
   - 필요한 쿼리 메서드 정의

3. **DTO** (`dto/`)
   - Request: `@Valid` 어노테이션과 함께 사용
   - Response: 엔티티와 분리된 응답 객체

4. **Service** (`service/`)
   - `@Service`, `@Transactional(readOnly = true)`
   - 쓰기 작업은 `@Transactional`

5. **Controller** (`controller/`)
   - `@RestController`, `@RequestMapping`
   - 응답은 `ApiResponse<T>` 래핑

### API 응답 포맷

```java
// 성공 응답 (데이터 포함)
return ResponseEntity.ok(ApiResponse.success(data));

// 성공 응답 (메시지 포함)
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success("생성되었습니다.", data));

// 실패 응답은 GlobalExceptionHandler에서 처리
throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
```

### 에러 코드 규칙

| 범위 | 도메인 |
|------|--------|
| E1xxx | 공통 에러 |
| E2xxx | 인증/인가 |
| E3xxx | 사용자 |
| E4xxx | 미니어처 |
| E5xxx | 백로그 아이템 |
| E6xxx | 진행 로그 |
| E7xxx | 이미지 |

### 인증이 필요한 API에서 현재 사용자 조회

```java
@GetMapping
public ResponseEntity<?> getMyData(@AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    // ...
}
```

### 소유권 검증 패턴

```java
public void validateOwnership(Miniature miniature, Long userId) {
    if (!miniature.getUserId().equals(userId)) {
        throw new BusinessException(ErrorCode.MINIATURE_ACCESS_DENIED);
    }
}
```

### 로컬 서버 실행

```bash
# 환경변수 로드 후 실행
export $(grep -v '^#' .env | xargs) && ./gradlew bootRun
```

### 빌드 및 테스트

```bash
# 빌드 (테스트 포함)
./gradlew clean build

# 테스트만 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "AuthServiceTest"
```

---

## 현재 진행 상태

- **완료**: Phase 1 (기반 설정), Phase 2 (인증)
- **다음**: Phase 3 (Miniature 백로그 CRUD)
- **참고**: `doc/ROADMAP.md`에서 상세 진행 상황 확인.
