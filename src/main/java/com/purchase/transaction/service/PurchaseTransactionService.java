package com.purchase.transaction.service;

import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.exception.TransactionNotFoundException;
import com.purchase.transaction.exception.TransactionValidationException;
import com.purchase.transaction.model.ConvertedTransaction;
import com.purchase.transaction.model.ExchangeRate;
import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.repository.ITransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseTransactionService implements IPurchaseTransactionService {
    private static final Logger log = LoggerFactory.getLogger(PurchaseTransactionService.class);
    
    private final ITransactionRepository transactionRepository;
    private final IExchangeRateService exchangeRateService;
    
    public PurchaseTransactionService(ITransactionRepository transactionRepository, IExchangeRateService exchangeRateService) {
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
    }
    
    @Override
    public PurchaseTransaction createTransaction(String description, LocalDate transactionDate, BigDecimal amount) {
        log.info("Creating new transaction: description='{}', date={}, amount={}", description, transactionDate, amount);
        
        if (description == null || description.trim().isEmpty()) 
            throw new TransactionValidationException("Description cannot be null or empty");
        if (description.length() > 50) 
            throw new TransactionValidationException("Description exceeds 50 characters: %d".formatted(description.length()));
        if (transactionDate == null) 
            throw new TransactionValidationException("Transaction date cannot be null");
        if (transactionDate.isAfter(LocalDate.now())) 
            throw new TransactionValidationException("Transaction date cannot be in the future");
        if (amount == null || amount.signum() <= 0) 
            throw new TransactionValidationException("Amount must be a positive number");
        
        BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        
        if (roundedAmount.signum() <= 0) 
            throw new TransactionValidationException("Amount must be a positive number");
        
        PurchaseTransaction transaction = PurchaseTransaction.create(description, transactionDate, roundedAmount);
        
        if (!transaction.isValid()) 
            throw new TransactionValidationException("Transaction validation failed");
        
        PurchaseTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Successfully created transaction with ID: {}", savedTransaction.getTransactionId());
        return savedTransaction;
    }
    
    @Override
    public PurchaseTransaction getTransaction(String transactionId) {
        log.debug("Retrieving transaction with ID: {}", transactionId);
        if (transactionId == null || transactionId.trim().isEmpty()) 
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> {
                log.warn("Transaction not found with ID: {}", transactionId);
                return new TransactionNotFoundException("Transaction not found with ID: %s".formatted(transactionId));
            });
    }
    
    @Override
    public List<PurchaseTransaction> getAllTransactions() {
        log.debug("Retrieving all transactions");
        List<PurchaseTransaction> transactions = transactionRepository.findAll();
        log.info("Retrieved {} transactions", transactions.size());
        return transactions;
    }
    
    @Override
    public void deleteTransaction(String transactionId) {
        log.info("Deleting transaction with ID: {}", transactionId);
        if (transactionId == null || transactionId.trim().isEmpty()) 
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        
        boolean deleted = transactionRepository.deleteById(transactionId);
        if (!deleted) {
            log.warn("Transaction not found with ID: {}", transactionId);
            throw new TransactionNotFoundException("Transaction not found with ID: %s".formatted(transactionId));
        }
        log.info("Successfully deleted transaction with ID: {}", transactionId);
    }
    
    @Override
    public ConvertedTransaction convertTransaction(String transactionId, String country, String currency, String country_currency_desc) {
        log.info("Converting transaction {} with country={}, currency={}, country_currency_desc={}", 
                transactionId, country, currency, country_currency_desc);
        
        if (transactionId == null || transactionId.trim().isEmpty()) 
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        
        // At least one filter parameter must be provided
        if ((country == null || country.trim().isEmpty()) &&
            (currency == null || currency.trim().isEmpty()) &&
            (country_currency_desc == null || country_currency_desc.trim().isEmpty())) {
            throw new IllegalArgumentException("Must specify at least one of: country, currency, or country_currency_desc");
        }
        
        PurchaseTransaction transaction = getTransaction(transactionId);
        
        // Requirement: Use the latest exchange rate <= purchase date within the last 6 months
        LocalDate purchaseDate = transaction.getTransactionDate();
        LocalDate cutoffDate = purchaseDate.minusMonths(6);

        Optional<ExchangeRate> maybeRate = exchangeRateService.getMostRecentExchangeRateWithinRange(
                country, currency, country_currency_desc, cutoffDate, purchaseDate);
        if (maybeRate.isEmpty()) {
            String msg = String.format("Cannot convert purchase to target currency (country=%s, currency=%s, country_currency_desc=%s): no exchange rate within 6 months on or before %s",
                    country, currency, country_currency_desc, purchaseDate);
            log.error(msg);
            throw new ExchangeRateRetrievalException(msg);
        }

        ExchangeRate exchangeRate = maybeRate.get();
        
        BigDecimal convertedAmount = transaction.getAmount()
            .multiply(exchangeRate.getExchangeRate())
            .setScale(2, RoundingMode.HALF_UP);
        
        ConvertedTransaction converted = new ConvertedTransaction(
            transaction.getTransactionId(),
            transaction.getDescription(),
            transaction.getTransactionDate(),
            transaction.getAmount(),
            exchangeRate.getCurrencyCode(),
            exchangeRate.getExchangeRate(),
            convertedAmount,
            exchangeRate.getEffectiveDate()
        );
        
        log.info("Successfully converted transaction {} to (country={}, currency={}, country_currency_desc={}): {} -> {}", 
                transactionId, country, currency, country_currency_desc, transaction.getAmount(), convertedAmount);
        return converted;
    }
    
    @Override
    public List<String> getAvailableCurrencies() {
        log.debug("Retrieving available currencies");
        return exchangeRateService.getAvailableCurrencies();
    }
}
