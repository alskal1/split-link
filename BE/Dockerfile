FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY settings.xml .
COPY pom.xml .
COPY src ./src
RUN mvn -s settings.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-Xmx350m", "-jar", "app.jar"]