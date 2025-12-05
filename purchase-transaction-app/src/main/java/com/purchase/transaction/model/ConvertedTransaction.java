package com.purchase.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ConvertedTransaction {
    private String transactionId;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    private BigDecimal originalAmountUsd;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate exchangeRateDate;
    
    public ConvertedTransaction() {
    }
    
    public ConvertedTransaction(String transactionId, String description, LocalDate transactionDate, BigDecimal originalAmountUsd, String currencyCode, BigDecimal exchangeRate, BigDecimal convertedAmount, LocalDate exchangeRateDate) {
        this.transactionId = transactionId;
        this.description = description;
        this.transactionDate = transactionDate;
        this.originalAmountUsd = originalAmountUsd;
        this.currencyCode = currencyCode;
        this.exchangeRate = exchangeRate;
        this.convertedAmount = convertedAmount;
        this.exchangeRateDate = exchangeRateDate;
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
    
    public BigDecimal getOriginalAmountUsd() {
        return originalAmountUsd;
    }
    
    public void setOriginalAmountUsd(BigDecimal originalAmountUsd) {
        this.originalAmountUsd = originalAmountUsd;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
    
    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
    
    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }
    
    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }
    
    public LocalDate getExchangeRateDate() {
        return exchangeRateDate;
    }
    
    public void setExchangeRateDate(LocalDate exchangeRateDate) {
        this.exchangeRateDate = exchangeRateDate;
    }
}
