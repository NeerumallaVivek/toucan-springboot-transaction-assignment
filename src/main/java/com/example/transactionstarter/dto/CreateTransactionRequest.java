package com.example.transactionstarter.dto;

import com.example.transactionstarter.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateTransactionRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 64, message = "Transaction ID must not exceed 64 characters")
    private String transactionId;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 64, message = "Customer ID must not exceed 64 characters")
    private String customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a valid 3-letter currency code")
    private String currency;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    public CreateTransactionRequest() {
    }

    public CreateTransactionRequest(String transactionId, String customerId, BigDecimal amount, String currency, TransactionType type) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
