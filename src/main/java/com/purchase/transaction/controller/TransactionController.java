package com.purchase.transaction.controller;

import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.service.IPurchaseTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    
    @PostMapping
    public ResponseEntity<PurchaseTransaction> createTransaction(
            @RequestParam String description,
            @RequestParam String transactionDate,
            @RequestParam BigDecimal amount) {
        log.info("Received request to create transaction: description='{}', date={}, amount={}", description, transactionDate, amount);
        LocalDate date = LocalDate.parse(transactionDate);
        PurchaseTransaction transaction = transactionService.createTransaction(description, date, amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
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