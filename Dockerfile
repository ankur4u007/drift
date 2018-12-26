FROM openjdk:8u181-jdk-alpine3.8

WORKDIR /home

COPY build/libs/websocketHttpTunnel.jar .

ENTRYPOINT java -jar websocketHttpTunnel.jar
