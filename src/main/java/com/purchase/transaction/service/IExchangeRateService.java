package com.purchase.transaction.service;
import com.purchase.transaction.model.ExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IExchangeRateService {
    List<ExchangeRate> getExchangeRatesForDate(LocalDate date);
    Optional<ExchangeRate> getExchangeRateForCurrency(String currencyCode, LocalDate date);
    Optional<ExchangeRate> getMostRecentExchangeRateWithinRange(String currencyCode, LocalDate fromDate, LocalDate toDate);
    List<String> getAvailableCurrencies();
}
