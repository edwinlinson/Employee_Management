# Employee Management REST API

A Spring Boot 3 / Java 17 REST API for managing employees, built with a clean layered architecture,
Bean Validation, centralised exception handling, pagination, search, status filtering, Swagger/OpenAPI and unit tests.

## Features

| Requirement | Implementation |
|---|---|
| Create / view all / view by ID / update / delete | `EmployeeController` → `EmployeeService` → `EmployeeRepository` |
| Input validation | Jakarta Bean Validation on `EmployeeRequest` (`@Valid`) |
| Exception handling | `GlobalExceptionHandler` (`@RestControllerAdvice`) with one JSON error format |
| Database | PostgreSQL (default) or MySQL via environment variables |
| Pagination & sorting | `page`, `size`, `sort` query params (sort fields whitelisted) |
| Search | `search` param: name, email, department, designation (case-insensitive) |
| Status filter | `status` param: `ACTIVE`, `INACTIVE`, `ON_LEAVE` |
| API docs | Swagger UI + OpenAPI JSON + Postman collection |
| Tests | Mockito service tests, `@WebMvcTest` controller tests, context-load test (H2) |

## Tech stack

Java 17, Spring Boot 3.3.5, Spring Web, Spring Data JPA (Hibernate), Bean Validation, springdoc-openapi 2.6.0,
PostgreSQL / MySQL, JUnit 5, Mockito, H2 (tests only), Maven.

## Project structure

```
src/main/java/com/example/employee
├── EmployeeManagementApplication.java
├── config/          OpenApiConfig
├── controller/      EmployeeController          – HTTP only; no business logic
├── dto/             EmployeeRequest, EmployeeResponse, PageResponse, ErrorResponse
├── entity/          Employee, EmployeeStatus
├── exception/       ResourceNotFound/DuplicateResource/BadRequest + GlobalExceptionHandler
├── mapper/          EmployeeMapper              – entity <-> DTO
├── repository/      EmployeeRepository          – Spring Data JPA + Specifications
├── service/         EmployeeService (interface) + impl/EmployeeServiceImpl
└── specification/   EmployeeSpecifications      – composable search/filter predicates
```

### Design notes (SOLID & best practices)

- **Single Responsibility** – controller handles HTTP, service holds business rules, repository handles persistence,
  mapper converts objects, `GlobalExceptionHandler` builds error responses.
- **Open/Closed** – new filters are added as new `Specification` methods without touching existing query code.
- **Liskov / Interface Segregation** – the controller depends on the `EmployeeService` interface, so any implementation can be substituted (e.g. a mock in tests).
- **Dependency Inversion** – constructor injection everywhere; high-level layers depend on abstractions.
- **DTOs** – entities are never exposed; the API contract is independent of the DB model.
- **Emails** are trimmed and lower-cased, and uniqueness is enforced both in the service (clean 409) and by a DB unique constraint.
- **Transactions** – service is `@Transactional(readOnly = true)` by default, with write methods overriding.
- **`spring.jpa.open-in-view=false`**, stack traces and internal messages are never leaked to clients.
- **Sort whitelist** prevents clients from sorting on arbitrary properties.

## Prerequisites

- JDK 17+
- Maven 3.9+
- Docker (optional, for the database) **or** a local PostgreSQL/MySQL

## Setup & run

### 1. Start the database

```bash
docker compose up -d postgres
```

This creates PostgreSQL 16 with database `employee_db`, user `postgres`, password `postgres` on port 5432.
Without Docker, create the DB yourself: `CREATE DATABASE employee_db;`

### 2. Run the application

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**. The `employees` table is created automatically
(`ddl-auto=update`; use `validate` plus Flyway/Liquibase in production).

### 3. Run the tests

```bash
mvn test
```

Tests use an in-memory H2 database (profile `test`), so no external DB is required.

### Build a jar

```bash
mvn clean package
java -jar target/employee-management-1.0.0.jar
```

## Database configuration

All settings live in `src/main/resources/application.yml` and can be overridden with environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/employee_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `DDL_AUTO` | `update` | Hibernate schema mode (`update`, `validate`, `none`) |
| `SERVER_PORT` | `8080` | HTTP port |

### Using MySQL

```bash
docker compose --profile mysql up -d mysql

export DB_URL="jdbc:mysql://localhost:3306/employee_db?createDatabaseIfNotExist=true&serverTimezone=UTC"
export DB_USERNAME=root
export DB_PASSWORD=root
mvn spring-boot:run
```

Both drivers are on the classpath; Hibernate detects the dialect automatically from the connection.

## API documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Postman collection: `docs/Employee-Management-API.postman_collection.json` (import into Postman; `baseUrl` defaults to `http://localhost:8080`)

### Endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| POST | `/api/v1/employees` | Create employee | 201 + `Location` header |
| GET | `/api/v1/employees` | List (paged, search, filter, sort) | 200 |
| GET | `/api/v1/employees/{id}` | Get by ID | 200 |
| PUT | `/api/v1/employees/{id}` | Update employee | 200 |
| DELETE | `/api/v1/employees/{id}` | Delete employee | 204 |

**List query params:** `page` (0-based, default 0), `size` (default 10), `sort` (e.g. `salary,desc`),
`search`, `status`. Sortable fields: `id, firstName, lastName, email, department, designation, salary, dateOfJoining, status, createdAt`.

### Validation rules

| Field | Rule |
|---|---|
| `firstName`, `lastName` | required, max 50 |
| `email` | required, valid format, max 100, unique (case-insensitive) |
| `phone` | optional, 7–15 digits, optional leading `+` |
| `department`, `designation` | required, max 50 |
| `salary` | required, > 0, up to 10 integer + 2 fraction digits |
| `dateOfJoining` | required, `yyyy-MM-dd`, not in the future |
| `status` | optional, `ACTIVE` \| `INACTIVE` \| `ON_LEAVE` (default `ACTIVE`) |

## Sample requests & responses

### Create employee

```bash
curl -i -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Asha",
    "lastName": "Menon",
    "email": "asha.menon@example.com",
    "phone": "+919876543210",
    "department": "Engineering",
    "designation": "Software Engineer",
    "salary": 85000.00,
    "dateOfJoining": "2024-06-15",
    "status": "ACTIVE"
  }'
```

`201 Created` — `Location: http://localhost:8080/api/v1/employees/1`

```json
{
  "id": 1,
  "firstName": "Asha",
  "lastName": "Menon",
  "email": "asha.menon@example.com",
  "phone": "+919876543210",
  "department": "Engineering",
  "designation": "Software Engineer",
  "salary": 85000.00,
  "dateOfJoining": "2024-06-15",
  "status": "ACTIVE",
  "createdAt": "2026-10-21T09:30:12.481Z",
  "updatedAt": "2026-10-21T09:30:12.481Z"
}
```

### List with pagination, search, filter and sort

```bash
curl "http://localhost:8080/api/v1/employees?search=engineering&status=ACTIVE&page=0&size=5&sort=salary,desc"
```

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Asha",
      "lastName": "Menon",
      "email": "asha.menon@example.com",
      "phone": "+919876543210",
      "department": "Engineering",
      "designation": "Software Engineer",
      "salary": 85000.00,
      "dateOfJoining": "2024-06-15",
      "status": "ACTIVE",
      "createdAt": "2026-10-21T09:30:12.481Z",
      "updatedAt": "2026-10-21T09:30:12.481Z"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Get by ID

```bash
curl http://localhost:8080/api/v1/employees/1
```

Returns `200 OK` with the employee object shown above.

### Update

```bash
curl -X PUT http://localhost:8080/api/v1/employees/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Asha",
    "lastName": "Menon",
    "email": "asha.menon@example.com",
    "phone": "+919876543210",
    "department": "Engineering",
    "designation": "Senior Software Engineer",
    "salary": 110000.00,
    "dateOfJoining": "2024-06-15",
    "status": "ON_LEAVE"
  }'
```

Returns `200 OK` with the updated employee.

### Delete

```bash
curl -i -X DELETE http://localhost:8080/api/v1/employees/1
```

Returns `204 No Content`.

## Error responses

All errors share one shape.

**400 – validation failed**

```json
{
  "timestamp": "2026-10-21T09:35:01.120Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/employees",
  "fieldErrors": {
    "firstName": "First name is required",
    "email": "Email must be a valid email address",
    "salary": "Salary must be greater than zero"
  }
}
```

**404 – not found**

```json
{
  "timestamp": "2026-10-21T09:36:44.902Z",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 99",
  "path": "/api/v1/employees/99"
}
```

**409 – duplicate email**

```json
{
  "timestamp": "2026-10-21T09:37:10.511Z",
  "status": 409,
  "error": "Conflict",
  "message": "Employee already exists with email: asha.menon@example.com",
  "path": "/api/v1/employees"
}
```

| Status | When |
|---|---|
| 400 | Validation errors, malformed JSON, bad enum/date, invalid path/query value, invalid sort field |
| 404 | Employee ID does not exist |
| 405 | HTTP method not supported |
| 409 | Duplicate email / DB integrity conflict |
| 500 | Unexpected error (details logged server-side only) |

## Publishing to GitHub

```bash
git init
git add .
git commit -m "Employee Management REST API"
git branch -M main
git remote add origin https://github.com/<your-username>/employee-management.git
git push -u origin main
```

## Possible improvements

Flyway migrations, Spring Security/JWT, soft delete, `PATCH` for partial updates, Dockerfile for the app,
Testcontainers integration tests against real PostgreSQL/MySQL, and auditing of who changed a record.
