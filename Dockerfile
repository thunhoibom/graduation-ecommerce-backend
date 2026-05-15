FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
RUN addgroup --system --gid 1001 spring \
  && adduser --system --uid 1001 --ingroup spring spring
COPY --from=build --chown=spring:spring /app/target/*.war app.war
USER spring
EXPOSE 8081
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.war"]
