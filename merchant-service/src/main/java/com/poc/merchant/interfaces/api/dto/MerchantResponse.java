package com.poc.merchant.interfaces.api.dto;

import com.poc.merchant.domain.model.Merchant;

public record MerchantResponse(
        String merchantId,
        String legalName,
        String documentNumber,
        String status
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getMerchantId(),
                merchant.getLegalName(),
                merchant.getDocumentNumber(),
                merchant.getStatus().name()
        );
    }
}
