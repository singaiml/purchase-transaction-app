package com.purchase.transaction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purchase.transaction.model.PurchaseTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Repository
public class FileBasedTransactionRepository implements ITransactionRepository {
    private static final Logger log = LoggerFactory.getLogger(FileBasedTransactionRepository.class);
    
    private static final String TRANSACTIONS_FILENAME = "transactions.json";
    private final String repositoryPath;
    private final ObjectMapper objectMapper;
    private final Map<String, PurchaseTransaction> transactionCache;
    
    public FileBasedTransactionRepository(ObjectMapper objectMapper,
                                          @Value("${app.repository.path:./data}") String repositoryPath) {
        this.objectMapper = objectMapper;
        this.repositoryPath = repositoryPath;
        this.transactionCache = new HashMap<>();
        initializeRepository();
        loadTransactionsFromFile();
    }
    
    private void initializeRepository() {
        try {
            Path path = Path.of(repositoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created repository directory: {}", repositoryPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize repository directory", e);
            throw new RuntimeException("Failed to initialize repository", e);
        }
    }
    
    private void loadTransactionsFromFile() {
        try {
            File file = getRepositoryFile();
            transactionCache.clear();
            
            if (file.exists()) {
                PurchaseTransaction[] transactions = objectMapper.readValue(file, PurchaseTransaction[].class);
                for (PurchaseTransaction transaction : transactions) {
                    transactionCache.put(transaction.getTransactionId(), transaction);
                }
                log.info("Loaded {} transactions from file", transactions.length);
            } else {
                log.info("Repository file does not exist. Starting with empty repository.");
                saveTransactionsToFile();
            }
        } catch (IOException e) {
            log.error("Failed to load transactions from file", e);
            throw new RuntimeException("Failed to load transactions", e);
        }
    }
    
    private synchronized void saveTransactionsToFile() {
        try {
            File file = getRepositoryFile();
            List<PurchaseTransaction> transactions = new ArrayList<>(transactionCache.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, transactions);
            log.debug("Persisted {} transactions to file", transactions.size());
        } catch (IOException e) {
            log.error("Failed to save transactions to file", e);
            throw new RuntimeException("Failed to save transactions", e);
        }
    }
    
    private File getRepositoryFile() {
        return Path.of(repositoryPath, TRANSACTIONS_FILENAME).toFile();
    }
    
    @Override
    public PurchaseTransaction save(PurchaseTransaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("Transaction cannot be null");
        transactionCache.put(transaction.getTransactionId(), transaction);
        saveTransactionsToFile();
        log.info("Saved transaction with ID: {}", transaction.getTransactionId());
        return transaction;
    }
    
    @Override
    public Optional<PurchaseTransaction> findById(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) return Optional.empty();
        return Optional.ofNullable(transactionCache.get(transactionId));
    }
    
    @Override
    public List<PurchaseTransaction> findAll() {
        return new ArrayList<>(transactionCache.values());
    }
    
    @Override
    public boolean deleteById(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) return false;
        boolean existed = transactionCache.containsKey(transactionId);
        if (existed) {
            transactionCache.remove(transactionId);
            saveTransactionsToFile();
            log.info("Deleted transaction with ID: {}", transactionId);
        }
        return existed;
    }
    
    @Override
    public boolean existsById(String transactionId) {
        return transactionId != null && !transactionId.trim().isEmpty() && transactionCache.containsKey(transactionId);
    }
    
    @Override
    public long count() {
        return transactionCache.size();
    }
    
    @Override
    public void deleteAll() {
        transactionCache.clear();
        saveTransactionsToFile();
        log.info("Deleted all transactions");
    }
}
