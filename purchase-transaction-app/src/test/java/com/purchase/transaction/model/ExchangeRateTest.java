package com.purchase.transaction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExchangeRate Model Tests")
class ExchangeRateTest {
    
    private ExchangeRate exchangeRate;
    
    @BeforeEach
    void setUp() {
        exchangeRate = new ExchangeRate();
    }
    
    @Test
    @DisplayName("Should create exchange rate with all parameters")
    void testExchangeRateConstructor() {
        String currencyCode = "EUR";
        String currencyName = "Euro";
        BigDecimal rate = new BigDecimal("1.10");
        LocalDate effectiveDate = LocalDate.now();
        String countryCode = "DE";
        
        ExchangeRate rate1 = new ExchangeRate(currencyCode, currencyName, rate, effectiveDate, countryCode);
        
        assertEquals(currencyCode, rate1.getCurrencyCode());
        assertEquals(currencyName, rate1.getCurrencyName());
        assertEquals(rate, rate1.getExchangeRate());
        assertEquals(effectiveDate, rate1.getEffectiveDate());
        assertEquals(countryCode, rate1.getCountryCode());
    }
    
    @Test
    @DisplayName("Should set and get currency code")
    void testCurrencyCodeGetterSetter() {
        exchangeRate.setCurrencyCode("GBP");
        assertEquals("GBP", exchangeRate.getCurrencyCode());
    }
    
    @Test
    @DisplayName("Should set and get currency name")
    void testCurrencyNameGetterSetter() {
        exchangeRate.setCurrencyName("British Pound");
        assertEquals("British Pound", exchangeRate.getCurrencyName());
    }
    
    @Test
    @DisplayName("Should set and get exchange rate")
    void testExchangeRateGetterSetter() {
        BigDecimal rate = new BigDecimal("0.85");
        exchangeRate.setExchangeRate(rate);
        assertEquals(rate, exchangeRate.getExchangeRate());
    }
    
    @Test
    @DisplayName("Should set and get effective date")
    void testEffectiveDateGetterSetter() {
        LocalDate date = LocalDate.now();
        exchangeRate.setEffectiveDate(date);
        assertEquals(date, exchangeRate.getEffectiveDate());
    }
    
    @Test
    @DisplayName("Should set and get country code")
    void testCountryCodeGetterSetter() {
        exchangeRate.setCountryCode("FR");
        assertEquals("FR", exchangeRate.getCountryCode());
    }
    
    @Test
    @DisplayName("Should create multiple exchange rates with different values")
    void testMultipleExchangeRates() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "Euro", new BigDecimal("1.10"), LocalDate.now(), "DE");
        ExchangeRate rate2 = new ExchangeRate("GBP", "British Pound", new BigDecimal("0.85"), LocalDate.now(), "GB");
        
        assertEquals("EUR", rate1.getCurrencyCode());
        assertEquals("GBP", rate2.getCurrencyCode());
        assertNotEquals(rate1.getExchangeRate(), rate2.getExchangeRate());
    }
}
