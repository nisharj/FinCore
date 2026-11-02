
package com.fincore.auth_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fincore.auth_service.dto.CustomerLookupResponse;

@Service
public class CustomerLookupClient {

    private final RestClient restClient;
    private final String internalSecret;

    public CustomerLookupClient(
            RestClient.Builder builder,
            @Value("${fincore.customer-service.url:http://localhost:8081}")
            String customerServiceUrl,
            @Value("${fincore.internal.secret}")
            String internalSecret) {

        this.restClient = builder
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
