# SinTower API 명세서 (프론트엔드 개발용)

> 최종 수정일: 2026-01-23

## 목차
1. [기본 정보](#기본-정보)
2. [응답 형식](#응답-형식)
3. [인증 시스템 (상세)](#인증-시스템-상세)
4. [API 엔드포인트](#api-엔드포인트)
5. [에러 코드](#에러-코드)
6. [TypeScript 타입 정의](#typescript-타입-정의)

---

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `https://api.sintower.com` (운영) / `http://localhost:8080` (개발) |
| Content-Type | `application/json` |
| 인증 방식 | JWT Bearer Token |

---

## 응답 형식

### 성공 응답
```json
{
  "success": true,
  "message": "선택적 메시지",
  "data": { /* 응답 데이터 */ },
  "timestamp": "2026-01-23T10:00:00"
}
```

### 실패 응답
```json
{
  "success": false,
  "error": {
    "code": "E2002",
    "message": "유효하지 않은 토큰입니다.",
    "detail": "선택적 상세 정보"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

---

## 인증 시스템 (상세)

### 인증 흐름 개요

```
┌─────────────┐     로그인 요청      ┌─────────────┐
│   Frontend  │ ──────────────────▶ │   Backend   │
│             │ ◀────────────────── │             │
└─────────────┘   accessToken 반환   └─────────────┘
       │
       │  토큰 저장 (메모리/localStorage)
       ▼
┌─────────────┐   Authorization 헤더  ┌─────────────┐
│   Frontend  │ ──────────────────▶ │   Backend   │
│             │  Bearer {token}      │  (보호된 API) │
└─────────────┘                      └─────────────┘
       │
       │  토큰 만료 시
       ▼
┌─────────────┐   토큰 갱신 요청     ┌─────────────┐
│   Frontend  │ ──────────────────▶ │   Backend   │
│             │ ◀────────────────── │             │
└─────────────┘   새 accessToken     └─────────────┘
```

### 토큰 관리 규칙

| 항목 | 값 | 설명 |
|------|-----|------|
| 토큰 유효기간 | 24시간 | 서버 설정에 따라 변경 가능 |
| 갱신 유예기간 | 7일 | 만료 후 7일 이내 갱신 가능 |
| 저장 위치 권장 | 메모리 또는 httpOnly 쿠키 | localStorage는 XSS 취약 |

### 프론트엔드 구현 가이드

#### 1. 로그인 후 토큰 저장
```typescript
// 로그인 API 호출 후
const response = await fetch('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});

const { data } = await response.json();
const accessToken = data.accessToken;

// 메모리에 저장 (권장)
authStore.setToken(accessToken);
```

#### 2. API 요청 시 토큰 첨부
```typescript
// 모든 보호된 API 요청에 Authorization 헤더 필수
const response = await fetch('/miniatures', {
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  }
});
```

#### 3. 토큰 갱신 로직 (Axios Interceptor 예시)
```typescript
// 응답 인터셉터에서 401 에러 처리
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // 401 에러이고, 재시도하지 않은 요청인 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 토큰 갱신 시도
        const refreshResponse = await axios.post('/auth/refresh', null, {
          headers: { 'Authorization': `Bearer ${currentToken}` }
        });

        const newToken = refreshResponse.data.data.accessToken;
        authStore.setToken(newToken);

        // 원래 요청 재시도
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return axios(originalRequest);

      } catch (refreshError) {
        // 갱신 실패 → 로그아웃 처리
        authStore.logout();
        router.push('/login');
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

#### 4. 앱 시작 시 토큰 검증
```typescript
// 앱 초기화 시 저장된 토큰이 있다면 갱신 시도
async function initializeAuth() {
  const storedToken = localStorage.getItem('accessToken');

  if (storedToken) {
    try {
      const response = await fetch('/auth/refresh', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${storedToken}` }
      });

      if (response.ok) {
        const { data } = await response.json();
        authStore.setToken(data.accessToken);
      } else {
        // 갱신 실패 → 토큰 삭제, 로그인 페이지로
        authStore.logout();
      }
    } catch (e) {
      authStore.logout();
    }
  }
}
```

---

## API 엔드포인트

### 1. 인증 (Auth)

#### POST /auth/register - 회원가입

**요청**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "닉네임"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| email | string | O | 이메일 형식 |
| password | string | O | 8~100자 |
| nickname | string | O | 2~50자 |

**응답 (201 Created)**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 이메일 중복 | E3001 | 409 |
| 입력값 오류 | E1001 | 400 |

---

#### POST /auth/login - 로그인

**요청**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| email | string | O | 이메일 형식 |
| password | string | O | - |

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 사용자 없음 | E3000 | 404 |
| 비밀번호 오류 | E3002 | 400 |

---

#### POST /auth/refresh - 토큰 갱신

**요청**
```
POST /auth/refresh
Authorization: Bearer {현재_토큰}
```
- Body 없음
- 만료된 토큰도 7일 이내면 갱신 가능

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP | 프론트엔드 처리 |
|------|------|------|----------------|
| 토큰 없음/형식 오류 | E2002 | 401 | 로그인 페이지로 이동 |
| 갱신 불가 (7일 초과) | E2002 | 401 | 로그인 페이지로 이동 |
| 사용자 삭제됨 | E3000 | 404 | 로그인 페이지로 이동 |

---

### 2. 미니어처 (Miniatures) - 인증 필요

> 모든 요청에 `Authorization: Bearer {token}` 헤더 필수

#### GET /miniatures - 내 백로그 목록 조회

**응답 (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "스페이스 마린",
      "isPublic": false,
      "progress": 40,
      "createdAt": "2026-01-20T10:00:00"
    }
  ],
  "timestamp": "2026-01-23T10:00:00"
}
```

