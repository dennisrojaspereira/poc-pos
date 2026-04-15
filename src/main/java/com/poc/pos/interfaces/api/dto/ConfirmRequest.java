package com.poc.pos.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(@NotBlank String transactionId) {
}
