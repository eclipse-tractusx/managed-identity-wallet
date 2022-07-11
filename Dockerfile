FROM amazoncorretto:17-alpine
EXPOSE 8080:8080
# run as non-root user
RUN groupadd -g 1001 -r user && useradd -u 1001 -r -s /bin/false -g user user
USER user
COPY ./build/install/net.catenax.core.managedidentitywallets/ /app/
COPY ./static /app/static
WORKDIR /app/bin
CMD ["./net.catenax.core.managedidentitywallets"]
