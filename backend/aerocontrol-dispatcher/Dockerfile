FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY aerocontrol-dispatcher/build.gradle ./build.gradle
COPY aerocontrol-dispatcher/src ./src

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
rootProject.name = "aerocontrol-dispatcher"\n' > settings.gradle

RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]