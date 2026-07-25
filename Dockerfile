# 1. 빌드 - JDK 17 이미지 기반
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 프로젝트 모든 파일 컨테이너 안으로 복사
COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 2. 실행
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# ============================================================
# [추가] 컨테이너 타임존을 Asia/Seoul 로 고정
# alpine 베이스 이미지는 tzdata 가 없어 OS 기본 타임존이 UTC
# 이로 인해 LocalDateTime.now() 가 UTC 를 반환하고,
# 실시간 클릭 스트림의 minute(yyyyMMddHHmm) 값이 9시간 밀려 나감
# ============================================================
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# [추가] JVM 및 셸이 참조할 타임존 환경변수
ENV TZ=Asia/Seoul
# ============================================================

# 빌드 단계에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# [변경] -Duser.timezone 명시 (TZ 환경변수와 무관하게 JVM 기본 타임존 확정)
# ENTRYPOINT ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]