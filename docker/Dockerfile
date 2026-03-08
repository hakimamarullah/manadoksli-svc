FROM ghcr.io/graalvm/graalvm-community:23 AS builder


WORKDIR /opt/app

# Copy Maven wrapper and pom.xml (relative to project root)
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./

RUN chmod +x ./mvnw

# Copy source code
COPY src/ ./src/
RUN ./mvnw clean install -Pnative -DskipTests


FROM debian:bookworm-slim

ENV LOG_DIR=/opt/app/logs
ENV UPLOADS_DIR=/opt/app/uploads

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    groupadd -r spring && useradd -r -g spring spring && \
    mkdir -p "$LOG_DIR" && chown -R spring:spring "$LOG_DIR" && \
    mkdir -p "$UPLOADS_DIR" && chown -R spring:spring "$UPLOADS_DIR"


COPY --from=builder --chown=spring:spring /opt/app/target/manadoksli-svc /opt/app/manadoksli-svc

# Switch to non-root user
USER spring

EXPOSE 8080

# Set default values for environment variables
ENV JAVA_OPTS="" \
    JAVA_ARGS=""

# Use exec form with sh to handle environment variables properly
ENTRYPOINT ["sh", "-c", "exec /opt/app/manadoksli-svc $JAVA_OPTS $JAVA_ARGS"]