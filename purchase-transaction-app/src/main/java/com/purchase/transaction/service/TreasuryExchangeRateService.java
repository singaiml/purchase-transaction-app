package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TreasuryExchangeRateService implements IExchangeRateService {
    private static final Logger log = LoggerFactory.getLogger(TreasuryExchangeRateService.class);
    
    private static final String TREASURY_API_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, ExchangeRate> exchangeRateCache;
    
    @Value("${app.exchange-rate.cache-enabled:true}")
    private boolean cacheEnabled;
    
    public TreasuryExchangeRateService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.exchangeRateCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<ExchangeRate> getExchangeRatesForDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        
        try {
            String formattedDate = date.format(DATE_FORMATTER);
            String filter = "exchange_rate_date:eq:\"%s\"".formatted(formattedDate);
            String url = "%s?filter=%s&limit=500".formatted(TREASURY_API_URL, encodeFilter(filter));
            
            log.debug("Fetching exchange rates from Treasury API for date: {}", formattedDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            return parseExchangeRates(response, date);
        } catch (Exception e) {
            log.error("Failed to retrieve exchange rates for date: {}", date, e);
            throw new ExchangeRateRetrievalException("Failed to retrieve exchange rates for date: %s".formatted(date), e);
        }
    }
    
    @Override
    public Optional<ExchangeRate> getExchangeRateForCurrency(String currencyCode, LocalDate date) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) throw new IllegalArgumentException("Currency code cannot be null or empty");
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        
        String cacheKey = getCacheKey(currencyCode, date);
        if (cacheEnabled && exchangeRateCache.containsKey(cacheKey)) {
            return Optional.of(exchangeRateCache.get(cacheKey));
        }
        
        try {
            String formattedDate = date.format(DATE_FORMATTER);
            String filter = "exchange_rate_date:eq:\"%s\" and currency_code:eq:\"%s\"".formatted(formattedDate, currencyCode.toUpperCase());
            String url = "%s?filter=%s".formatted(TREASURY_API_URL, encodeFilter(filter));
            
            log.debug("Fetching exchange rate from Treasury API for currency: {} on date: {}", currencyCode, formattedDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response, date);
            
            if (!rates.isEmpty()) {
                ExchangeRate rate = rates.get(0);
                if (cacheEnabled) exchangeRateCache.put(cacheKey, rate);
                return Optional.of(rate);
            }
            
            log.warn("No exchange rate found for currency: {} on date: {}", currencyCode, date);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve exchange rate for currency: {} on date: {}", currencyCode, date, e);
            throw new ExchangeRateRetrievalException("Failed to retrieve exchange rate for currency: %s".formatted(currencyCode), e);
        }
    }
    
    @Override
    public List<String> getAvailableCurrencies() {
        try {
            LocalDate today = LocalDate.now();
            String formattedDate = today.format(DATE_FORMATTER);
            String filter = "exchange_rate_date:eq:\"%s\"".formatted(formattedDate);
            String url = "%s?filter=%s&limit=500".formatted(TREASURY_API_URL, encodeFilter(filter));
            
            log.debug("Fetching available currencies from Treasury API");
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response, today);
            
            List<String> currencies = new ArrayList<>();
            Set<String> uniqueCurrencies = new LinkedHashSet<>();
            
            for (ExchangeRate rate : rates) {
                uniqueCurrencies.add(rate.getCurrencyCode());
            }
            
            currencies.addAll(uniqueCurrencies);
            Collections.sort(currencies);
            
            log.info("Found {} available currencies", currencies.size());
            return currencies;
        } catch (Exception e) {
            log.error("Failed to retrieve available currencies", e);
            throw new ExchangeRateRetrievalException("Failed to retrieve available currencies", e);
        }
    }
    
    private List<ExchangeRate> parseExchangeRates(String jsonResponse, LocalDate date) throws IOException {
        List<ExchangeRate> rates = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode dataNode = root.path("data");
        
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                try {
                    ExchangeRate rate = new ExchangeRate(
                        node.path("currency_code").asText(),
                        node.path("currency_name").asText(),
                        new BigDecimal(node.path("exchange_rate").asText()),
                        date,
                        node.path("country_code").asText()
                    );
                    rates.add(rate);
                } catch (Exception e) {
                    log.warn("Failed to parse exchange rate record", e);
                }
            }
        }
        
        log.info("Parsed {} exchange rates from API response", rates.size());
        return rates;
    }
    
    private String encodeFilter(String filter) {
        return filter.replace(" ", "%20").replace("\"", "%22").replace(":", "%3A");
    }
    
    private String getCacheKey(String currencyCode, LocalDate date) {
        return currencyCode.toUpperCase() + "_" + date.format(DATE_FORMATTER);
    }
}