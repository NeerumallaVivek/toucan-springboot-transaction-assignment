# Toucan Payments - Customer Transactions Service

This repository contains the completed implementation for the Toucan Payments Java Spring Boot Engineering Challenge.

---

## Overview

The service provides a RESTful API for managing customer financial transactions. It supports creating transactions, retrieving a transaction by its identifier, updating transaction statuses according to strict lifecycle transition rules, and querying all transactions associated with a given customer.

---

## Technologies Used

* **Java 17**
* **Spring Boot 3.5.5**
  * Spring Web (MVC / REST)
  * Spring Data JPA / Hibernate ORM
  * Spring Boot Starter Validation (Jakarta Bean Validation)
  * Spring Boot Starter Test (JUnit Jupiter, Mockito, MockMvc)
* **H2 Database** (In-memory SQL database)
* **Maven** (with bundled Maven Wrapper `mvnw` / `mvnw.cmd`)

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/example/transactionstarter/
│   │       ├── controller/
│   │       │   └── TransactionController.java
│   │       ├── dto/
│   │       │   ├── CreateTransactionRequest.java
│   │       │   ├── UpdateTransactionStatusRequest.java
│   │       │   └── TransactionResponse.java
│   │       ├── exception/
│   │       │   ├── DuplicateTransactionException.java
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   ├── InvalidStatusTransitionException.java
│   │       │   └── TransactionNotFoundException.java
│   │       ├── model/
│   │       │   ├── Transaction.java
│   │       │   ├── TransactionStatus.java
│   │       │   └── TransactionType.java
│   │       ├── repository/
│   │       │   └── TransactionRepository.java
│   │       ├── service/
│   │       │   └── TransactionService.java
│   │       └── TransactionStarterApplication.java
│   │
│   └── resources/
│       └── application.yml
│
└── test/
    └── java/
        └── com/example/transactionstarter/
            ├── TransactionStarterApplicationTests.java
            ├── controller/
            │   └── TransactionControllerTest.java
            └── service/
                └── TransactionServiceTest.java
```

---

## Transaction Model & Fields

Every transaction record contains:

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `transactionId` | String | Unique transaction identifier (Primary Key) |
| `customerId` | String | Identifier for the customer |
| `amount` | BigDecimal | Monetary amount (must be positive, > 0.00) |
| `currency` | String | 3-letter currency code (e.g. `USD`, `EUR`, `GBP`) |
| `type` | Enum | Transaction type: `PAYMENT`, `REFUND`, `TRANSFER` |
| `status` | Enum | Transaction lifecycle status: `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `createdAt` | Instant | Creation timestamp (auto-populated) |
| `updatedAt` | Instant | Last update timestamp (auto-populated) |

---

## Implemented REST APIs

### 1. Create Transaction
* **Method**: `POST`
* **Path**: `/api/transactions`
* **Request Body**:
  ```json
  {
    "transactionId": "TX-1001",
    "customerId": "CUST-001",
    "amount": 150.75,
    "currency": "USD",
    "type": "PAYMENT"
  }
  ```
* **Success Response (`201 Created`)**:
  ```json
  {
    "transactionId": "TX-1001",
    "customerId": "CUST-001",
    "amount": 150.75,
    "currency": "USD",
    "type": "PAYMENT",
    "status": "PENDING",
    "createdAt": "2026-08-30T16:24:08.981Z",
    "updatedAt": "2026-08-30T16:24:08.981Z"
  }
  ```
* **Error Responses**:
  * `400 Bad Request` — Validation failure (blank fields, non-positive amount, invalid currency).
  * `409 Conflict` — Duplicate `transactionId`.

---

### 2. Get Transaction
* **Method**: `GET`
* **Path**: `/api/transactions/{transactionId}`
* **Success Response (`200 OK`)**:
  ```json
  {
    "transactionId": "TX-1001",
    "customerId": "CUST-001",
    "amount": 150.75,
    "currency": "USD",
    "type": "PAYMENT",
    "status": "PENDING",
    "createdAt": "2026-08-30T16:24:08.981Z",
    "updatedAt": "2026-08-30T16:24:08.981Z"
  }
  ```
* **Error Response**:
  * `404 Not Found` — Transaction ID does not exist.

---

### 3. Update Transaction Status
* **Method**: `PATCH`
* **Path**: `/api/transactions/{transactionId}/status`
* **Request Body**:
  ```json
  {
    "status": "COMPLETED"
  }
  ```
