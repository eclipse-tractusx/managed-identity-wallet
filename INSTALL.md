# Managed Identity Wallets

If you just want to try out MIW (without any developer setup), then you can find a quick start guide here:

(It will only work on macOS or GNU/Linux - For Windows,
you need to use the [WSL](https://learn.microsoft.com/de-de/windows/wsl/install))

- You need to install these tools:
    - [Docker](https://docs.docker.com/desktop/) (or from your package manager)
      -> Configure it to run without root permission
    - Docker [compose plugin](https://docs.docker.com/compose/)
    - [Taskfile](https://taskfile.dev)
    - [jq](https://jqlang.github.io/jq/)
- Clone this repo
- (Optional) Checkout main (stable) or develop (latest), if not already checked out
- Copy `dev-assets/env-files/env.docker.dist` to `dev-assets/env-files/env.docker`
- Set these variables in `env.docker`
    - POSTGRES_PASSWORD
    - POSTGRES_PASSWORD_MIW
    - KEYCLOAK_ADMIN_PASSWORD
    - ENCRYPTION_KEY (32 random alphanumeric characters)
- Follow the "docker" path of the "Development setup" to get it up and running:
    1. Run `task docker:start-app` and wait until it shows "Started ManagedIdentityWalletsApplication in ... seconds"
    2. Run `task app:get-token` in another shell and copy the token (including "BEARER" prefix) (Mac users have the token already in their clipboard :) )
    3. Open API doc on http://localhost:8000
    4. Click on Authorize on Swagger UI and on the dialog paste the token (incl. "Bearer") into the "value" input
    5. Click on "Authorize" and "close"
    6. MIW is up, running, and you are authorized to fire requests in the Swagger UI
    7. If you're done, then run `task docker:stop-app` to clean up everything

> [!IMPORTANT]  
> You need to use Java 17!

> [!WARNING]
> If you encounter some kind of database connection errors, then execute `task docker:stop-app`.
> This will remove all existing Docker volumes, which may cause this error.

> [!IMPORTANT]
> Ensure you have exactly 32 random alphanumeric characters set 
> for `ENCRYPTION_KEY` in `dev-assets/env-files/env.docker`
