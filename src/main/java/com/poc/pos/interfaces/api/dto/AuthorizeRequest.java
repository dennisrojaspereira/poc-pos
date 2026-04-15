package com.poc.pos.interfaces.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AuthorizeRequest(
        @NotBlank String transactionId,
        @NotBlank String terminalId,
        @NotBlank String nsu,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
