package com.example.transactionstarter.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }

    public DuplicateTransactionException(String transactionId, boolean isId) {
        super("Transaction already exists with ID: " + transactionId);
    }
}
