package com.purchase.transaction.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration with Connection Pooling and Bulkhead Pattern
 * 
 * This configuration implements:
 * 1. Connection Pooling: Reuses HTTP connections to improve performance and reduce overhead
 * 2. Bulkhead Pattern: Limits maximum concurrent connections to prevent resource exhaustion
 * 3. Timeout Configuration: Prevents hanging requests from blocking threads indefinitely
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate with advanced connection pooling and timeout configurations.
     * 
     * BULKHEAD PATTERN IMPLEMENTATION:
     * - maxTotal (100): Maximum total connections across all routes (bulkhead limit)
     * - defaultMaxPerRoute (20): Maximum connections per route/host (per-host bulkhead)
     * 
     * These limits ensure that:
     * - No single external service can consume all available connections
     * - System resources (threads, memory, file descriptors) are protected
     * - Application remains responsive even when external services are slow
     * 
     * TIMEOUT CONFIGURATION:
     * - connectTimeout (5s): Time to establish a connection
     * - socketTimeout (10s): Time to read data from an established connection
     * - connectionRequestTimeout (3s): Time to get a connection from the pool
     * 
     * @param builder Spring's RestTemplateBuilder
     * @return Configured RestTemplate with connection pooling and timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // BULKHEAD PATTERN: Connection pool manager limits concurrent connections
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        
        // BULKHEAD: Maximum 100 total connections across all routes
        // This prevents the application from opening too many connections and exhausting resources
        connectionManager.setMaxTotal(100);
        
        // BULKHEAD: Maximum 20 connections per route (per destination host)
        // This ensures fair resource distribution across multiple external services
        connectionManager.setDefaultMaxPerRoute(20);
        
        // Request timeout configuration
        RequestConfig requestConfig = RequestConfig.custom()
                // Socket timeout: Maximum time to wait for data after connection established (10 seconds)
                .setResponseTimeout(Timeout.ofSeconds(10))
                // Connection request timeout: Maximum time to wait for a connection from the pool (3 seconds)
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .build();
        
        // Build HTTP client with connection pooling and timeout configuration
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)  // Apply connection pool (bulkhead)
                .setDefaultRequestConfig(requestConfig)    // Apply timeout configuration
                .build();
        
        // Create HTTP request factory with our configured HTTP client
        @SuppressWarnings("null")
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // Set connection and read timeouts on the factory
        factory.setConnectTimeout(5000);  // Connection timeout: 5 seconds
        factory.setConnectionRequestTimeout(3000);  // Pool timeout: 3 seconds
        
        // Build and return RestTemplate with our custom factory
        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
