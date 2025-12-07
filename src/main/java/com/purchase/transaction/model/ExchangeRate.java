package com.purchase.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExchangeRate {
    private String currencyCode;
    private String currencyName;
    private BigDecimal exchangeRate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;
    private String countryCode;
    
    public ExchangeRate() {
    }
    
    public ExchangeRate(String currencyCode, String currencyName, BigDecimal exchangeRate, LocalDate effectiveDate, String countryCode) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.exchangeRate = exchangeRate;
        this.effectiveDate = effectiveDate;
        this.countryCode = countryCode;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public String getCurrencyName() {
        return currencyName;
    }
    
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
    
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
    
    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
