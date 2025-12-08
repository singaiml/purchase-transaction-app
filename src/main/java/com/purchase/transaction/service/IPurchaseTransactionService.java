package com.purchase.transaction.service;
import com.purchase.transaction.model.ConvertedTransaction;
import com.purchase.transaction.model.PurchaseTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IPurchaseTransactionService {
    PurchaseTransaction createTransaction(String description, LocalDate transactionDate, BigDecimal amount);
    PurchaseTransaction getTransaction(String transactionId);
    List<PurchaseTransaction> getAllTransactions();
    void deleteTransaction(String transactionId);
    ConvertedTransaction convertTransaction(String transactionId, String country, String currency, String country_currency_desc);
    List<String> getAvailableCurrencies();
}
