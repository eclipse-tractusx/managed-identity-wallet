# Setup Aca-Py on a Ubuntu Server

The following steps describe how to set up an Aca-Py agent with nginx on Ubuntu 22.04.

## The Goal and Usage of the Agent
This separate ACA-Py agent can be used to test the external connections and credential exchanges with the managed wallets in the [Managed-Identity-Wallet](https://github.com/eclipse-tractusx/managed-identity-wallets). 
- To interact with the agent you can use
  * either the Postman collection `./dev-containers/postman/Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection` after modifying the URLs and apikey.
  * Or using the provided swagger doc `https://mydomain.example.com/api/doc/` after replacing `https://mydomain.example.com/api/doc/` with your subdomain
- The files `./docs/ExternalWalletInteraction.md` and `./docs/SelfManagedWallets.md` describe how the MIW can interact with an external (issuer) wallet and a self-managed (company) wallet

## Setup Steps

Requirements:
  - 1 CPU & 1 GiB RAM
  - 10 GB storage
  - Static IP address with a domain that is assigned to it e.g. `mydomain.example.com`
  - Docker and Docker-compose

- Create a folder `mkdir acapy-agent`
- Generate letsencrypt certificates
    - download certbot and get certificates. Please replace the domain in the last command
        ```
        sudo snap install core; sudo snap refresh core
        sudo snap install --classic certbot
        sudo ln -s /snap/bin/certbot /usr/bin/certbot
        sudo certbot certonly --standalone -d mydomain.example.com
        ```
    - Move the generated files private.pem and fullchain.pem to `./acapy-agent`
    - Lets Encrypt certificates expire after 90 and must be renewed. This can be done using the command `sudo certbot renew`. To verify that the certificate renewed, run `sudo certbot renew --dry-run`

- Create `.env` file with `vi .env` and then add the environment variables to it after changing the placeholders. Also replace `mydomain.example.com` with your domain
    ```
    POSTGRES_USER=postgres
    POSTGRES_PASSWORD=postgres-password-placeholder
    PGDATA=/data/postgres-data
    POSTGRES_PORT=5432

    WAIT_HOSTS=acapy_postgres:5432
    WAIT_HOSTS_TIMEOUT=300
    WAIT_SLEEP_INTERVAL=5
    WAIT_HOST_CONNECT_TIMEOUT=3

    ACAPY_CONNECTION_PORT=8000
    ACAPY_ADMIN_PORT=11000
    ACAPY_ENDPOINT=https://mydomain.example.com/didcomm/
    ACAPY_WALLET_KEY=acapy-wallet-key-placeholder
    ACAPY_SEED=acapy-seed-placeholder
    LEDGER_URL=http://dev.greenlight.bcovrin.vonx.io/genesis
    ACAPY_ADMIN_KEY=acapy-admin-api-key-placeholder
    JWT_SECRET=acapy-jwt-secret-placeholder
    ```

- Create the `nginx.conf` file. If the Ports of AcaPy in `.env` file are changed, then they must be changed in the `nginx.conf` file. Also the paths of the certificates should match the given paths in `docker-compose.yml` file
    ```
    events {
        worker_connections  1024;
    }

    http {

      # enforce redirect to https
      server {
        listen 80 default_server;
        listen [::]:80 default_server;
        server_name _;
        return 301 https://$host$request_uri;
      }

      server {
        listen 443 ssl;
        listen [::]:443 default_server;
        root /usr/share/nginx/html;
        index index.html index.htm;

        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload;" always;

        # RSA certificate
        ssl_certificate /etc/certificates/fullchain.pem;
        ssl_certificate_key /etc/certificates/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;


        ssl_ciphers "EECDH+ECDSA+AESGCM EECDH+aRSA+AESGCM EECDH+ECDSA+SHA384 EECDH+ECDSA+SHA256 EECDH+aRSA+SHA384 EECDH+aRSA+SHA256 EECDH+aRSA+RC4 EECDH EDH+aRSA RC4 !aNULL !eNULL !LOW !3DES !MD5 !EXP !PSK !SRP !DSS !RC4";
        ssl_prefer_server_ciphers on;

        location /didcomm/ {
            proxy_pass http://acapy_agent:8000/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        location /api/doc/ {
            proxy_pass http://acapy_agent:11000/api/doc#/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        location / {
            proxy_pass http://acapy_agent:11000/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # Hide the Nginx version number (in error pages / headers)
        server_tokens off;

      }
    }
    ```

- Create the `docker-compose.yml` file. The file is almost generic and you can either change the values in `.env` file or create a new enviroment file e.x. `dev.env` and then change the `env_file` property in the docker-compose.yml file. 
    ```yml
    version: '3'

    services:
      acapy_nginx:
        image: nginx:1.23.3
        container_name: acapy_nginx
        depends_on:
          - acapy_postgres
          - acapy_agent
        ports:
          - 443:443
        volumes:
          - ./nginx.conf:/etc/nginx/nginx.conf
          - ./fullchain.pem:/etc/certificates/fullchain.pem
          - ./privkey.pem:/etc/certificates/privkey.pem

      acapy_postgres:
        image: postgres:14-alpine3.17
        container_name: acapy_postgres
        env_file:
          - ./.env
        volumes:
          - postgres-data:/data/postgres-data

      acapy_agent:
        image: bcgovimages/aries-cloudagent:py36-1.16-1_0.7.5
        container_name: acapy_agent
        env_file:
          - ./.env
        depends_on:
          - acapy_postgres
        entrypoint: /bin/bash
        command: [
          "-c",
          "aca-py start \
          -e ${ACAPY_ENDPOINT} \
          --auto-provision \
          --inbound-transport http '0.0.0.0' ${ACAPY_CONNECTION_PORT:-8000} \
          --outbound-transport http \
          --admin '0.0.0.0' ${ACAPY_ADMIN_PORT:-11000} \
          --wallet-name External_Wallet \
          --wallet-type askar \
          --wallet-key ${ACAPY_WALLET_KEY} \
          --wallet-storage-type postgres_storage
          --wallet-storage-config '{\"url\":\"acapy_postgres:${POSTGRES_PORT:-5432}\",\"max_connections\":5}'
          --wallet-storage-creds '{\"account\":\"postgres\",\"password\":\"${POSTGRES_PASSWORD}\",\"admin_account\":\"postgres\",\"admin_password\":\"${POSTGRES_PASSWORD}\"}'
          --seed ${ACAPY_SEED} \
          --genesis-url ${LEDGER_URL} \
          --label External_Wallet \
          --admin-api-key ${ACAPY_ADMIN_KEY} \
          --auto-ping-connection \
          --jwt-secret ${JWT_SECRET} \
          --public-invites \
          --log-level DEBUG"
        ]

    volumes:
      postgres-data:
    ```
- Check the permission of the files `private.pem` and `fullchain.pem` to make sure that they are accessible by nginx
- You can change the used environment file by changing the property `env_file` in `docker-compose.yml`
- Now run the following command `docker-compose up -d` to start the agent. This command will start 3 docker containers:

    * acapy-agent: the acapy instance v0.7.5
    * acapy_postgres: the database where the wallets are stored
    * acapy_nginx: nginx instance

- To remove the containers run `docker-compose down`
- To delete all containers with the database run `docker-compose down -v`

