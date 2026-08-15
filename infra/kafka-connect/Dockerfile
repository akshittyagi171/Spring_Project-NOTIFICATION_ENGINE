FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY priority-partitioner/pom.xml .
COPY priority-partitioner/src ./src
RUN mvn clean package -DskipTests

FROM debezium/connect:2.7.3.Final

USER root

RUN cd /kafka/connect/debezium-connector-mysql && \
    curl -sSL -o debezium-scripting.jar \
    https://repo1.maven.org/maven2/io/debezium/debezium-scripting/2.7.3.Final/debezium-scripting-2.7.3.Final.jar

RUN cd /kafka/connect/debezium-connector-mysql && \
    curl -sSL -o groovy.jar \
    https://repo1.maven.org/maven2/org/codehaus/groovy/groovy-jsr223/3.0.19/groovy-jsr223-3.0.19.jar && \
    curl -sSL -o groovy-core.jar \
    https://repo1.maven.org/maven2/org/codehaus/groovy/groovy/3.0.19/groovy-3.0.19.jar

COPY --from=builder /build/target/priority-partitioner-1.0.0.jar /kafka/libs/priority-partitioner-1.0.0.jar

USER kafka