FROM openjdk:8u181-jdk-alpine3.8

WORKDIR /home

COPY build/libs/drift.jar .
COPY src/main/resources/application.yml .
COPY .docker/runApplication.sh .

ENTRYPOINT sh /home/runApplication.sh