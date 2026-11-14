package com.fincore.transaction_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransferReconciliationScheduler {

    private final TransactionService transactionService;

    public TransferReconciliationScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedDelayString = "${fincore.reconciliation.fixed-delay:30000}")
    public void reconcilePendingTransactions() {

        System.out.println("Starting pending transaction reconciliation...");

        transactionService.reconcilePendingDeposits();

        transactionService.reconcilePendingWithdrawals();

        transactionService.reconcilePendingTransfers();

        System.out.println("Pending transaction reconciliation completed.");
    }
}