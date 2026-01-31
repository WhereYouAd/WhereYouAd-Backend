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

# 빌드 단계에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]