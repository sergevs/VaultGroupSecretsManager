FROM gradle:6.8.3-jre11 AS gradle
COPY src gscm/src
COPY *.gradle gscm/
RUN cd gscm && gradle --no-daemon build

FROM openjdk:17-jdk-buster
ARG APP_UID=2542
ARG APP_GID=2542

# use tini as subreaper in Docker container to adopt zombie processes
ARG TINI_VERSION=v0.16.1
RUN curl -fsSL https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-static-$(dpkg --print-architecture) -o /sbin/tini \
  && chmod +x /sbin/tini

RUN addgroup --gid $APP_GID gscm && \
    adduser --uid $APP_UID --ingroup gscm \
            --disabled-password \
            --gecos "Vault Groups Secrets Manager" \
            --home /app \
            gscm

WORKDIR /app

USER gscm

COPY --from=gradle /home/gradle/gscm/build/libs/*.jar /app

ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/vault.gscm-1.0.jar"]
