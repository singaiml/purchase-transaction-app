package com.purchase.transaction.controller;

import com.purchase.transaction.model.ConvertedTransaction;
import com.purchase.transaction.service.IPurchaseTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversions")
public class CurrencyConversionController {
    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionController.class);
    
    private final IPurchaseTransactionService transactionService;
    
    public CurrencyConversionController(IPurchaseTransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<ConvertedTransaction> convertTransaction(
            @PathVariable String transactionId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String country_currency_desc) {
        log.info("Received request to convert transaction {} with country={}, currency={}, country_currency_desc={}", 
                transactionId, country, currency, country_currency_desc);
        
        // Validate that at least one parameter is provided
        if ((country == null || country.trim().isEmpty()) &&
            (currency == null || currency.trim().isEmpty()) &&
            (country_currency_desc == null || country_currency_desc.trim().isEmpty())) {
            log.warn("No currency parameters provided. Must specify at least one of: country, currency, or country_currency_desc");
            throw new IllegalArgumentException("Must specify at least one of: country, currency, or country_currency_desc");
        }
        
        ConvertedTransaction converted = transactionService.convertTransaction(
                transactionId, country, currency, country_currency_desc);
        return ResponseEntity.ok(converted);
    }
    
    @GetMapping("/currencies/available")
    public ResponseEntity<List<String>> getAvailableCurrencies() {
        log.info("Received request to get available currencies");
        List<String> currencies = transactionService.getAvailableCurrencies();
        return ResponseEntity.ok(currencies);
    }
}
