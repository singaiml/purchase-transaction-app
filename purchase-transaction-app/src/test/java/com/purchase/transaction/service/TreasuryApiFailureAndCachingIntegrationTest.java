package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
public class TreasuryApiFailureAndCachingIntegrationTest {

    @TestConfiguration
    static class Config {
        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }
    }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TreasuryExchangeRateService treasuryService;

    private MockRestServiceServer server;

    @BeforeEach
    void beforeEach() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void getExchangeRate_throwsWhenApiFails() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 05);

        // Simulate server error for the requested date
        server.expect(requestTo(org.hamcrest.Matchers.containsString(date.toString())))
                .andRespond(withServerError());

        // The service wraps exceptions in ExchangeRateRetrievalException
        assertThatThrownBy(() -> treasuryService.getExchangeRateForCurrency("EUR", date))
                .isInstanceOf(RuntimeException.class);

        server.verify();
    }

    @Test
    public void caching_preventsSecondHttpCall() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 05);
        String rateResponse = "{\"data\":[{\"currency_code\":\"EUR\",\"currency_name\":\"Euro\",\"exchange_rate\":\"0.5\",\"exchange_rate_date\":\"2025-12-05\",\"country_code\":\"EU\"}]}";

        // Expect a single HTTP call (first time). The second call should be served from cache.
        server.expect(requestTo(org.hamcrest.Matchers.containsString(date.toString())))
                .andRespond(withSuccess(rateResponse, MediaType.APPLICATION_JSON));

        // Enable cache for this instance so the second call is served from cache
        treasuryService.setCacheEnabled(true);

        Optional<?> first = treasuryService.getExchangeRateForCurrency("EUR", date);
        Optional<?> second = treasuryService.getExchangeRateForCurrency("EUR", date);

        assertThat(first).isPresent();
        assertThat(second).isPresent();

        server.verify();
    }
}
