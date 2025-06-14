#FROM gradle:latest as cache
#ENV APP_HOME=/usr/app/
#RUN mkdir $APP_HOME
#ENV GRADLE_USER_HOME /usr/gradle/cache
#
#RUN mkdir -p /usr/gradle/cache
#COPY build.gradle $APP_HOME
#WORKDIR $APP_HOME
#RUN ./gradlew build || return 0
#
#FROM gradle:latest AS build
#ENV APP_HOME=/usr/app/
#COPY --from=cache /usr/gradle/cache /home/gradle/.gradle
#WORKDIR $APP_HOME
#COPY . .
#RUN gradle bootJar
#
#FROM eclipse-temurin:21-jdk-alpine
#ENV APP_HOME=/usr/app
#WORKDIR $APP_HOME
#COPY --from=build $APP_HOME/build/libs/supervisor-0.0.1-SNAPSHOT.jar supervisor.jar
#ENTRYPOINT ["java", "-jar", "supervisor.jar"]


FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app
COPY . .

RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

RUN apk add --no-cache docker-cli

VOLUME /var/run/docker.sock:/var/run/docker.sock

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]