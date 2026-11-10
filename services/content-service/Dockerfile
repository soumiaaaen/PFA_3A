# ---------- Étape 1 : compilation ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Les dépendances d'abord : Docker les garde en cache tant que pom.xml ne change pas
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q package -DskipTests

# ---------- Étape 2 : exécution ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]