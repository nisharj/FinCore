package com.fincore.auth_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fincore.auth_service.dto.CustomerLookupResponse;

@Service
public class CustomerLookupService {

    private final RestClient restClient;
    private final String internalSecret;

    public CustomerLookupService(
            RestClient.Builder restClientBuilder,
            @Value("${customer-service.url}") String customerServiceUrl,
            @Value("${fincore.internal.secret}") String internalSecret) {

        this.restClient = restClientBuilder
                .baseUrl(customerServiceUrl)
                .build();

        this.internalSecret = internalSecret;
    }

    public CustomerLookupResponse findByEmail(String email) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customers/internal/by-email")
                        .queryParam("email", email)
                        .build())
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(CustomerLookupResponse.class);
    }
}