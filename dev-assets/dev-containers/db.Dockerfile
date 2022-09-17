FROM postgres:14.2-alpine
COPY revocation/V1.0.0__Create_DB.sql /docker-entrypoint-initdb.d/
