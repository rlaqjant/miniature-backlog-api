# SinTower - 죄악의 탑: 백엔드 API 로드맵

## 현재 진행률: 25% (Phase 2/8 완료)

> 이 문서는 `doc/miniature_backlog_prd_and_api.md` PRD를 기반으로 작성되었습니다.
> 마지막 업데이트: 2026-01-23

---

## 기능 요약

| 도메인 | API 엔드포인트 | 상태 |
|--------|---------------|------|
| Health | `GET /health` | ✅ 완료 |
| Auth | `POST /auth/login` | ✅ 완료 |
| Auth | `POST /auth/register` (회원가입) | ✅ 완료 |
| Miniature | `GET /miniatures` | 미시작 |
| Miniature | `POST /miniatures` | 미시작 |
| Miniature | `GET /miniatures/{id}` | 미시작 |
| BacklogItem | `PATCH /backlog-items/{id}` | 미시작 |
| ProgressLog | `POST /progress-logs` | 미시작 |
| ProgressLog | `GET /public/progress-logs` | 미시작 |
| Image | `POST /images/presign` | 미시작 |
| Image | `POST /images` | 미시작 |

---

## Phase 1: 프로젝트 기반 설정 (Foundation) ✅ 완료

프로젝트의 핵심 인프라와 기본 설정을 구축합니다.

### 백엔드
- [x] Spring Boot Web 의존성 추가
- [x] PostgreSQL/JPA 의존성 및 설정
- [x] 환경 변수 설정 (application.yml / profiles)
- [x] CORS 설정 (프론트엔드 도메인 허용)
- [x] 공통 에러 응답 구조 (GlobalExceptionHandler)
- [x] Health Check API (`GET /health`)
- [x] API 응답 공통 포맷 정의

### 완료 기준
- ✅ 애플리케이션이 정상 기동됨
- ✅ `/health` 엔드포인트가 `ok` 응답
- ✅ PostgreSQL 연결 설정 완료

### 생성된 파일
- `src/main/java/com/rlaqjant/miniature_backlog_api/config/CorsConfig.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/config/WebMvcConfig.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/common/dto/ApiResponse.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/common/exception/ErrorCode.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/common/exception/BusinessException.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/health/HealthController.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/health/HealthService.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/health/HealthResponse.java`
- `src/main/resources/application.yaml` (수정)
- `src/test/resources/application.yaml` (생성)

---

## Phase 2: 사용자 인증 (Authentication) ✅ 완료

JWT 기반 인증 시스템을 구축합니다.

### 백엔드
- [x] User 엔티티 설계 및 생성
- [x] Spring Security 설정
- [x] JWT 토큰 발급/검증 유틸리티
- [x] `POST /auth/register` - 회원가입 API
- [x] `POST /auth/login` - 로그인 API
- [x] JWT 인증 필터 구현
- [x] 인증 실패 시 401 응답 처리
- [x] 비밀번호 암호화 (BCrypt)

### 완료 기준
- ✅ 회원가입 후 로그인 가능
- ✅ JWT 토큰 발급 및 검증 동작
- ✅ 보호된 API 접근 시 인증 확인

### 생성된 파일
- `src/main/java/com/rlaqjant/miniature_backlog_api/user/domain/User.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/user/repository/UserRepository.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/security/jwt/JwtTokenProvider.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/security/jwt/JwtAuthenticationFilter.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/security/userdetails/CustomUserDetails.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/security/userdetails/CustomUserDetailsService.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/security/handler/JwtAuthenticationEntryPoint.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/config/SecurityConfig.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/auth/dto/RegisterRequest.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/auth/dto/LoginRequest.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/auth/dto/TokenResponse.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/auth/service/AuthService.java`
- `src/main/java/com/rlaqjant/miniature_backlog_api/auth/controller/AuthController.java`

### 수정된 파일
- `build.gradle` - Spring Security, JWT 의존성 추가
- `src/main/resources/application.yaml` - JWT 설정 추가
- `src/test/resources/application.yaml` - 테스트용 JWT 설정 추가
- `.env.example` - JWT_SECRET 환경변수 추가

---

## Phase 3: 백로그 관리 - Miniature (Core Feature)

핵심 도메인인 미니어처 백로그 CRUD를 구현합니다.

### 백엔드
- [ ] Miniature 엔티티 설계 및 생성
- [ ] BacklogItem 엔티티 설계 및 생성 (진행 단계)
- [ ] Miniature Repository 구현
- [ ] BacklogItem Repository 구현
- [ ] `GET /miniatures` - 내 백로그 목록 조회
- [ ] `POST /miniatures` - 백로그 생성
- [ ] `GET /miniatures/{id}` - 백로그 상세 조회
- [ ] 소유권 검증 로직 (본인 백로그만 접근)
- [ ] 백로그 생성 시 기본 BacklogItem 자동 생성

