# DB2API

DB2API is a Spring Boot application for configuring database connections and exposing selected tables through generated REST and GraphQL APIs. Administrators manage connections, organizations and API clients through a Vaadin web interface.

## Features

- Discover schemas from configured JDBC databases.
- Define REST or GraphQL APIs for selected tables, columns and operations.
- Issue JWT access tokens through an OAuth2-style client credentials endpoint.
- Manage database connections, organizations, API clients and administrator accounts in the web UI.
- Encrypt saved connection passwords and client secrets.

## Technology

- Java 25 and Spring Boot 4
- Vaadin 25
- Apache Cayenne 4.2.3
- Spring Security and GraphQL Java
- PostgreSQL for DB2API's own system database

## Requirements

- JDK 25
- Apache Maven
- PostgreSQL
- Node.js 24 for Vaadin frontend builds. Vaadin can download a compatible Node.js runtime when needed.
- A JDBC driver for each external database you plan to connect. PostgreSQL is included by default; add other vendor drivers to `pom.xml`.

## Run locally

1. Clone the repository and enter the project directory:

   ```sh
   git clone https://github.com/prashant7526/DB2API.git
   cd DB2API
   ```

2. Create the PostgreSQL database used by DB2API:

   ```sql
   CREATE DATABASE db2api_system;
   ```

3. Configure the system database and application secrets. You can set these as environment variables or update `src/main/resources/application.properties` for local development:

   | Environment variable | Purpose |
   | --- | --- |
   | `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL for `db2api_system` |
   | `SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
   | `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
   | `APP_JWT_SECRET` | Secret used to sign and verify JWTs; use a random value of at least 32 bytes |
   | `APP_ENCRYPTION_SECRET` | Secret used to encrypt saved database passwords and client secrets |

   The checked-in properties contain fixed credentials and fallback secrets. Treat them as public and override them in every environment; rotate any values that have been used outside disposable local development. Keep `APP_ENCRYPTION_SECRET` stable: changing it without first re-encrypting stored values makes existing saved passwords and client secrets unreadable.

4. Build and run the application:

   ```sh
   mvn clean test
   mvn spring-boot:run
   ```

5. Open [http://localhost:8080](http://localhost:8080) and sign in. On a new system database, the application creates an initial administrator account with username `admin` and password `admin`. Change this password immediately; it is only suitable for first-run local setup.

6. In the UI, configure a database connection, create an organization and API client, then define the REST or GraphQL APIs to expose.

## API usage

### Get an access token

Create a client in the UI, then exchange its credentials for a bearer token:

```sh
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=YOUR_CLIENT_ID" \
  --data-urlencode "client_secret=YOUR_CLIENT_SECRET"
```

The response contains an `access_token` and `token_type` of `Bearer`.

### Generated REST API

REST endpoints use `/api/dynamic/{tableName}`. The available HTTP methods and columns depend on the API definition:

```sh
curl http://localhost:8080/api/dynamic/YOUR_TABLE \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Generated GraphQL API

The GraphQL endpoint is `POST /graphql`. Its schema is built from the GraphQL API definitions configured in the UI. Send a standard GraphQL JSON request and include the same bearer token in the `Authorization` header.

### Revoke a token

Send a form-encoded `token` parameter to `POST /oauth2/revoke`.

## Security

The administration UI requires login. Dynamic REST and GraphQL requests require a valid JWT bearer token. Use HTTPS in deployed environments, protect client credentials and tokens, and do not use the development defaults outside local development. See [SECURITY.md](SECURITY.md) for private vulnerability reporting instructions.
