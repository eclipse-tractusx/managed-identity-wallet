FROM eclipse-temurin:19-jre-alpine

EXPOSE 8080:8080

# run as non-root user
RUN addgroup -g 11111 -S miw && adduser -u 11111 -S -s /bin/false -G miw miw

USER miw

COPY /build/libs/miw-latest.jar /app/

WORKDIR /app

CMD ["java", "-jar", "miw-latest.jar"]
