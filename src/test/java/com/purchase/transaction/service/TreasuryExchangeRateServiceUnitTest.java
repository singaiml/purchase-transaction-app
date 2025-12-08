package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purchase.transaction.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TreasuryExchangeRateServiceUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper;
    private TreasuryExchangeRateService service;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        service = new TreasuryExchangeRateService(restTemplate, new RestTemplateBuilder(), objectMapper, "http://test");
        // enable cache during tests
        service.setCacheEnabled(true);
    }

    @Test
    void parsesValidResponse_andReturnsRates() throws Exception {
        String json = "{\"data\":[{\"currency_code\":\"EUR\",\"currency_name\":\"Euro\",\"exchange_rate\":\"0.3333\",\"exchange_rate_date\":\"2025-12-01\",\"country_code\":\"EU\"}]}";
        server.expect(requestTo(startsWith("http://test"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = service.getExchangeRatesForDate(LocalDate.of(2025,12,1));
        assertNotNull(rates);
        assertEquals(1, rates.size());
        ExchangeRate r = rates.get(0);
        assertEquals("EUR", r.getCurrencyCode());
        assertEquals("Euro", r.getCurrencyName());
        assertEquals(new BigDecimal("0.3333"), r.getExchangeRate());
        server.verify();
    }

    @Test
    void malformedRecord_isSkipped() throws Exception {
        String json = "{\"data\":[{\"currency_code\":\"EUR\",\"currency_name\":\"Euro\",\"exchange_rate\":\"0.3333\",\"exchange_rate_date\":\"2025-12-01\",\"country_code\":\"EU\"},{\"currency_code\":\"BAD\",\"exchange_rate\":\"N/A\"}]}";
        server.expect(requestTo(startsWith("http://test"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<ExchangeRate> rates = service.getExchangeRatesForDate(LocalDate.of(2025,12,1));
        assertEquals(1, rates.size());
        server.verify();
    }

    @Test
    void getMostRecentExchangeRateWithinRange_picksMostRecent() throws Exception {
        String json = "{\"data\":[{\"currency_code\":\"EUR\",\"exchange_rate\":\"0.3\",\"exchange_rate_date\":\"2025-01-01\"},{\"currency_code\":\"EUR\",\"exchange_rate\":\"0.4\",\"exchange_rate_date\":\"2025-06-30\"}]}";
        server.expect(requestTo(startsWith("http://test"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<ExchangeRate> opt = service.getMostRecentExchangeRateWithinRange("EUR", LocalDate.of(2025,1,1), LocalDate.of(2025,6,30));
        assertTrue(opt.isPresent());
        assertEquals(new BigDecimal("0.4"), opt.get().getExchangeRate());
        server.verify();
    }

    @Test
    void getExchangeRateForCurrency_cachesResult() throws Exception {
        String json = "{\"data\":[{\"currency_code\":\"EUR\",\"exchange_rate\":\"0.3333\",\"exchange_rate_date\":\"2025-12-01\"}]}";
        // expect only one HTTP request because the second call should be served from cache
        server.expect(requestTo(startsWith("http://test"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var first = service.getExchangeRateForCurrency("EUR", LocalDate.of(2025,12,1));
        assertTrue(first.isPresent());

        var second = service.getExchangeRateForCurrency("EUR", LocalDate.of(2025,12,1));
        assertTrue(second.isPresent());

        server.verify();
    }
}
