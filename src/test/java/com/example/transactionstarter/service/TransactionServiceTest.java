package com.example.transactionstarter.service;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.TransactionResponse;
import com.example.transactionstarter.dto.UpdateTransactionStatusRequest;
import com.example.transactionstarter.exception.DuplicateTransactionException;
import com.example.transactionstarter.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.exception.TransactionNotFoundException;
import com.example.transactionstarter.model.Transaction;
import com.example.transactionstarter.model.TransactionStatus;
import com.example.transactionstarter.model.TransactionType;
import com.example.transactionstarter.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("createTransaction saves new transaction with PENDING status")
    void testCreateTransaction_Success() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX-1", "CUST-1", new BigDecimal("100.00"), "usd", TransactionType.PAYMENT
        );

        when(transactionRepository.existsByTransactionId("TX-1")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals("TX-1", response.getTransactionId());
        assertEquals("CUST-1", response.getCustomerId());
        assertEquals("USD", response.getCurrency());
        assertEquals(TransactionStatus.PENDING, response.getStatus());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction throws DuplicateTransactionException when ID exists")
    void testCreateTransaction_DuplicateId_ThrowsException() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX-1", "CUST-1", new BigDecimal("100.00"), "USD", TransactionType.PAYMENT
        );

        when(transactionRepository.existsByTransactionId("TX-1")).thenReturn(true);

        assertThrows(DuplicateTransactionException.class, () -> transactionService.createTransaction(request));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransactionById returns transaction when found")
    void testGetTransactionById_Success() {
        Transaction tx = new Transaction("TX-1", "CUST-1", new BigDecimal("100.00"), "USD", TransactionType.PAYMENT, TransactionStatus.PENDING);
        when(transactionRepository.findByTransactionId("TX-1")).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.getTransactionById("TX-1");

        assertNotNull(response);
        assertEquals("TX-1", response.getTransactionId());
    }

    @Test
    @DisplayName("getTransactionById throws TransactionNotFoundException when ID does not exist")
    void testGetTransactionById_NotFound_ThrowsException() {
        when(transactionRepository.findByTransactionId("TX-MISSING")).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById("TX-MISSING"));
    }

    @Test
    @DisplayName("updateTransactionStatus transitions from PENDING to FAILED successfully")
    void testUpdateTransactionStatus_PendingToFailed_Success() {
        Transaction tx = new Transaction("TX-1", "CUST-1", new BigDecimal("100.00"), "USD", TransactionType.PAYMENT, TransactionStatus.PENDING);
        when(transactionRepository.findByTransactionId("TX-1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTransactionStatusRequest request = new UpdateTransactionStatusRequest(TransactionStatus.FAILED);
        TransactionResponse response = transactionService.updateTransactionStatus("TX-1", request);

        assertEquals(TransactionStatus.FAILED, response.getStatus());
    }

    @Test
    @DisplayName("updateTransactionStatus rejects transition from terminal FAILED to COMPLETED")
    void testUpdateTransactionStatus_FailedToCompleted_ThrowsException() {
        Transaction tx = new Transaction("TX-1", "CUST-1", new BigDecimal("100.00"), "USD", TransactionType.PAYMENT, TransactionStatus.FAILED);
        when(transactionRepository.findByTransactionId("TX-1")).thenReturn(Optional.of(tx));

        UpdateTransactionStatusRequest request = new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);

        assertThrows(InvalidStatusTransitionException.class, () -> transactionService.updateTransactionStatus("TX-1", request));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransactionsByCustomerId returns empty list when customer has no transactions")
    void testGetTransactionsByCustomerId_Empty() {
        when(transactionRepository.findByCustomerId("CUST-NONE")).thenReturn(List.of());

        List<TransactionResponse> results = transactionService.getTransactionsByCustomerId("CUST-NONE");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
