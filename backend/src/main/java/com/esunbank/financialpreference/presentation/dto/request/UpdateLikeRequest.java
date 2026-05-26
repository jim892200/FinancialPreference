package com.esunbank.financialpreference.presentation.dto.request;

import com.esunbank.financialpreference.business.command.UpdateLikeCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateLikeRequest(

        @NotBlank @Size(max = 100)
        String productName,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 16, fraction = 2)
        BigDecimal price,

        @NotNull
        @DecimalMin(value = "0.0000", inclusive = true)
        @DecimalMax(value = "1.0000", inclusive = true)
        @Digits(integer = 1, fraction = 4)
        BigDecimal feeRate,

        @Min(1)
        int purchaseQuantity,

        @NotBlank @Size(max = 20)
        String account
) {
    public UpdateLikeCommand toCommand(long sn) {
        return new UpdateLikeCommand(sn, productName, price, feeRate, purchaseQuantity, account);
    }
}