---

#### POST /miniatures - 백로그 생성

**요청**
```json
{
  "title": "스페이스 마린",
  "description": "울트라마린 챕터 전술 분대"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| title | string | O | 최대 200자 |
| description | string | X | 최대 1000자 |

**응답 (201 Created)**
```json
{
  "success": true,
  "message": "백로그가 생성되었습니다.",
  "data": {
    "id": 1,
    "title": "스페이스 마린",
    "description": "울트라마린 챕터 전술 분대",
    "isPublic": false,
    "progress": 0,
    "backlogItems": [
      { "id": 1, "stepName": "조립", "status": "TODO", "orderIndex": 0 },
      { "id": 2, "stepName": "밑작업", "status": "TODO", "orderIndex": 1 },
      { "id": 3, "stepName": "기본 도색", "status": "TODO", "orderIndex": 2 },
      { "id": 4, "stepName": "세부 도색", "status": "TODO", "orderIndex": 3 },
      { "id": 5, "stepName": "마감", "status": "TODO", "orderIndex": 4 }
    ],
    "createdAt": "2026-01-23T10:00:00",
    "updatedAt": "2026-01-23T10:00:00"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

---

#### GET /miniatures/{id} - 백로그 상세 조회

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "스페이스 마린",
    "description": "울트라마린 챕터 전술 분대",
    "isPublic": false,
    "progress": 40,
    "backlogItems": [
      { "id": 1, "stepName": "조립", "status": "DONE", "orderIndex": 0 },
      { "id": 2, "stepName": "밑작업", "status": "DONE", "orderIndex": 1 },
      { "id": 3, "stepName": "기본 도색", "status": "IN_PROGRESS", "orderIndex": 2 },
      { "id": 4, "stepName": "세부 도색", "status": "TODO", "orderIndex": 3 },
      { "id": 5, "stepName": "마감", "status": "TODO", "orderIndex": 4 }
    ],
    "createdAt": "2026-01-20T10:00:00",
    "updatedAt": "2026-01-23T10:00:00"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 미니어처 없음 | E4000 | 404 |
| 접근 권한 없음 | E4001 | 403 |

---

### 3. 백로그 항목 (Backlog Items) - 인증 필요

#### PATCH /backlog-items/{id} - 상태 변경

**요청**
```json
{
  "status": "IN_PROGRESS"
}
```

| 필드 | 타입 | 필수 | 값 |
|------|------|------|-----|
| status | enum | O | `TODO`, `IN_PROGRESS`, `DONE` |

**응답 (200 OK)**
```json
{
  "success": true,
  "message": "상태가 변경되었습니다.",
  "data": {
    "id": 3,
    "stepName": "기본 도색",
    "status": "IN_PROGRESS",
    "orderIndex": 2,
    "progress": 40
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 항목 없음 | E5000 | 404 |
| 접근 권한 없음 | E4001 | 403 |

---

### 4. 진행 로그 (Progress Logs) - 인증 필요

#### POST /progress-logs - 진행 로그 작성

**요청**
```json
{
  "miniatureId": 1,
  "content": "오늘은 기본 도색을 시작했습니다. 블루 베이스코트 완료!",
  "isPublic": true
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| miniatureId | number | O | - |
| content | string | O | 최대 2000자 |
| isPublic | boolean | X | 기본값 false |

**응답 (201 Created)**
```json
{
  "success": true,
  "message": "진행 로그가 작성되었습니다.",
  "data": {
    "id": 1,
    "miniatureId": 1,
    "miniatureTitle": "스페이스 마린",
    "userId": 1,
    "userNickname": "닉네임",
    "content": "오늘은 기본 도색을 시작했습니다...",
    "isPublic": true,
    "createdAt": "2026-01-23T10:00:00"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 미니어처 없음 | E4000 | 404 |
| 접근 권한 없음 | E4001 | 403 |

---

#### GET /progress-logs - 내 진행 로그 조회

**파라미터**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| miniatureId | number | O | - | 미니어처 ID |
| page | number | X | 0 | 페이지 번호 (0부터 시작) |
| size | number | X | 10 | 페이지 크기 (1~100) |

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "miniatureId": 1,
        "miniatureTitle": "스페이스 마린",
        "userId": 1,
        "userNickname": "닉네임",
        "content": "오늘은 기본 도색을...",
        "isPublic": true,
        "createdAt": "2026-01-23T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

---

### 5. 공개 게시판 (Public) - 인증 불필요

#### GET /public/progress-logs - 공개 진행 로그 조회

**파라미터**
| 파라미터 | 타입 | 필수 | 기본값 |
|----------|------|------|--------|
| page | number | X | 0 |
| size | number | X | 10 |

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5,
        "miniatureId": 3,
        "miniatureTitle": "오크 워보스",
        "userId": 2,
        "userNickname": "다른사용자",
        "content": "WAAAGH! 워보스 도색 완료!",
        "isPublic": true,
        "createdAt": "2026-01-23T09:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

---

### 6. 이미지 (Images) - 인증 필요

#### POST /images/presign - Presigned URL 발급

클라이언트가 Cloudflare R2에 직접 업로드하기 위한 URL을 발급받습니다.

**요청**
```json
{
  "fileName": "progress_01.jpg",
  "contentType": "image/jpeg"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| fileName | string | O | 255자 이하, 영문/한글/숫자/._- 만 허용 |
| contentType | string | O | image/jpeg, image/png, image/gif, image/webp |

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://r2.cloudflarestorage.com/bucket/...",
    "objectKey": "users/1/550e8400-e29b-41d4-a716-446655440000_progress_01.jpg"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**이미지 업로드 플로우**
```typescript
// 1. Presigned URL 받기
const presignResponse = await api.post('/images/presign', {
  fileName: file.name,
  contentType: file.type
});
const { uploadUrl, objectKey } = presignResponse.data.data;

// 2. R2에 직접 업로드 (PUT 요청, 인증 헤더 없이)
await fetch(uploadUrl, {
  method: 'PUT',
  body: file,
  headers: { 'Content-Type': file.type }
});

// 3. 업로드 완료 후 메타데이터 저장 API 호출
await api.post('/images', {
  progressLogId: progressLogId,
  objectKey: objectKey
});
```

---

#### POST /images - 이미지 메타데이터 저장

R2 업로드 완료 후 호출하여 DB에 메타데이터를 저장합니다.

**요청**
```json
{
  "progressLogId": 1,
  "objectKey": "users/1/550e8400-e29b-41d4-a716-446655440000_progress_01.jpg"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
|------|------|------|----------|
| progressLogId | number | O | - |
| objectKey | string | O | presign에서 받은 값 사용 |

**응답 (201 Created)**
```json
{
  "success": true,
  "message": "이미지가 저장되었습니다.",
  "data": {
    "id": 1,
    "progressLogId": 1,
    "objectKey": "users/1/550e8400-e29b-41d4-a716-446655440000_progress_01.jpg",
    "fileName": "progress_01.jpg",
    "contentType": "image/jpeg",
    "createdAt": "2026-01-23T10:00:00"
  },
  "timestamp": "2026-01-23T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 진행 로그 없음 | E6000 | 404 |
| 잘못된 objectKey | E1001 | 400 |

---

## 에러 코드

### 공통 (1xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E1000 | 서버 내부 오류가 발생했습니다. | 500 | 서버 에러 |
| E1001 | 입력값이 올바르지 않습니다. | 400 | 유효성 검증 실패 |
| E1002 | 지원하지 않는 HTTP 메서드입니다. | 405 | 잘못된 HTTP 메서드 |
| E1003 | 요청한 리소스를 찾을 수 없습니다. | 404 | 존재하지 않는 경로 |

### 인증/인가 (2xxx)
| 코드 | 메시지 | HTTP | 프론트엔드 처리 |
|------|--------|------|----------------|
| E2000 | 인증이 필요합니다. | 401 | 로그인 페이지로 이동 |
| E2001 | 접근 권한이 없습니다. | 403 | 권한 없음 안내 표시 |
| E2002 | 유효하지 않은 토큰입니다. | 401 | 토큰 갱신 시도 → 실패 시 로그아웃 |
| E2003 | 만료된 토큰입니다. | 401 | 토큰 갱신 시도 |

### 사용자 (3xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E3000 | 사용자를 찾을 수 없습니다. | 404 | 존재하지 않는 사용자 |
| E3001 | 이미 사용 중인 이메일입니다. | 409 | 이메일 중복 |
| E3002 | 비밀번호가 올바르지 않습니다. | 400 | 로그인 실패 |

### 미니어처 (4xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E4000 | 미니어처를 찾을 수 없습니다. | 404 | 존재하지 않는 미니어처 |
| E4001 | 해당 미니어처에 접근 권한이 없습니다. | 403 | 다른 사용자의 미니어처 |

### 백로그 아이템 (5xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E5000 | 백로그 항목을 찾을 수 없습니다. | 404 | 존재하지 않는 항목 |

### 진행 로그 (6xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E6000 | 진행 로그를 찾을 수 없습니다. | 404 | 존재하지 않는 로그 |

### 이미지 (7xxx)
| 코드 | 메시지 | HTTP | 설명 |
|------|--------|------|------|
| E7000 | 이미지를 찾을 수 없습니다. | 404 | 존재하지 않는 이미지 |
| E7001 | 이미지 업로드에 실패했습니다. | 500 | R2 업로드 실패 |

---

## TypeScript 타입 정의

프론트엔드에서 사용할 수 있는 TypeScript 타입 정의입니다.

```typescript
// ==================== 공통 ====================

/** API 공통 응답 */
interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: ApiError;
  timestamp: string;
}

/** API 에러 */
interface ApiError {
  code: string;
  message: string;
  detail?: string;
}

/** 페이지네이션 응답 */
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// ==================== 인증 ====================

/** 로그인 요청 */
interface LoginRequest {
  email: string;
  password: string;
}

/** 회원가입 요청 */
interface RegisterRequest {
  email: string;
  password: string;
  nickname: string;
}

/** 토큰 응답 */
interface TokenResponse {
  accessToken: string;
}

// ==================== 미니어처 ====================

/** 미니어처 목록 응답 */
interface MiniatureResponse {
  id: number;
  title: string;
  isPublic: boolean;
  progress: number;
  createdAt: string;
}

/** 미니어처 상세 응답 */
interface MiniatureDetailResponse {
  id: number;
  title: string;
  description?: string;
  isPublic: boolean;
  progress: number;
  backlogItems: BacklogItemResponse[];
  createdAt: string;
  updatedAt: string;
}

/** 미니어처 생성 요청 */
interface MiniatureCreateRequest {
  title: string;
  description?: string;
}

// ==================== 백로그 아이템 ====================

/** 백로그 아이템 상태 */
type BacklogItemStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

/** 백로그 아이템 응답 */
interface BacklogItemResponse {
  id: number;
  stepName: string;
  status: BacklogItemStatus;
  orderIndex: number;
  progress?: number; // 상태 변경 후 전체 진행률
}

/** 백로그 아이템 상태 변경 요청 */
interface BacklogItemUpdateRequest {
  status: BacklogItemStatus;
}

// ==================== 진행 로그 ====================

/** 진행 로그 응답 */
interface ProgressLogResponse {
  id: number;
  miniatureId: number;
  miniatureTitle: string;
  userId: number;
  userNickname: string;
  content: string;
  isPublic: boolean;
  createdAt: string;
}

/** 진행 로그 생성 요청 */
interface ProgressLogCreateRequest {
  miniatureId: number;
  content: string;
  isPublic?: boolean;
}

/** 진행 로그 페이지 응답 */
type ProgressLogPageResponse = PageResponse<ProgressLogResponse>;

// ==================== 이미지 ====================

/** Presigned URL 요청 */
interface PresignRequest {
  fileName: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/gif' | 'image/webp';
}

/** Presigned URL 응답 */
interface PresignResponse {
  uploadUrl: string;
  objectKey: string;
}

/** 이미지 메타데이터 저장 요청 */
interface ImageCreateRequest {
  progressLogId: number;
  objectKey: string;
}

/** 이미지 응답 */
interface ImageResponse {
  id: number;
  progressLogId: number;
  objectKey: string;
  fileName: string;
  contentType: string;
  createdAt: string;
}
```

---

## API 호출 예시 (Fetch)

```typescript
const API_BASE_URL = 'http://localhost:8080';

// 공통 fetch 헬퍼
async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const token = authStore.getToken();

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers,
    },
  });

  const data = await response.json();

  if (!response.ok) {
    throw data;
  }

  return data;
}

// 사용 예시
// 로그인
const loginResult = await apiRequest<TokenResponse>('/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email: 'user@example.com', password: 'password123' })
});

// 미니어처 목록 조회
const miniatures = await apiRequest<MiniatureResponse[]>('/miniatures');

// 미니어처 생성
const newMiniature = await apiRequest<MiniatureDetailResponse>('/miniatures', {
  method: 'POST',
  body: JSON.stringify({ title: '스페이스 마린', description: '울트라마린' })
});
```
