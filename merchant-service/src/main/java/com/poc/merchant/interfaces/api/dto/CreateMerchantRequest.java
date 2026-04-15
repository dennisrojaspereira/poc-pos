package com.poc.merchant.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(
        @NotBlank String merchantId,
        @NotBlank String legalName,
        @NotBlank String documentNumber
) {
}
