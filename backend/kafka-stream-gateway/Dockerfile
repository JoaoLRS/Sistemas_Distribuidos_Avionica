FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle

COPY kafka-stream-gateway/build.gradle ./build.gradle

RUN printf 'pluginManagement {\n\
    repositories {\n\
        gradlePluginPortal()\n\
        mavenCentral()\n\
    }\n\
}\n\
dependencyResolutionManagement {\n\
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n\
    repositories {\n\
        mavenCentral()\n\
    }\n\
}\n\
rootProject.name = "kafka-stream-gateway"\n' > settings.gradle

RUN sed -i 's/\r$//' gradlew
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY kafka-stream-gateway/src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon --build-cache

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]