* **Success Response (`200 OK`)**:
  ```json
  {
    "transactionId": "TX-1001",
    "customerId": "CUST-001",
    "amount": 150.75,
    "currency": "USD",
    "type": "PAYMENT",
    "status": "COMPLETED",
    "createdAt": "2026-08-30T16:24:08.981Z",
    "updatedAt": "2026-08-30T16:25:00.000Z"
  }
  ```
* **Error Responses**:
  * `400 Bad Request` — Invalid status value or illegal lifecycle transition from a terminal state.
  * `404 Not Found` — Transaction ID does not exist.

---

### 4. Get Customer Transactions
* **Method**: `GET`
* **Path**: `/api/customers/{customerId}/transactions`
* **Success Response (`200 OK`)**:
  ```json
  [
    {
      "transactionId": "TX-1001",
      "customerId": "CUST-001",
      "amount": 150.75,
      "currency": "USD",
      "type": "PAYMENT",
      "status": "COMPLETED",
      "createdAt": "2026-08-30T16:24:08.981Z",
      "updatedAt": "2026-08-30T16:25:00.000Z"
    }
  ]
  ```
  *(Returns an empty JSON list `[]` with status `200 OK` if the customer has no transactions).*

---

## Validation Behavior

* **`transactionId`**: Mandatory non-blank string (`@NotBlank`, max 64 characters); uniqueness checked before persistence.
* **`customerId`**: Mandatory non-blank string (`@NotBlank`, max 64 characters).
* **`amount`**: Mandatory `BigDecimal` strictly greater than `0.00` (`@DecimalMin("0.01")`).
* **`currency`**: Mandatory 3-letter alphabetic ISO code (`@Pattern(regexp = "^[A-Za-z]{3}$")`), normalized to uppercase.
* **`type`**: Mandatory valid enum value (`PAYMENT`, `REFUND`, `TRANSFER`).
* **`status`**: Mandatory on update (`PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`); automatically initialized to `PENDING` upon creation.

---

## Transaction Status-Transition Rules

```text
               ┌─────────────┐
               │   PENDING   │ (Initial State)
               └──────┬──────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
  ┌───────────┐ ┌───────────┐ ┌───────────┐
  │ COMPLETED │ │  FAILED   │ │ CANCELLED │
  └───────────┘ └───────────┘ └───────────┘
   (Terminal)    (Terminal)    (Terminal)
```

* Transactions start in the `PENDING` state upon creation.
* A `PENDING` transaction can transition to `COMPLETED`, `FAILED`, or `CANCELLED`.
* `COMPLETED`, `FAILED`, and `CANCELLED` are terminal states. Any attempt to modify the status of a transaction in a terminal state is rejected with HTTP `400 Bad Request`.

---

## Error Handling

Centralized exception handling is implemented via `@RestControllerAdvice` in `GlobalExceptionHandler`:

* `400 Bad Request` — Validation failures, illegal status transitions, unparseable JSON/enum values.
* `404 Not Found` — Resource not found.
* `409 Conflict` — Duplicate transaction ID.
* `500 Internal Server Error` — Unhandled server errors (sanitized without exposing internal stack traces).

Example error payload:
```json
{
  "timestamp": "2026-08-30T16:24:15.260Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request body validation failed",
  "details": [
    "amount: Amount must be greater than zero",
    "currency: Currency must be a valid 3-letter currency code"
  ]
}
```

---

## Database Information

* **Engine**: H2 In-Memory Database (`jdbc:h2:mem:transactions;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`).
* **DDL Mode**: `create-drop` (tables generated from JPA entities).
* **Console**: H2 Web Console is enabled at `/h2-console` (`sa` / empty password).

---

## How to Build and Run

### Run Locally
* **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```
* **Windows**:
  ```bat
  mvnw.cmd spring-boot:run
  ```
The server will start on port `8080`.

### Run Tests
* **Linux / macOS**:
  ```bash
  ./mvnw clean test
  ```
* **Windows**:
  ```bat
  mvnw.cmd clean test
  ```

---

## Test Suite Summary

The automated test suite contains **18 automated tests**:
* **10 Integration Tests (`TransactionControllerTest`)**: End-to-end API execution using `MockMvc` verifying all 4 operations, status transitions, validation errors, 404 handling, and 409 duplicate ID rejection.
* **7 Service Unit Tests (`TransactionServiceTest`)**: Isolated business logic tests verifying repository interactions and state machine rules with Mockito.
* **1 Context Smoke Test (`TransactionStarterApplicationTests`)**: Verifies clean Spring ApplicationContext initialization.
