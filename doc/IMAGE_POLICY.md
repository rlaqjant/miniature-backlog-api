# 이미지 URL 정책

## 개요

미니어처 백로그 서비스의 이미지 저장 및 접근 정책을 정의합니다.
모든 이미지는 Cloudflare R2에 저장되며, 접근 방식은 공개/비공개 여부에 따라 다릅니다.

---

## URL 유형별 정책

| 유형 | 용도 | 유효 시간 | URL 형식 |
|------|------|----------|----------|
| **PUT Presigned URL** | 이미지 업로드 | 15분 | `https://{account}.r2.cloudflarestorage.com/...?X-Amz-Signature=...` |
| **GET Presigned URL** | 비공개 이미지 조회 | 15분 | `https://{account}.r2.cloudflarestorage.com/...?X-Amz-Signature=...` |
| **공개 URL** | 공개 게시판 이미지 | 무제한 | `https://{R2_PUBLIC_URL_BASE}/{objectKey}` |

---

## API별 적용 정책

### 이미지 업로드

| API | URL 유형 | 설명 |
|-----|----------|------|
| `POST /images/presign` | PUT Presigned URL (15분) | 클라이언트가 R2에 직접 업로드 |
| `POST /images` | GET Presigned URL (15분) | 업로드 완료 후 메타데이터 저장, imageUrl 반환 |

### Presign 요청 형식

```json
{
  "contentType": "image/png"
}
```

**참고**: `fileName` 필드는 더 이상 필요하지 않습니다. 서버에서 UUID 기반 파일명을 자동 생성합니다.

### 진행 로그 조회

| API | URL 유형 | 설명 |
|-----|----------|------|
| `GET /miniatures/{id}/progress-logs` | GET Presigned URL (15분) | 내 진행 로그 (비공개) |
| `GET /public/progress-logs` | 공개 URL | 공개 게시판 (CDN 캐시 활용) |

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `R2_ENDPOINT_URL` | - | R2 API 엔드포인트 |
| `R2_ACCESS_KEY_ID` | - | IAM 액세스 키 |
| `R2_SECRET_ACCESS_KEY` | - | IAM 시크릿 키 |
| `R2_BUCKET_NAME` | `miniature-backlog` | 버킷명 |
| `R2_PRESIGN_EXPIRATION_MINUTES` | `15` | Presigned URL 유효 시간 (분) |
| `R2_PUBLIC_URL_BASE` | (없음) | 공개 URL 기본 경로 |

---

## 공개 URL 설정 방법

### 옵션 A: R2.dev 서브도메인 (권장)

1. Cloudflare 대시보드 → R2 → 버킷 선택
2. 설정 → "r2.dev 서브도메인" 활성화
3. 환경 변수 설정:
   ```bash
   R2_PUBLIC_URL_BASE=https://pub-{hash}.r2.dev
   ```

### 옵션 B: 커스텀 도메인

1. Cloudflare DNS에 서브도메인 추가 (예: `cdn.sintower.com`)
2. R2 버킷에 커스텀 도메인 연결
3. 환경 변수 설정:
   ```bash
   R2_PUBLIC_URL_BASE=https://cdn.sintower.com
   ```

### 폴백 동작

`R2_PUBLIC_URL_BASE`가 설정되지 않은 경우, 공개 게시판에서도 Presigned URL을 사용합니다.
이 경우 CDN 캐싱의 이점을 활용할 수 없습니다.

---

## 캐시 전략

### 공개 이미지 (공개 URL 사용 시)

- **CDN 캐시**: Cloudflare CDN에서 자동 캐싱
- **권장 Cache-Control**: `public, max-age=31536000, immutable`
- **캐시 무효화**: 이미지 경로에 UUID가 포함되어 있어 동일 파일명이라도 고유 URL 보장

### 비공개 이미지 (Presigned URL)

- **캐시 불가**: 매 요청마다 다른 서명이 포함된 URL
- **의도적 설계**: 비공개 이미지는 캐시되지 않아야 함

---

## Object Key 형식

```
users/{userId}/{UUID}.{ext}
```

**예시**:
```
users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
```

**보안 특징**:
- 사용자 ID 포함 → 소유권 검증 가능
- UUID 기반 파일명 → 파일명 충돌 방지, 파일명 유추 불가
- 확장자만 유지 → 한글 파일명 인코딩 이슈 원천 차단
- Path Traversal 완전 방지 → 사용자 입력 파일명 미사용

---

## 보안 고려사항

1. **Object Key 소유권 검증**: 이미지 저장 시 인증된 사용자 ID와 Object Key의 사용자 ID 일치 여부 확인
2. **UUID 기반 파일명**: 서버에서 자동 생성하여 악의적 파일명 입력 원천 차단
3. **ContentType 검증**: 허용된 이미지 타입만 업로드 가능 (`image/png`, `image/jpeg`, `image/gif`, `image/webp`)
4. **Presigned URL 짧은 유효시간**: 15분으로 제한하여 URL 유출 시 피해 최소화

---

## 응답 예시

### Presigned URL 발급 요청

```bash
curl -X POST http://localhost:8080/images/presign \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=..." \
  -d '{"contentType": "image/png"}'
```

### Presigned URL 발급 응답

```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://account.r2.cloudflarestorage.com/bucket/users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png?X-Amz-Algorithm=...",
    "objectKey": "users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png"
  }
}
```

### 비공개 이미지 (Presigned URL)

```json
{
  "id": 1,
  "progressLogId": 10,
  "objectKey": "users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "fileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "contentType": "image/png",
  "imageUrl": "https://account.r2.cloudflarestorage.com/bucket/users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png?X-Amz-Signature=...",
  "createdAt": "2025-01-25T10:00:00"
}
```

### 공개 이미지 (공개 URL)

```json
{
  "id": 1,
  "progressLogId": 10,
  "objectKey": "users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "fileName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "contentType": "image/png",
  "imageUrl": "https://pub-hash.r2.dev/users/5/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "createdAt": "2025-01-25T10:00:00"
}
```
