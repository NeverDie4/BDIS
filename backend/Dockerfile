FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
COPY import ./import
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=builder /app/target/bdis-api-*.jar /app/app.jar
COPY --from=builder /app/import /app/import
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
