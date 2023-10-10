# create keys

```shell
openssl req -new -sha256 \
     -reqexts cofca \
     -days 1095 \
     -nodes \
     -keyout rootCAKey.pem \
     -subj "/C=DE/ST=NRW/O=XignSys, GmbH/OU=Wiremock Test/CN=Wiremock CA" \
     -config openssl.cnf \
     -out rootCACert.pem \
     -x509 \
     -extensions cofca 
```

# package into pkcs12 keystore
```shell
openssl pkcs12 -export -out ca.p12 -inkey rootCAKey2.pem -in rootCACert2.pem
```

# convert pem certificate to der
```shell
openssl x509 -outform der -in rootCACert.pem -out rootCACert.der
```

# import certificate into truststore
```shell
keytool -import -trustcacerts -alias wiremock_ca -file rootCACert.der -keystore wiremock-truststore.jks
```