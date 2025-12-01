CREATE TABLE organization (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE client (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL,
    organization_id INTEGER REFERENCES organization(id)
);

CREATE TABLE db_connection (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    driver_class VARCHAR(255) NOT NULL
);

CREATE TABLE api_definition (
    id SERIAL PRIMARY KEY,
    connection_id INTEGER REFERENCES db_connection(id),
    name VARCHAR(255) NOT NULL,
    api_type VARCHAR(50) NOT NULL, -- REST, GraphQL
    table_name VARCHAR(255) NOT NULL,
    allowed_operations VARCHAR(255), -- GET, PUT, POST, DELETE
    included_columns TEXT
);

CREATE TABLE admin_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL -- ADMIN, EDITOR, VIEWER
);
