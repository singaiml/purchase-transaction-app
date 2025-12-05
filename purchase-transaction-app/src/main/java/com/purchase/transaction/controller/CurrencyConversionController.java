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
            @RequestParam String currency) {
        log.info("Received request to convert transaction {} to currency: {}", transactionId, currency);
        ConvertedTransaction converted = transactionService.convertTransaction(transactionId, currency);
        return ResponseEntity.ok(converted);
    }
    
    @GetMapping("/currencies/available")
    public ResponseEntity<List<String>> getAvailableCurrencies() {
        log.info("Received request to get available currencies");
        List<String> currencies = transactionService.getAvailableCurrencies();
        return ResponseEntity.ok(currencies);
    }
}
