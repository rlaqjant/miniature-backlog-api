# frontend-prd.md

## Frontend PRD
미니어처 백로그 추적 서비스

가제 : SinTower - 죄악의 탑

### 기술 스택
Vite/React/TypeScript

### 1. 목적
- 사용자가 개인 미니어처 도색 백로그를 시각적으로 관리할 수 있도록 한다.
- 개인 백로그와 공개 게시판을 명확히 분리된 UX로 제공한다.
- 프론트엔드는 UI/UX와 사용자 상호작용에만 집중한다.

### 2. 배포 환경
- 배포 플랫폼: Cloudflare Pages
- 서비스 형태: 정적 프론트엔드
- API Base URL: `https://api.example.com`

#### 환경 변수
```env
NEXT_PUBLIC_API_BASE_URL=https://api.example.com
```

### 3. 사용자 유형
| 사용자 | 권한 |
|------|------|
| 비로그인 사용자 | 공개 게시판 조회 |
| 로그인 사용자 | 개인 백로그 관리, 공개 게시글 작성 |

### 4. 주요 화면

#### 4.1 랜딩 페이지
- 서비스 소개
- 주요 기능 설명
- 로그인 / 회원가입 버튼

#### 4.2 로그인 / 회원가입
- 이메일 + 비밀번호 기반
- 로그인 성공 시 JWT 토큰 저장
- 이후 API 요청에 Authorization 헤더 포함

#### 4.3 개인 백로그 대시보드
- 로그인 필수
- 사용자 소유 미니어처 목록 표시
- 각 항목:
  - 제목
  - 현재 진행 단계
  - 전체 진행률
- 기능:
  - 백로그 추가
  - 상태 변경
  - 상세 페이지 이동

#### 4.4 백로그 상세 페이지
- 미니어처 정보
- 단계별 진행 상태
- 진행 로그 목록
- 이미지 업로드 UI
- 공개 여부 설정 토글

#### 4.5 공개 게시판
- 공개 설정된 진행 로그 목록
- 최신순 정렬
- 썸네일 이미지 표시

#### 4.6 공개 게시글 상세
- 작성자 닉네임
- 진행 로그 텍스트
- 첨부 이미지

### 5. 프론트엔드 책임 범위

#### 수행
- UI 렌더링
- 사용자 입력 처리
- API 호출
- JWT 전달
- 이미지 업로드 UI 제공

#### 비수행
- 인증/권한 판단
- 데이터 검증
- 비즈니스 로직
- 이미지 파일 저장

### 6. 이미지 업로드 UX
1. 백엔드에 presigned URL 요청
2. Object Storage로 직접 업로드
3. 업로드 완료 후 메타데이터 저장 요청

### 7. 에러 처리
- 401: 로그인 페이지 이동
- 403: 접근 불가 안내
- 500: 공통 에러 UI

### 8. 비기능 요구사항
- 반응형 UI
- 빠른 초기 로딩
- 공개 페이지 SEO 고려

---

# backend-prd.md

## Backend PRD
미니어처 백로그 추적 서비스

가제 : SinTower - 죄악의 탑

### 기술스택
Spring/PostgreSQL

### 1. 목적
- 서비스의 모든 비즈니스 로직과 데이터 무결성을 책임진다.
- 프론트엔드를 신뢰하지 않는다.
- 개인 데이터와 공개 데이터를 명확히 구분한다.

### 2. 배포 환경
- Framework: Spring Boot
- 배포 플랫폼: Render (Docker)
- API Domain: `https://api.example.com`
- Database: Neon (PostgreSQL)
- Object Storage: Cloudflare R2

### 3. 인증 정책
- JWT 기반 인증
- 보호된 API는 Authorization 헤더 필수

```http
Authorization: Bearer <JWT>
```

### 4. 주요 도메인

#### User
- 계정 및 인증 주체

#### Miniature
- 사용자 소유 백로그 단위
- 공개 여부 포함

#### BacklogItem
- 미니어처의 단계별 진행 상태

#### ProgressLog
- 진행 기록
- 공개/비공개 설정 가능

#### Image
- object storage key만 저장

### 5. 백엔드 책임
- 인증/인가
- 데이터 CRUD
- 공개 게시판 조회
- presigned URL 발급
- 소유권 및 권한 검증

### 6. 공개 정책
| 데이터 | 접근 |
|------|------|
| 개인 백로그 | 소유자만 |
| 공개 로그 | 전체 공개 |
| 이미지 | 공개 여부에 따라 제어 |

### 7. 이미지 처리 정책
- 파일은 Object Storage에 저장
- 백엔드는 presigned URL만 발급
- 파일 업로드 트래픽은 백엔드 미경유

### 8. CORS 정책
- 허용 Origin:
  - https://app.example.com
  - 개발용 localhost
- JWT 방식 → Allow-Credentials 비활성화

### 9. 에러 정책
| 코드 | 의미 |
|----|----|
| 400 | 입력 오류 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |

### 10. 확장 고려
- 댓글 / 좋아요
- 태그 / 검색
- 통계 기능

---

# api-spec.md

## API Specification
미니어처 백로그 추적 서비스

가제 : SinTower - 죄악의 탑

Base URL: `https://api.example.com`

### 1. Auth

#### POST /auth/login
로그인

Request:
```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Response:
```json
{
  "accessToken": "jwt-token"
}
```

---

### 2. Miniature (백로그)

#### GET /miniatures
내 백로그 목록 조회

Header:
```
Authorization: Bearer <JWT>
```

Response:
```json
[
  {
    "id": "uuid",
    "title": "Space Marine",
    "isPublic": false,
    "progress": 60
  }
]
```

---

#### POST /miniatures
백로그 생성

Request:
```json
{
  "title": "Ork Boy",
  "description": "첫 오크 도색"
}
```

---

#### GET /miniatures/{id}
백로그 상세 조회

---

### 3. Backlog Items

#### PATCH /backlog-items/{id}
단계 상태 변경

Request:
```json
{
  "status": "DONE"
}
```

---

### 4. Progress Logs

#### POST /progress-logs
진행 로그 작성

Request:
```json
{
  "miniatureId": "uuid",
  "content": "베이스 코트 완료",
  "isPublic": true
}
```

---

#### GET /public/progress-logs
공개 게시판 조회

Response:
```json
[
  {
    "id": "uuid",
    "author": "nickname",
    "content": "도색 완료",
    "createdAt": "2026-01-01T12:00:00Z"
  }
]
```

---

### 5. Image Upload

#### POST /images/presign
업로드용 presigned URL 발급

Request:
```json
{
  "fileName": "image.png",
  "contentType": "image/png"
}
```

Response:
```json
{
  "uploadUrl": "https://r2....",
  "objectKey": "users/123/image.png"
}
```

---

#### POST /images
이미지 메타데이터 저장

Request:
```json
{
  "progressLogId": "uuid",
  "objectKey": "users/123/image.png"
}
```

---

### 6. Health

#### GET /health
서버 상태 확인

Response:
```
ok
```

