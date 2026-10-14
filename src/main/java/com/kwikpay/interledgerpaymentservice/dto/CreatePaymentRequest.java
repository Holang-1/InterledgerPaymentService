package com.kwikpay.interledgerpaymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "Shop ID is required") String shopId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "ZAR", message = "Currency must be ZAR") String currency) {
}
