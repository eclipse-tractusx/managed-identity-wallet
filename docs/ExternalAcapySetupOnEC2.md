# Setup Aca-Py on EC2 Instance

The following steps describe how to set up an Aca-Py agent with nginx on an EC2 instance

- Login in your AWS account
- Create a new EC2 instance
  - ubuntu 22.04
  - name e.x. Acapy_external
  - t2.micro with 1 cpu & 1 GiB RAM
  - 10 GB memory
  - enable access using ssh 
- Create an elastic IP and assign it to the created EC2 instance
- Create a domain e.x. `cx-dev-acapy.51nodes.io` and assign it to the elastic IP
- Set up the inbound rules in the security groups of your EC2 instance
   * add Port 80 and 443
- Connect to the EC2 instance using ssh `ssh -i "AcapyExternal.pem" ubuntu@cx-dev-acapy.51nodes.io`
- Create a folder `mkdir acapy-agent`
- Generate letsencrypt certificates
    - download certbot and get certificates. Please replace the domain in the last command
        ```
        sudo snap install core; sudo snap refresh core
        sudo snap install --classic certbot
        sudo ln -s /snap/bin/certbot /usr/bin/certbot
        sudo certbot certonly --standalone -d cx-dev-acapy.51nodes.io
        ```
    - Move the generated files private.pem and fullchain.pem to `./acapy-agent`
    - Lets Encrypt certificates expire after 90 and must be [renewed](https://www.cyberciti.biz/faq/how-to-forcefully-renew-lets-encrypt-certificate/#:~:text=Renewing%20the%20LetsEncrypt%20certificate%20using%20the%20certbot&text=Obtain%20a%20browser%2Dtrusted%20certificate,forcefully%20if%20the%20need%20arises) regularly
- Download Docker and Docker-compose for ubuntu 22.04
- Create `.env` file with `vi .env` and then add the environment variables to it after changing the placeholders. Also replace `cx-dev-acapy.51nodes.io` with your domain
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
    ACAPY_ENDPOINT=https://cx-dev-acapy.51nodes.io/didcomm/
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
        ssl_certificate /etc/letsencrypt/live/cx-dev-acapy.51nodes.io/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/cx-dev-acapy.51nodes.io/privkey.pem;
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
            - ./fullchain.pem:/etc/letsencrypt/live/cx-dev-acapy.51nodes.io/fullchain.pem
            - ./privkey.pem:/etc/letsencrypt/live/cx-dev-acapy.51nodes.io/privkey.pem

        acapy_postgres:
            image: postgres:14-alpine3.17
            container_name: acapy_postgres
            env_file:
            - .env
            volumes:
            - postgres-data:/data/postgres-data

        acapy_agent:
            image: bcgovimages/aries-cloudagent:py36-1.16-1_0.7.5
            container_name: acapy_agent
            env_file:
            - .env
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
- Now run the following command `docker-compose --env-file .env up -d` to start the agent. This command will start 3 docker containers:
    
    * acapy-agent: the acapy instance v0.7.5
    * acapy_postgres: the database where the wallets are stored
    * acapy_nginx: nginx instance

- To interact with the agent you can use
  * either the postman collection `./dev-containers/postman/Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection` after modifying the URLs and apikey.
  * Or using the provided swagger doc `https://cx-dev-acapy.51nodes.io/api/doc/` after replacing `https://cx-dev-acapy.51nodes.io/api/doc/` with your subdomain
- The files `./docs/ExternalWalletInteraction.md` and `./docs/SelfManagedWallets.md` describe how MIW can interact with an external wallet and a self managed wallet
- To remove the containers run `docker-compose down`
- To delete all containers with the database run `docker-compose down -v`
