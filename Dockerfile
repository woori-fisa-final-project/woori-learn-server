# ------------ Stage 1: Build ------------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# ------------ Stage 2: Run ------------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 실행 가능한 Boot JAR만 복사 (plain.jar 제외)
COPY --from=builder /app/build/libs/wooriLearn-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


