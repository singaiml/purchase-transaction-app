package com.purchase.transaction.repository;
import com.purchase.transaction.model.PurchaseTransaction;
import java.util.List;
import java.util.Optional;

public interface ITransactionRepository {
    PurchaseTransaction save(PurchaseTransaction transaction);
    Optional<PurchaseTransaction> findById(String transactionId);
    List<PurchaseTransaction> findAll();
    boolean deleteById(String transactionId);
    boolean existsById(String transactionId);
    long count();
    void deleteAll();
}
