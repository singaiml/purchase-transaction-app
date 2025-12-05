package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
    private final String treasuryApiUrl;
    
    @Value("${app.exchange-rate.cache-enabled:true}")
    private boolean cacheEnabled;

    // Package-private setter to control cache during tests
    void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
    }
    
    // Primary constructor used by Spring: allow injecting a RestTemplateBuilder and a configurable base URL.
    public TreasuryExchangeRateService(RestTemplateBuilder restTemplateBuilder,
                                      ObjectMapper objectMapper,
                                      @Value("${app.exchange-rate.url:https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange}") String treasuryApiUrl) {
        this(restTemplateBuilder.build(), objectMapper, treasuryApiUrl);
    }

    // Constructor used when a RestTemplate bean is available (Spring will autowire this)
    @Autowired
    public TreasuryExchangeRateService(RestTemplate restTemplate, ObjectMapper objectMapper, @Value("${app.exchange-rate.url:" + TREASURY_API_URL + "}") String treasuryApiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.exchangeRateCache = new ConcurrentHashMap<>();
        this.treasuryApiUrl = treasuryApiUrl == null || treasuryApiUrl.isBlank() ? TREASURY_API_URL : treasuryApiUrl;
    }
    
    @Override
    public List<ExchangeRate> getExchangeRatesForDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        
        try {
            String formattedDate = date.format(DATE_FORMATTER);
            String filter = "exchange_rate_date:eq:\"%s\"".formatted(formattedDate);
            String url = "%s?filter=%s&limit=500".formatted(this.treasuryApiUrl, encodeFilter(filter));
            
            log.debug("Fetching exchange rates from Treasury API for date: {}", formattedDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            return parseExchangeRates(response);
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
            String url = "%s?filter=%s".formatted(this.treasuryApiUrl, encodeFilter(filter));
            
            log.debug("Fetching exchange rate from Treasury API for currency: {} on date: {}", currencyCode, formattedDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response);
            
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
            String url = "%s?filter=%s&limit=500".formatted(this.treasuryApiUrl, encodeFilter(filter));
            
            log.debug("Fetching available currencies from Treasury API");
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response);
            
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
    
    private List<ExchangeRate> parseExchangeRates(String jsonResponse) throws IOException {
        List<ExchangeRate> rates = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode dataNode = root.path("data");
        
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                try {
                    String dateStr = node.path("exchange_rate_date").asText(null);
                    LocalDate effectiveDate = dateStr == null || dateStr.isBlank() ? null : LocalDate.parse(dateStr, DATE_FORMATTER);
                    ExchangeRate rate = new ExchangeRate(
                        node.path("currency_code").asText(),
                        node.path("currency_name").asText(),
                        new BigDecimal(node.path("exchange_rate").asText()),
                        effectiveDate,
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

    @Override
    public Optional<ExchangeRate> getMostRecentExchangeRateWithinRange(String currencyCode, LocalDate fromDate, LocalDate toDate) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) throw new IllegalArgumentException("Currency code cannot be null or empty");
        if (fromDate == null || toDate == null) throw new IllegalArgumentException("Dates cannot be null");
        try {
            String filter = "exchange_rate_date:gte:\"%s\" and exchange_rate_date:lte:\"%s\" and currency_code:eq:\"%s\""
                    .formatted(fromDate.format(DATE_FORMATTER), toDate.format(DATE_FORMATTER), currencyCode.toUpperCase());
            String url = "%s?filter=%s&limit=500".formatted(this.treasuryApiUrl, encodeFilter(filter));
            log.debug("Fetching exchange rates from Treasury API for currency {} between {} and {}", currencyCode, fromDate, toDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response);
            if (rates.isEmpty()) return Optional.empty();
            // pick the most recent by effective date
            rates.sort(Comparator.comparing(ExchangeRate::getEffectiveDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            ExchangeRate rate = rates.get(0);
            return Optional.of(rate);
        } catch (Exception e) {
            log.error("Failed to retrieve exchange rates for currency {} between {} and {}", currencyCode, fromDate, toDate, e);
            throw new ExchangeRateRetrievalException("Failed to retrieve exchange rates for currency: %s".formatted(currencyCode), e);
        }
    }
}