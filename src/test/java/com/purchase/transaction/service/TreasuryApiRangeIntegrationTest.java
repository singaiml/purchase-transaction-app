package com.purchase.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.purchase.transaction.model.PurchaseTransaction;
import com.purchase.transaction.model.ConvertedTransaction;
import com.purchase.transaction.repository.ITransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
public class TreasuryApiRangeIntegrationTest {

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
        private PurchaseTransactionService purchaseService;

        @Autowired
        private TreasuryExchangeRateService treasuryService;

        @MockBean
        private ITransactionRepository repo;

        private MockRestServiceServer server;

        @BeforeEach
        void beforeEach() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                server = MockRestServiceServer.createServer(restTemplate);
        }

        @Test
        public void convertTransaction_usesMostRecentRateWithinSixMonths() throws Exception {
                // Transaction date: try a specific date
                LocalDate purchaseDate = LocalDate.of(2025, 12, 05);

                // Prepare responses: simulate one API call returning the rate within the range
                String rateResponse = "{\"data\":[{\"currency\":\"Euro\",\"country_currency_desc\":\"Euro Zone-Euro\",\"exchange_rate\":\"0.3333\",\"record_date\":\"2025-12-03\",\"country\":\"Euro Zone\"}]}";

                server.expect(requestTo(org.hamcrest.Matchers.containsString("record_date")))
                                .andRespond(withSuccess(rateResponse, MediaType.APPLICATION_JSON));

                // Mock repository to return a PurchaseTransaction with the purchaseDate
                PurchaseTransaction tx = PurchaseTransaction.create("Test Purchase", purchaseDate, new BigDecimal("100.00"));
                when(repo.findById(tx.getTransactionId())).thenReturn(Optional.of(tx));

                // Execute conversion; should find the rate returned by the single API call
                ConvertedTransaction converted = purchaseService.convertTransaction(tx.getTransactionId(), "Euro Zone", "Euro", null);

                // Assert the converted amount is 100.00 * 0.3333 rounded to 2 decimals -> 33.33
                assertThat(converted).isNotNull();
                assertThat(converted.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("33.33"));
                assertThat(converted.getExchangeRate()).isEqualByComparingTo(new BigDecimal("0.3333"));

                server.verify();
        }
}
