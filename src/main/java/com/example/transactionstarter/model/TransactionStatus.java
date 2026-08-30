package com.example.transactionstarter.model;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean canTransitionTo(TransactionStatus target) {
        if (this == target) {
            return true;
        }
        if (this == PENDING) {
            return target == COMPLETED || target == FAILED || target == CANCELLED;
        }
        return false;
    }
}
