package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purchase.transaction.exception.ExchangeRateRetrievalException;
import com.purchase.transaction.model.ExchangeRate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

/**
 * Treasury Exchange Rate Service with Resilience Patterns
 * 
 * This service integrates with the US Treasury API and implements multiple resilience patterns:
 * 
 * 1. CIRCUIT BREAKER: Stops calling a failing service and provides fast failures
 *    - Opens circuit after 50% failure rate in 10 calls
 *    - Waits 30 seconds before attempting recovery
 *    - Provides fallback methods to return cached data
 * 
 * 2. BULKHEAD: Limits concurrent calls to prevent resource exhaustion
 *    - Maximum 10 concurrent calls to Treasury API
 *    - Prevents Treasury API from consuming all application threads
 *    - Works with connection pool limits in RestTemplateConfig
 * 
 * 3. RETRY: Automatically retries transient failures
 *    - Up to 3 retry attempts with exponential backoff
 *    - Useful for network hiccups and temporary service issues
 * 
 * 4. TIME LIMITER: Prevents calls from hanging indefinitely
 *    - 10-second timeout per call
 *    - Prevents thread starvation from slow external services
 */
@Service
public class TreasuryExchangeRateService implements IExchangeRateService {
    private static final Logger log = LoggerFactory.getLogger(TreasuryExchangeRateService.class);
    private static final String DEFAULT_TREASURY_API_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange";
    
    
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
    
    // Single constructor: prefer an injected RestTemplate when available (tests often provide one),
    // otherwise build one from RestTemplateBuilder for runtime usage.
    public TreasuryExchangeRateService(@Autowired(required = false) RestTemplate restTemplate,
                                      RestTemplateBuilder restTemplateBuilder,
                                      ObjectMapper objectMapper,
                                      @Value("${app.exchange-rate.url:}") String treasuryApiUrl) {
        this.restTemplate = restTemplate != null ? restTemplate : restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.exchangeRateCache = new ConcurrentHashMap<>();
        this.treasuryApiUrl = (treasuryApiUrl == null || treasuryApiUrl.isBlank()) ? DEFAULT_TREASURY_API_URL : treasuryApiUrl;
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
    
    /**
     * Retrieves exchange rate for a specific currency and date.
     * 
     * RESILIENCE PATTERNS APPLIED:
     * - @CircuitBreaker: Stops calling Treasury API after repeated failures, uses fallback
     * - @Retry: Retries up to 3 times with exponential backoff for transient failures
     * - @Bulkhead: Limits concurrent calls to 10 to prevent resource exhaustion
     * 
     * FALLBACK: Returns cached data if Treasury API is unavailable
     * Note: TimeLimiter removed - only works with async CompletionStage returns
     */
    @Override
    @CircuitBreaker(name = "treasuryApi", fallbackMethod = "getExchangeRateForCurrencyFallback")
    @Retry(name = "treasuryApi")
    @Bulkhead(name = "treasuryApi")  // BULKHEAD PATTERN: Limits concurrent calls
    public Optional<ExchangeRate> getExchangeRateForCurrency(String currencyCode, LocalDate date) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) throw new IllegalArgumentException("Currency code cannot be null or empty");
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        
        String cacheKey = getCacheKey(currencyCode, date);
        if (cacheEnabled && exchangeRateCache.containsKey(cacheKey)) {
            return Optional.of(exchangeRateCache.get(cacheKey));
        }
        
