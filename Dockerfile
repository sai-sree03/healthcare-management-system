# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home hms
COPY --from=build /app/target/*.jar app.jar
USER hms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
