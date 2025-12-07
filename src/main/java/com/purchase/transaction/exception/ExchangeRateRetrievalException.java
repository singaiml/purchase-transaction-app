package com.purchase.transaction.exception;

public class ExchangeRateRetrievalException extends RuntimeException {
    public ExchangeRateRetrievalException(String message) { super(message); }
    public ExchangeRateRetrievalException(String message, Throwable cause) { super(message, cause); }
}