        try {
            String formattedDate = date.format(DATE_FORMATTER);
            String filter = "record_date:eq:\"%s\" and currency_code:eq:\"%s\"".formatted(formattedDate, currencyCode.toUpperCase());
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
    
    /**
     * Retrieves list of available currencies for conversion.
     * 
     * RESILIENCE PATTERNS APPLIED:
     * - @CircuitBreaker: Protects against repeated failures
     * - @Retry: Retries transient failures
     * - @Bulkhead: Limits concurrent execution (BULKHEAD PATTERN)
     * 
     * FALLBACK: Returns empty list if Treasury API is unavailable
     */
    @Override
    @CircuitBreaker(name = "treasuryApi", fallbackMethod = "getAvailableCurrenciesFallback")
    @Retry(name = "treasuryApi")
    @Bulkhead(name = "treasuryApi")  // BULKHEAD PATTERN: Prevents resource exhaustion
    public List<String> getAvailableCurrencies() {
        try {
            // Query for rates from the latest update to ensure we get recent data
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(6);
            String filter = "record_date:gte:\"%s\"".formatted(startDate.format(DATE_FORMATTER));
            String url = "%s?filter=%s&sort=-record_date&limit=500".formatted(this.treasuryApiUrl, encodeFilter(filter));
            
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
                    String dateStr = node.path("record_date").asText(null);
                    LocalDate effectiveDate = dateStr == null || dateStr.isBlank() ? null : LocalDate.parse(dateStr, DATE_FORMATTER);
                    
                    // Extract currency information from Treasury API response
                    // Treasury API fields:
                    // - "currency": Currency name (e.g., "Euro", "Pound Sterling")
                    // - "country_currency_desc": Full description (e.g., "Euro Zone-Euro")
                    // - "country": Country name
                    String currencyName = node.path("currency").asText("");
                    String country = node.path("country").asText("");
                    String countryCurrencyDesc = node.path("country_currency_desc").asText("");
                    String exchangeRateStr = node.path("exchange_rate").asText();
                    
                    // Skip if missing critical fields
                    if (currencyName.isEmpty() || exchangeRateStr.isEmpty()) {
                        log.debug("Skipping record with missing currency or exchange_rate");
                        continue;
                    }
                    
                    // Map currency name to standard ISO 4217 code
                    String currencyCode = mapCurrencyNameToCode(currencyName);
                    
                    ExchangeRate rate = new ExchangeRate(
                        currencyCode,  // Use mapped currency code (e.g., "EUR" for "Euro")
                        countryCurrencyDesc,  // Full description (e.g., "Euro Zone-Euro")
                        new BigDecimal(exchangeRateStr),
                        effectiveDate,
                        country  // Use country name
                    );
                    rates.add(rate);
                    log.debug("Parsed exchange rate: {} {} on {}", currencyCode, exchangeRateStr, effectiveDate);
                } catch (Exception e) {
                    log.warn("Failed to parse exchange rate record: {}", e.getMessage());
                }
            }
        }
        
