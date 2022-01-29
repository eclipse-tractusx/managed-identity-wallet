FROM openjdk:17-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/net.catenax.core.custodian/ /app/
COPY ./static /app/static
WORKDIR /app/bin
CMD ["./net.catenax.core.custodian"]
