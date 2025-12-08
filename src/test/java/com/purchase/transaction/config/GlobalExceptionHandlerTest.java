package com.purchase.transaction.config;

import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.exception.TransactionNotFoundException;
import com.purchase.transaction.exception.TransactionValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationException_returnsBadRequest() {
        TransactionValidationException ex = new TransactionValidationException("invalid");
        ResponseEntity<Map<String, Object>> resp = handler.handleValidationException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("message"));
    }

    @Test
    void handleNotFoundException_returnsNotFound() {
        TransactionNotFoundException ex = new TransactionNotFoundException("missing");
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFoundException(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("message"));
    }

    @Test
    void handleExchangeRateException_returnsServiceUnavailable() {
        ExchangeRateRetrievalException ex = new ExchangeRateRetrievalException("down");
        ResponseEntity<Map<String, Object>> resp = handler.handleExchangeRateException(ex);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("message"));
    }

    @Test
    void handleIllegalArgumentException_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");
        ResponseEntity<Map<String, Object>> resp = handler.handleIllegalArgumentException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("message"));
    }

    @Test
    void handleGenericException_returnsInternalServerError() {
        Exception ex = new Exception("fail");
        ResponseEntity<Map<String, Object>> resp = handler.handleGenericException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("message"));
    }
}
