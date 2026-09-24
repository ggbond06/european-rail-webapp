# syntax=docker/dockerfile:1

FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /app/backend

COPY backend/pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY backend/src/ ./src/
RUN mvn --batch-mode --no-transfer-progress -DskipTests package \
    && mvn --batch-mode --no-transfer-progress dependency:copy-dependencies \
        -DincludeScope=runtime \
        -DoutputDirectory=target/dependency


FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN useradd --create-home --shell /usr/sbin/nologin appuser

COPY --from=backend-build --chown=appuser:appuser \
    /app/backend/target/classes/ ./backend/classes/
COPY --from=backend-build --chown=appuser:appuser \
    /app/backend/target/dependency/ ./backend/lib/
COPY --from=frontend-build --chown=appuser:appuser \
    /app/frontend/dist/ ./frontend/dist/
COPY --chown=appuser:appuser data/europeanRail.dot ./data/europeanRail.dot

USER appuser

ENV PORT=10000

EXPOSE 10000

CMD ["sh", "-c", "exec java -cp 'backend/classes:backend/lib/*' WebApp \"$PORT\""]
