package com.poc.merchant.application.command;

public record CreateMerchantCommand(
        String merchantId,
        String legalName,
        String documentNumber
) {
}
