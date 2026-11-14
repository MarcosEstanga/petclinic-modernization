# Multi-stage image for every PetClinic service.
#
#   docker build --build-arg MODULE=services/customers-service --build-arg PORT=8081 -t petclinic/customers .
#
# The module is built with the Maven wrapper so the image does not depend on the host.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN ./mvnw -B -q -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre
# curl is only used by the Compose health checks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG MODULE
ARG PORT
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
