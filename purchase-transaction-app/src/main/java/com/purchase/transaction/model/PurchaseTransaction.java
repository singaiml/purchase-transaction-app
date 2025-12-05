package com.purchase.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class PurchaseTransaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String transactionId;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    private BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
    
    public PurchaseTransaction() {
    }
    
    public PurchaseTransaction(String transactionId, String description, LocalDate transactionDate, BigDecimal amount, LocalDate createdAt) {
        this.transactionId = transactionId;
        this.description = description;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.createdAt = createdAt;
    }
    
    public static PurchaseTransaction create(String description, LocalDate transactionDate, BigDecimal amount) {
        return new PurchaseTransaction(
            UUID.randomUUID().toString(),
            description,
            transactionDate,
            amount,
            LocalDate.now()
        );
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isDescriptionValid() {
        return description != null && !description.trim().isEmpty() && description.length() <= 50;
    }
    
    public boolean isAmountValid() {
        if (amount == null || amount.signum() <= 0) return false;
        return amount.scale() <= 2;
    }
    
    public boolean isTransactionDateValid() {
        return transactionDate != null && !transactionDate.isAfter(LocalDate.now());
    }
    
    public boolean isTransactionIdValid() {
        return transactionId != null && !transactionId.trim().isEmpty();
    }
    
    public boolean isValid() {
        return isTransactionIdValid() && isDescriptionValid() && isTransactionDateValid() && isAmountValid();
    }
}
