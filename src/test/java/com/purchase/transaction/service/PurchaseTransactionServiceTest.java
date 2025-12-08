package com.purchase.transaction.service;

import com.purchase.transaction.exception.TransactionValidationException;
import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.repository.ITransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.purchase.transaction.model.ExchangeRate;
import com.purchase.transaction.model.ConvertedTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PurchaseTransactionService Tests")
@ExtendWith(MockitoExtension.class)
class PurchaseTransactionServiceTest {
    
    @Mock
    private ITransactionRepository transactionRepository;
    
    @Mock
    private IExchangeRateService exchangeRateService;
    
    private PurchaseTransactionService purchaseTransactionService;
    
    @BeforeEach
    void setUp() {
        purchaseTransactionService = new PurchaseTransactionService(transactionRepository, exchangeRateService);
    }
    
    @Test
    @DisplayName("Should create transaction successfully with valid parameters")
    void testCreateTransactionSuccess() {
        String description = "Test Purchase";
        LocalDate date = LocalDate.now().minusDays(1);
        BigDecimal amount = new BigDecimal("100.00");
        
        PurchaseTransaction mockTransaction = PurchaseTransaction.create(description, date, amount);
        when(transactionRepository.save(any(PurchaseTransaction.class))).thenReturn(mockTransaction);
        
        PurchaseTransaction result = purchaseTransactionService.createTransaction(description, date, amount);
        
        assertNotNull(result);
        assertEquals(description, result.getDescription());
        assertEquals(date, result.getTransactionDate());
        assertEquals(amount, result.getAmount());
        verify(transactionRepository, times(1)).save(any(PurchaseTransaction.class));
    }
    
