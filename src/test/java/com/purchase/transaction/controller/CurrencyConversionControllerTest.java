package com.purchase.transaction.controller;

import com.purchase.transaction.model.ConvertedTransaction;
import com.purchase.transaction.service.IPurchaseTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CurrencyConversionController Tests")
@ExtendWith(MockitoExtension.class)
class CurrencyConversionControllerTest {
    
    @Mock
    private IPurchaseTransactionService transactionService;
    
    private CurrencyConversionController currencyConversionController;
    
    @BeforeEach
    void setUp() {
        currencyConversionController = new CurrencyConversionController(transactionService);
    }
    
    @Test
    @DisplayName("Should convert transaction to target currency with OK status")
    void testConvertTransactionSuccess() {
        String transactionId = "tx-123";
        String country = "Euro Zone";
        String currency = "Euro";
        String country_currency_desc = null;
        
        ConvertedTransaction mockConverted = new ConvertedTransaction(
            transactionId,
            "Test Purchase",
            LocalDate.now(),
            new BigDecimal("100.00"),
            "EUR",
            new BigDecimal("1.10"),
            new BigDecimal("110.00"),
            LocalDate.now()
        );
        
        when(transactionService.convertTransaction(transactionId, country, currency, country_currency_desc)).thenReturn(mockConverted);
        
        ResponseEntity<ConvertedTransaction> response = currencyConversionController.convertTransaction(transactionId, country, currency, country_currency_desc);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConvertedTransaction body = response.getBody();
        assertNotNull(body);
        assertEquals(transactionId, body.getTransactionId());
        assertEquals("EUR", body.getCurrencyCode());
    }
    
    @Test
    @DisplayName("Should retrieve available currencies with OK status")
    void testGetAvailableCurrenciesSuccess() {
        List<String> mockCurrencies = new ArrayList<>();
        mockCurrencies.add("EUR");
        mockCurrencies.add("GBP");
        mockCurrencies.add("JPY");
        
        when(transactionService.getAvailableCurrencies()).thenReturn(mockCurrencies);
        
        ResponseEntity<List<String>> response = currencyConversionController.getAvailableCurrencies();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<String> body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.size());
        assertTrue(body.contains("EUR"));
        assertTrue(body.contains("GBP"));
        assertTrue(body.contains("JPY"));
    }
}
