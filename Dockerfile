# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle 래퍼 복사 및 의존성 다운로드 (캐시 활용)
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안: 비루트 사용자로 실행
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
USER appuser

# JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

# 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
