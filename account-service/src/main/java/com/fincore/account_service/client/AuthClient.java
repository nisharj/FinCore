package com.fincore.account_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.fincore.account_service.dto.CustomerLinkResponse;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/internal/customer-link")
    CustomerLinkResponse getCustomerLink(
            @RequestParam("username") String username,
            @RequestHeader("X-Internal-Secret") String internalSecret
    );
}