    @Test
    @DisplayName("Should throw exception when description is null")
    void testCreateTransactionNullDescription() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction(null, LocalDate.now(), new BigDecimal("100.00")));
    }
    
    @Test
    @DisplayName("Should throw exception when description is empty")
    void testCreateTransactionEmptyDescription() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("", LocalDate.now(), new BigDecimal("100.00")));
    }
    
    @Test
    @DisplayName("Should throw exception when description exceeds 50 characters")
    void testCreateTransactionLongDescription() {
        String longDescription = "a".repeat(51);
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction(longDescription, LocalDate.now(), new BigDecimal("100.00")));
    }
    
    @Test
    @DisplayName("Should throw exception when date is null")
    void testCreateTransactionNullDate() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("Valid", null, new BigDecimal("100.00")));
    }
    
    @Test
    @DisplayName("Should throw exception when date is in the future")
    void testCreateTransactionFutureDate() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("Valid", LocalDate.now().plusDays(1), new BigDecimal("100.00")));
    }
    
    @Test
    @DisplayName("Should throw exception when amount is null")
    void testCreateTransactionNullAmount() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("Valid", LocalDate.now(), null));
    }
    
    @Test
    @DisplayName("Should throw exception when amount is zero or negative")
    void testCreateTransactionInvalidAmount() {
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("Valid", LocalDate.now(), new BigDecimal("0")));
        
        assertThrows(TransactionValidationException.class, 
            () -> purchaseTransactionService.createTransaction("Valid", LocalDate.now(), new BigDecimal("-100")));
    }
    
    @Test
    @DisplayName("Should retrieve transaction by ID")
    void testGetTransactionSuccess() {
        String transactionId = UUID.randomUUID().toString();
        PurchaseTransaction mockTransaction = PurchaseTransaction.create("Test", LocalDate.now(), new BigDecimal("100"));
        mockTransaction.setTransactionId(transactionId);
        
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
        
        PurchaseTransaction result = purchaseTransactionService.getTransaction(transactionId);
        
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        verify(transactionRepository, times(1)).findById(transactionId);
    }
    
    @Test
    @DisplayName("Should return all transactions")
    void testGetAllTransactionsSuccess() {
        List<PurchaseTransaction> mockTransactions = new ArrayList<>();
        mockTransactions.add(PurchaseTransaction.create("Purchase 1", LocalDate.now(), new BigDecimal("100")));
        mockTransactions.add(PurchaseTransaction.create("Purchase 2", LocalDate.now(), new BigDecimal("200")));
        
        when(transactionRepository.findAll()).thenReturn(mockTransactions);
        
        List<PurchaseTransaction> result = purchaseTransactionService.getAllTransactions();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should delete transaction successfully")
    void testDeleteTransactionSuccess() {
        String transactionId = UUID.randomUUID().toString();
        when(transactionRepository.deleteById(transactionId)).thenReturn(true);
        
        assertDoesNotThrow(() -> purchaseTransactionService.deleteTransaction(transactionId));
        verify(transactionRepository, times(1)).deleteById(transactionId);
    }

    @Test
    @DisplayName("Should convert transaction using latest rate on or before purchase date within 6 months")
    void testConvertTransactionUsesLatestWithinSixMonths() {
        String transactionId = "tx-100";
        LocalDate purchaseDate = LocalDate.of(2025, 12, 1);
        PurchaseTransaction tx = PurchaseTransaction.create("Test", purchaseDate, new BigDecimal("100.00"));
        tx.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        // Mock exchangeRateService to return the most recent rate within the 6-month window (2025-06-01)
        ExchangeRate foundRate = new ExchangeRate("EUR", "Euro", new BigDecimal("1.2345"), LocalDate.of(2025,6,1), "EU");
        LocalDate cutoff = purchaseDate.minusMonths(6);
        when(exchangeRateService.getMostRecentExchangeRateWithinRange(eq("Euro Zone"), eq("Euro"), isNull(), eq(cutoff), eq(purchaseDate))).thenReturn(Optional.of(foundRate));

        ConvertedTransaction converted = purchaseTransactionService.convertTransaction(transactionId, "Euro Zone", "Euro", null);
        assertNotNull(converted);
        assertEquals("EUR", converted.getCurrencyCode());
        assertEquals(foundRate.getExchangeRate(), converted.getExchangeRate());
        // Amount 100 * 1.2345 = 123.45 -> rounded 123.45
        assertEquals(new BigDecimal("123.45"), converted.getConvertedAmount());
    }

    @Test
    @DisplayName("Should fail conversion when no rate within 6 months")
    void testConvertTransactionFailsWhenNoRateWithinSixMonths() {
        String transactionId = "tx-200";
        LocalDate purchaseDate = LocalDate.of(2025, 12, 1);
        PurchaseTransaction tx = PurchaseTransaction.create("Test", purchaseDate, new BigDecimal("100.00"));
        tx.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        LocalDate cutoff = purchaseDate.minusMonths(6);
        when(exchangeRateService.getMostRecentExchangeRateWithinRange(eq("Euro Zone"), eq("Euro"), isNull(), eq(cutoff), eq(purchaseDate))).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> purchaseTransactionService.convertTransaction(transactionId, "Euro Zone", "Euro", null));
        assertTrue(ex.getMessage().contains("Cannot convert purchase to target currency"));
    }

    @Test
    @DisplayName("Should round converted amount to two decimals")
    void testConvertTransactionRounding() {
        String transactionId = "tx-300";
        LocalDate purchaseDate = LocalDate.of(2025, 12, 1);
        PurchaseTransaction tx = PurchaseTransaction.create("Test", purchaseDate, new BigDecimal("100.00"));
        tx.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        ExchangeRate rate = new ExchangeRate("EUR", "Euro", new BigDecimal("0.333333"), LocalDate.of(2025,11,30), "EU");
        LocalDate cutoff = purchaseDate.minusMonths(6);
        when(exchangeRateService.getMostRecentExchangeRateWithinRange(eq("Euro Zone"), eq("Euro"), isNull(), eq(cutoff), eq(purchaseDate))).thenReturn(Optional.of(rate));

        ConvertedTransaction converted = purchaseTransactionService.convertTransaction(transactionId, "Euro Zone", "Euro", null);
        // 100 * 0.333333 = 33.3333 -> rounded to 33.33
        assertEquals(new BigDecimal("33.33"), converted.getConvertedAmount());
    }
}
