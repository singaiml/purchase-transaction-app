package com.purchase.transaction.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Exception Classes Tests")
class ExceptionTest {
    
    @Test
    @DisplayName("Should create TransactionValidationException with message")
    void testTransactionValidationException() {
        String message = "Validation failed";
        TransactionValidationException exception = new TransactionValidationException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    @DisplayName("Should create TransactionNotFoundException with message")
    void testTransactionNotFoundException() {
        String message = "Transaction not found";
        TransactionNotFoundException exception = new TransactionNotFoundException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    @DisplayName("Should create ExchangeRateRetrievalException with message")
    void testExchangeRateRetrievalException() {
        String message = "Failed to retrieve exchange rate";
        ExchangeRateRetrievalException exception = new ExchangeRateRetrievalException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    @DisplayName("Should create TransactionValidationException with message and cause")
    void testTransactionValidationExceptionWithCause() {
        String message = "Validation error";
        Throwable cause = new RuntimeException("Root cause");
        TransactionValidationException exception = new TransactionValidationException(message, cause);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
