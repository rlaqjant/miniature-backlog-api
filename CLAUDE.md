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

- JWT 기반 인증
- 보호된 API는 `Authorization: Bearer <JWT>` 헤더 필수
- 개인 백로그: 소유자만 접근
- 공개 로그: 전체 공개

## 이미지 처리

- 파일은 Cloudflare R2에 저장
- 백엔드는 presigned URL만 발급
- 파일 업로드 트래픽은 백엔드 미경유

## CORS 정책

- 허용 Origin: 프론트엔드 도메인, 개발용 localhost
- JWT 방식 → Allow-Credentials 비활성화

## 패키지명 참고

원래 패키지명 `com.rlaqjant.miniature-backlog-api`가 유효하지 않아 `com.rlaqjant.miniature_backlog_api` 사용.,....
