package com.purchase.transaction.service;

import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.exception.TransactionNotFoundException;
import com.purchase.transaction.exception.TransactionValidationException;
import com.purchase.transaction.model.ExchangeRate;
import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.repository.ITransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseTransactionServiceUnitTest {

    @Mock
    private ITransactionRepository repository;

    @Mock
    private IExchangeRateService exchangeRateService;

    @InjectMocks
    private PurchaseTransactionService service;

    @BeforeEach
    void setUp() {
        // injected by Mockito
    }

    @Test
    void createTransaction_validRoundsAndSaves() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseTransaction saved = service.createTransaction("desc", LocalDate.now(), new BigDecimal("10.129"));

        assertNotNull(saved.getTransactionId());
        assertEquals(new BigDecimal("10.13"), saved.getAmount());
        verify(repository).save(any());
    }

    @Test
    void createTransaction_invalidDescription_throws() {
        assertThrows(TransactionValidationException.class,
                () -> service.createTransaction("", LocalDate.now(), BigDecimal.TEN));
    }

    @Test
    void getTransaction_notFound_throws() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundException.class, () -> service.getTransaction("missing"));
    }

    @Test
    void deleteTransaction_notFound_throws() {
        when(repository.deleteById("id")).thenReturn(false);
        assertThrows(TransactionNotFoundException.class, () -> service.deleteTransaction("id"));
    }

    @Test
    void convertTransaction_noRate_throws() {
        PurchaseTransaction tx = PurchaseTransaction.create("x", LocalDate.now().minusDays(1), new BigDecimal("1"));
        when(repository.findById("id")).thenReturn(Optional.of(tx));
        when(exchangeRateService.getMostRecentExchangeRateWithinRange(eq("Euro Zone"), eq("Euro"), isNull(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ExchangeRateRetrievalException.class, () -> service.convertTransaction("id", "Euro Zone", "Euro", null));
    }

    @Test
    void getAvailableCurrencies_delegates() {
        when(exchangeRateService.getAvailableCurrencies()).thenReturn(List.of("EUR", "USD"));
        List<String> available = service.getAvailableCurrencies();
        assertTrue(available.contains("EUR"));
        verify(exchangeRateService).getAvailableCurrencies();
    }
}
