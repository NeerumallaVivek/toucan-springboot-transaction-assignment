package com.example.transactionstarter.controller;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.UpdateTransactionStatusRequest;
import com.example.transactionstarter.model.TransactionStatus;
import com.example.transactionstarter.model.TransactionType;
import com.example.transactionstarter.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario 1: Successful transaction creation returns 201 Created and initial PENDING status")
    void testCreateTransaction_Success() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX-1001",
                "CUST-001",
                new BigDecimal("150.75"),
                "USD",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId", is("TX-1001")))
                .andExpect(jsonPath("$.customerId", is("CUST-001")))
                .andExpect(jsonPath("$.amount", is(150.75)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.type", is("PAYMENT")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Scenario 2: Validation failure on invalid request fields returns 400 Bad Request")
    void testCreateTransaction_ValidationFailure() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "", // Blank ID
                "", // Blank Customer ID
                new BigDecimal("-50.00"), // Negative amount
                "INVALID_CURRENCY", // Invalid currency format
                null // Missing type
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("Scenario 3: Duplicate Transaction ID returns 409 Conflict")
    void testCreateTransaction_DuplicateId() throws Exception {
        CreateTransactionRequest initial = new CreateTransactionRequest(
                "TX-DUP-01",
                "CUST-001",
                new BigDecimal("100.00"),
                "USD",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        CreateTransactionRequest duplicate = new CreateTransactionRequest(
                "TX-DUP-01",
                "CUST-002",
                new BigDecimal("200.00"),
                "EUR",
                TransactionType.REFUND
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("TX-DUP-01")));
    }

    @Test
    @DisplayName("Scenario 4: Get non-existent transaction returns 404 Not Found")
    void testGetTransaction_NotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/NON-EXISTENT-ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("NON-EXISTENT-ID")));
    }

    @Test
    @DisplayName("Scenario 5: Successful transaction retrieval by ID returns 200 OK")
    void testGetTransaction_Success() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX-GET-01",
                "CUST-001",
                new BigDecimal("75.50"),
                "GBP",
                TransactionType.TRANSFER
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions/TX-GET-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TX-GET-01")))
                .andExpect(jsonPath("$.customerId", is("CUST-001")))
                .andExpect(jsonPath("$.amount", is(75.50)))
                .andExpect(jsonPath("$.currency", is("GBP")))
                .andExpect(jsonPath("$.type", is("TRANSFER")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("Scenario 6: Successful status update from PENDING to COMPLETED returns 200 OK")
    void testUpdateTransactionStatus_Success() throws Exception {
        CreateTransactionRequest createReq = new CreateTransactionRequest(
                "TX-UPDATE-01",
                "CUST-001",
                new BigDecimal("50.00"),
                "USD",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        UpdateTransactionStatusRequest updateReq = new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TX-UPDATE-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TX-UPDATE-01")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Scenario 7: Invalid status transition from terminal state COMPLETED returns 400 Bad Request")
    void testUpdateTransactionStatus_InvalidTransition() throws Exception {
        CreateTransactionRequest createReq = new CreateTransactionRequest(
                "TX-TERM-01",
                "CUST-001",
                new BigDecimal("99.99"),
                "USD",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // First transition: PENDING -> COMPLETED (valid)
        UpdateTransactionStatusRequest completeReq = new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);
        mockMvc.perform(patch("/api/transactions/TX-TERM-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk());

        // Second transition attempt: COMPLETED -> CANCELLED (invalid terminal transition)
        UpdateTransactionStatusRequest cancelReq = new UpdateTransactionStatusRequest(TransactionStatus.CANCELLED);
        mockMvc.perform(patch("/api/transactions/TX-TERM-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Cannot transition transaction status from COMPLETED to CANCELLED")));
    }

    @Test
    @DisplayName("Scenario 8: Status update for a non-existent transaction returns 404 Not Found")
    void testUpdateTransactionStatus_NotFound() throws Exception {
        UpdateTransactionStatusRequest updateReq = new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/UNKNOWN-TX/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("UNKNOWN-TX")));
    }

    @Test
    @DisplayName("Scenario 9: Customer transaction retrieval returns matching list of transactions")
    void testGetCustomerTransactions_Success() throws Exception {
        CreateTransactionRequest tx1 = new CreateTransactionRequest(
                "TX-CUST-1",
                "CUST-VIP-1",
                new BigDecimal("120.00"),
                "USD",
                TransactionType.PAYMENT
        );
        CreateTransactionRequest tx2 = new CreateTransactionRequest(
                "TX-CUST-2",
                "CUST-VIP-1",
                new BigDecimal("30.00"),
                "USD",
                TransactionType.REFUND
        );
        CreateTransactionRequest txOther = new CreateTransactionRequest(
                "TX-CUST-3",
                "CUST-OTHER",
                new BigDecimal("500.00"),
                "USD",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx2))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txOther))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers/CUST-VIP-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerId", is("CUST-VIP-1")))
                .andExpect(jsonPath("$[1].customerId", is("CUST-VIP-1")));
    }

    @Test
    @DisplayName("Scenario 10: Customer with no transactions returns empty list and 200 OK")
    void testGetCustomerTransactions_EmptyList() throws Exception {
        mockMvc.perform(get("/api/customers/CUST-EMPTY/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
