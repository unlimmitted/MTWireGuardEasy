# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS frontend

ARG FRONTEND_REPOSITORY=https://github.com/unlimmitted/MTWireGuardEasy-frontend.git
ARG FRONTEND_REF=master

RUN apk add --no-cache git
WORKDIR /workspace
RUN git init . && \
    git remote add origin "${FRONTEND_REPOSITORY}" && \
    git fetch --depth 1 origin "${FRONTEND_REF}" && \
    git checkout --detach FETCH_HEAD
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund && \
    npm run build


FROM gradle:8.10-jdk21-alpine AS backend

WORKDIR /workspace

# Dependency metadata is copied first so source changes do not invalidate the
# Gradle dependency cache.
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle libs/ ./libs/
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle --no-daemon dependencies --configuration runtimeClasspath

COPY --chown=gradle:gradle src/ ./src/
COPY --from=frontend --chown=gradle:gradle /workspace/dist/ ./src/main/resources/static/
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle --no-daemon bootJar


FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S mtwgeasy && \
    adduser -S -G mtwgeasy mtwgeasy && \
    mkdir -p /app /data && \
    chown -R mtwgeasy:mtwgeasy /app /data

WORKDIR /app
ENV DB_PATH=/data/db.sqlite

COPY --from=backend --chown=mtwgeasy:mtwgeasy /workspace/build/libs/*.jar /app/MTWGEasy.jar

USER mtwgeasy
VOLUME ["/data"]
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/MTWGEasy.jar"]