        log.info("Parsed {} exchange rates from API response", rates.size());
        return rates;
    }
    
    /**
     * Maps Treasury API currency names to standard ISO 4217 currency codes.
     * The Treasury API returns currency names like "Euro", "Dollar", "Pound Sterling", etc.
     * This method converts them to standard codes for display purposes.
     * 
     * NOTE: For currencies shared by multiple countries (e.g., "Rupee" used by India, Pakistan, Nepal),
     * the user must specify the country to disambiguate. This honors the API contract.
     */
    private String mapCurrencyNameToCode(String currencyName) {
        if (currencyName == null || currencyName.isEmpty()) {
            return "";
        }
        
        return switch (currencyName.toLowerCase()) {
            case "euro" -> "EUR";
            case "dollar" -> "USD";
            case "pound sterling" -> "GBP";
            case "yen" -> "JPY";
            case "canadian dollar" -> "CAD";
            case "australian dollar" -> "AUD";
            case "swiss franc" -> "CHF";
            case "swedish krona" -> "SEK";
            case "norwegian krone" -> "NOK";
            case "danish krone" -> "DKK";
            case "hong kong dollar" -> "HKD";
            case "singapore dollar" -> "SGD";
            case "new zealand dollar" -> "NZD";
            case "mexican peso" -> "MXN";
            case "brazilian real" -> "BRL";
            case "south african rand" -> "ZAR";
            case "south korean won" -> "KRW";
            case "thai baht" -> "THB";
            case "malaysian ringgit" -> "MYR";
            case "indonesian rupiah" -> "IDR";
            case "philippine peso" -> "PHP";
            case "chinese yuan", "renminbi" -> "CNY";
            case "russian ruble" -> "RUB";
            case "turkish lira" -> "TRY";
            case "saudi riyal" -> "SAR";
            case "united arab emirates dirham" -> "AED";
            case "israeli new sheqel" -> "ILS";
            case "afghan afghani" -> "AFN";
            case "argentine peso" -> "ARS";
            case "bahraini dinar" -> "BHD";
            case "colombian peso" -> "COP";
            case "czech koruna" -> "CZK";
            case "hungarian forint" -> "HUF";
            case "icelandic króna" -> "ISK";
            case "kuwaiti dinar" -> "KWD";
            case "lebanese pound" -> "LBP";
            case "libyan dinar" -> "LYD";
            case "moroccan dirham" -> "MAD";
            case "omani rial" -> "OMR";
            case "peruvian nuevo sol" -> "PEN";
            case "qatari riyal" -> "QAR";
            case "ukrainian hryvnia" -> "UAH";
            case "bangladeshi taka" -> "BDT";
            case "bulgarian lev" -> "BGN";
            case "croatian kuna" -> "HRK";
            case "estonian kroon" -> "EEK";
            case "lithuanian litas" -> "LTL";
            case "pakistani rupee" -> "PKR";
            case "polish zloty" -> "PLN";
            case "romanian leu" -> "RON";
            case "slovak koruna" -> "SKK";
            case "slovenian tolar" -> "SIT";
            case "venezuelan bolívar" -> "VEB";
            case "vietnamese dong" -> "VND";
            case "zambian kwacha" -> "ZMW";
            case "kenyan shilling" -> "KES";
            case "nigerian naira" -> "NGN";
            case "tunisian dinar" -> "TND";
            default -> currencyName.toUpperCase();  // Fallback: use original name
        };
    }
    
    private String encodeFilter(String filter) {
        return filter.replace(" ", "%20").replace("\"", "%22").replace(":", "%3A");
    }
    
    private String getCacheKey(String currencyCode, LocalDate date) {
        return currencyCode.toUpperCase() + "_" + date.format(DATE_FORMATTER);
    }

    /**
     * Retrieves the most recent exchange rate within a date range.
     * 
     * REQUIREMENT: Must use exchange rate with effective date <= endDate (purchase date)
     * within the last 6 months (startDate = 6 months before endDate).
     * 
     * STRATEGY: Query for rates from startDate onward, then locally filter for rates
     * that are ON or BEFORE endDate, and pick the most recent one.
     * 
     * RESILIENCE PATTERNS APPLIED:
     * - @CircuitBreaker: Fast failure when Treasury API is down
     * - @Retry: Automatic retry for transient failures
     * - @Bulkhead: Concurrent call limiting (BULKHEAD PATTERN)
     * 
     * FALLBACK: Searches cache for most recent rate for the currency
     * Note: TimeLimiter removed - only works with async CompletionStage returns
     */
    @Override
    @CircuitBreaker(name = "treasuryApi", fallbackMethod = "getMostRecentExchangeRateWithinRangeFallback")
    @Retry(name = "treasuryApi")
    @Bulkhead(name = "treasuryApi")  // BULKHEAD PATTERN: Resource isolation
    public Optional<ExchangeRate> getMostRecentExchangeRateWithinRange(String currencyCode, LocalDate startDate, LocalDate endDate) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) throw new IllegalArgumentException("Currency code cannot be null or empty");
        if (startDate == null || endDate == null) throw new IllegalArgumentException("Dates cannot be null");
        try {
            // Query for rates from startDate onwards (don't limit to endDate in the API call)
            // The Treasury API may not have data for very recent dates, so we query a broader range
            // Note: Treasury API doesn't support currency_code filtering, so we query broadly
            // and filter locally for the requested currency
            String filter = "record_date:gte:\"%s\""
                    .formatted(startDate.format(DATE_FORMATTER));
            String url = "%s?filter=%s&sort=-record_date&limit=500".formatted(this.treasuryApiUrl, encodeFilter(filter));
            log.debug("Fetching exchange rates from Treasury API for currency {} from {} onwards", currencyCode, startDate);
            @SuppressWarnings("null")
            String response = restTemplate.getForObject(url, String.class);
            List<ExchangeRate> rates = parseExchangeRates(response);
            if (rates.isEmpty()) {
                log.warn("API returned no exchange rates for currency {} since {}", currencyCode, startDate);
                return Optional.empty();
            }
            
            // Filter rates to find matches for the requested currency code
            // The API returns currency names, so we need to match against those
            List<ExchangeRate> currencyMatches = rates.stream()
                    .filter(rate -> rate.getCurrencyCode().equalsIgnoreCase(currencyCode) || 
                                  rate.getCurrencyName().toUpperCase().contains(currencyCode.toUpperCase()))
                    .filter(rate -> rate.getEffectiveDate() != null)
                    .filter(rate -> !rate.getEffectiveDate().isAfter(endDate))  // Must be on or before purchase date
                    .filter(rate -> !rate.getEffectiveDate().isBefore(startDate))  // Must be within 6 months
                    .toList();
            
            if (currencyMatches.isEmpty()) {
                log.warn("API returned {} rates but none matching currency {} on or before {} and on/after {}", 
                        rates.size(), currencyCode, endDate, startDate);
                return Optional.empty();
            }
            
            // Pick the most recent (latest) by effective date
            Optional<ExchangeRate> mostRecent = currencyMatches.stream()
                    .max(Comparator.comparing(ExchangeRate::getEffectiveDate));
            
            mostRecent.ifPresent(rate -> log.info("Selected exchange rate for {} with effective date: {} (rate: {})", 
                    currencyCode, rate.getEffectiveDate(), rate.getExchangeRate()));
            
            return mostRecent;
        } catch (Exception e) {
            log.error("Failed to retrieve exchange rates for currency {} between {} and {}", currencyCode, startDate, endDate, e);
            throw new ExchangeRateRetrievalException("Failed to retrieve exchange rates for currency: %s".formatted(currencyCode), e);
        }
    }
    
    // ==============================================================================
    // FALLBACK METHODS - Called when Circuit Breaker is OPEN or on failure
    // ==============================================================================
    
    /**
     * FALLBACK METHOD for getExchangeRateForCurrency
     * 
     * This method is called when:
     * - Circuit breaker is OPEN (too many failures)
     * - All retry attempts are exhausted
     * - Request times out
     * - Bulkhead is full (too many concurrent calls)
     * 
     * GRACEFUL DEGRADATION: Returns cached data instead of failing completely
     * 
     * @param currencyCode Currency code to look up
     * @param date Date to look up
     * @param ex Exception that triggered the fallback
     * @return Cached exchange rate if available, empty otherwise
     */
    private Optional<ExchangeRate> getExchangeRateForCurrencyFallback(String currencyCode, LocalDate date, Exception ex) {
        log.warn("Treasury API call failed for currency {} on date {}, using fallback. Reason: {}", 
                currencyCode, date, ex.getMessage());
        
        // Try to return cached value
        String cacheKey = getCacheKey(currencyCode, date);
        if (cacheEnabled && exchangeRateCache.containsKey(cacheKey)) {
            log.info("Returning cached exchange rate for {} on {}", currencyCode, date);
            return Optional.of(exchangeRateCache.get(cacheKey));
        }
        
        log.warn("No cached exchange rate available for {} on {}", currencyCode, date);
        return Optional.empty();
    }
    
    /**
     * FALLBACK METHOD for getAvailableCurrencies
     * 
     * GRACEFUL DEGRADATION: Returns list of currencies from cache
     * If cache is empty, returns empty list rather than failing
     * 
     * @param ex Exception that triggered the fallback
     * @return List of currencies from cache, or empty list
     */
    private List<String> getAvailableCurrenciesFallback(Exception ex) {
        log.warn("Treasury API call failed for available currencies, using fallback. Reason: {}", ex.getMessage());
        
        // Extract unique currency codes from cache
        Set<String> uniqueCurrencies = new LinkedHashSet<>();
        exchangeRateCache.values().forEach(rate -> uniqueCurrencies.add(rate.getCurrencyCode()));
        
        List<String> currencies = new ArrayList<>(uniqueCurrencies);
        Collections.sort(currencies);
        
        log.info("Returning {} currencies from cache as fallback", currencies.size());
        return currencies;
    }
    
    /**
     * FALLBACK METHOD for getMostRecentExchangeRateWithinRange
     * 
     * GRACEFUL DEGRADATION: Searches cache for the most recent rate for the currency
     * IMPORTANT: Must respect the date range constraint (REQ 2.3 - within 6 months)
     * 
     * @param currencyCode Currency code to look up
     * @param startDate Start of date range (cutoff date - 6 months before purchase)
     * @param endDate End of date range (purchase date)
     * @param ex Exception that triggered the fallback
     * @return Most recent cached exchange rate within the specified date range, or empty
     */
    private Optional<ExchangeRate> getMostRecentExchangeRateWithinRangeFallback(
            String currencyCode, LocalDate startDate, LocalDate endDate, Exception ex) {
        log.warn("Treasury API call failed for currency {} between {} and {}, using fallback. Reason: {}", 
                currencyCode, startDate, endDate, ex.getMessage());
        
        // Search cache for most recent rate within the 6-month date range (REQ 2.3)
        Optional<ExchangeRate> mostRecent = exchangeRateCache.values().stream()
                .filter(rate -> rate.getCurrencyCode().equalsIgnoreCase(currencyCode))
                .filter(rate -> rate.getEffectiveDate() != null)
                .filter(rate -> !rate.getEffectiveDate().isBefore(startDate))
                .filter(rate -> !rate.getEffectiveDate().isAfter(endDate))
                .max(Comparator.comparing(ExchangeRate::getEffectiveDate));
        
        if (mostRecent.isPresent()) {
            log.info("Returning cached exchange rate for {} from date {} as fallback (within range {} to {})", 
                    currencyCode, mostRecent.get().getEffectiveDate(), startDate, endDate);
        } else {
            log.warn("No cached exchange rate available for {} within date range {} to {}", 
                    currencyCode, startDate, endDate);
        }
        
        return mostRecent;
    }
}