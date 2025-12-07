package com.purchase.transaction.controller;

import com.purchase.transaction.model.PurchaseTransaction;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TransactionController Tests")
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    
    @Mock
    private IPurchaseTransactionService transactionService;
    
    private TransactionController transactionController;
    
    @BeforeEach
    void setUp() {
        transactionController = new TransactionController(transactionService);
    }
    
    @Test
    @DisplayName("Should create transaction and return CREATED status")
    void testCreateTransactionSuccess() {
        String description = "Test Purchase";
        String dateStr = LocalDate.now().minusDays(1).toString();
        BigDecimal amount = new BigDecimal("100.00");
        
        PurchaseTransaction mockTransaction = PurchaseTransaction.create(description, LocalDate.parse(dateStr), amount);
        when(transactionService.createTransaction(anyString(), any(LocalDate.class), any(BigDecimal.class)))
            .thenReturn(mockTransaction);
        
        ResponseEntity<PurchaseTransaction> response = transactionController.createTransaction(description, dateStr, amount);
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PurchaseTransaction body = response.getBody();
        assertNotNull(body);
        assertEquals(description, body.getDescription());
    }
    
    @Test
    @DisplayName("Should retrieve transaction by ID with OK status")
    void testGetTransactionSuccess() {
        String transactionId = UUID.randomUUID().toString();
        PurchaseTransaction mockTransaction = PurchaseTransaction.create("Test", LocalDate.now(), new BigDecimal("100"));
        mockTransaction.setTransactionId(transactionId);
        
        when(transactionService.getTransaction(transactionId)).thenReturn(mockTransaction);
        
        ResponseEntity<PurchaseTransaction> response = transactionController.getTransaction(transactionId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PurchaseTransaction body = response.getBody();
        assertNotNull(body);
        assertEquals(transactionId, body.getTransactionId());
    }
    
    @Test
    @DisplayName("Should retrieve all transactions with OK status")
    void testGetAllTransactionsSuccess() {
        List<PurchaseTransaction> mockTransactions = new ArrayList<>();
        mockTransactions.add(PurchaseTransaction.create("Purchase 1", LocalDate.now(), new BigDecimal("100")));
        mockTransactions.add(PurchaseTransaction.create("Purchase 2", LocalDate.now(), new BigDecimal("200")));
        
        when(transactionService.getAllTransactions()).thenReturn(mockTransactions);
        
        ResponseEntity<List<PurchaseTransaction>> response = transactionController.getAllTransactions();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<PurchaseTransaction> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }
    
    @Test
    @DisplayName("Should delete transaction and return success message")
    void testDeleteTransactionSuccess() {
        String transactionId = UUID.randomUUID().toString();
        doNothing().when(transactionService).deleteTransaction(transactionId);
        
        ResponseEntity<Map<String, String>> response = transactionController.deleteTransaction(transactionId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Transaction deleted successfully", body.get("message"));
        assertEquals(transactionId, body.get("transactionId"));
    }
}
