package com.purchase.transaction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.purchase.transaction.model.PurchaseTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FileBasedTransactionRepositoryUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void save_find_count_delete_flow() {
        String repoFile = tempDir.resolve("repo.json").toString();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        FileBasedTransactionRepository repo = new FileBasedTransactionRepository(mapper, repoFile);

        PurchaseTransaction tx = PurchaseTransaction.create("desc", LocalDate.of(2025,12,1), new BigDecimal("10.00"));
        PurchaseTransaction saved = repo.save(tx);
        assertNotNull(saved.getTransactionId());

        assertTrue(repo.existsById(saved.getTransactionId()));
        assertEquals(1, repo.count());

        var found = repo.findById(saved.getTransactionId());
        assertTrue(found.isPresent());

        boolean deleted = repo.deleteById(saved.getTransactionId());
        assertTrue(deleted);
        assertFalse(repo.existsById(saved.getTransactionId()));

        repo.deleteAll();
        assertEquals(0, repo.count());
    }
}
