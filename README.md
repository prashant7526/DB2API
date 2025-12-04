
# DB2API: Automatic REST and GraphQL API Generation for DB2 Databases

## Overview

DB2API is a powerful web application that empowers you to instantly create REST and GraphQL APIs from your DB2 databases. Say goodbye to tedious boilerplate code and manual API development. With DB2API, you can connect to your database, select tables and columns, and generate a fully functional and secure API in minutes.

This application is built with a modern technology stack, featuring a Java-based backend with Vaadin for the user interface, and a robust set of features for developers and administrators.

## Features

*   **Automatic API Generation:** Instantly create REST and GraphQL APIs from your existing database schemas.
*   **Secure by Design:** The application includes a robust security model with JWT-based authentication and role-based access control.
*   **Intuitive User Interface:** A user-friendly web interface built with Vaadin allows for easy management of database connections, API definitions, and users.
*   **Dynamic and Flexible:** The generated APIs are dynamic and can be easily customized to your needs.
*   **Database Schema Discovery:** The application can automatically discover your database schema, making it easy to select the tables and columns you want to expose through your APIs.
*   **Cayenne ORM Integration:** The application uses Apache Cayenne for object-relational mapping, providing a powerful and flexible way to interact with your database.

## Technologies Used

*   **Backend:**
    *   Java 21
    *   Spring Boot
    *   Vaadin
    *   Apache Cayenne
    *   Spring Security
    *   GraphQL Java
*   **Frontend:**
    *   HTML/CSS
    *   TypeScript
*   **Database:**
    *   DB2 (can be extended to other SQL databases)

## Getting Started

### Prerequisites

*   **JDK 21:** Make sure you have JDK 21 installed and configured in your IDE.
*   **Maven:** A recent version of Apache Maven.
*   **A DB2 instance:** You will need a running DB2 database to connect to.

### Installation and Running

1.  **Clone the repository:**

    ```bash
    git clone <repository-url>
    cd <repository-directory>
    ```

2.  **Configure the database connection:**

    Open the `src/main/resources/application.properties` file and update the database connection properties with your DB2 instance details.

3.  **Build and run the application:**

    ```bash
    mvn spring-boot:run
    ```

    The application will be available at `http://localhost:8080`.

## Project Structure

```
.
├── frontend/                # Vaadin frontend files (TypeScript, CSS)
├── src/main/java/com/db2api/ # Java source code
│   ├── config/             # Spring and application configuration
│   ├── controller/         # REST and GraphQL controllers
│   ├── persistent/         # Cayenne data model objects
│   ├── security/           # Spring Security configuration
│   ├── service/            # Business logic services
│   └── ui/                 # Vaadin UI views
├── src/main/resources/       # Application resources
│   ├── application.properties # Application configuration
│   └── schema.sql        # Initial database schema
├── pom.xml                   # Maven project configuration
└── README.md                 # This file
```

## API Endpoints

The application exposes both REST and GraphQL endpoints.

### REST API

The REST API is dynamically generated based on the API definitions you create in the application. The base URL for the REST API is `/api/`.

### GraphQL API

The GraphQL endpoint is available at `/graphql`. You can use a GraphQL client like Postman or Insomnia to explore the schema and make queries.

## Authentication

The application uses JWT-based authentication. To access the protected API endpoints, you need to obtain a JWT token by authenticating with the `/auth/login` endpoint.
