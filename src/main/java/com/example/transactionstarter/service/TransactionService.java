package com.example.transactionstarter.service;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.TransactionResponse;
import com.example.transactionstarter.dto.UpdateTransactionStatusRequest;
import com.example.transactionstarter.exception.DuplicateTransactionException;
import com.example.transactionstarter.exception.InvalidStatusTransitionException;
import com.example.transactionstarter.exception.TransactionNotFoundException;
import com.example.transactionstarter.model.Transaction;
import com.example.transactionstarter.model.TransactionStatus;
import com.example.transactionstarter.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        String txId = request.getTransactionId().trim();

        if (transactionRepository.existsByTransactionId(txId)) {
            throw new DuplicateTransactionException(txId, true);
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(txId);
        transaction.setCustomerId(request.getCustomerId().trim());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency().trim().toUpperCase());
        transaction.setType(request.getType());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID must not be blank");
        }

        Transaction transaction = transactionRepository.findByTransactionId(transactionId.trim())
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.trim(), true));

        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse updateTransactionStatus(String transactionId, UpdateTransactionStatusRequest request) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID must not be blank");
        }

        Transaction transaction = transactionRepository.findByTransactionId(transactionId.trim())
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.trim(), true));

        TransactionStatus currentStatus = transaction.getStatus();
        TransactionStatus requestedStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(requestedStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, requestedStatus);
        }

        transaction.setStatus(requestedStatus);
        Transaction updated = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID must not be blank");
        }

        return transactionRepository.findByCustomerId(customerId.trim())
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
