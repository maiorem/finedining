# arm64(t4g/Graviton)에서 빌드한다 (CLAUDE.md §13.5).
#
# 주의(CLAUDE.md §13.5와의 차이): 원문은 `eclipse-temurin:17-jre-alpine`을 지정하지만,
# temurin 17의 alpine 태그는 현재 amd64만 배포되고 arm64 매니페스트가 없다(2026-08 확인).
# arm64 배포가 요구사항이므로 alpine 대신 arm64를 지원하는 jammy(Ubuntu) 태그를 쓴다.
# alpine보다 이미지가 커지는 대신 로컬(Apple Silicon)·CI·EC2(Graviton) 아키텍처가 일치한다.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --version
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"
EXPOSE 8080 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
