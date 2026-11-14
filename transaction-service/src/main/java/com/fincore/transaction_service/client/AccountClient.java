package com.fincore.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;

import com.fincore.transaction_service.dto.AccountOperationRequest;
import com.fincore.transaction_service.dto.AccountOperationResponse;
import com.fincore.transaction_service.enums.AccountOperationType;

import java.util.UUID;


@FeignClient(name = "account-service")
public interface AccountClient {

    @PostMapping("/accounts/internal/{accountId}/debit")
    AccountOperationResponse debit(
        @PathVariable("accountId") Long accountId,
        @RequestBody AccountOperationRequest request,
        @RequestHeader("X-Internal-Secret") String internalSecret
    );

    @PostMapping("/accounts/internal/{accountId}/credit")
    AccountOperationResponse credit(
        @PathVariable("accountId") Long accountId,
        @RequestBody AccountOperationRequest request,
        @RequestHeader("X-Internal-Secret") String internalSecret
    );

    @GetMapping("/accounts/internal/operations/{transactionReference}/{operationType}")
    AccountOperationResponse getOperationStatus(
        @PathVariable("transactionReference") UUID transactionReference,
        @PathVariable("operationType") AccountOperationType operationType,
        @RequestHeader("X-Internal-Secret") String internalSecret
    );

    @PostMapping("/accounts/internal/transfers/{accountId}/credit")
    AccountOperationResponse creditTransfer(
        @PathVariable("accountId") Long accountId,
        @RequestBody AccountOperationRequest request,
        @RequestHeader("X-Internal-Secret") String internalSecret
    );
}