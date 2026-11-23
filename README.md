# DB2API

DB2API is a Spring Boot application for configuring database connections and exposing selected tables through generated REST and GraphQL APIs. Administrators manage connections, organizations and API clients through a Vaadin web interface.

## Features

- Discover schemas from configured JDBC databases.
- Define REST or GraphQL APIs for selected tables, columns and operations.
- Issue JWT access tokens through an OAuth2-style client credentials endpoint.
- Manage database connections, organizations, API clients and administrator accounts in the web UI.
- Organize API credential users under named clients, which belong to an organization.
- Create guided connections to PostgreSQL, MySQL, MariaDB, Microsoft SQL Server, Oracle Database, SQLite and IBM Db2.
- Bundle the selected databases' JDBC drivers as Maven-managed runtime dependencies.
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
- No separate JDBC driver installation is needed for the supported external databases; Maven downloads the pinned driver dependencies during the build.

## Database connections

On the **Users & Connections** page, choose a database type in the connection editor. The form then asks for the details relevant to that database, such as host, port, database name, or Oracle service name. SQLite uses a local database file and does not require network credentials. JDBC URLs and driver class names are generated automatically.

The supported databases are PostgreSQL, MySQL, MariaDB, Microsoft SQL Server, Oracle Database, SQLite and IBM Db2. Their JDBC drivers are bundled using explicit Maven versions, so builds are reproducible and do not download or execute unverified driver JARs at application runtime. Update the version properties in `pom.xml` when adopting newer vendor releases.

Use **Organization & Clients** to manage organizations and their named clients (for example, **Tech World → Education Unlimited LLC**). Use **Users & Connections** to manage API credential users beneath a named client and database connections assigned to that client. The API Builder is available as its own **API** page. Existing connections remain available and can be assigned to a client in the connection editor.

Organizations, named clients, credential users and APIs can be active or inactive. An organization can only be inactivated after all its clients are inactive; a client requires all its users to be inactive; and a user requires all assigned APIs to be inactive. Active APIs must be assigned to at least one active credential user. Inactive users and their parent organization/client cannot authenticate or access APIs.

Organizations can be deleted from the **Organization & Clients** page; lower levels do not have delete controls. Before an organization can be deleted, all of its named clients, credential users and assigned APIs must be inactive. Deleting an organization also removes its named clients and credential users, so use deactivation when you only need to suspend access.

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

6. In the UI, create an organization and named client, add an API credential user and database connection, then define the REST or GraphQL APIs to expose.

## API usage

### Choose the authentication type

When creating an API credential user on **Users & Connections**, select exactly one authentication type. New users default to OAuth2 client credentials. On the **API** page, assign each API to the credential users that may access it.

- **Simple login (HTTP Basic):** send the client ID and client secret on each request.
- **Bearer API token:** send the generated token as a Bearer value. It is shown once when created or when switching a client to this mode; save it securely.
- **OAuth2 client credentials:** exchange the client ID and client secret for a short-lived JWT access token.

### OAuth2 client credentials

Create an API credential user in the UI, then exchange its credentials for a bearer token:

```sh
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=YOUR_CLIENT_ID" \
  --data-urlencode "client_secret=YOUR_CLIENT_SECRET"
```

The response contains an `access_token` and `token_type` of `Bearer`.

### Generated REST API

REST endpoints use `/api/dynamic/{tableName}`. The available HTTP methods and columns depend on the API definition. Use the authentication type selected for the client:

```sh
# OAuth2 JWT or a Bearer API token
curl http://localhost:8080/api/dynamic/YOUR_TABLE \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# HTTP Basic
curl http://localhost:8080/api/dynamic/YOUR_TABLE \
  -u "YOUR_CLIENT_ID:YOUR_CLIENT_SECRET"
```

### Generated GraphQL API

The GraphQL endpoint is `POST /graphql`. Its schema is built from the GraphQL API definitions configured in the UI. Send a standard GraphQL JSON request using the authentication type selected for the client.

### Revoke a token

Send a form-encoded `token` parameter to `POST /oauth2/revoke`.

## Security

The administration UI requires login. Dynamic REST and GraphQL requests accept only the authentication type selected for their client. Use HTTPS in deployed environments, protect client credentials and tokens, and do not use the development defaults outside local development. See [SECURITY.md](SECURITY.md) for private vulnerability reporting instructions.
