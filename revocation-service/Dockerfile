FROM eclipse-temurin:latest

# run as non-root user
#RUN addgroup -g 11111 -S miw && adduser -u 11111 -S -s /bin/false -G miw miw

# add curl for healthcheck
RUN apt-get update && apt-get install -y curl

COPY ./LICENSE ./NOTICE.md ./revocation-service/DEPENDENCIES ./SECURITY.md ./revocation-service/build/libs/revocation-service-latest.jar /app/

WORKDIR /app

HEALTHCHECK --start-period=30s CMD curl --fail http://localhost:8090/actuator/health/liveness || exit 1

CMD ["java", "-jar", "revocation-service-latest.jar"]
