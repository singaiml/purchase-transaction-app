package com.purchase.transaction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConvertedTransaction Model Tests")
class ConvertedTransactionTest {
    
    private ConvertedTransaction convertedTransaction;
    
    @BeforeEach
    void setUp() {
        convertedTransaction = new ConvertedTransaction();
    }
    
    @Test
    @DisplayName("Should create converted transaction with all parameters")
    void testConvertedTransactionConstructor() {
        String transactionId = "tx-123";
        String description = "Test Purchase";
        LocalDate transactionDate = LocalDate.now();
        BigDecimal originalAmount = new BigDecimal("100.00");
        String currencyCode = "EUR";
        BigDecimal exchangeRate = new BigDecimal("1.10");
        BigDecimal convertedAmount = new BigDecimal("110.00");
        LocalDate exchangeRateDate = LocalDate.now();
        
        ConvertedTransaction ct = new ConvertedTransaction(
            transactionId, description, transactionDate, originalAmount, 
            currencyCode, exchangeRate, convertedAmount, exchangeRateDate
        );
        
        assertEquals(transactionId, ct.getTransactionId());
        assertEquals(description, ct.getDescription());
        assertEquals(transactionDate, ct.getTransactionDate());
        assertEquals(originalAmount, ct.getOriginalAmountUsd());
        assertEquals(currencyCode, ct.getCurrencyCode());
        assertEquals(exchangeRate, ct.getExchangeRate());
        assertEquals(convertedAmount, ct.getConvertedAmount());
        assertEquals(exchangeRateDate, ct.getExchangeRateDate());
    }
    
    @Test
    @DisplayName("Should set and get transaction ID")
    void testTransactionIdGetterSetter() {
        convertedTransaction.setTransactionId("tx-456");
        assertEquals("tx-456", convertedTransaction.getTransactionId());
    }
    
    @Test
    @DisplayName("Should set and get description")
    void testDescriptionGetterSetter() {
        convertedTransaction.setDescription("Converted Purchase");
        assertEquals("Converted Purchase", convertedTransaction.getDescription());
    }
    
    @Test
    @DisplayName("Should set and get transaction date")
    void testTransactionDateGetterSetter() {
        LocalDate date = LocalDate.now();
        convertedTransaction.setTransactionDate(date);
        assertEquals(date, convertedTransaction.getTransactionDate());
    }
    
    @Test
    @DisplayName("Should set and get original amount in USD")
    void testOriginalAmountUsdGetterSetter() {
        BigDecimal amount = new BigDecimal("200.00");
        convertedTransaction.setOriginalAmountUsd(amount);
        assertEquals(amount, convertedTransaction.getOriginalAmountUsd());
    }
    
    @Test
    @DisplayName("Should set and get currency code")
    void testCurrencyCodeGetterSetter() {
        convertedTransaction.setCurrencyCode("GBP");
        assertEquals("GBP", convertedTransaction.getCurrencyCode());
    }
    
    @Test
    @DisplayName("Should set and get exchange rate")
    void testExchangeRateGetterSetter() {
        BigDecimal rate = new BigDecimal("0.85");
        convertedTransaction.setExchangeRate(rate);
        assertEquals(rate, convertedTransaction.getExchangeRate());
    }
    
    @Test
    @DisplayName("Should set and get converted amount")
    void testConvertedAmountGetterSetter() {
        BigDecimal amount = new BigDecimal("170.00");
        convertedTransaction.setConvertedAmount(amount);
        assertEquals(amount, convertedTransaction.getConvertedAmount());
    }
    
    @Test
    @DisplayName("Should set and get exchange rate date")
    void testExchangeRateDateGetterSetter() {
        LocalDate date = LocalDate.now();
        convertedTransaction.setExchangeRateDate(date);
        assertEquals(date, convertedTransaction.getExchangeRateDate());
    }
    
    @Test
    @DisplayName("Should correctly represent converted transaction with multiple fields")
    void testCompleteConvertedTransaction() {
        ConvertedTransaction ct = new ConvertedTransaction(
            "tx-789",
            "International Purchase",
            LocalDate.now().minusDays(1),
            new BigDecimal("500.00"),
            "JPY",
            new BigDecimal("0.0067"),
            new BigDecimal("3350.00"),
            LocalDate.now()
        );
        
        assertNotNull(ct.getTransactionId());
        assertNotNull(ct.getDescription());
        assertNotNull(ct.getTransactionDate());
        assertNotNull(ct.getOriginalAmountUsd());
        assertEquals("JPY", ct.getCurrencyCode());
    }
}