### 완료 기준
- 로그인 사용자가 백로그 생성/조회 가능
- 타인의 백로그 접근 시 403 응답
- 백로그 생성 시 기본 단계 항목 자동 생성

---

## Phase 4: 진행 상태 관리 - BacklogItem

미니어처의 단계별 진행 상태를 관리합니다.

### 백엔드
- [ ] BacklogItem 상태 enum 정의 (TODO, IN_PROGRESS, DONE 등)
- [ ] `PATCH /backlog-items/{id}` - 단계 상태 변경
- [ ] 상태 변경 시 진행률 자동 계산
- [ ] 소유권 검증 (해당 Miniature 소유자만 변경 가능)

### 완료 기준
- 백로그 단계 상태 변경 가능
- 진행률이 자동 계산되어 반환

---

## Phase 5: 진행 로그 - ProgressLog

진행 기록 작성 및 공개 게시판 기능을 구현합니다.

### 백엔드
- [ ] ProgressLog 엔티티 설계 및 생성
- [ ] ProgressLog Repository 구현
- [ ] `POST /progress-logs` - 진행 로그 작성
- [ ] `GET /progress-logs` - 내 진행 로그 목록 (특정 Miniature)
- [ ] `GET /public/progress-logs` - 공개 게시판 조회
- [ ] 공개/비공개 설정 로직
- [ ] 페이지네이션 구현
- [ ] 최신순 정렬

### 완료 기준
- 진행 로그 작성 및 공개 설정 가능
- 공개 게시판에서 공개 로그만 조회
- 페이지네이션 동작

---

## Phase 6: 이미지 업로드 - Image

Cloudflare R2 연동 및 이미지 메타데이터 관리를 구현합니다.

### 백엔드
- [ ] Image 엔티티 설계 및 생성
- [ ] Cloudflare R2 SDK 연동
- [ ] `POST /images/presign` - presigned URL 발급
- [ ] `POST /images` - 이미지 메타데이터 저장
- [ ] 이미지-ProgressLog 연결 로직
- [ ] 이미지 접근 권한 검증 (공개 여부에 따라)

### 완료 기준
- presigned URL 발급 및 R2 업로드 가능
- 이미지 메타데이터가 진행 로그와 연결
- 공개 이미지만 외부 접근 허용

---

## Phase 7: 고도화 및 최적화 (Enhancement)

서비스 안정성과 사용성을 개선합니다.

### 백엔드
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 입력값 유효성 검증 강화
- [ ] 로깅 및 모니터링 설정
- [ ] Rate Limiting 적용
- [ ] 테스트 코드 작성 (단위/통합)
- [ ] Docker 배포 설정 최적화

### 완료 기준
- API 문서 자동 생성
- 주요 기능 테스트 커버리지 확보
- 프로덕션 배포 준비 완료

---

## Phase 8: 확장 기능 (Future)

PRD에 언급된 향후 확장 기능입니다.

### 백엔드
- [ ] 댓글 기능
- [ ] 좋아요 기능
- [ ] 태그 기능
- [ ] 검색 기능
- [ ] 통계 기능

### 완료 기준
- 각 기능별 API 구현 및 테스트 완료

---

## 의존성 다이어그램

```
Phase 1 (기반 설정) ✅
    │
    ▼
Phase 2 (인증) ✅ ◄─── 모든 보호된 API의 선행 조건
    │
    ▼
Phase 3 (Miniature) ◄─── Phase 4, 5, 6의 선행 조건
    │
    ├──► Phase 4 (BacklogItem)
    │
    ├──► Phase 5 (ProgressLog) ◄─── Phase 6의 선행 조건
    │         │
    │         ▼
    │    Phase 6 (Image)
    │
    ▼
Phase 7 (고도화) - 병렬 진행 가능
    │
    ▼
Phase 8 (확장) - 우선순위에 따라 선택적 구현
```

---

## 다음 작업 추천

**현재 권장 작업**: Phase 3 - 백로그 관리 (Miniature)

1. Miniature 엔티티 설계 및 생성
   - id, userId, name, description, isPublic, progress, createdAt, updatedAt

2. BacklogItem 엔티티 설계 및 생성
   - id, miniatureId, stepName, status, orderIndex

3. Repository 구현
   - MiniatureRepository
   - BacklogItemRepository

4. API 구현
   - `GET /miniatures` - 내 백로그 목록 조회
   - `POST /miniatures` - 백로그 생성
   - `GET /miniatures/{id}` - 백로그 상세 조회

5. 소유권 검증 로직 구현

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-22 | 초기 로드맵 작성 - PRD 기반 전체 기능 목록 및 Phase 정의 |
| 2026-01-22 | Phase 1 완료 - 기반 설정, CORS, 에러 처리, Health API |
| 2026-01-23 | Phase 2 완료 - JWT 인증, 회원가입/로그인 API, Spring Security |
