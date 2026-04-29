package com.ahmed.pfa.cvplatform.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Configuration with Timeout Protection
 *
 * Configures RestTemplate beans with appropriate timeouts
 * to prevent blocking when calling external APIs.
 *
 * @author Ahmed
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate for IA Service with aggressive timeout
     *
     * Timeouts:
     * - Connect: 5 seconds (max time to establish connection)
     * - Read: 30 seconds (max time to receive response)
     *
     * Total max time: 35 seconds
     */
    @Bean(name = "iaServiceRestTemplate")
    public RestTemplate iaServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5000);  // 5 seconds
                    factory.setReadTimeout(30000);     // 30 seconds
                    return new BufferingClientHttpRequestFactory(factory);
                })
                .build();
    }

    /**
     * Default RestTemplate for other services
     *
     * More relaxed timeouts for internal services
     */
    @Bean(name = "defaultRestTemplate")
    public RestTemplate defaultRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }
}