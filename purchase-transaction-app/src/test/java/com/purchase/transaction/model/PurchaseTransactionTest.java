package com.purchase.transaction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PurchaseTransaction Model Tests")
class PurchaseTransactionTest {
    
    private PurchaseTransaction transaction;
    private String testDescription;
    private LocalDate testDate;
    private BigDecimal testAmount;
    
    @BeforeEach
    void setUp() {
        testDescription = "Test Purchase";
        testDate = LocalDate.now().minusDays(1);
        testAmount = new BigDecimal("100.00");
    }
    
    @Test
    @DisplayName("Should create transaction with valid parameters")
    void testCreateTransaction() {
        transaction = PurchaseTransaction.create(testDescription, testDate, testAmount);
        
        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());
        assertEquals(testDescription, transaction.getDescription());
        assertEquals(testDate, transaction.getTransactionDate());
        assertEquals(testAmount, transaction.getAmount());
    }
    
    @Test
    @DisplayName("Should validate valid description")
    void testIsDescriptionValid() {
        PurchaseTransaction tx = new PurchaseTransaction();
        tx.setDescription("Valid Description");
        assertTrue(tx.isDescriptionValid());
        
        tx.setDescription("");
        assertFalse(tx.isDescriptionValid());
        
        tx.setDescription("a".repeat(51));
        assertFalse(tx.isDescriptionValid());
        
        tx.setDescription(null);
        assertFalse(tx.isDescriptionValid());
    }
    
    @Test
    @DisplayName("Should validate valid amount")
    void testIsAmountValid() {
        PurchaseTransaction tx = new PurchaseTransaction();
        
        tx.setAmount(new BigDecimal("100.00"));
        assertTrue(tx.isAmountValid());
        
        tx.setAmount(new BigDecimal("0.01"));
        assertTrue(tx.isAmountValid());
        
        tx.setAmount(new BigDecimal("0"));
        assertFalse(tx.isAmountValid());
        
        tx.setAmount(new BigDecimal("-100"));
        assertFalse(tx.isAmountValid());
        
        tx.setAmount(null);
        assertFalse(tx.isAmountValid());
    }
    
    @Test
    @DisplayName("Should validate valid transaction date")
    void testIsTransactionDateValid() {
        PurchaseTransaction tx = new PurchaseTransaction();
        
        tx.setTransactionDate(LocalDate.now().minusDays(1));
        assertTrue(tx.isTransactionDateValid());
        
        tx.setTransactionDate(LocalDate.now());
        assertTrue(tx.isTransactionDateValid());
        
        tx.setTransactionDate(LocalDate.now().plusDays(1));
        assertFalse(tx.isTransactionDateValid());
        
        tx.setTransactionDate(null);
        assertFalse(tx.isTransactionDateValid());
    }
    
    @Test
    @DisplayName("Should validate full transaction")
    void testIsValid() {
        transaction = PurchaseTransaction.create(testDescription, testDate, testAmount);
        assertTrue(transaction.isValid());
    }
    
    @Test
    @DisplayName("Should generate unique transaction IDs")
    void testUniqueTransactionIds() {
        PurchaseTransaction tx1 = PurchaseTransaction.create("Purchase 1", testDate, testAmount);
        PurchaseTransaction tx2 = PurchaseTransaction.create("Purchase 2", testDate, testAmount);
        
        assertNotEquals(tx1.getTransactionId(), tx2.getTransactionId());
    }
    
    @Test
    @DisplayName("Should handle getters and setters correctly")
    void testGettersSetters() {
        transaction = new PurchaseTransaction();
        String txId = UUID.randomUUID().toString();
        String description = "New Description";
        LocalDate date = LocalDate.now();
        BigDecimal amount = new BigDecimal("250.50");
        LocalDate createdAt = LocalDate.now();
        
        transaction.setTransactionId(txId);
        transaction.setDescription(description);
        transaction.setTransactionDate(date);
        transaction.setAmount(amount);
        transaction.setCreatedAt(createdAt);
        
        assertEquals(txId, transaction.getTransactionId());
        assertEquals(description, transaction.getDescription());
        assertEquals(date, transaction.getTransactionDate());
        assertEquals(amount, transaction.getAmount());
        assertEquals(createdAt, transaction.getCreatedAt());
    }
}
