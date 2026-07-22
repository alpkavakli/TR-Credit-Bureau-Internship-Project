# --- 1. asama: derleme ---
# Maven + JDK 17 imajinda uygulamayi paketliyoruz. mvnw yerine hazir maven imaji
# kullaniyoruz: wrapper script'inin Windows/Linux satir sonu (CRLF) sorunlarini atlatir.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Once sadece pom.xml -> bagimliliklar bir katmanda cache'lenir; kaynak degisince
# tekrar indirilmez (Docker katman cache mantigi).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- 2. asama: calistirma ---
# Sadece JRE + jar -> imaj kucuk kalir (derleme araclari son imajda yok).
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
