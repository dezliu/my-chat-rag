FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 先复制 POM，利用 Docker 层缓存依赖
COPY pom.xml .
COPY myrag-common/pom.xml myrag-common/pom.xml
COPY myrag-rag-core/pom.xml myrag-rag-core/pom.xml
COPY myrag-rag-query/pom.xml myrag-rag-query/pom.xml
COPY myrag-rag-admin/pom.xml myrag-rag-admin/pom.xml
COPY myrag-rag-mcp/pom.xml myrag-rag-mcp/pom.xml
COPY myrag-rag-monitor/pom.xml myrag-rag-monitor/pom.xml
COPY myrag-chat/pom.xml myrag-chat/pom.xml
COPY myrag-server/pom.xml myrag-server/pom.xml

RUN mvn -pl myrag-server -am dependency:go-offline -B

COPY myrag-common myrag-common
COPY myrag-rag-core myrag-rag-core
COPY myrag-rag-query myrag-rag-query
COPY myrag-rag-admin myrag-rag-admin
COPY myrag-rag-mcp myrag-rag-mcp
COPY myrag-rag-monitor myrag-rag-monitor
COPY myrag-chat myrag-chat
COPY myrag-server myrag-server

RUN mvn -pl myrag-server -am package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S myrag && adduser -S myrag -G myrag

USER myrag

COPY --from=builder /build/myrag-server/target/myrag-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
