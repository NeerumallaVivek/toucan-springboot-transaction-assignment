package com.example.transactionstarter.exception;

import com.example.transactionstarter.model.TransactionStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(TransactionStatus currentStatus, TransactionStatus newStatus) {
        super(String.format("Cannot transition transaction status from %s to %s", currentStatus, newStatus));
    }
}
