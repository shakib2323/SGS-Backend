# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
RUN groupadd -r sgs && useradd -r -g sgs -u 1001 sgs \
 && mkdir -p /data/uploads && chown -R sgs:sgs /data
WORKDIR /app
COPY --from=build /src/target/sgs-backend.jar app.jar
USER sgs
EXPOSE 8081
# Small-VPS friendly: serial GC, capped heap, headless AWT (image processing).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k -Djava.awt.headless=true"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
