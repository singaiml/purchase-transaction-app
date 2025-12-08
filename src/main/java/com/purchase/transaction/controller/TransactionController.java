package com.purchase.transaction.controller;

import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.service.IPurchaseTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    
    private final IPurchaseTransactionService transactionService;
    
    public TransactionController(IPurchaseTransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    // JSON body handler - primary endpoint
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseTransaction> createTransactionJson(@RequestBody TransactionRequest request) {
        log.debug("Handling JSON request");
        return createTransactionInternal(request.getDescription(), request.getTransactionDate(), request.getAmount());
    }

    // Form URL encoded handler - for backward compatibility
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PurchaseTransaction> createTransactionForm(
            @RequestParam String description,
            @RequestParam String transactionDate,
            @RequestParam BigDecimal amount) {
        log.debug("Handling form-urlencoded request");
        return createTransactionInternal(description, transactionDate, amount);
    }

    private ResponseEntity<PurchaseTransaction> createTransactionInternal(String description, String transactionDate, BigDecimal amount) {
        log.info("Received request to create transaction: description='{}', date={}, amount={}", description, transactionDate, amount);
        try {
            LocalDate date = LocalDate.parse(transactionDate);
            PurchaseTransaction transaction = transactionService.createTransaction(description, date, amount);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid transactionDate format. Expected yyyy-MM-dd: " + transactionDate);
        }
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<PurchaseTransaction> getTransaction(@PathVariable String transactionId) {
        log.info("Received request to get transaction: {}", transactionId);
        PurchaseTransaction transaction = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }
    
    @GetMapping
    public ResponseEntity<List<PurchaseTransaction>> getAllTransactions() {
        log.info("Received request to get all transactions");
        List<PurchaseTransaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }
    
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Map<String, String>> deleteTransaction(@PathVariable String transactionId) {
        log.info("Received request to delete transaction: {}", transactionId);
        transactionService.deleteTransaction(transactionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Transaction deleted successfully");
        response.put("transactionId", transactionId);
        return ResponseEntity.ok(response);
    }
}