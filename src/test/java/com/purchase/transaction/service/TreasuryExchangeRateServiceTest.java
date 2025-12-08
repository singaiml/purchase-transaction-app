package com.purchase.transaction.service;

import com.purchase.transaction.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
class TreasuryExchangeRateServiceTest {

    private TreasuryExchangeRateService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate restTemplate = builder.build();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new TreasuryExchangeRateService(restTemplate, builder, new com.fasterxml.jackson.databind.ObjectMapper(), "");
        service.setCacheEnabled(false);
    }

    @Test
    void parseAndGetAvailableCurrencies_success() {
        String json = "{\"data\":[{\"currency\":\"Euro\",\"country_currency_desc\":\"Euro Zone-Euro\",\"exchange_rate\":\"0.3333\",\"record_date\":\"2025-12-01\",\"country\":\"Euro Zone\"},{\"currency\":\"Dollar\",\"country_currency_desc\":\"United States-Dollar\",\"exchange_rate\":\"1\",\"record_date\":\"2025-12-01\",\"country\":\"United States\"}]}";
        mockServer.expect(once(), requestTo(org.hamcrest.Matchers.containsString("filter="))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<String> currencies = service.getAvailableCurrencies();
        assertTrue(currencies.contains("EUR"));
        assertTrue(currencies.contains("USD"));
        mockServer.verify();
    }

    @Test
    void getMostRecentExchangeRateWithinRange_picksMostRecent() {
        String json = "{\"data\":[{\"currency\":\"Euro\",\"country_currency_desc\":\"Euro Zone-Euro\",\"exchange_rate\":\"0.2\",\"record_date\":\"2025-01-01\",\"country\":\"Euro Zone\"},{\"currency\":\"Euro\",\"country_currency_desc\":\"Euro Zone-Euro\",\"exchange_rate\":\"0.4\",\"record_date\":\"2025-06-01\",\"country\":\"Euro Zone\"}]}";
        mockServer.expect(once(), requestTo(org.hamcrest.Matchers.containsString("filter="))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<ExchangeRate> maybe = service.getMostRecentExchangeRateWithinRange("Euro Zone", "Euro", null, LocalDate.of(2025,1,1), LocalDate.of(2025,6,30));
        assertTrue(maybe.isPresent());
        ExchangeRate rate = maybe.get();
        assertEquals(new BigDecimal("0.4"), rate.getExchangeRate());
        mockServer.verify();
    }

    @Test
    void getExchangeRateForCurrency_empty_returnsEmpty() {
        String json = "{\"data\":[]}";
        mockServer.expect(once(), requestTo(org.hamcrest.Matchers.containsString("filter="))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<ExchangeRate> maybe = service.getExchangeRateForCurrency("EUR", LocalDate.of(2025,12,1));
        assertTrue(maybe.isEmpty());
        mockServer.verify();
    }
}
