package com.example.transactionstarter.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String transactionId, boolean isId) {
        super("Transaction not found with ID: " + transactionId);
    }
}
