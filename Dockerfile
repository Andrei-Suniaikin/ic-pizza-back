FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/*.jar /app/app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
ENV TZ=UTC
EXPOSE 8000
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8000} -jar /app/app.jar"]