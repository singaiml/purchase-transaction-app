package com.purchase.transaction.controller;

import java.math.BigDecimal;

public class TransactionRequest {
    private String description;
    private String transactionDate;
    private BigDecimal amount;

    public TransactionRequest() {}

    public TransactionRequest(String description, String transactionDate, BigDecimal amount) {
        this.description = description;
        this.transactionDate = transactionDate;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
