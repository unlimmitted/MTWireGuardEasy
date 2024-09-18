FROM node:lts-alpine as nodejs
RUN apk add --no-cache git
WORKDIR /usr/src/node
RUN git clone https://github.com/unlimmitted/MTWireGuardEasy-frontend.git
WORKDIR /usr/src/node/MTWireGuardEasy-frontend
RUN yarn install
RUN yarn build

FROM gradle:8.7.0-jdk21-alpine AS gradle
COPY --chown=gradle:gradle . /home/gradle/
COPY --from=nodejs /usr/src/node/MTWireGuardEasy-frontend/dist/. /home/gradle/src/main/resources/static/
WORKDIR /home/gradle/
RUN gradle bootJar

FROM alpine/java:21-jdk AS java
WORKDIR /home/java/
COPY --from=gradle /home/gradle/build/libs/*.jar /home/java/MTWGEasy.jar
CMD ["java", "-jar", "MTWGEasy.jar"]